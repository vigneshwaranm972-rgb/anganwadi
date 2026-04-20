package com.anganwadi.app.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Background sync started");
        SyncManager syncManager = new SyncManager(getApplicationContext());
        
        if (!syncManager.isOnline()) {
            Log.d(TAG, "Offline - sync rescheduled");
            return Result.retry();
        }

        final boolean[] finished = {false};
        final boolean[] success = {false};
        final String[] message = {""};
        final Object lock = new Object();

        syncManager.syncIfOnline(new SyncManager.SyncCallback() {
            @Override
            public void onResult(boolean isSuccess, String msg) {
                synchronized (lock) {
                    success[0] = isSuccess;
                    message[0] = msg;
                    finished[0] = true;
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                if (!finished[0]) {
                    lock.wait(60000); // Wait up to 1 minute
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Sync interrupted", e);
                return Result.failure();
            }
        }

        if (success[0]) {
            Log.d(TAG, "Background sync completed: " + message[0]);
            return Result.success();
        } else {
            Log.e(TAG, "Background sync failed: " + message[0]);
            return Result.retry();
        }
    }
}
