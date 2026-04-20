package com.anganwadi.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.User;
import com.anganwadi.app.utils.SessionManager;
import com.anganwadi.app.utils.SyncManager;
import com.google.android.material.tabs.TabLayout;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private String selectedRole = "worker";
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        etPhone     = findViewById(R.id.etPhone);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tabLayout   = findViewById(R.id.tabLayout);

        tabLayout.addTab(tabLayout.newTab().setText("Worker"));
        tabLayout.addTab(tabLayout.newTab().setText("Parent"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                selectedRole = tab.getPosition() == 0 ? "worker" : "parent";
            }
            public void onTabUnselected(TabLayout.Tab tab) {}
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogin.setOnClickListener(v -> doLogin());

        btnRegister.setOnClickListener(v -> {
            Intent intent = selectedRole.equals("worker")
                    ? new Intent(this, WorkerRegisterActivity.class)
                    : new Intent(this, ParentRegisterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tvServerSettings).setOnClickListener(v -> showServerSettings());
    }

    private void showServerSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Server Settings");
        
        final EditText input = new EditText(this);
        input.setHint("http://192.168.1.5:8000/api/");
        input.setText(session.getServerUrl());
        input.setPadding(50, 20, 50, 20);
        
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                session.saveServerUrl(url);
                Toast.makeText(this, "Server URL updated!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void doLogin() {
        String phone = etPhone.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();
        if (phone.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter phone and password", Toast.LENGTH_SHORT).show();
            return;
        }
        new LoginTask(phone, pass).execute();
    }

    private class LoginTask extends AsyncTask<Void, Void, User> {
        final String phone, password;
        LoginTask(String ph, String pw) { phone = ph; password = pw; }

        @Override
        protected void onPreExecute() {
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected User doInBackground(Void... v) {
            return db.loginUser(phone, password, selectedRole);
        }

        @Override
        protected void onPostExecute(User user) {
            btnLogin.setEnabled(true);
            btnLogin.setText("LOGIN");
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (user != null) {
                // Save session
                session.saveSession(user.getId(), user.getName(), user.getRole(),
                        user.getCenterId() != null ? user.getCenterId() : "AWC_001",
                        user.getChildId());

                // ✅ Save credentials for auto re-login during sync
                session.saveCredentials(phone, password);

                // ✅ Try to get JWT token from Django in background
                SyncManager syncManager = new SyncManager(LoginActivity.this);
                syncManager.loginAndGetToken(phone, password, selectedRole,
                        (success, token) -> {
                            if (success) {
                                android.util.Log.d("Login", "JWT token obtained for sync");
                            }
                        });

                Intent intent = new Intent(LoginActivity.this,
                        user.getRole().equals("worker")
                                ? WorkerDashboardActivity.class
                                : ParentDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        "Invalid credentials. Try again.", Toast.LENGTH_LONG).show();
                etPassword.setText("");
                etPassword.requestFocus();
            }
        }
    }
}
