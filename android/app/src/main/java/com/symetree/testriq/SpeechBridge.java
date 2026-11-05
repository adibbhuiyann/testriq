package com.symetree.testriq;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * window.TestriqSpeech — the examiner's voice (text-to-speech) and the candidate's
 * microphone (speech recognition) for the web app.
 *
 * Voice: "sajid" = Sajid Shahriar (male), "jishad" = Jishad Mozmder (female).
 * Events go back to the page through window.testriqSpeechEvent(id, type, start, end)
 * and window.testriqListenEvent(type, text, confidence).
 */
public class SpeechBridge implements TextToSpeech.OnInitListener {
    private static final String TAG = "TestriqSpeech";
    private static final String GOOGLE_TTS = "com.google.android.tts";

    // Google text-to-speech voice codes, by gender.
    private static final String[] MALE_CODES = {"gbb", "gbd", "rjs", "iol", "iom", "tpd", "aub", "aud", "end"};
    private static final String[] FEMALE_CODES = {"gba", "gbc", "gbg", "iob", "iog", "sfg", "tpc", "tpf", "aua", "auc", "ahp", "cxx", "ene", "enc"};

    private final MainActivity act;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private volatile boolean ttsReady;
    private volatile boolean ttsFailed;

    private Job current;
    private Job pending;
    private float userRate = 0.95f;
    private String accent = "en-GB";
    private Voice maleVoice, femaleVoice;
    private boolean maleExact, femaleExact;

    private SpeechRecognizer recognizer;
    private String recognitionLang = "en-GB";
    private boolean listening;
    private boolean quietBeeps = true;
    private boolean beepsMuted;
    private long lastRms;

    private static final class Job {
        final String text, id, examinerId;
        final float volume;
        int[] offsets = new int[0];
        int chunks;
        boolean started;

        Job(String text, String id, float volume, String examinerId) {
            this.text = text;
            this.id = id;
            this.volume = volume;
            this.examinerId = examinerId;
        }
    }

    SpeechBridge(MainActivity activity) {
        act = activity;
        initTts();
    }

    private void initTts() {
        ttsReady = false;
        ttsFailed = false;
        try {
            tts = new TextToSpeech(act.getApplicationContext(), this, GOOGLE_TTS);
        } catch (Exception e) {
            Log.w(TAG, "TTS init failed", e);
            ttsFailed = true;
        }
    }

    @Override
    public void onInit(int status) {
        main.post(() -> {
            if (status != TextToSpeech.SUCCESS || tts == null) {
                ttsFailed = true;
                if (pending != null) {
                    emit(pending.id, "error", 0, 0);
                    pending = null;
                }
                return;
            }
            ttsReady = true;
            tts.setOnUtteranceProgressListener(progress);
            chooseVoices();
            if (pending != null) {
                Job j = pending;
                pending = null;
                startJob(j);
            }
        });
    }

    // ------------------------------------------------------------ voices

