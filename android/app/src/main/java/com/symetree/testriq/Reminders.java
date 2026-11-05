package com.symetree.testriq;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Study reminders:
 * - a daily reminder at the learner's chosen time,
 * - smart reminders planned by the page (unfinished goal, streak at risk, unfinished test…),
 * - "come back" reminders after days away, sent by {@link NudgeJobService}.
 * Alarms use setAndAllowWhileIdle so they also fire while the phone is in Doze.
 */
final class Reminders {
    static final String CHANNEL = "study_reminders";
    static final String ACTION_FIRE = "com.symetree.testriq.REMINDER";
    static final String ACTION_NUDGE = "com.symetree.testriq.NUDGE";
    static final int MAX_NUDGES = 6;
    private static final String PREFS = "testriq_reminder";
    private static final long HOUR = 60 * 60 * 1000L;
    private static final long DAY = 24 * HOUR;
    /** Days away after which a "come back" reminder is sent (one each). */
    private static final int[] AWAY_DAYS = {1, 2, 3, 5, 7, 10, 14, 21, 30};

    private Reminders() {
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static void save(Context c, boolean enabled, int hour, int minute, String title, String body) {
        prefs(c).edit()
                .putBoolean("enabled", enabled)
                .putInt("hour", Math.max(0, Math.min(23, hour)))
                .putInt("minute", Math.max(0, Math.min(59, minute)))
                .putString("title", title == null || title.isEmpty() ? "Time for today's practice" : title)
                .putString("body", body == null || body.isEmpty() ? "A focused 15 minutes keeps your band moving up." : body)
                .apply();
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_FIRE);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(AlarmManager am, long at, PendingIntent pi) {
        try {
            // Inexact but delivered even in Doze; needs no special alarm permission.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static long nextDailyAt(SharedPreferences p) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, p.getInt("hour", 19));
        next.set(Calendar.MINUTE, p.getInt("minute", 0));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis() + 30_000) next.add(Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis();
    }

    /** Schedules the next daily reminder (or cancels it when disabled). */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(c);
        am.cancel(pi);
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("enabled", false)) return;
        setAlarm(am, nextDailyAt(p), pi);
        NudgeJobService.schedule(c);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Your daily study reminder and gentle nudges to keep your preparation on track");
        nm.createNotificationChannel(ch);
    }

    // ------------------------------------------------------------ smart reminders planned by the page

    static void saveNudges(Context c, String json) {
        prefs(c).edit().putString("nudges", json == null ? "[]" : json).apply();
    }

