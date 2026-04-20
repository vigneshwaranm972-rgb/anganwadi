package com.anganwadi.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.adapters.AttendanceAdapter;
import com.anganwadi.app.adapters.VaccinationAdapter;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.*;
import com.anganwadi.app.utils.SessionManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChildDetailActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private int childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_detail);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);
        childId = getIntent().getIntExtra("child_id", 1);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ✅ FEATURE 3: Edit button — only for workers
        Button btnEdit = findViewById(R.id.btnEdit);
        if ("worker".equals(session.getUserRole())) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditChildActivity.class);
                intent.putExtra("child_id", childId);
                startActivityForResult(intent, 100);
            });
        } else {
            btnEdit.setVisibility(View.GONE);
        }

        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Reload after edit
            loadData();
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new AsyncTask<Void, Void, ChildDetailData>() {
            @Override
            protected ChildDetailData doInBackground(Void... v) {
                ChildDetailData d = new ChildDetailData();
                d.child      = db.getChildById(childId);
                d.latest     = db.getLatestGrowth(childId);
                d.growthList = db.getChildGrowth(childId);
                d.attList    = db.getChildAttendance(childId);
                d.vaccList   = db.getChildVaccinations(childId);
                return d;
            }

            @Override
            protected void onPostExecute(ChildDetailData d) {
                if (isFinishing() || isDestroyed()) return;
                bindUI(d);
            }
        }.execute();
    }

    private void bindUI(ChildDetailData d) {
        TextView tvName      = findViewById(R.id.tvName);
        TextView tvAge       = findViewById(R.id.tvAge);
        TextView tvGender    = findViewById(R.id.tvGender);
        TextView tvMother    = findViewById(R.id.tvMother);
        TextView tvWeight    = findViewById(R.id.tvWeight);
        TextView tvHeight    = findViewById(R.id.tvHeight);
        TextView tvNutrition = findViewById(R.id.tvNutrition);
        LineChart chart      = findViewById(R.id.weightChart);
        RecyclerView rvAtt   = findViewById(R.id.rvAttendance);
        // RecyclerView rvVacc  = findViewById(R.id.rvVaccinations);
        Button btnMarkAtt    = findViewById(R.id.btnMarkAttendance);
        Button btnAddGrowth  = findViewById(R.id.btnAddGrowth);

        if (d.child != null) {
            tvName.setText(d.child.getName());
            tvAge.setText(d.child.getAgeString());
            tvGender.setText(d.child.getGender());
            tvMother.setText("Mother: " + d.child.getMotherName());
        }

        if (d.latest != null) {
            tvWeight.setText(d.latest.getWeight() + " kg");
            tvHeight.setText(d.latest.getHeight() + " cm");
            tvNutrition.setText(d.latest.getNutritionStatus().toUpperCase());
            int color = "normal".equals(d.latest.getNutritionStatus())
                    ? Color.parseColor("#27AE60") : Color.parseColor("#E74C3C");
            tvNutrition.setTextColor(color);
        }

        // Growth chart
        if (!d.growthList.isEmpty()) {
            List<Entry> entries = new ArrayList<>();
            List<String> dates  = new ArrayList<>();
            for (int i = 0; i < d.growthList.size(); i++) {
                entries.add(new Entry(i, d.growthList.get(i).getWeight()));
                String dt = d.growthList.get(i).getDate();
                dates.add(dt.length() >= 7 ? dt.substring(5) : dt);
            }
            LineDataSet ds = new LineDataSet(entries, "Weight (kg)");
            ds.setColor(Color.parseColor("#1B7340"));
            ds.setCircleColor(Color.parseColor("#1B7340"));
            ds.setLineWidth(2f); ds.setCircleRadius(4f);
            ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            ds.setDrawFilled(true);
            ds.setFillColor(Color.parseColor("#EAFAF1")); ds.setFillAlpha(200);
            chart.setData(new LineData(ds));
            chart.getDescription().setEnabled(false);
            chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
            chart.getAxisRight().setEnabled(false);
            chart.animateX(600); chart.invalidate();
        }

        // Attendance list
        rvAtt.setLayoutManager(new LinearLayoutManager(this));
        rvAtt.setAdapter(new AttendanceAdapter(d.attList));

        // Removed vaccination loading logic per requirement
        /*
        rvVacc.setLayoutManager(new LinearLayoutManager(this));
        if ("worker".equals(session.getUserRole())) {
            rvVacc.setAdapter(new VaccinationAdapter(this, d.vaccList,
                    db, childId, this::loadData));
        } else {
            rvVacc.setAdapter(new VaccinationAdapter(d.vaccList));
        }
        */

        // Mark attendance
        btnMarkAtt.setOnClickListener(v -> {
            String today = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(new Date());
            boolean marked = db.markAttendance(childId, today,
                    "present", session.getUserName());
            Toast.makeText(this,
                    marked ? "Attendance marked present!" : "Already marked today",
                    Toast.LENGTH_SHORT).show();
        });

        // Add growth
        btnAddGrowth.setOnClickListener(v -> showAddGrowthDialog());
    }

    private void showAddGrowthDialog() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add Growth Record");
        android.view.View view = getLayoutInflater()
                .inflate(R.layout.dialog_add_growth, null);
        builder.setView(view);
        EditText etWeight  = view.findViewById(R.id.etWeight);
        EditText etHeight  = view.findViewById(R.id.etHeight);
        Spinner  spStatus  = view.findViewById(R.id.spStatus);
        EditText etRemarks = view.findViewById(R.id.etRemarks);
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                GrowthRecord gr = new GrowthRecord();
                gr.setChildId(childId);
                gr.setWeight(Float.parseFloat(etWeight.getText().toString()));
                gr.setHeight(Float.parseFloat(etHeight.getText().toString()));
                gr.setNutritionStatus(spStatus.getSelectedItem().toString().toLowerCase());
                gr.setRemarks(etRemarks.getText().toString());
                gr.setDate(new SimpleDateFormat("yyyy-MM-dd",
                        Locale.getDefault()).format(new Date()));
                db.addGrowthRecord(gr);
                Toast.makeText(this, "Growth record saved!", Toast.LENGTH_SHORT).show();
                loadData();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter valid weight and height", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static class ChildDetailData {
        Child child; GrowthRecord latest;
        List<GrowthRecord> growthList;
        List<Attendance>   attList;
        List<Vaccination>  vaccList;
    }
}
