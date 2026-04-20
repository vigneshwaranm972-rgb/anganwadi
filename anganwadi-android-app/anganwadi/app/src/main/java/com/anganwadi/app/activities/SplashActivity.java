package com.anganwadi.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent intent;
            if (session.isLoggedIn()) {
                String role = session.getUserRole();
                intent = new Intent(this, role.equals("worker")
                        ? WorkerDashboardActivity.class
                        : ParentDashboardActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1500);
    }
}
