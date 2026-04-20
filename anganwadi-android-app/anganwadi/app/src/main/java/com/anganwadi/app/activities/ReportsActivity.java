package com.anganwadi.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.utils.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        db = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String centerId = session.getCenterId();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        int total       = db.getTotalChildren(centerId);
        int present     = db.getTodayAttendance(centerId, today);
        int malnour     = db.getMalnourishedCount(centerId);
        int vaccPending = db.getPendingVaccinations(centerId);
        int syncPending = db.getPendingSyncCount();

        TextView tvTotal    = findViewById(R.id.tvTotal);
        TextView tvPresent  = findViewById(R.id.tvPresent);
        TextView tvMalnour  = findViewById(R.id.tvMalnour);
        TextView tvVacc     = findViewById(R.id.tvVacc);
        TextView tvSync     = findViewById(R.id.tvSync);

        tvTotal.setText(String.valueOf(total));
        tvPresent.setText(present + " / " + total);
        tvMalnour.setText(String.valueOf(malnour));
        tvVacc.setText(String.valueOf(vaccPending));
        tvSync.setText(syncPending > 0 ? syncPending + " pending" : "All synced ✓");

        setupCharts(centerId, present);
        setupInsights(centerId);
    }

    private void setupCharts(String centerId, int todayPresent) {
        // Pie chart - nutrition status
        PieChart pieChart = findViewById(R.id.pieChart);
        int total = db.getTotalChildren(centerId);
        int malnour = db.getMalnourishedCount(centerId);
        int normal = Math.max(0, total - malnour);
        
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(normal, "Normal"));
        pieEntries.add(new PieEntry(malnour, "At Risk"));
        PieDataSet pieDs = new PieDataSet(pieEntries, "Nutrition Status");
        pieDs.setColors(Color.parseColor("#27AE60"), Color.parseColor("#E74C3C"));
        pieDs.setValueTextSize(14f);
        pieDs.setValueTextColor(Color.WHITE);
        pieChart.setData(new PieData(pieDs));
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setCenterText("Nutrition");
        pieChart.animateY(800);
        pieChart.invalidate();

        // Bar chart - Real Weekly Attendance
        BarChart barChart = findViewById(R.id.barChart);
        List<Map<String, Object>> weeklyData = db.getWeeklyAttendance(centerId);
        List<BarEntry> barEntries = new ArrayList<>();
        String[] days = new String[7];
        
        // Initialize with 0s for the week
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String dateStr = dbFmt.format(cal.getTime());
            days[i] = fmt.format(cal.getTime());
            float count = 0;
            for (Map<String, Object> data : weeklyData) {
                if (dateStr.equals(data.get("date"))) {
                    count = ((Number) data.get("count")).floatValue();
                    break;
                }
            }
            barEntries.add(new BarEntry(i, count));
            cal.add(Calendar.DATE, 1);
        }

        BarDataSet barDs = new BarDataSet(barEntries, "Present Children");
        barDs.setColor(Color.parseColor("#2980B9"));
        barChart.setData(new BarData(barDs));
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(days));
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private void setupInsights(String centerId) {
        TextView tvInsights = findViewById(R.id.tvInsights);
        if (tvInsights == null) return;

        List<Map<String, Object>> absentees = db.getFrequentAbsentees(centerId);
        if (absentees.isEmpty()) {
            tvInsights.setText("Attendance trend is stable. No frequent absentees.");
        } else {
            StringBuilder sb = new StringBuilder("Frequent Absentees (3+ days):\n");
            for (Map<String, Object> entry : absentees) {
                sb.append("• ").append(entry.get("name"))
                  .append(" (").append(entry.get("absent_count")).append(" days)\n");
            }
            tvInsights.setText(sb.toString());
        }
    }
}
