package com.symetree.testriq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

/**
 * Records the candidate's spoken answers during an IELTS speaking test so the AI examiner can
 * mark pronunciation from the real voice.
 *
 * One microphone stream does both jobs: it is saved as a 16 kHz mono WAV file for each answer and,
 * on Android 13 and later, the same audio is handed to the speech recogniser through a pipe
 * (RecognizerIntent.EXTRA_AUDIO_SOURCE), so the two never compete for the microphone. Phones whose
 * recogniser can't read from a pipe are detected on the first answer and recording is switched off
 * there for good — speech recognition always comes first.
 */
final class VoiceRecorder {
    private static final String TAG = "TestriqRec";
    static final int RATE = 16000;
    private static final int CHUNK = RATE / 10 * 2;          // 100 ms of 16-bit mono audio
    private static final int MAX_BYTES = RATE * 2 * 120;      // two minutes per answer
    private static final String PREF_BROKEN = "recPipeBroken";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");

    interface Level {
        void onLevel(float db);
    }

    private final Context ctx;
    private final File dir;
    private final SharedPreferences prefs;
    private final Object pipeLock = new Object();

    private volatile boolean running;
    private Thread thread;
    private AudioRecord rec;
    private RandomAccessFile out;
    private String id;
    private long bytes;
    private volatile long voicedMs;
    private ParcelFileDescriptor pipeRead, pipeWrite;
    private Level level;

    VoiceRecorder(Context c) {
        ctx = c.getApplicationContext();
        dir = new File(ctx.getCacheDir(), "rec");
        prefs = ctx.getSharedPreferences("testriq_rec", Context.MODE_PRIVATE);
    }

    /** Recording needs Android 13+ (the recogniser reads our audio) and a phone where that works. */
    boolean supported() {
        return Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean(PREF_BROKEN, false);
    }

    void markBroken(String why) {
        Log.w(TAG, "voice recording switched off on this phone: " + why);
        prefs.edit().putBoolean(PREF_BROKEN, true).apply();
        stopAndDiscard();
    }

    boolean isRunning() {
        return running;
    }

    long voicedMs() {
        return voicedMs;
    }

    void setLevelListener(Level l) {
        level = l;
    }

