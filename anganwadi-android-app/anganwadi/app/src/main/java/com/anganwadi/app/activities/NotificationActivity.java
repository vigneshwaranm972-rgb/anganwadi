package com.anganwadi.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.utils.SMSNotificationManager;
import com.anganwadi.app.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;

    private DatabaseHelper db;
    private SessionManager session;
    private Spinner spChild, spNotificationType;
    private EditText etCustomMessage, etNutritionFood;
    private Button btnSend, btnSendAll;
    private TextView tvParentPhone, tvStatus, tvPermissionWarning, tvEmulatorWarning;
    private ProgressBar progressBar;
    private List<Child> childList = new ArrayList<>();

    private static final String[] NOTIFICATION_TYPES = {
            "Attendance Marked - Present",
            "Attendance Marked - Absent",
            "Vaccination Reminder",
            "Vaccination Given Confirmation",
            "Nutrition Provided Today",
            "Growth - Normal Status",
            "Growth - Moderate Alert",
            "Growth - SEVERE Alert (Urgent)",
            "Monthly Health Summary",
            "Custom Message"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        spChild              = findViewById(R.id.spChild);
        spNotificationType   = findViewById(R.id.spNotificationType);
        etCustomMessage      = findViewById(R.id.etCustomMessage);
        etNutritionFood      = findViewById(R.id.etNutritionFood);
        btnSend              = findViewById(R.id.btnSend);
        btnSendAll           = findViewById(R.id.btnSendAll);
        tvParentPhone        = findViewById(R.id.tvParentPhone);
        tvStatus             = findViewById(R.id.tvStatus);
        tvPermissionWarning  = findViewById(R.id.tvPermissionWarning);
        tvEmulatorWarning    = findViewById(R.id.tvEmulatorWarning);
        progressBar          = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup notification type spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, NOTIFICATION_TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNotificationType.setAdapter(typeAdapter);

        // Show/hide optional fields
        spNotificationType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                etNutritionFood.setVisibility(pos == 4 ? View.VISIBLE : View.GONE);
                etCustomMessage.setVisibility(pos == 9 ? View.VISIBLE : View.GONE);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        // Show phone when child selected
        spChild.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (childList != null && pos < childList.size()) {
                    String phone = childList.get(pos).getPhone();
                    boolean hasPhone = phone != null && !phone.trim().isEmpty();
                    tvParentPhone.setText(hasPhone
                            ? "SMS will be sent to: " + phone
                            : "No phone registered for this child's parent");
                    tvParentPhone.setTextColor(getResources().getColor(
                            hasPhone ? R.color.green_dark : R.color.red));
                }
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        btnSend.setOnClickListener(v -> {
            if (!checkSMSPermission()) return;
            sendNotification();
        });

        btnSendAll.setOnClickListener(v -> {
            if (!checkSMSPermission()) return;
            sendAbsenceAlertsToAll();
        });

        // Check if running on emulator — warn user
        if (isEmulator()) {
            tvEmulatorWarning.setVisibility(View.VISIBLE);
            tvEmulatorWarning.setText(
                    "EMULATOR DETECTED: SMS cannot be sent from emulator (no SIM card).\n" +
                            "Install the app on a REAL Android phone to send SMS to parents.");
        } else {
            tvEmulatorWarning.setVisibility(View.GONE);
        }

        // Check SMS permission
        if (!hasSMSPermission()) {
            tvPermissionWarning.setVisibility(View.VISIBLE);
            tvPermissionWarning.setText("SMS permission not granted. Tap here to allow.");
            tvPermissionWarning.setOnClickListener(v -> requestSMSPermission());
        } else {
            tvPermissionWarning.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ FIX: Reload children every time screen appears
        // This ensures newly registered children always show up
        loadChildren();
    }

    // ── Load children using session's centerId (not hardcoded) ──
    private void loadChildren() {
        // ✅ FIX: Use session centerId instead of hardcoded "AWC_001"
        String centerId = session.getCenterId();

        new AsyncTask<Void, Void, List<Child>>() {
            @Override
            protected List<Child> doInBackground(Void... v) {
                // ✅ FIX: Load ALL children across all centers for worker
                return db.getAllChildrenForWorker();
            }

            @Override
            protected void onPostExecute(List<Child> children) {
                if (isFinishing() || isDestroyed()) return;
                childList = children;

                if (children.isEmpty()) {
                    tvStatus.setText("No children found. Register children first.");
                    tvStatus.setVisibility(View.VISIBLE);
                    btnSend.setEnabled(false);
                    return;
                }

                btnSend.setEnabled(true);
                tvStatus.setVisibility(View.GONE);

                String[] names = new String[children.size()];
                for (int i = 0; i < children.size(); i++) {
                    Child c = children.get(i);
                    String phone = c.getPhone();
                    // Show phone status next to name so worker knows who has phone
                    String phoneInfo = (phone != null && !phone.isEmpty())
                            ? " \u2713" : " (no phone)";
                    names[i] = c.getName() + " (" + c.getAgeString() + ")" + phoneInfo;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        NotificationActivity.this,
                        android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spChild.setAdapter(adapter);

                // Show phone for first child
                if (!children.isEmpty()) {
                    String phone = children.get(0).getPhone();
                    boolean hasPhone = phone != null && !phone.trim().isEmpty();
                    tvParentPhone.setText(hasPhone
                            ? "SMS will be sent to: " + phone
                            : "No phone registered for this child's parent");
                }
            }
        }.execute();
    }

    private void sendNotification() {
        if (childList == null || childList.isEmpty()) {
            Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPos = spChild.getSelectedItemPosition();
        if (selectedPos < 0 || selectedPos >= childList.size()) {
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }

        Child child = childList.get(selectedPos);
        String phone = child.getPhone();

        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this,
                    "No phone number registered for " + child.getName() + "'s parent.\n" +
                            "Update the phone number in child profile.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int type = spNotificationType.getSelectedItemPosition();
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String message;

        switch (type) {
            case 0:  message = SMSNotificationManager.buildAttendanceMessage(child.getName(), today, "present"); break;
            case 1:  message = SMSNotificationManager.buildAttendanceMessage(child.getName(), today, "absent"); break;
            case 2:  message = SMSNotificationManager.buildVaccinationMessage(child.getName(), "scheduled vaccine", "as notified"); break;
            case 3:  message = SMSNotificationManager.buildVaccinationGivenMessage(child.getName(), "vaccine", today); break;
            case 4:
                String food = etNutritionFood.getText().toString().trim();
                if (food.isEmpty()) food = "Nutritious meal + supplements";
                message = SMSNotificationManager.buildNutritionMessage(child.getName(), today, food);
                break;
            case 5:  message = SMSNotificationManager.buildGrowthNormalMessage(child.getName(), 10.5f, 84f); break;
            case 6:  message = SMSNotificationManager.buildGrowthModerateAlertMessage(child.getName(), 8.2f, 78f); break;
            case 7:  message = SMSNotificationManager.buildGrowthSevereAlertMessage(child.getName(), 6.5f, 72f); break;
            case 8:  message = SMSNotificationManager.buildMonthlySummaryMessage(child.getName(), 80, 10.5f, "normal"); break;
            case 9:
                message = etCustomMessage.getText().toString().trim();
                if (message.isEmpty()) {
                    Toast.makeText(this, "Enter your custom message", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            default: message = "Anganwadi Health Update for " + child.getName(); break;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Sending SMS to " + phone + "...");
        tvStatus.setTextColor(getResources().getColor(R.color.muted));

        final String finalMessage = message;
        SMSNotificationManager.sendSMS(this, phone, finalMessage, success -> {
            progressBar.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            if (success) {
                tvStatus.setText("\u2713 SMS sent successfully to " + phone);
                tvStatus.setTextColor(getResources().getColor(R.color.green_dark));
                Toast.makeText(this, "SMS sent to " + child.getName() + "'s parent!", Toast.LENGTH_LONG).show();
                // Log to DB
                db.logNotification(child.getId(), "sms", NOTIFICATION_TYPES[type], phone);
            } else {
                tvStatus.setText("\u2717 SMS failed.\n" +
                        (isEmulator() ? "Emulator has no SIM — test on real device." :
                                "Check: 1) SEND_SMS permission granted  2) SIM has balance  3) Phone number correct"));
                tvStatus.setTextColor(getResources().getColor(R.color.red));
            }
        });
    }

    private void sendAbsenceAlertsToAll() {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Scanning all children for 3+ absent days...");
        tvStatus.setTextColor(getResources().getColor(R.color.muted));
        progressBar.setVisibility(View.VISIBLE);

        SMSNotificationManager.checkAndSendAbsenceAlerts(this, db, session.getCenterId());

        progressBar.setVisibility(View.GONE);
        tvStatus.setText("\u2713 Absence alerts sent to all parents with 3+ absent days.");
        tvStatus.setTextColor(getResources().getColor(R.color.green_dark));
    }

    // ── SMS Permission helpers ────────────────────────────────
    private boolean hasSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkSMSPermission() {
        if (!hasSMSPermission()) {
            requestSMSPermission();
            return false;
        }
        return true;
    }

    private void requestSMSPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE},
                SMS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvPermissionWarning.setVisibility(View.GONE);
                Toast.makeText(this, "SMS permission granted! You can now send messages.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "SMS permission denied. Go to Settings > Apps > Anganwadi > Permissions > Allow SMS",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── Detect emulator ──────────────────────────────────────
    private boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic"));
    }
}