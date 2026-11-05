package com.symetree.testriq;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/** Shows study reminders and restores them after a reboot or app update. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Reminders.ACTION_NUDGE.equals(action)) {
            long at = intent.getLongExtra("at", 0);
            if (!Reminders.wasDelivered(context, at)) {
                post(context, 2000 + intent.getIntExtra("i", 0), intent.getStringExtra("title"), intent.getStringExtra("body"), at);
            }
            return;
        }
        if (Reminders.ACTION_FIRE.equals(action)) showDaily(context);
        Reminders.schedule(context);
        NudgeJobService.schedule(context);
        if (!Reminders.ACTION_FIRE.equals(action)) Reminders.scheduleNudges(context); // after reboot / update
    }

    /** The daily reminder; skipped when the learner has already practised today. */
    static void showDaily(Context c) {
        SharedPreferences p = Reminders.prefs(c);
        if (!p.getBoolean("enabled", false)) return;
        String today = Reminders.today();
        if (today.equals(p.getString("dailyFired", ""))) return;
        p.edit().putString("dailyFired", today).apply();
        if (Reminders.practisedToday(c)) return;
        post(c, 1001, p.getString("title", "Time for today's practice"),
                p.getString("body", "A focused 15 minutes keeps your band moving up."), 0);
    }

    @SuppressWarnings("deprecation")
    static void post(Context c, int id, String title, String body, long at) {
        if (title == null || title.isEmpty()) return;
        if (body == null) body = "";
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        Reminders.ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 8, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, Reminders.CHANNEL)
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_testriq)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFF3968F5)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(pi);
        nm.notify(id, b.build());
        Reminders.markPosted(c, at);
    }
}
