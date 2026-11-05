package com.symetree.testriq;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Backs up the learner's account, progress and reminder settings with Android's backup service
 * (Google account backup). When Testriq is deleted and installed again on the same Google account,
 * or moved to a new phone, Android restores this data before the app first opens, and the page
 * picks it up so the learner finds their account and progress again.
 */
public class TestriqBackupAgent extends BackupAgentHelper {
    @Override
    public void onCreate() {
        addHelper("progress", new SharedPreferencesBackupHelper(this, Backup.PREFS));
        addHelper("reminders", new SharedPreferencesBackupHelper(this, "testriq_reminder"));
    }
}
