package com.symetree.testriq;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/** Stores and schedules the optional daily study reminder. */
final class Reminders {
    static final String CHANNEL = "study_reminders";
    static final String ACTION_FIRE = "com.symetree.testriq.REMINDER";
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
}
