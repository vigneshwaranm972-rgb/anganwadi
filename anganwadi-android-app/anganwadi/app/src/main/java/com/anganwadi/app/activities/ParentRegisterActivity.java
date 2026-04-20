package com.anganwadi.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import java.util.List;

public class ParentRegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etPassword, etConfirmPassword, etCenterId;
    private Spinner  spChild;
    private Button   btnRegister;
    private ProgressBar progressBar;
    private TextView tvChildNote;
    private DatabaseHelper db;
    private List<Child> childList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_register);

        db = DatabaseHelper.getInstance(this);

        etName            = findViewById(R.id.etName);
        etPhone           = findViewById(R.id.etPhone);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etCenterId        = findViewById(R.id.etCenterId);
        spChild           = findViewById(R.id.spChild);
        btnRegister       = findViewById(R.id.btnRegister);
        progressBar       = findViewById(R.id.progressBar);
        tvChildNote       = findViewById(R.id.tvChildNote);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Load children when center ID is entered
        findViewById(R.id.btnLoadChildren).setOnClickListener(v -> loadChildren());

        btnRegister.setOnClickListener(v -> validateAndRegister());
    }

    private void loadChildren() {
        String centerId = etCenterId.getText().toString().trim();
        if (centerId.isEmpty()) {
            etCenterId.setError("Enter Center ID first");
            etCenterId.requestFocus();
            return;
        }

        new LoadChildrenTask(centerId).execute();
    }

    private class LoadChildrenTask extends AsyncTask<Void, Void, List<Child>> {
        final String centerId;
        LoadChildrenTask(String centerId) { this.centerId = centerId; }

        @Override
        protected List<Child> doInBackground(Void... voids) {
            return db.getAllChildren(centerId);
        }

        @Override
        protected void onPostExecute(List<Child> children) {
            childList = children;
            if (children.isEmpty()) {
                tvChildNote.setText("No children found for this center. Ask the worker to register your child first.");
                tvChildNote.setVisibility(View.VISIBLE);
                spChild.setEnabled(false);
                return;
            }

            tvChildNote.setText("Select your child from the list below:");
            tvChildNote.setVisibility(View.VISIBLE);
            spChild.setEnabled(true);

            // Build name list for spinner
            String[] names = new String[children.size()];
            for (int i = 0; i < children.size(); i++) {
                names[i] = children.get(i).getName() + "  (" + children.get(i).getAgeString() + ")";
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    ParentRegisterActivity.this,
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spChild.setAdapter(adapter);

            Toast.makeText(ParentRegisterActivity.this,
                    children.size() + " children found", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndRegister() {
        String name     = etName.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();
        String centerId = etCenterId.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Your name is required"); etName.requestFocus(); return;
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
        if (childList == null || childList.isEmpty()) {
            Toast.makeText(this, "Please load and select your child first", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedChildId = childList.get(spChild.getSelectedItemPosition()).getId();
        new RegisterParentTask(name, phone, password, centerId, selectedChildId).execute();
    }

    private class RegisterParentTask extends AsyncTask<Void, Void, Integer> {
        final String name, phone, password, centerId;
        final int childId;

        RegisterParentTask(String n, String ph, String pw, String c, int cid) {
            name = n; phone = ph; password = pw; centerId = c; childId = cid;
        }

        @Override
        protected void onPreExecute() {
            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            if (db.isPhoneRegistered(phone)) return 1;
            long id = db.registerUser(name, phone, password, "parent", centerId, childId);
            return id > 0 ? 0 : -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            btnRegister.setEnabled(true);
            btnRegister.setText("REGISTER");
            progressBar.setVisibility(View.GONE);

            if (result == 0) {
                Toast.makeText(ParentRegisterActivity.this,
                        "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(ParentRegisterActivity.this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else if (result == 1) {
                etPhone.setError("This phone number is already registered");
                etPhone.requestFocus();
            } else {
                Toast.makeText(ParentRegisterActivity.this,
                        "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
