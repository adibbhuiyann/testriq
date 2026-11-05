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

import java.util.Calendar;

/** Stores and schedules the optional daily study reminder. */
final class Reminders {
    static final String CHANNEL = "study_reminders";
    static final String ACTION_FIRE = "com.symetree.testriq.REMINDER";
    static final String ACTION_NUDGE = "com.symetree.testriq.NUDGE";
    static final int MAX_NUDGES = 6;
    private static final String PREFS = "testriq_reminder";

    private Reminders() {
    }

    static void save(Context c, boolean enabled, int hour, int minute, String title, String body) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", enabled)
                .putInt("hour", Math.max(0, Math.min(23, hour)))
                .putInt("minute", Math.max(0, Math.min(59, minute)))
                .putString("title", title == null || title.isEmpty() ? "Time for today's practice" : title)
                .putString("body", body == null || body.isEmpty() ? "A focused 15 minutes keeps your band moving up." : body)
                .apply();
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_FIRE);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Schedules the next reminder (or cancels it when disabled). */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(c);
        am.cancel(pi);
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("enabled", false)) return;
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, p.getInt("hour", 19));
        next.set(Calendar.MINUTE, p.getInt("minute", 0));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis() + 30_000) next.add(Calendar.DAY_OF_YEAR, 1);
        // A 10-minute window needs no special alarm permission and is battery friendly.
        am.setWindow(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), 10 * 60 * 1000L, pi);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("A daily nudge to complete your study goal");
        nm.createNotificationChannel(ch);
    }

    // ------------------------------------------------------------ smart reminders

    static void saveNudges(Context c, String json) {
        prefs(c).edit().putString("nudges", json == null ? "[]" : json).apply();
    }

    private static PendingIntent nudgeIntent(Context c, int i, String title, String body, int flags) {
        Intent intent = new Intent(c, ReminderReceiver.class).setAction(ACTION_NUDGE)
                .putExtra("i", i).putExtra("title", title).putExtra("body", body);
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
                PendingIntent pi = nudgeIntent(c, n, o.optString("title"), o.optString("body"), PendingIntent.FLAG_UPDATE_CURRENT);
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 15 * 60 * 1000L, pi);
                n++;
            }
        } catch (Exception ignored) {
        }
    }

    /** Cancels pending reminders (called whenever the app is opened). */
    static void cancelNudges(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < MAX_NUDGES; i++) {
            PendingIntent pi = nudgeIntent(c, i, "", "", PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);
        }
    }
}
