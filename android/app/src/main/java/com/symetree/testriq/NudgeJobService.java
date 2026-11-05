package com.symetree.testriq;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

/**
 * A periodic background check (every few hours, survives reboots) that makes sure reminders
 * really arrive: it delivers any reminder whose alarm was lost (some phones kill alarms when an
 * app is swiped away), catches up a missed daily reminder, and sends a friendly "come back"
 * notification after days without practice.
 */
public class NudgeJobService extends JobService {
    static final int JOB_ID = 4711;
    private static final long PERIOD_MS = 4 * 60 * 60 * 1000L;

    static void schedule(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            for (JobInfo j : js.getAllPendingJobs()) {
                if (j.getId() == JOB_ID) return; // already scheduled
            }
            JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, NudgeJobService.class))
                    .setPeriodic(PERIOD_MS, 60 * 60 * 1000L)
                    .setPersisted(true)
                    .build();
            js.schedule(info);
        } catch (Exception e) {
            Log.w(MainActivity.TAG, "Reminder check not scheduled", e);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            Reminders.catchUp(getApplicationContext());
        } catch (Exception e) {
            Log.w(MainActivity.TAG, "Reminder check failed", e);
        }
        return false; // all work is done synchronously
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
