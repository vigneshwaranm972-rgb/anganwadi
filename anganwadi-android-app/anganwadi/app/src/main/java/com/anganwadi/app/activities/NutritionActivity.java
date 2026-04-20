package com.anganwadi.app.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.models.NutritionRecord;
import com.anganwadi.app.utils.SMSNotificationManager;
import com.anganwadi.app.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class NutritionActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private Spinner  spChild, spMealType;
    private EditText etFoodItems, etQuantity, etRemarks;
    private Button   btnSave;
    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private TextView tvDate;
    private List<Child> childList = new ArrayList<>();

    // Common meal types provided at Anganwadi
    private static final String[] MEAL_TYPES = {
        "Morning Snack",
        "Mid-Day Meal",
        "Take-Home Ration",
        "Supplementary Nutrition (THR)",
        "Iron + Folic Acid Tablet",
        "Vitamin A Supplement",
        "Protein Supplement",
        "Other"
    };

    // Common foods for quick selection
    private static final String[] COMMON_FOODS = {
        "Rice + Dal",
        "Khichdi",
        "Roti + Sabzi",
        "Boiled Egg",
        "Banana",
        "Groundnut Chikki",
        "Milk",
        "Fortified Biscuits"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        spChild      = findViewById(R.id.spChild);
        spMealType   = findViewById(R.id.spMealType);
        etFoodItems  = findViewById(R.id.etFoodItems);
        etQuantity   = findViewById(R.id.etQuantity);
        etRemarks    = findViewById(R.id.etRemarks);
        btnSave      = findViewById(R.id.btnSave);
        rvHistory    = findViewById(R.id.rvHistory);
        progressBar  = findViewById(R.id.progressBar);
        tvDate       = findViewById(R.id.tvDate);

        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText("Date: " + today);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Meal type spinner
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MEAL_TYPES);
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMealType.setAdapter(mealAdapter);

        // Quick food buttons
        setupQuickFoodButtons();

        // Load children
        loadChildren();

        btnSave.setOnClickListener(v -> saveNutritionRecord());
    }

    private void setupQuickFoodButtons() {
        LinearLayout quickFoodLayout = findViewById(R.id.quickFoodLayout);
        if (quickFoodLayout == null) return;

        for (String food : COMMON_FOODS) {
            Button btn = new Button(this);
            btn.setText(food);
            btn.setTextSize(11f);
            btn.setPadding(16, 8, 16, 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);

            // Outline style
            btn.setBackgroundResource(R.drawable.badge_green);
            btn.setTextColor(getResources().getColor(R.color.green_dark));

            btn.setOnClickListener(v -> {
                String current = etFoodItems.getText().toString().trim();
                if (current.isEmpty()) {
                    etFoodItems.setText(food);
                } else {
                    etFoodItems.setText(current + ", " + food);
                }
            });

            quickFoodLayout.addView(btn);
        }
    }

    private void loadChildren() {
        new AsyncTask<Void, Void, List<Child>>() {
            @Override
            protected List<Child> doInBackground(Void... v) {
                return db.getAllChildren(session.getCenterId());
            }

            @Override
            protected void onPostExecute(List<Child> children) {
                childList = children;
                String[] names = new String[children.size()];
                for (int i = 0; i < children.size(); i++)
                    names[i] = children.get(i).getName()
                            + " (" + children.get(i).getAgeString() + ")";
                ArrayAdapter<String> a = new ArrayAdapter<>(NutritionActivity.this,
                        android.R.layout.simple_spinner_item, names);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spChild.setAdapter(a);

                // Load today's history
                loadNutritionHistory();
            }
        }.execute();
    }

    private void saveNutritionRecord() {
        if (childList.isEmpty()) {
            Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show();
            return;
        }

        String food = etFoodItems.getText().toString().trim();
        if (food.isEmpty()) {
            etFoodItems.setError("Enter food items provided");
            etFoodItems.requestFocus();
            return;
        }

        Child child    = childList.get(spChild.getSelectedItemPosition());
        String mealType= spMealType.getSelectedItem().toString();
        String quantity = etQuantity.getText().toString().trim();
        String remarks  = etRemarks.getText().toString().trim();
        String today    = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault()).format(new Date());
        String todayDisplay = new SimpleDateFormat("dd-MM-yyyy",
                Locale.getDefault()).format(new Date());

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                btnSave.setEnabled(false);
                btnSave.setText("Saving...");
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... v) {
                return db.addNutritionRecord(child.getId(), today,
                        mealType, food, quantity, remarks,
                        session.getUserName());
            }

            @Override
            protected void onPostExecute(Boolean success) {
                btnSave.setEnabled(true);
                btnSave.setText("SAVE & NOTIFY PARENT");
                progressBar.setVisibility(View.GONE);

                if (success) {
                    Toast.makeText(NutritionActivity.this,
                            "Nutrition record saved for " + child.getName(),
                            Toast.LENGTH_SHORT).show();

                    // Auto send SMS to parent
                    String phone = child.getPhone();
                    if (phone != null && !phone.isEmpty()) {
                        String smsFood = mealType + ": " + food;
                        SMSNotificationManager.notifyNutritionProvided(
                                NutritionActivity.this, db, child.getId(), smsFood);
                        Toast.makeText(NutritionActivity.this,
                                "SMS sent to parent: " + phone,
                                Toast.LENGTH_SHORT).show();
                    }

                    // Clear form
                    etFoodItems.setText("");
                    etQuantity.setText("");
                    etRemarks.setText("");

                    // Reload history
                    loadNutritionHistory();
                } else {
                    Toast.makeText(NutritionActivity.this,
                            "Save failed. Try again.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void loadNutritionHistory() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new AsyncTask<Void, Void, List<NutritionRecord>>() {
            @Override
            protected List<NutritionRecord> doInBackground(Void... v) {
                return db.getTodayNutritionRecords(session.getCenterId(), today);
            }

            @Override
            protected void onPostExecute(List<NutritionRecord> records) {
                if (isFinishing() || isDestroyed()) return;
                NutritionHistoryAdapter adapter =
                        new NutritionHistoryAdapter(records);
                rvHistory.setLayoutManager(new LinearLayoutManager(NutritionActivity.this));
                rvHistory.setAdapter(adapter);
            }
        }.execute();
    }

    // ── Inline History Adapter ───────────────────────────────
    static class NutritionHistoryAdapter
            extends RecyclerView.Adapter<NutritionHistoryAdapter.VH> {
        final List<NutritionRecord> list;
        NutritionHistoryAdapter(List<NutritionRecord> l) { this.list = l; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_nutrition_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NutritionRecord r = list.get(pos);
            h.tvChild.setText(r.getChildName());
            h.tvMeal.setText(r.getMealType());
            h.tvFood.setText(r.getFoodItems());
            h.tvTime.setText(r.getDate());
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvChild, tvMeal, tvFood, tvTime;
            VH(View v) {
                super(v);
                tvChild = v.findViewById(R.id.tvChildName);
                tvMeal  = v.findViewById(R.id.tvMealType);
                tvFood  = v.findViewById(R.id.tvFoodItems);
                tvTime  = v.findViewById(R.id.tvTime);
            }
        }
    }
}
