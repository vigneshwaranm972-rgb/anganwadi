package com.anganwadi.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID   = "anganwadi_channel";
    private static final String CHANNEL_NAME = "Health Reminders";
    private static int notifId = 1000;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Vaccination and health reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void sendVaccinationReminder(Context context, String childName, String vaccineName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Vaccination Due — " + childName)
                .setContentText(vaccineName + " is due. Visit your Anganwadi center.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Handle missing POST_NOTIFICATIONS permission on Android 13+
        }
    }

    public static void sendAttendanceReminder(Context context, String childName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Attendance Reminder")
                .setContentText(childName + " hasn't attended in 3+ days. Please visit the center.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Handle missing POST_NOTIFICATIONS permission on Android 13+
        }
    }

    public static void sendGrowthAlert(Context context, String childName, String status) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Growth Alert — " + childName)
                .setContentText("Nutrition status: " + status + ". Please consult the Anganwadi worker.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(context).notify(notifId++, builder.build());
        } catch (SecurityException e) {
            // Handle missing POST_NOTIFICATIONS permission on Android 13+
        }
    }
}
