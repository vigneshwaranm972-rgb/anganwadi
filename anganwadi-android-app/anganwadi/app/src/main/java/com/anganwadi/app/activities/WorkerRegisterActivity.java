package com.anganwadi.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;

public class WorkerRegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etPassword, etConfirmPassword, etCenterId, etDesignation;
    private Button btnRegister;
    private ProgressBar progressBar;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_register);

        db = DatabaseHelper.getInstance(this);

        etName            = findViewById(R.id.etName);
        etPhone           = findViewById(R.id.etPhone);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etCenterId        = findViewById(R.id.etCenterId);
        etDesignation     = findViewById(R.id.etDesignation);
        btnRegister       = findViewById(R.id.btnRegister);
        progressBar       = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String name     = etName.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();
        String centerId = etCenterId.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required"); etName.requestFocus(); return;
        }
        if (phone.isEmpty() || phone.length() < 10) {
            etPhone.setError("Enter valid 10-digit phone"); etPhone.requestFocus(); return;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters"); etPassword.requestFocus(); return;
        }
        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return;
        }
        if (centerId.isEmpty()) {
            etCenterId.setError("Center ID is required"); etCenterId.requestFocus(); return;
        }

        new RegisterWorkerTask(name, phone, password, centerId).execute();
    }

    private class RegisterWorkerTask extends AsyncTask<Void, Void, Integer> {
        // Returns: 0 = success, 1 = phone already exists, -1 = error
        final String name, phone, password, centerId;

        RegisterWorkerTask(String name, String phone, String password, String centerId) {
            this.name = name; this.phone = phone;
            this.password = password; this.centerId = centerId;
        }

        @Override
        protected void onPreExecute() {
            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            // Check if phone already registered
            if (db.isPhoneRegistered(phone)) return 1;
            // Register worker
            long id = db.registerUser(name, phone, password, "worker", centerId, 0);
            return id > 0 ? 0 : -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            btnRegister.setEnabled(true);
            btnRegister.setText("REGISTER");
            progressBar.setVisibility(View.GONE);

            if (result == 0) {
                Toast.makeText(WorkerRegisterActivity.this,
                        "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(WorkerRegisterActivity.this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else if (result == 1) {
                etPhone.setError("This phone number is already registered");
                etPhone.requestFocus();
            } else {
                Toast.makeText(WorkerRegisterActivity.this,
                        "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