    @SuppressLint("MissingPermission")
    synchronized boolean start(String name) {
        if (!supported() || name == null || !NAME.matcher(name).matches()) return false;
        stopAndDiscard();
        try {
            if (!dir.exists() && !dir.mkdirs()) return false;
            int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) return false;
            rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 2, CHUNK * 8));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                release();
                return false;
            }
            File f = new File(dir, name + ".wav");
            out = new RandomAccessFile(f, "rw");
            out.setLength(0);
            out.write(new byte[44]);
            id = name;
            bytes = 0;
            voicedMs = 0;
            rec.startRecording();
            running = true;
            thread = new Thread(this::loop, "testriq-rec");
            thread.start();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "start failed", e);
            release();
            return false;
        }
    }

    private void loop() {
        byte[] buf = new byte[CHUNK];
        long lastLevel = 0;
        while (running) {
            AudioRecord r = rec;
            if (r == null) break;
            int n = r.read(buf, 0, buf.length);
            if (n <= 0) {
                if (n == AudioRecord.ERROR_INVALID_OPERATION || n == AudioRecord.ERROR_DEAD_OBJECT) break;
                continue;
            }
            try {
                if (bytes + n <= MAX_BYTES) {
                    out.write(buf, 0, n);
                    bytes += n;
                }
            } catch (IOException ignored) {
            }
            double sum = 0;
            int samples = n / 2;
            for (int i = 0; i + 1 < n; i += 2) {
                int s = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                sum += (double) s * s;
            }
            double dbfs = 20 * Math.log10(Math.max(1, Math.sqrt(sum / Math.max(1, samples))) / 32768.0);
            if (dbfs > -40) voicedMs += samples * 1000L / RATE;
            long now = System.currentTimeMillis();
            Level l = level;
            if (l != null && now - lastLevel >= 90) {
                lastLevel = now;
                // the same scale as the recogniser's RMS values (about -2 to 10)
                l.onLevel((float) ((dbfs + 60) / 4.5 - 2));
            }
            feedPipe(buf, n);
        }
    }

    /** A fresh pipe for the next recogniser session; returns the end the recogniser reads. */
    ParcelFileDescriptor newPipe() {
        synchronized (pipeLock) {
            closePipeLocked();
            if (!running) return null;
            try {
                ParcelFileDescriptor[] p = ParcelFileDescriptor.createPipe();
                pipeRead = p[0];
                pipeWrite = p[1];
                return pipeRead;
            } catch (IOException e) {
                return null;
            }
        }
    }

    void closePipe() {
        synchronized (pipeLock) {
            closePipeLocked();
        }
    }

    private void closePipeLocked() {
        try {
            if (pipeWrite != null) pipeWrite.close();
        } catch (IOException ignored) {
        }
        try {
            if (pipeRead != null) pipeRead.close();
        } catch (IOException ignored) {
        }
        pipeWrite = null;
        pipeRead = null;
    }

    // Never blocks: if the recogniser has stopped reading, the audio is dropped for it (the WAV keeps it).
    private void feedPipe(byte[] buf, int n) {
        synchronized (pipeLock) {
            if (pipeWrite == null) return;
            FileDescriptor fd = pipeWrite.getFileDescriptor();
            try {
                StructPollfd p = new StructPollfd();
                p.fd = fd;
                p.events = (short) OsConstants.POLLOUT;
                if (Os.poll(new StructPollfd[]{p}, 0) <= 0 || (p.revents & OsConstants.POLLOUT) == 0) {
                    if ((p.revents & (OsConstants.POLLERR | OsConstants.POLLHUP)) != 0) closePipeLocked();
                    return;
                }
                Os.write(fd, buf, 0, n);
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EAGAIN && e.errno != OsConstants.EINTR) closePipeLocked();
            } catch (Exception e) {
                closePipeLocked();
            }
        }
    }

    /** Stops the answer and finishes its WAV file. Returns {"id":..,"ms":..,"voiced":..} or "{}". */
    synchronized String finish() {
        if (!running && id == null) return "{}";
        String name = id;
        long ms = bytes * 1000L / (RATE * 2L), voiced = voicedMs;
        stopRecording();
        try {
            if (out != null) {
                writeHeader(out, bytes);
                out.close();
            }
        } catch (IOException e) {
            ms = 0;
        }
        out = null;
        id = null;
        // too short or silent: nothing worth sending
        if (name == null || ms < 1500 || voiced < 800) {
            if (name != null) delete(name);
            return "{}";
        }
        return "{\"id\":\"" + name + "\",\"ms\":" + ms + ",\"voiced\":" + voiced + "}";
    }

    synchronized void stopAndDiscard() {
        String name = id;
        stopRecording();
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
        }
        out = null;
        id = null;
        if (name != null) delete(name);
    }

    private void stopRecording() {
        running = false;
        closePipe();
        Thread t = thread;
        thread = null;
        AudioRecord r = rec;
        if (r != null) {
            try {
                r.stop();
            } catch (Exception ignored) {
            }
        }
        if (t != null) {
            try {
                t.join(600);
            } catch (InterruptedException ignored) {
            }
        }
        release();
    }

    private void release() {
        AudioRecord r = rec;
        rec = null;
        if (r != null) {
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
    }

    void clearAll() {
        stopAndDiscard();
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    private void delete(String name) {
        //noinspection ResultOfMethodCallIgnored
        new File(dir, name + ".wav").delete();
    }

    /** The saved answer for /__rec/<name>.wav, or null. */
    File file(String fileName) {
        if (fileName == null || !fileName.endsWith(".wav")) return null;
        String name = fileName.substring(0, fileName.length() - 4);
        if (!NAME.matcher(name).matches()) return null;
        File f = new File(dir, fileName);
        return f.isFile() && !name.equals(id) ? f : null;
    }

    private static void writeHeader(RandomAccessFile f, long dataBytes) throws IOException {
        byte[] h = new byte[44];
        put(h, 0, "RIFF");
        le32(h, 4, 36 + dataBytes);
        put(h, 8, "WAVE");
        put(h, 12, "fmt ");
        le32(h, 16, 16);
        le16(h, 20, 1);
        le16(h, 22, 1);
        le32(h, 24, RATE);
        le32(h, 28, RATE * 2);
        le16(h, 32, 2);
        le16(h, 34, 16);
        put(h, 36, "data");
        le32(h, 40, dataBytes);
        f.seek(0);
        f.write(h);
    }

    private static void put(byte[] b, int at, String s) {
        for (int i = 0; i < s.length(); i++) b[at + i] = (byte) s.charAt(i);
    }

    private static void le32(byte[] b, int at, long v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
        b[at + 2] = (byte) (v >> 16);
        b[at + 3] = (byte) (v >> 24);
    }

    private static void le16(byte[] b, int at, int v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
    }
}
