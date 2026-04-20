package com.anganwadi.app.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;

import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.models.GrowthRecord;
import com.anganwadi.app.models.Vaccination;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SMSNotificationManager
 *
 * Sends real SMS messages to parent's registered phone number.
 * Works on ANY phone — basic feature phones, Jio phones, or smartphones.
 * No internet needed. Uses device SIM card.
 *
 * Messages are in simple English + optionally Tamil/Hindi
 * (low literacy friendly — short, clear, actionable).
 *
 * REQUIRES permission in AndroidManifest:
 *   <uses-permission android:name="android.permission.SEND_SMS"/>
 *   <uses-permission android:name="android.permission.RECEIVE_SMS"/>
 */
public class SMSNotificationManager {

    private static final String TAG = "SMSNotificationManager";

    // ── SMS Message Templates ─────────────────────────────────

    // Attendance message
    public static String buildAttendanceMessage(String childName, String date, String status) {
        String emoji = status.equals("present") ? "[PRESENT]" : "[ABSENT]";
        return "Anganwadi Health Alert\n" +
               emoji + " " + childName + " was marked " + status.toUpperCase() +
               " at Anganwadi center on " + date + ".\n" +
               "Contact your Anganwadi worker for more info.\n" +
               "- Anganwadi Health System";
    }

    // Vaccination due reminder
    public static String buildVaccinationMessage(String childName, String vaccineName, String dueDate) {
        return "Anganwadi Vaccination Reminder\n" +
               "Dear Parent,\n" +
               childName + "'s " + vaccineName + " vaccination is due on " + dueDate + ".\n" +
               "Please visit your Anganwadi center on time.\n" +
               "Missing vaccines can harm your child's health.\n" +
               "- Anganwadi Health System";
    }

    // Vaccination given confirmation
    public static String buildVaccinationGivenMessage(String childName, String vaccineName, String givenDate) {
        return "Anganwadi Update\n" +
               "[DONE] " + childName + " received " + vaccineName + " vaccine on " + givenDate + ".\n" +
               "Keep this record safe.\n" +
               "Next vaccination reminder will be sent automatically.\n" +
               "- Anganwadi Health System";
    }

    // Nutrition provided message
    public static String buildNutritionMessage(String childName, String date, String foodProvided) {
        return "Anganwadi Nutrition Update\n" +
               childName + " received nutrition today (" + date + ").\n" +
               "Food provided: " + foodProvided + "\n" +
               "Please continue providing nutritious food at home too.\n" +
               "- Anganwadi Health System";
    }

    // Growth alert - normal
    public static String buildGrowthNormalMessage(String childName, float weight, float height) {
        return "Anganwadi Health Update\n" +
               "[GOOD NEWS] " + childName + "'s latest checkup:\n" +
               "Weight: " + weight + " kg | Height: " + height + " cm\n" +
               "Growth is NORMAL. Keep it up!\n" +
               "Continue regular Anganwadi visits.\n" +
               "- Anganwadi Health System";
    }

    // Growth alert - moderate malnutrition
    public static String buildGrowthModerateAlertMessage(String childName, float weight, float height) {
        return "ANGANWADI HEALTH ALERT\n" +
               "[WARNING] " + childName + "'s weight is LOW.\n" +
               "Weight: " + weight + " kg | Height: " + height + " cm\n" +
               "Status: MODERATE MALNUTRITION\n" +
               "Action needed: Visit Anganwadi center immediately.\n" +
               "Extra nutrition will be provided.\n" +
               "- Anganwadi Health System";
    }

    // Growth alert - severe malnutrition
    public static String buildGrowthSevereAlertMessage(String childName, float weight, float height) {
        return "URGENT - ANGANWADI HEALTH ALERT\n" +
               "[URGENT] " + childName + " needs IMMEDIATE medical attention.\n" +
               "Weight: " + weight + " kg | Height: " + height + " cm\n" +
               "Status: SEVERE MALNUTRITION\n" +
               "Please visit PHC or hospital TODAY.\n" +
               "Inform your Anganwadi worker immediately.\n" +
               "- Anganwadi Health System";
    }

    // Absence alert (3 days absent)
    public static String buildAbsenceAlertMessage(String childName, int absentDays) {
        return "Anganwadi Attendance Alert\n" +
               childName + " has been ABSENT for " + absentDays + " days.\n" +
               "Regular attendance is important for:\n" +
               "- Free nutritious meals\n" +
               "- Health checkups\n" +
               "- Vaccinations\n" +
               "Please send your child to the Anganwadi center.\n" +
               "- Anganwadi Health System";
    }

    // Monthly health summary
    public static String buildMonthlySummaryMessage(String childName, int attendancePercent,
                                                     float weight, String nutritionStatus) {
        return "Anganwadi Monthly Report - " + childName + "\n" +
               "Attendance: " + attendancePercent + "%\n" +
               "Weight: " + weight + " kg\n" +
               "Nutrition Status: " + nutritionStatus.toUpperCase() + "\n" +
               "Visit Anganwadi center for full details.\n" +
               "- Anganwadi Health System";
    }

    // ── Send SMS ─────────────────────────────────────────────

    /**
     * Send SMS to parent's phone number.
     * Runs on background thread — safe to call from UI.
     *
     * @param context    App context
     * @param phoneNumber Parent's registered phone (10 digits)
     * @param message    Text message to send
     * @param onResult   Callback: true = sent, false = failed
     */
    public static void sendSMS(Context context, String phoneNumber,
                                String message, SMSCallback onResult) {
        new SendSMSTask(context, phoneNumber, message, onResult).execute();
    }