    private static PendingIntent nudgeIntent(Context c, int i, String title, String body, long at, int flags) {
        Intent intent = new Intent(c, ReminderReceiver.class).setAction(ACTION_NUDGE)
                .putExtra("i", i).putExtra("title", title).putExtra("body", body).putExtra("at", at);
        return PendingIntent.getBroadcast(c, 100 + i, intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Schedules the saved reminders (called when the app goes to the background and after a reboot). */
    static void scheduleNudges(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        cancelNudges(c);
        try {
            JSONArray list = new JSONArray(prefs(c).getString("nudges", "[]"));
            long now = System.currentTimeMillis();
            int n = 0;
            for (int i = 0; i < list.length() && n < MAX_NUDGES; i++) {
                JSONObject o = list.getJSONObject(i);
                long at = o.optLong("at", 0);
                if (at < now + 60_000) continue;
                setAlarm(am, at, nudgeIntent(c, n, o.optString("title"), o.optString("body"), at, PendingIntent.FLAG_UPDATE_CURRENT));
                n++;
            }
        } catch (Exception ignored) {
        }
        NudgeJobService.schedule(c);
    }

    /** Cancels pending reminders (called whenever the app is opened). */
    static void cancelNudges(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < MAX_NUDGES; i++) am.cancel(nudgeIntent(c, i, "", "", 0, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    // ------------------------------------------------------------ bookkeeping

    /** The app was opened: absence reminders start counting again from now. */
    static void markOpened(Context c) {
        prefs(c).edit().putLong("lastOpen", System.currentTimeMillis()).putInt("awayStep", 0).apply();
    }

    static boolean wasDelivered(Context c, long at) {
        return at > 0 && prefs(c).getStringSet("delivered", new HashSet<>()).contains(String.valueOf(at));
    }

    /** Records a shown notification so the alarm and the background check never show it twice. */
    static void markPosted(Context c, long at) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor e = p.edit().putLong("lastPosted", System.currentTimeMillis());
        if (at > 0) {
            Set<String> old = p.getStringSet("delivered", new HashSet<>());
            Set<String> keep = new HashSet<>();
            long cutoff = System.currentTimeMillis() - 7 * DAY;
            for (String s : old) {
                try {
                    if (Long.parseLong(s) > cutoff) keep.add(s);
                } catch (NumberFormatException ignored) {
                }
            }
            keep.add(String.valueOf(at));
            e.putStringSet("delivered", keep);
        }
        e.apply();
    }

    static boolean practisedToday(Context c) {
        return today().equals(prefs(c).getString("lastPractice", ""));
    }

    // ------------------------------------------------------------ background catch-up

    /** Runs every few hours: delivers lost reminders and sends "come back" reminders after days away. */
    static void catchUp(Context c) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean daytime = hour >= 9 && hour < 21;
        long lastOpen = p.getLong("lastOpen", 0);
        boolean appOpen = p.getBoolean("foreground", false) && now - p.getLong("foregroundAt", 0) < 6 * HOUR;
        if (appOpen) return;

        // 1) a planned reminder whose alarm never fired
        try {
            JSONArray list = new JSONArray(p.getString("nudges", "[]"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject o = list.getJSONObject(i);
                long at = o.optLong("at", 0);
                if (at <= now && at > now - 6 * HOUR && at > lastOpen && !wasDelivered(c, at) && daytime) {
                    ReminderReceiver.post(c, 2000 + i, o.optString("title"), o.optString("body"), at);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        // 2) the daily reminder, if its alarm was lost today
        if (p.getBoolean("enabled", false) && !practisedToday(c) && !today().equals(p.getString("dailyFired", ""))) {
            Calendar due = Calendar.getInstance();
            due.set(Calendar.HOUR_OF_DAY, p.getInt("hour", 19));
            due.set(Calendar.MINUTE, p.getInt("minute", 0));
            long dueAt = due.getTimeInMillis();
            if (now >= dueAt && now - dueAt < 3 * HOUR && hour < 22) {
                ReminderReceiver.showDaily(c);
                schedule(c);
                return;
            }
        }

        // 3) "come back" reminders after days away (one per milestone, never twice in a day)
        if (lastOpen <= 0 || !daytime) return;
        if (now - p.getLong("lastPosted", 0) < 20 * HOUR) return;
        int daysAway = (int) ((now - lastOpen) / DAY);
        int step = p.getInt("awayStep", 0);
        if (step >= AWAY_DAYS.length || daysAway < AWAY_DAYS[step]) return;
        while (step + 1 < AWAY_DAYS.length && daysAway >= AWAY_DAYS[step + 1]) step++;
        String[] msg = awayMessage(AWAY_DAYS[step], p.getString("name", ""), p.getString("target", ""));
        p.edit().putInt("awayStep", step + 1).apply();
        ReminderReceiver.post(c, 3000 + step, msg[0], msg[1], 0);
    }

    private static String[] awayMessage(int days, String name, String target) {
        String who = name == null || name.isEmpty() ? "" : name + ", ";
        String goal = target == null || target.isEmpty() ? "your IELTS goal" : "band " + target;
        switch (days) {
            case 1:
                return new String[]{who + "your practice is waiting", "Ten focused minutes today keeps your band moving up."};
            case 2:
                return new String[]{"Don't lose your momentum", "Pick up where you left off — your examiner is ready when you are."};
            case 3:
                return new String[]{"Three days without practice", "A quick speaking test today shows you exactly where you stand."};
            case 5:
                return new String[]{who + goal + " is still within reach", "Small daily steps add up. Try one short practice set today."};
            case 7:
                return new String[]{"It's been a week", "Come back for a five-minute warm-up. Your progress is saved and waiting."};
            case 10:
                return new String[]{"Ready for a fresh start?", "Take one speaking test today and get a new live band score."};
            case 14:
                return new String[]{who + "we miss you at Testriq", "Two weeks away — a single practice session gets you back on track."};
            case 21:
                return new String[]{"Your exam won't wait", "Restart with today's goal: about fifteen minutes is all it takes."};
            default:
                return new String[]{"Still preparing for IELTS?", "Your account and progress are safe. Open Testriq whenever you're ready."};
        }
    }

    /** For Settings: whether reminders can be shown and when the next daily one is due. */
    static String status(Context c, boolean allowed) {
        try {
            SharedPreferences p = prefs(c);
            JSONObject o = new JSONObject();
            o.put("allowed", allowed);
            o.put("daily", p.getBoolean("enabled", false));
            o.put("nextDaily", p.getBoolean("enabled", false) ? nextDailyAt(p) : 0);
            o.put("lastPosted", p.getLong("lastPosted", 0));
            android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
            o.put("batteryOptimised", pm != null && !pm.isIgnoringBatteryOptimizations(c.getPackageName()));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
