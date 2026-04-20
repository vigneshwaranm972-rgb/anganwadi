package com.anganwadi.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.adapters.VaccinationAdapter;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.models.GrowthRecord;
import com.anganwadi.app.models.Vaccination;
import com.anganwadi.app.utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.*;

public class ParentDashboardActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);
        db = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        int childId = session.getChildId();
        Child child = db.getChildById(childId);

        TextView tvChildName   = findViewById(R.id.tvChildName);
        TextView tvChildAge    = findViewById(R.id.tvChildAge);
        TextView tvWeight      = findViewById(R.id.tvWeight);
        TextView tvHeight      = findViewById(R.id.tvHeight);
        TextView tvStatus      = findViewById(R.id.tvStatus);
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        TextView tvWelcome     = findViewById(R.id.tvWelcome);
        LineChart weightChart  = findViewById(R.id.weightChart);
        RecyclerView rvVacc    = findViewById(R.id.rvVaccinations);
        Button btnLogout       = findViewById(R.id.btnLogout);

        if (child != null) {
            tvChildName.setText(child.getName());
            tvChildAge.setText(child.getAgeString());
            tvWelcome.setText("Hello, " + session.getUserName() + " 👋");
        }

        // Latest growth
        GrowthRecord latest = db.getLatestGrowth(childId);
        if (latest != null) {
            tvWeight.setText(latest.getWeight() + " kg");
            tvHeight.setText(latest.getHeight() + " cm");
            String status = latest.getNutritionStatus();
            tvStatus.setText(status.substring(0,1).toUpperCase() + status.substring(1));
            switch (status) {
                case "normal":   tvStatusBadge.setText("✓ Normal");   tvStatusBadge.setBackgroundResource(R.drawable.badge_green);  break;
                case "moderate": tvStatusBadge.setText("⚠ Moderate"); tvStatusBadge.setBackgroundResource(R.drawable.badge_orange); break;
                case "severe":   tvStatusBadge.setText("✗ Severe");   tvStatusBadge.setBackgroundResource(R.drawable.badge_red);    break;
            }
        }

        // Weight chart
        List<GrowthRecord> growthList = db.getChildGrowth(childId);
        setupWeightChart(weightChart, growthList);

        // Vaccinations
        List<Vaccination> vaccList = db.getChildVaccinations(childId);
        VaccinationAdapter vaccAdapter = new VaccinationAdapter(vaccList);
        rvVacc.setLayoutManager(new LinearLayoutManager(this));
        rvVacc.setAdapter(vaccAdapter);

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupWeightChart(LineChart chart, List<GrowthRecord> records) {
        if (records.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        List<String> dates  = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            entries.add(new Entry(i, records.get(i).getWeight()));
            String d = records.get(i).getDate();
            dates.add(d.length() >= 7 ? d.substring(5) : d);
        }
        LineDataSet ds = new LineDataSet(entries, "Weight (kg)");
        ds.setColor(Color.parseColor("#27AE60"));
        ds.setCircleColor(Color.parseColor("#27AE60"));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(true);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setFillAlpha(80);
        ds.setFillColor(Color.parseColor("#27AE60"));
        ds.setDrawFilled(true);

        chart.setData(new LineData(ds));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        chart.getXAxis().setGranularity(1f);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(800);
        chart.invalidate();
    }
}