    private static class SendSMSTask extends AsyncTask<Void, Void, Boolean> {
        final Context context;
        final String phone, message;
        final SMSCallback callback;

        SendSMSTask(Context ctx, String phone, String msg, SMSCallback cb) {
            this.context = ctx; this.phone = phone;
            this.message = msg; this.callback = cb;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SmsManager smsManager = SmsManager.getDefault();

                // Split long messages automatically
                if (message.length() > 160) {
                    java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(
                            phone, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(
                            phone, null, message, null, null);
                }

                // Log to database for tracking
                Log.d(TAG, "SMS sent to: " + phone);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "SMS failed: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (callback != null) callback.onResult(success);
        }
    }

    public interface SMSCallback {
        void onResult(boolean success);
    }

    // ── Bulk notification helpers ─────────────────────────────

    /**
     * Send vaccination reminder SMS to parent when vaccine is due.
     * Call this from worker when marking a vaccination as given/due.
     */
    public static void notifyVaccinationDue(Context context, DatabaseHelper db,
                                             int childId, String vaccineName, String dueDate) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                Child child = db.getChildById(childId);
                return child != null ? child.getPhone() : null;
            }
            @Override
            protected void onPostExecute(String phone) {
                if (phone == null || phone.isEmpty()) return;
                Child child = db.getChildById(childId); // safe for display
                if (child == null) return;
                String msg = buildVaccinationMessage(child.getName(), vaccineName, dueDate);
                sendSMS(context, phone, msg, success ->
                        Log.d(TAG, "Vaccination reminder SMS: " + (success ? "sent" : "failed")));
                // Also send in-app notification if smartphone
                NotificationHelper.sendVaccinationReminder(context, child.getName(), vaccineName);
            }
        }.execute();
    }

    /**
     * Send growth alert SMS to parent after a new growth record is added.
     * Call this from worker after adding growth record.
     */
    public static void notifyGrowthAlert(Context context, DatabaseHelper db,
                                          int childId, float weight, float height,
                                          String nutritionStatus) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... v) {
                Child child = db.getChildById(childId);
                if (child == null) return null;
                return new String[]{child.getPhone(), child.getName()};
            }
            @Override
            protected void onPostExecute(String[] result) {
                if (result == null || result[0].isEmpty()) return;
                String phone = result[0];
                String name  = result[1];
                String msg;
                switch (nutritionStatus.toLowerCase()) {
                    case "severe":
                        msg = buildGrowthSevereAlertMessage(name, weight, height);
                        NotificationHelper.sendGrowthAlert(context, name, "SEVERE - Urgent attention needed");
                        break;
                    case "moderate":
                        msg = buildGrowthModerateAlertMessage(name, weight, height);
                        NotificationHelper.sendGrowthAlert(context, name, "Moderate malnutrition detected");
                        break;
                    default:
                        msg = buildGrowthNormalMessage(name, weight, height);
                        break;
                }
                sendSMS(context, phone, msg, success ->
                        Log.d(TAG, "Growth alert SMS: " + (success ? "sent" : "failed")));
            }
        }.execute();
    }

    /**
     * Send attendance SMS to parent every time child attendance is marked.
     */
    public static void notifyAttendance(Context context, DatabaseHelper db,
                                         int childId, String date, String status) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... v) {
                Child child = db.getChildById(childId);
                if (child == null) return null;
                return new String[]{child.getPhone(), child.getName()};
            }
            @Override
            protected void onPostExecute(String[] result) {
                if (result == null || result[0].isEmpty()) return;
                String msg = buildAttendanceMessage(result[1], date, status);
                sendSMS(context, result[0], msg, success ->
                        Log.d(TAG, "Attendance SMS: " + (success ? "sent" : "failed")));
            }
        }.execute();
    }

    /**
     * Send nutrition provided SMS to parent.
     * Call when nutritious food/supplement is given at center.
     */
    public static void notifyNutritionProvided(Context context, DatabaseHelper db,
                                                int childId, String foodProvided) {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... v) {
                Child child = db.getChildById(childId);
                if (child == null) return null;
                return new String[]{child.getPhone(), child.getName()};
            }
            @Override
            protected void onPostExecute(String[] result) {
                if (result == null || result[0].isEmpty()) return;
                String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String msg = buildNutritionMessage(result[1], today, foodProvided);
                sendSMS(context, result[0], msg, success ->
                        Log.d(TAG, "Nutrition SMS: " + (success ? "sent" : "failed")));
            }
        }.execute();
    }

    /**
     * Check all children for absence — call daily from WorkerDashboard.
     * Sends SMS if child absent 3+ consecutive days.
     */
    public static void checkAndSendAbsenceAlerts(Context context, DatabaseHelper db, String centerId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                List<Child> children = db.getAllChildren(centerId);
                for (Child child : children) {
                    int absentDays = db.getConsecutiveAbsentDays(child.getId());
                    if (absentDays >= 3) {
                        String phone = child.getPhone();
                        if (phone != null && !phone.isEmpty()) {
                            String msg = buildAbsenceAlertMessage(child.getName(), absentDays);
                            sendSMS(context, phone, msg, null);
                            NotificationHelper.sendAttendanceReminder(context, child.getName());
                        }
                    }
                }
                return null;
            }
        }.execute();
    }
}
