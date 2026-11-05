package com.symetree.testriq;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** A copy of the web app's saved data (account, progress, settings) kept where Android backs it up. */
final class Backup {
    static final String PREFS = "testriq_backup";
    private static final int MAX_CHARS = 4_500_000;

    private Backup() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Saves the snapshot and asks Android to include it in the next backup pass. */
    static boolean save(Context c, String json) {
        if (json == null || json.isEmpty() || json.length() > MAX_CHARS) return false;
        String old = prefs(c).getString("data", "");
        if (json.equals(old)) return true;
        prefs(c).edit().putString("data", json).putLong("saved", System.currentTimeMillis()).apply();
        try {
            new BackupManager(c).dataChanged();
        } catch (Exception ignored) {
        }
        return true;
    }

    static String load(Context c) {
        return prefs(c).getString("data", "");
    }

    static String info(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("saved", prefs(c).getLong("saved", 0));
            o.put("size", load(c).length());
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    static void clear(Context c) {
        prefs(c).edit().clear().apply();
        try {
            new BackupManager(c).dataChanged();
        } catch (Exception ignored) {
        }
    }
}
