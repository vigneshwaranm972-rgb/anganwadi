package com.anganwadi.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * SMSPermissionHelper
 * Handles runtime SMS permission request for Android 6.0+
 *
 * HOW TO USE in WorkerDashboardActivity:
 *
 *   // In onCreate():
 *   SMSPermissionHelper.requestIfNeeded(this);
 *
 *   // Override onRequestPermissionsResult():
 *   @Override
 *   public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
 *       super.onRequestPermissionsResult(req, perms, results);
 *       if (req == SMSPermissionHelper.REQUEST_CODE) {
 *           if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
 *               Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
 *           } else {
 *               Toast.makeText(this, "SMS permission denied. Parents will not receive SMS alerts.", Toast.LENGTH_LONG).show();
 *           }
 *       }
 *   }
 */
public class SMSPermissionHelper {

    public static final int REQUEST_CODE = 101;

    public static boolean hasPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestIfNeeded(Activity activity) {
        if (!hasPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    REQUEST_CODE);
        }
    }
}
