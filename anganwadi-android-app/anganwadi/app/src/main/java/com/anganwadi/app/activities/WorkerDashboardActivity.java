package com.anganwadi.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.adapters.ChildAdapter;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.utils.SessionManager;
import com.anganwadi.app.utils.SMSNotificationManager;
import com.anganwadi.app.utils.SyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkerDashboardActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private RecyclerView rvChildren;
    private ChildAdapter adapter;
    private EditText etSearch;
    private TextView tvTotal, tvPresent, tvMalnourished, tvVaccPending;
    private TextView tvWelcome, tvDate, tvSyncStatus, tvNoResult;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;
    private String centerId, today;

    // Full list kept for filtering
    private List<Child> fullChildList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        tvWelcome      = findViewById(R.id.tvWelcome);
        tvDate         = findViewById(R.id.tvDate);
        tvTotal        = findViewById(R.id.tvTotal);
        tvPresent      = findViewById(R.id.tvPresent);
        tvMalnourished = findViewById(R.id.tvMalnourished);
        tvVaccPending  = findViewById(R.id.tvVaccPending);
        tvSyncStatus   = findViewById(R.id.tvSyncStatus);
        tvNoResult     = findViewById(R.id.tvNoResult);
        rvChildren     = findViewById(R.id.rvChildren);
        progressBar    = findViewById(R.id.progressBar);
        bottomNav      = findViewById(R.id.bottomNav);

        findViewById(R.id.btnSync).setOnClickListener(v -> manualSync());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        Button btnTestSync = findViewById(R.id.btnTestSync);
        btnTestSync.setOnClickListener(v -> {
            SyncManager sm = new SyncManager(this);
            sm.testConnection((success, message) ->
                    runOnUiThread(() ->
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
        });

        // ── FEATURE 1: Search bar ────────────────────────────
        etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChildren(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        centerId = session.getCenterId();
        today    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String displayDate = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(new Date());

        tvWelcome.setText("Hello, " + session.getUserName() + " \uD83D\uDC4B");
        tvDate.setText(displayDate);

        // Setup RecyclerView
        adapter = new ChildAdapter(new ArrayList<>(), child -> {
            Intent intent = new Intent(this, ChildDetailActivity.class);
            intent.putExtra("child_id", child.getId());
            startActivity(intent);
        });
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        rvChildren.setAdapter(adapter);

        // FAB
        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddChildActivity.class)));

        // Bottom nav
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceActivity.class));
            } else if (id == R.id.nav_notify) {
                startActivity(new Intent(this, NotificationActivity.class));
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
            }
             else if (id == R.id.nav_nutrition) {
                startActivity(new Intent(this, NutritionActivity.class));
            }
            return true;
        });

        // Auto absence check
        SMSNotificationManager.checkAndSendAbsenceAlerts(this, db, centerId);

        // Schedule background sync
        new SyncManager(this).scheduleBackgroundSync();

        new LoadDashboardTask().execute();
    }

    private void manualSync() {
        Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        new SyncManager(this).syncIfOnline((success, message) -> {
            runOnUiThread(() -> {
                tvSyncStatus.setText(success ? "✓ " + message : "⚠ " + message);
                tvSyncStatus.setTextColor(getResources().getColor(
                        success ? R.color.green_dark : R.color.orange));
                if (success) {
                    new LoadDashboardTask().execute();
                }
            });
        });
    }

    private void logout() {
        session.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear search and reload on return
        if (etSearch != null) etSearch.setText("");
        new LoadDashboardTask().execute();

        // Trigger manual sync attempt on resume
        manualSync();
    }

    // ── Filter children by search query ──────────────────────
    private void filterChildren(String query) {
        if (query.isEmpty()) {
            adapter.updateList(fullChildList);
            tvNoResult.setVisibility(View.GONE);
            return;
        }
        String lower = query.toLowerCase().trim();
        List<Child> filtered = new ArrayList<>();
        for (Child c : fullChildList) {
            if (c.getName().toLowerCase().contains(lower)
                    || (c.getMotherName() != null && c.getMotherName().toLowerCase().contains(lower))
                    || (c.getPhone() != null && c.getPhone().contains(lower))) {
                filtered.add(c);
            }
        }
        adapter.updateList(filtered);
        tvNoResult.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Background data load ─────────────────────────────────
    private class LoadDashboardTask extends AsyncTask<Void, Void, DashboardData> {
        @Override
        protected void onPreExecute() {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected DashboardData doInBackground(Void... voids) {
            DashboardData data  = new DashboardData();
            data.children       = db.getAllChildren(centerId);
            data.total          = db.getTotalChildren(centerId);
            data.present        = db.getTodayAttendance(centerId, today);
            data.malnourished   = db.getMalnourishedCount(centerId);
            data.vaccPending    = db.getPendingVaccinations(centerId);
            data.syncPending    = db.getPendingSyncCount();
            return data;
        }

        @Override
        protected void onPostExecute(DashboardData data) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (isFinishing() || isDestroyed()) return;

            tvTotal.setText(String.valueOf(data.total));
            tvPresent.setText(String.valueOf(data.present));
            tvMalnourished.setText(String.valueOf(data.malnourished));
            tvVaccPending.setText(String.valueOf(data.vaccPending));
            tvSyncStatus.setText(data.syncPending > 0
                    ? "\u26A0 " + data.syncPending + " records pending sync"
                    : "\u2713 All synced");

            fullChildList = data.children;
            adapter.updateList(fullChildList);
        }
    }

    private static class DashboardData {
        List<Child> children;
        int total, present, malnourished, vaccPending, syncPending;
    }
}
