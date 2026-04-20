package com.anganwadi.app.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;

public class EditChildActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private EditText etName, etDob, etMother, etFather, etPhone, etAddress;
    private Spinner  spGender;
    private Button   btnSave;
    private ProgressBar progressBar;
    private int childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_child);

        db      = DatabaseHelper.getInstance(this);
        childId = getIntent().getIntExtra("child_id", -1);

        etName    = findViewById(R.id.etName);
        etDob     = findViewById(R.id.etDob);
        etMother  = findViewById(R.id.etMother);
        etFather  = findViewById(R.id.etFather);
        etPhone   = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        spGender  = findViewById(R.id.spGender);
        btnSave   = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (childId == -1) {
            Toast.makeText(this, "Child not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Pre-fill existing data ───────────────────────────
        new LoadChildTask().execute();

        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private class LoadChildTask extends AsyncTask<Void, Void, Child> {
        @Override
        protected Child doInBackground(Void... v) {
            return db.getChildById(childId);
        }

        @Override
        protected void onPostExecute(Child child) {
            if (child == null) {
                Toast.makeText(EditChildActivity.this,
                        "Child not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // Pre-fill all fields
            etName.setText(child.getName());
            etDob.setText(child.getDob());
            etMother.setText(child.getMotherName());
            etFather.setText(child.getFatherName());
            etPhone.setText(child.getPhone());
            etAddress.setText(child.getAddress());

            // Set gender spinner
            String[] genders = {"Male", "Female"};
            for (int i = 0; i < genders.length; i++) {
                if (genders[i].equalsIgnoreCase(child.getGender())) {
                    spGender.setSelection(i);
                    break;
                }
            }
        }
    }

    private void validateAndSave() {
        String name   = etName.getText().toString().trim();
        String dob    = etDob.getText().toString().trim();
        String mother = etMother.getText().toString().trim();
        String father = etFather.getText().toString().trim();
        String phone  = etPhone.getText().toString().trim();
        String address= etAddress.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (dob.isEmpty()) {
            etDob.setError("Date of birth is required");
            etDob.requestFocus();
            return;
        }

        new SaveChildTask(name, dob, gender, mother, father, phone, address).execute();
    }

    private class SaveChildTask extends AsyncTask<Void, Void, Boolean> {
        final String name, dob, gender, mother, father, phone, address;

        SaveChildTask(String n, String d, String g, String m,
                      String f, String ph, String a) {
            name=n; dob=d; gender=g; mother=m;
            father=f; phone=ph; address=a;
        }

        @Override
        protected void onPreExecute() {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
            progressBar.setVisibility(android.view.View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... v) {
            return db.updateChild(childId, name, dob, gender,
                    mother, father, phone, address);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            btnSave.setEnabled(true);
            btnSave.setText("SAVE CHANGES");
            progressBar.setVisibility(android.view.View.GONE);

            if (success) {
                Toast.makeText(EditChildActivity.this,
                        "Profile updated successfully!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(EditChildActivity.this,
                        "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
