package com.anganwadi.app.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.anganwadi.app.R;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.utils.SessionManager;

public class AddChildActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);
        db = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        EditText etName    = findViewById(R.id.etName);
        EditText etDob     = findViewById(R.id.etDob);
        EditText etMother  = findViewById(R.id.etMother);
        EditText etFather  = findViewById(R.id.etFather);
        EditText etPhone   = findViewById(R.id.etPhone);
        EditText etAddress = findViewById(R.id.etAddress);
        Spinner  spGender  = findViewById(R.id.spGender);
        Button   btnSave   = findViewById(R.id.btnSave);
        ImageButton btnBack= findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String dob  = etDob.getText().toString().trim();
            if (name.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "Name and DOB are required", Toast.LENGTH_SHORT).show();
                return;
            }
            Child child = new Child();
            child.setName(name); child.setDob(dob);
            child.setGender(spGender.getSelectedItem().toString());
            child.setMotherName(etMother.getText().toString());
            child.setFatherName(etFather.getText().toString());
            child.setPhone(etPhone.getText().toString());
            child.setAddress(etAddress.getText().toString());
            child.setCenterId(session.getCenterId());
            db.addChild(child);
            Toast.makeText(this, "Child registered successfully!", Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