    private static int genderOf(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("female") || n.contains("smtf") || n.contains("woman")) return -1;
        if (n.contains("male") || n.contains("smtm")) return 1;
        for (String c : MALE_CODES) if (n.contains("-x-" + c)) return 1;
        for (String c : FEMALE_CODES) if (n.contains("-x-" + c)) return -1;
        return 0;
    }

    private int localeScore(Locale l) {
        String c = l.getCountry();
        String want = accent.length() >= 5 ? accent.substring(3).toUpperCase(Locale.ROOT) : "GB";
        if (want.equals(c)) return 40;
        if ("GB".equals(c)) return 25;
        if ("US".equals(c)) return 20;
        if ("AU".equals(c)) return 15;
        if ("IN".equals(c)) return 8;
        return 0;
    }

    private void chooseVoices() {
        maleVoice = null;
        femaleVoice = null;
        maleExact = false;
        femaleExact = false;
        Set<Voice> voices;
        try {
            voices = tts.getVoices();
        } catch (Exception e) {
            voices = null;
        }
        if (voices == null) return;
        int bestMale = Integer.MIN_VALUE, bestFemale = Integer.MIN_VALUE;
        for (Voice v : voices) {
            Locale l = v.getLocale();
            if (l == null || !"en".equals(l.getLanguage())) continue;
            Set<String> f = v.getFeatures();
            if (f != null && f.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
            int g = genderOf(v.getName());
            int base = localeScore(l) + v.getQuality() / 100 + (v.isNetworkConnectionRequired() ? 0 : 6);
            int m = base + (g > 0 ? 100 : g < 0 ? -100 : 0);
            int fe = base + (g < 0 ? 100 : g > 0 ? -100 : 0);
            if (m > bestMale) {
                bestMale = m;
                maleVoice = v;
                maleExact = g > 0;
            }
            if (fe > bestFemale) {
                bestFemale = fe;
                femaleVoice = v;
                femaleExact = g < 0;
            }
        }
        Log.i(TAG, "Voices: male=" + (maleVoice == null ? "-" : maleVoice.getName())
                + " female=" + (femaleVoice == null ? "-" : femaleVoice.getName()));
    }

    private void applyVoice(String examinerId) {
        boolean female = "jishad".equals(examinerId);
        Voice v = female ? femaleVoice : maleVoice;
        boolean exact = female ? femaleExact : maleExact;
        try {
            if (v != null) tts.setVoice(v);
            else tts.setLanguage(Locale.forLanguageTag(accent));
        } catch (Exception e) {
            tts.setLanguage(Locale.UK);
        }
        // When the device has no voice of the right gender, shape the pitch instead.
        float pitch = exact ? (female ? 1.04f : 0.96f) : (female ? 1.18f : 0.80f);
        tts.setPitch(pitch);
        tts.setSpeechRate(userRate);
    }

    // ------------------------------------------------------------ speaking

    @JavascriptInterface
    public void speak(String text, String id, double volume, String examinerId) {
        final Job job = new Job(text == null ? "" : text, id == null ? "0" : id,
                (float) Math.max(0, Math.min(1, volume)), examinerId == null ? "sajid" : examinerId);
        main.post(() -> {
            if (ttsFailed) {
                emit(job.id, "error", 0, 0);
                return;
            }
            if (!ttsReady) {
                pending = job;
                return;
            }
            startJob(job);
        });
    }

    @JavascriptInterface
    public void speak(String text, String id, double volume) {
        speak(text, id, volume, "sajid");
    }

    @JavascriptInterface
    public void speak(String text, String id) {
        speak(text, id, 1.0, "sajid");
    }

    private List<String> chunk(String text, Job job) {
        int max = 3500;
        try {
            max = Math.min(max, TextToSpeech.getMaxSpeechInputLength() - 50);
        } catch (Exception ignored) {
        }
        List<String> parts = new ArrayList<>();
        List<Integer> offs = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + max);
            if (end < text.length()) {
                int cut = Math.max(text.lastIndexOf(". ", end), Math.max(text.lastIndexOf("? ", end), text.lastIndexOf("! ", end)));
                if (cut > start + 200) end = cut + 1;
                else {
                    int sp = text.lastIndexOf(' ', end);
                    if (sp > start + 50) end = sp;
                }
            }
            parts.add(text.substring(start, end));
            offs.add(start);
            start = end;
        }
        if (parts.isEmpty()) {
            parts.add(" ");
            offs.add(0);
        }
        job.offsets = new int[offs.size()];
        for (int i = 0; i < offs.size(); i++) job.offsets[i] = offs.get(i);
        job.chunks = parts.size();
        return parts;
    }

    private void startJob(Job job) {
        current = job;
        tts.stop();
        applyVoice(job.examinerId);
        List<String> parts = chunk(job.text, job);
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, job.volume);
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        for (int i = 0; i < parts.size(); i++) {
            int r = tts.speak(parts.get(i), i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                    params, job.id + "#" + i);
            if (r != TextToSpeech.SUCCESS) {
                current = null;
                emit(job.id, "error", 0, 0);
                return;
            }
        }
    }

    private Job jobFor(String utteranceId, int[] index) {
        if (utteranceId == null) return null;
        int hash = utteranceId.lastIndexOf('#');
        if (hash < 0) return null;
        String id = utteranceId.substring(0, hash);
        try {
            index[0] = Integer.parseInt(utteranceId.substring(hash + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        Job j = current;
        return j != null && j.id.equals(id) ? j : null;
    }

    private final UtteranceProgressListener progress = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            int[] i = new int[1];
            Job j = jobFor(utteranceId, i);
            if (j != null && !j.started) {
                j.started = true;
                emit(j.id, "start", 0, 0);
            }
        }

        @Override
        public void onRangeStart(String utteranceId, int start, int end, int frame) {
            int[] i = new int[1];
            Job j = jobFor(utteranceId, i);
            if (j == null || i[0] >= j.offsets.length) return;
            int off = j.offsets[i[0]];
            emit(j.id, "word", start + off, end + off);
        }

        @Override
        public void onDone(String utteranceId) {
            int[] i = new int[1];
            Job j = jobFor(utteranceId, i);
            if (j != null && i[0] >= j.chunks - 1) {
                if (current == j) current = null;
                emit(j.id, "end", 0, 0);
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onError(String utteranceId) {
            fail(utteranceId);
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            fail(utteranceId);
        }

        private void fail(String utteranceId) {
            int[] i = new int[1];
            Job j = jobFor(utteranceId, i);
            if (j != null) {
                if (current == j) current = null;
                emit(j.id, "error", 0, 0);
            }
        }
    };

    private void emit(String id, String type, int start, int end) {
        act.js("window.testriqSpeechEvent&&window.testriqSpeechEvent(" + JSONObject.quote(id) + ","
                + JSONObject.quote(type) + "," + start + "," + end + ")");
    }

    @JavascriptInterface
    public void stop() {
        main.post(() -> {
            current = null;
            pending = null;
            if (tts != null && ttsReady) tts.stop();
        });
    }

    @JavascriptInterface
    public boolean isSpeaking() {
        return tts != null && ttsReady && tts.isSpeaking();
    }

    /** rate: 0.6 – 1.4 (1 = normal). */
    @JavascriptInterface
    public void setRate(double rate) {
        userRate = (float) Math.max(0.6, Math.min(1.4, rate));
    }

    /** "en-GB", "en-US", "en-AU" or "en-IN": examiner accent preference. */
    @JavascriptInterface
    public void setAccent(String tag) {
        if (tag == null || !tag.startsWith("en")) return;
        main.post(() -> {
            accent = tag;
            if (ttsReady) chooseVoices();
        });
    }

    /** Language used to recognise the candidate's speech, e.g. "en-GB", "en-US", "en-IN". */
    @JavascriptInterface
    public void setRecognitionLanguage(String tag) {
        if (tag != null && tag.startsWith("en")) recognitionLang = tag;
    }

    @JavascriptInterface
    public void setQuietBeeps(boolean quiet) {
        quietBeeps = quiet;
    }

    @JavascriptInterface
    public String voiceInfo() {
        try {
            JSONObject o = new JSONObject();
            o.put("ready", ttsReady);
            o.put("failed", ttsFailed);
            o.put("engine", tts != null && ttsReady ? tts.getDefaultEngine() : "");
            o.put("male", maleVoice == null ? "" : maleVoice.getName());
            o.put("maleExact", maleExact);
            o.put("female", femaleVoice == null ? "" : femaleVoice.getName());
            o.put("femaleExact", femaleExact);
            o.put("recognition", SpeechRecognizer.isRecognitionAvailable(act));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public void openVoiceSettings() {
        act.runOnUiThread(() -> {
            try {
                Intent i = new Intent("com.android.settings.TTS_SETTINGS");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i);
            } catch (Exception e) {
                try {
                    Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(i);
                } catch (Exception ignored) {
                }
            }
        });
    }

    // ------------------------------------------------------------ listening

    @JavascriptInterface
    public boolean hasMic() {
        return act.hasMicPermission();
    }

    @JavascriptInterface
    public void requestPermissions() {
        act.runOnUiThread(act::requestMic);
    }

    @JavascriptInterface
    public boolean recognitionAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(act);
    }

    @JavascriptInterface
    public void listen() {
        main.post(this::startListening);
    }

    @JavascriptInterface
    public void stopListening() {
        main.post(this::stopListeningNow);
    }

    private void startListening() {
        if (!act.hasMicPermission()) {
            listenEvent("error", "9", -1);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(act)) {
            listenEvent("error", "unavailable", -1);
            return;
        }
        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(act);
                recognizer.setRecognitionListener(listener);
            } else {
                recognizer.cancel();
            }
        } catch (Exception e) {
            listenEvent("error", "unavailable", -1);
            return;
        }
        muteBeeps(quietBeeps);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLang);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionLang);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, act.getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 8000L);
        listening = true;
        try {
            recognizer.startListening(i);
        } catch (Exception e) {
            listening = false;
            listenEvent("error", "5", -1);
        }
    }

    private void stopListeningNow() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (Exception ignored) {
            }
        }
        muteBeeps(false);
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            listenEvent("ready", "", -1);
        }

        @Override
        public void onBeginningOfSpeech() {
            listenEvent("speech", "", -1);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            long now = SystemClock.uptimeMillis();
            if (now - lastRms < 90) return;
            lastRms = now;
            listenEvent("rms", String.valueOf(rmsdB), -1);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            if (!listening) return;
            listenEvent("error", String.valueOf(error), -1);
        }

        @Override
        public void onResults(Bundle results) {
            if (!listening) return;
            ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] conf = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            String t = texts != null && !texts.isEmpty() ? texts.get(0) : "";
            float c = conf != null && conf.length > 0 ? conf[0] : -1f;
            if (!t.isEmpty()) listenEvent("final", t, c);
            listenEvent("end", "", -1);
        }

        @Override
        public void onPartialResults(Bundle partial) {
            if (!listening) return;
            ArrayList<String> texts = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (texts != null && !texts.isEmpty() && !texts.get(0).isEmpty()) listenEvent("partial", texts.get(0), -1);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    private void listenEvent(String type, String text, float conf) {
        String c = conf >= 0 ? String.format(Locale.US, "%.3f", conf) : "null";
        act.js("window.testriqListenEvent&&window.testriqListenEvent(" + JSONObject.quote(type) + ","
                + JSONObject.quote(text) + "," + c + ")");
    }

    /** Silences the recogniser's start/stop chime while a speaking answer is being captured. */
    private void muteBeeps(boolean mute) {
        if (mute == beepsMuted) return;
        AudioManager am = (AudioManager) act.getSystemService(android.content.Context.AUDIO_SERVICE);
        if (am == null) return;
        int[] streams = {AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_MUSIC};
        for (int s : streams) {
            try {
                am.adjustStreamVolume(s, mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
            } catch (Exception ignored) {
            }
        }
        beepsMuted = mute;
    }

    // ------------------------------------------------------------ lifecycle

    void pauseAll() {
        main.post(() -> {
            current = null;
            pending = null;
            if (tts != null && ttsReady) tts.stop();
            stopListeningNow();
        });
    }

    void shutdown() {
        muteBeeps(false);
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (Exception ignored) {
            }
            recognizer = null;
        }
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Exception ignored) {
            }
            tts = null;
        }
    }
}
