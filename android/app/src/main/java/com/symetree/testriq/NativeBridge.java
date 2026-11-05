package com.symetree.testriq;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

/** window.TestriqNative — small helpers the web app uses when it runs inside the Android app. */
public class NativeBridge {
    private final MainActivity act;

    NativeBridge(MainActivity activity) {
        act = activity;
    }

    @JavascriptInterface
    public String info() {
        try {
            PackageInfo p = act.getPackageManager().getPackageInfo(act.getPackageName(), 0);
            JSONObject o = new JSONObject();
            o.put("platform", "android");
            o.put("package", act.getPackageName());
            o.put("versionName", p.versionName);
            o.put("versionCode", Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode);
            o.put("sdk", Build.VERSION.SDK_INT);
            o.put("device", Build.MANUFACTURER + " " + Build.MODEL);
            return o.toString();
        } catch (Exception e) {
            return "{\"platform\":\"android\"}";
        }
    }

    @JavascriptInterface
    public void openExternal(String url) {
        if (url == null) return;
        act.runOnUiThread(() -> act.openExternal(url));
    }

    /** Opens the Google / Facebook sign-in page in a browser tab. */
    @JavascriptInterface
    public void startAuth(String url) {
        if (url == null || !url.startsWith("https://")) return;
        act.runOnUiThread(() -> act.openAuthTab(url));
    }

    @JavascriptInterface
    public String takePendingAuth() {
        return act.takePendingAuth();
    }

    @JavascriptInterface
    public void clearPendingAuth() {
        act.clearPendingAuth();
    }

    @JavascriptInterface
    public void share(String title, String text) {
        act.runOnUiThread(() -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, title == null ? "Testriq" : title);
            send.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            act.startActivity(Intent.createChooser(send, title == null ? "Share" : title));
        });
    }

    /** kind: "tap", "success", "error", "heavy". */
    @JavascriptInterface
    public void haptic(String kind) {
        Vibrator v = (Vibrator) act.getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                int effect;
                if ("success".equals(kind)) effect = VibrationEffect.EFFECT_TICK;
                else if ("error".equals(kind)) effect = VibrationEffect.EFFECT_DOUBLE_CLICK;
                else if ("heavy".equals(kind)) effect = VibrationEffect.EFFECT_HEAVY_CLICK;
                else effect = VibrationEffect.EFFECT_CLICK;
                v.vibrate(VibrationEffect.createPredefined(effect));
            } else if (Build.VERSION.SDK_INT >= 26) {
                long ms = "error".equals(kind) ? 60 : "heavy".equals(kind) ? 40 : 15;
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate("error".equals(kind) ? 60 : 15);
            }
        } catch (Exception ignored) {
        }
    }

    @JavascriptInterface
    public void setTheme(boolean dark) {
        act.runOnUiThread(() -> act.applySystemBarAppearance(dark));
    }

    @JavascriptInterface
    public void keepAwake(boolean on) {
        act.runOnUiThread(() -> act.setKeepAwake(on));
    }

    @JavascriptInterface
    public boolean notificationsAllowed() {
        if (Build.VERSION.SDK_INT < 33) {
            android.app.NotificationManager nm = act.getSystemService(android.app.NotificationManager.class);
            return nm == null || nm.areNotificationsEnabled();
        }
        return act.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void requestNotifications() {
        act.runOnUiThread(act::requestNotifications);
    }

    /** Daily study reminder at hour:minute (device time). */
    @JavascriptInterface
    public void setReminder(boolean enabled, int hour, int minute, String title, String body) {
        Reminders.save(act, enabled, hour, minute, title, body);
        Reminders.schedule(act);
    }

    /**
     * Smart reminders shown while the app is closed. json = [{"at": epochMillis, "title": "...", "body": "..."}].
     * They are scheduled when the app goes to the background and cancelled when it is opened again.
     */
    @JavascriptInterface
    public void setNudges(String json) {
        Reminders.saveNudges(act, json);
        if (!act.resumedNow) Reminders.scheduleNudges(act);
    }

    /** Shows a sample reminder right away so people can check notifications work. */
    @JavascriptInterface
    public void testNotification(String title, String body) {
        android.content.Intent i = new android.content.Intent(act, ReminderReceiver.class)
                .setAction(Reminders.ACTION_NUDGE).putExtra("i", 5).putExtra("title", title).putExtra("body", body);
        act.sendBroadcast(i);
    }

    @JavascriptInterface
    public void rateApp() {
        act.runOnUiThread(() -> {
            String pkg = act.getPackageName();
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i);
            } catch (Exception e) {
                act.openExternal("https://play.google.com/store/apps/details?id=" + pkg);
            }
        });
    }

    @JavascriptInterface
    public void openAppSettings() {
        act.runOnUiThread(() -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + act.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            act.startActivity(i);
        });
    }

    @JavascriptInterface
    public void closeApp() {
        act.runOnUiThread(() -> act.moveTaskToBack(true));
    }
}
