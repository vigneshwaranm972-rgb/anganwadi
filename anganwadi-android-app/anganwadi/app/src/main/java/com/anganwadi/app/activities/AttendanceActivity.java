package com.anganwadi.app.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anganwadi.app.R;
import com.anganwadi.app.adapters.AttendanceToggleAdapter;
import com.anganwadi.app.adapters.ChildAdapter;
import com.anganwadi.app.database.DatabaseHelper;
import com.anganwadi.app.models.Child;
import com.anganwadi.app.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class AttendanceActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        db = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

        TextView tvDate   = findViewById(R.id.tvDate);
        TextView tvCount  = findViewById(R.id.tvAttCount);
        RecyclerView rv   = findViewById(R.id.rvChildren);
        ImageButton back  = findViewById(R.id.btnBack);

        tvDate.setText("Attendance — " + display);
        back.setOnClickListener(v -> finish());

        String centerId = session.getCenterId();
        List<Child> children = db.getAllChildren(centerId);
        
        // Fetch IDs of children already marked present today
        List<Integer> presentIds = getPresentIdsForToday(centerId, today);

        AttendanceToggleAdapter adapter = new AttendanceToggleAdapter(children, presentIds, (child, isPresent) -> {
            String status = isPresent ? "present" : "absent";
            db.markAttendance(child.getId(), today, status, session.getUserName());
            updateCount(centerId, today);
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        updateCount(centerId, today);
    }

    private void updateCount(String centerId, String date) {
        TextView tvCount = findViewById(R.id.tvAttCount);
        int count = db.getTodayAttendance(centerId, date);
        tvCount.setText(count + " children present today");
    }

    private List<Integer> getPresentIdsForToday(String centerId, String date) {
        List<Integer> ids = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase sdb = db.getReadableDatabase();
        android.database.Cursor c = sdb.rawQuery(
                "SELECT child_id FROM attendance a JOIN children c ON a.child_id = c.id " +
                "WHERE c.center_id = ? AND a.date = ? AND a.status = 'present'",
                new String[]{centerId, date});
        if (c.moveToFirst()) {
            do {
                ids.add(c.getInt(0));
            } while (c.moveToNext());
        }
        c.close();
        return ids;
    }
}
