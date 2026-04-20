package com.anganwadi.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anganwadi.app.models.Child;
import com.anganwadi.app.models.Attendance;
import com.anganwadi.app.models.GrowthRecord;
import com.anganwadi.app.models.NutritionRecord;
import com.anganwadi.app.models.User;
import com.anganwadi.app.models.Vaccination;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "anganwadi.db";
    private static final int DB_VERSION = 1;
    private static DatabaseHelper instance;

    // Table Names
    public static final String TABLE_USERS       = "users";
    public static final String TABLE_CHILDREN    = "children";
    public static final String TABLE_ATTENDANCE  = "attendance";
    public static final String TABLE_GROWTH      = "growth_records";
    public static final String TABLE_VACCINATION = "vaccinations";
    public static final String TABLE_SYNC_QUEUE  = "sync_queue";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_NUTRITION     = "nutrition_records";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // USERS table
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "phone TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL," +         // 'worker' or 'parent'
                "center_id TEXT," +
                "child_id INTEGER," +
                "created_at TEXT DEFAULT (datetime('now'))" +
                ")");

        // CHILDREN table
        db.execSQL("CREATE TABLE " + TABLE_CHILDREN + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "dob TEXT NOT NULL," +
                "gender TEXT NOT NULL," +
                "mother_name TEXT," +
                "father_name TEXT," +
                "phone TEXT," +
                "address TEXT," +
                "aadhaar TEXT," +
                "center_id TEXT," +
                "fingerprint_id INTEGER DEFAULT -1," +
                "photo_path TEXT," +
                "is_synced INTEGER DEFAULT 0," +
                "created_at TEXT DEFAULT (datetime('now'))" +
                ")");

        // ATTENDANCE table
        db.execSQL("CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "child_id INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "status TEXT DEFAULT 'present'," +
                "marked_by TEXT," +
                "is_synced INTEGER DEFAULT 0," +
                "FOREIGN KEY(child_id) REFERENCES children(id)," +
                "UNIQUE(child_id, date)" +
                ")");

        // GROWTH RECORDS table
        db.execSQL("CREATE TABLE " + TABLE_GROWTH + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "child_id INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "weight REAL," +
                "height REAL," +
                "muac REAL," +
                "nutrition_status TEXT," +   // 'normal','moderate','severe'
                "remarks TEXT," +
                "is_synced INTEGER DEFAULT 0," +
                "FOREIGN KEY(child_id) REFERENCES children(id)" +
                ")");

        // VACCINATION table
        db.execSQL("CREATE TABLE " + TABLE_VACCINATION + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "child_id INTEGER NOT NULL," +
                "vaccine_name TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "given_date TEXT," +
                "status TEXT DEFAULT 'pending'," +  // 'pending','given','missed'
                "is_synced INTEGER DEFAULT 0," +
                "FOREIGN KEY(child_id) REFERENCES children(id)" +
                ")");

        // SYNC QUEUE - stores offline changes to push when internet returns
        db.execSQL("CREATE TABLE " + TABLE_SYNC_QUEUE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "table_name TEXT NOT NULL," +
                "record_id INTEGER NOT NULL," +
                "action TEXT NOT NULL," +   // 'INSERT','UPDATE','DELETE'
                "created_at TEXT DEFAULT (datetime('now'))" +
                ")");

        // NOTIFICATIONS table
        db.execSQL("CREATE TABLE " + TABLE_NOTIFICATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "child_id INTEGER," +
                "type TEXT," +
                "message_type TEXT," +
                "recipient TEXT," +
                "sent_at TEXT DEFAULT (datetime('now'))" +
                ")");

        // NUTRITION table
        db.execSQL("CREATE TABLE " + TABLE_NUTRITION + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "child_id INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "meal_type TEXT," +
                "food_items TEXT," +
                "quantity TEXT," +
                "remarks TEXT," +
                "given_by TEXT," +
                "is_synced INTEGER DEFAULT 0," +
                "FOREIGN KEY(child_id) REFERENCES children(id)" +
                ")");

        // Insert demo data
        insertDemoData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHILDREN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROWTH);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VACCINATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYNC_QUEUE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
        onCreate(db);
    }

    private void insertDemoData(SQLiteDatabase db) {
        // Demo Worker
        ContentValues worker = new ContentValues();
        worker.put("name", "Meena Devi");
        worker.put("phone", "9876543210");
        worker.put("password", "worker123");
        worker.put("role", "worker");
        worker.put("center_id", "AWC_001");
        db.insert(TABLE_USERS, null, worker);

        // Demo Parent
        ContentValues parent = new ContentValues();
        parent.put("name", "Priya Sharma");
        parent.put("phone", "9123456789");
        parent.put("password", "parent123");
        parent.put("role", "parent");
        parent.put("child_id", 1);
        db.insert(TABLE_USERS, null, parent);

        // Demo Children
        String[][] children = {
            {"Aarav Sharma",   "2021-03-15", "Male",   "Priya Sharma",  "Rahul Sharma",  "9123456789"},
            {"Diya Patel",     "2020-07-22", "Female", "Sunita Patel",  "Ramesh Patel",  "9234567890"},
            {"Rohan Kumar",    "2022-01-10", "Male",   "Anita Kumar",   "Vijay Kumar",   "9345678901"},
            {"Priya Singh",    "2021-11-05", "Female", "Kavita Singh",  "Suresh Singh",  "9456789012"},
            {"Arjun Mehta",    "2020-05-18", "Male",   "Pooja Mehta",   "Amit Mehta",    "9567890123"},
            {"Riya Gupta",     "2022-08-30", "Female", "Neha Gupta",    "Sanjay Gupta",  "9678901234"},
            {"Karan Verma",    "2021-09-12", "Male",   "Rekha Verma",   "Mohan Verma",   "9789012345"},
            {"Ananya Mishra",  "2020-12-25", "Female", "Seema Mishra",  "Arun Mishra",   "9890123456"},
        };

        for (String[] c : children) {
            ContentValues cv = new ContentValues();
            cv.put("name", c[0]); cv.put("dob", c[1]); cv.put("gender", c[2]);
            cv.put("mother_name", c[3]); cv.put("father_name", c[4]);
            cv.put("phone", c[5]); cv.put("center_id", "AWC_001");
            db.insert(TABLE_CHILDREN, null, cv);
        }

        // Demo growth records for child 1
        String[][] growth = {
            {"2024-01-15", "10.2", "82.5", "normal"},
            {"2024-02-15", "10.4", "83.1", "normal"},
            {"2024-03-15", "10.3", "83.8", "normal"},
            {"2024-04-15", "10.6", "84.5", "normal"},
            {"2024-05-15", "10.5", "85.0", "normal"},
            {"2024-06-15", "10.8", "85.6", "normal"},
        };
        for (String[] g : growth) {
            ContentValues cv = new ContentValues();
            cv.put("child_id", 1); cv.put("date", g[0]);
            cv.put("weight", g[1]); cv.put("height", g[2]);
            cv.put("nutrition_status", g[3]);
            db.insert(TABLE_GROWTH, null, cv);
        }

        // Demo attendance for child 1 (last 7 days)
        String[] dates = {"2024-06-10","2024-06-11","2024-06-12","2024-06-13","2024-06-14","2024-06-17","2024-06-18"};
        String[] statuses = {"present","present","absent","present","present","present","absent"};
        for (int i = 0; i < dates.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put("child_id", 1); cv.put("date", dates[i]);
            cv.put("status", statuses[i]); cv.put("marked_by", "Meena Devi");
            db.insert(TABLE_ATTENDANCE, null, cv);
        }

        // Demo vaccinations
        String[][] vaccines = {
            {"BCG",        "2021-04-15", "2021-04-15", "given"},
            {"OPV-0",      "2021-04-15", "2021-04-15", "given"},
            {"Hepatitis-B","2021-04-15", "2021-04-15", "given"},
            {"DPT-1",      "2021-06-15", "2021-06-18", "given"},
            {"OPV-1",      "2021-06-15", "2021-06-18", "given"},
            {"Measles",    "2022-03-15", "2022-03-20", "given"},
            {"DPT Booster","2024-03-15", null,          "pending"},
            {"Vitamin A",  "2024-06-15", null,          "pending"},
        };
        for (String[] v : vaccines) {
            ContentValues cv = new ContentValues();
            cv.put("child_id", 1); cv.put("vaccine_name", v[0]);
            cv.put("due_date", v[1]);
            if (v[2] != null) cv.put("given_date", v[2]);
            cv.put("status", v[3]);
            db.insert(TABLE_VACCINATION, null, cv);
        }
    }


    

    // ─── USER METHODS ───────────────────────────────────────
    public User loginUser(String phone, String password, String role) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null,
                "phone=? AND password=? AND role=?",
                new String[]{phone, password, role}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = new User();
            user.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            user.setName(c.getString(c.getColumnIndexOrThrow("name")));
            user.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
            user.setRole(c.getString(c.getColumnIndexOrThrow("role")));
            user.setCenterId(c.getString(c.getColumnIndexOrThrow("center_id")));
            user.setChildId(c.getInt(c.getColumnIndexOrThrow("child_id")));
        }
        c.close();
        return user;
    }

    /**
     * Check if a phone number is already registered (any role).
     * Call from background thread only.
     */
    public boolean isPhoneRegistered(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{"id"},
                "phone=?", new String[]{phone}, null, null, null);
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    /**
     * Register a new user (worker or parent).
     * Returns the new row ID, or -1 if failed.
     * Call from background thread only.
     */
    public long registerUser(String name, String phone, String password,
                             String role, String centerId, int childId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name",      name);
        cv.put("phone",     phone);
        cv.put("password",  password);   // TODO: hash with BCrypt before production
        cv.put("role",      role);
        cv.put("center_id", centerId);
        cv.put("child_id",  childId);
        return db.insert(TABLE_USERS, null, cv);
    }
    

    // ─── CHILDREN METHODS ────────────────────────────────────
    public List<Child> getAllChildren(String centerId) {
        List<Child> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CHILDREN, null, "center_id=?",
                new String[]{centerId}, null, null, "name ASC");
        while (c.moveToNext()) list.add(cursorToChild(c));
        c.close();
        return list;
    }

    public List<Child> getAllChildrenForWorker() {
        List<Child> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CHILDREN, null, null, null, null, null, "name ASC");
        while (c.moveToNext()) list.add(cursorToChild(c));
        c.close();
        return list;
    }

    public Child getChildById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CHILDREN, null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Child child = null;
        if (c.moveToFirst()) child = cursorToChild(c);
        c.close();
        return child;
    }

    public long addChild(Child child) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", child.getName()); cv.put("dob", child.getDob());
        cv.put("gender", child.getGender()); cv.put("mother_name", child.getMotherName());
        cv.put("father_name", child.getFatherName()); cv.put("phone", child.getPhone());
        cv.put("address", child.getAddress()); cv.put("center_id", child.getCenterId());
        long id = db.insert(TABLE_CHILDREN, null, cv);
        addToSyncQueue("children", (int) id, "INSERT", db);
        return id;
    }

    public boolean updateChild(int id, String name, String dob, String gender, 
                               String mother, String father, String phone, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("dob", dob);
        cv.put("gender", gender);
        cv.put("mother_name", mother);
        cv.put("father_name", father);
        cv.put("phone", phone);
        cv.put("address", address);
        cv.put("is_synced", 0);

        int rows = db.update(TABLE_CHILDREN, cv, "id = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            addToSyncQueue("children", id, "UPDATE", db);
            return true;
        }
        return false;
    }

    private Child cursorToChild(Cursor c) {
        Child child = new Child();
        child.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        child.setName(c.getString(c.getColumnIndexOrThrow("name")));
        child.setDob(c.getString(c.getColumnIndexOrThrow("dob")));
        child.setGender(c.getString(c.getColumnIndexOrThrow("gender")));
        child.setMotherName(c.getString(c.getColumnIndexOrThrow("mother_name")));
        child.setFatherName(c.getString(c.getColumnIndexOrThrow("father_name")));
        child.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
        child.setCenterId(c.getString(c.getColumnIndexOrThrow("center_id")));
        return child;
    }

    public int getTotalChildren(String centerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHILDREN + " WHERE center_id=?",
                new String[]{centerId});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ─── ATTENDANCE METHODS ──────────────────────────────────
    public boolean markAttendance(int childId, String date, String status, String markedBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("child_id", childId);
        cv.put("date", date);
        cv.put("status", status);
        cv.put("marked_by", markedBy);
        cv.put("is_synced", 0);

        long id = db.insertWithOnConflict(TABLE_ATTENDANCE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id != -1) {
            // Use the provided ID for sync queue, or find the actual row if it was an update
            int finalId = (int) id;
            if (finalId <= 0) { // Some versions return -1 or 0 for REPLACE if it doesn't create new row
                 Cursor c = db.rawQuery("SELECT id FROM " + TABLE_ATTENDANCE + " WHERE child_id=? AND date=?", 
                        new String[]{String.valueOf(childId), date});
                 if (c.moveToFirst()) finalId = c.getInt(0);
                 c.close();
            }
            if (finalId > 0) addToSyncQueue("attendance", finalId, "REPLACE", db);
            return true;
        }
        return false;
    }

    public List<Map<String, Object>> getWeeklyAttendance(String centerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Get start and end of current week (Monday to Sunday)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String start = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        cal.add(Calendar.DATE, 6);
        String end = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        String query = "SELECT date, COUNT(*) as present_count FROM " + TABLE_ATTENDANCE + " a " +
                      "JOIN " + TABLE_CHILDREN + " c ON a.child_id = c.id " +
                      "WHERE c.center_id = ? AND a.date BETWEEN ? AND ? AND a.status = 'present' " +
                      "GROUP BY a.date ORDER BY a.date ASC";
        
        Cursor c = db.rawQuery(query, new String[]{centerId, start, end});
        if (c.moveToFirst()) {
            do {
                Map<String, Object> map = new HashMap<>();
                map.put("date", c.getString(0));
                map.put("count", c.getInt(1));
                list.add(map);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Map<String, Object>> getFrequentAbsentees(String centerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT c.name, COUNT(*) as absent_count FROM " + TABLE_ATTENDANCE + " a " +
                      "JOIN " + TABLE_CHILDREN + " c ON a.child_id = c.id " +
                      "WHERE c.center_id = ? AND a.status = 'absent' " +
                      "GROUP BY c.id HAVING absent_count >= 3 ORDER BY absent_count DESC LIMIT 5";
        Cursor c = db.rawQuery(query, new String[]{centerId});
        if (c.moveToFirst()) {
            do {
                Map<String, Object> map = new HashMap<>();
                map.put("name", c.getString(0));
                map.put("absent_count", c.getInt(1));
                list.add(map);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public int getTodayAttendance(String centerId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " a " +
                "JOIN " + TABLE_CHILDREN + " ch ON a.child_id=ch.id " +
                "WHERE ch.center_id=? AND a.date=? AND a.status='present'",
                new String[]{centerId, date});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public List<Attendance> getChildAttendance(int childId) {
        List<Attendance> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ATTENDANCE, null, "child_id=?",
                new String[]{String.valueOf(childId)}, null, null, "date DESC", "30");
        while (c.moveToNext()) {
            Attendance a = new Attendance();
            a.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            a.setChildId(c.getInt(c.getColumnIndexOrThrow("child_id")));
            a.setDate(c.getString(c.getColumnIndexOrThrow("date")));
            a.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
            list.add(a);
        }
        c.close();
        return list;
    }

    /**
     * Returns how many consecutive days a child has been absent (from today backwards).
     * Used to trigger absence alert SMS after 3+ days.
     */
    public int getConsecutiveAbsentDays(int childId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ATTENDANCE, null,
                "child_id=?", new String[]{String.valueOf(childId)},
                null, null, "date DESC", "10");
        int consecutiveAbsent = 0;
        while (c.moveToNext()) {
            String status = c.getString(c.getColumnIndexOrThrow("status"));
            if ("absent".equals(status)) {
                consecutiveAbsent++;
            } else {
                break; // stop at first "present"
            }
        }
        c.close();
        return consecutiveAbsent;
    }
    // ─── GROWTH METHODS ──────────────────────────────────────
    public long addGrowthRecord(GrowthRecord gr) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("child_id", gr.getChildId()); cv.put("date", gr.getDate());
        cv.put("weight", gr.getWeight()); cv.put("height", gr.getHeight());
        cv.put("nutrition_status", gr.getNutritionStatus());
        cv.put("remarks", gr.getRemarks());
        long id = db.insert(TABLE_GROWTH, null, cv);
        addToSyncQueue("growth_records", (int) id, "INSERT", db);
        return id;
    }

    public List<GrowthRecord> getChildGrowth(int childId) {
        List<GrowthRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, null, "child_id=?",
                new String[]{String.valueOf(childId)}, null, null, "date ASC");
        while (c.moveToNext()) {
            GrowthRecord gr = new GrowthRecord();
            gr.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            gr.setChildId(c.getInt(c.getColumnIndexOrThrow("child_id")));
            gr.setDate(c.getString(c.getColumnIndexOrThrow("date")));
            gr.setWeight(c.getFloat(c.getColumnIndexOrThrow("weight")));
            gr.setHeight(c.getFloat(c.getColumnIndexOrThrow("height")));
            gr.setNutritionStatus(c.getString(c.getColumnIndexOrThrow("nutrition_status")));
            gr.setRemarks(c.getString(c.getColumnIndexOrThrow("remarks")));
            list.add(gr);
        }
        c.close();
        return list;
    }

    public GrowthRecord getLatestGrowth(int childId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, null, "child_id=?",
                new String[]{String.valueOf(childId)}, null, null, "date DESC", "1");
        GrowthRecord gr = null;
        if (c.moveToFirst()) {
            gr = new GrowthRecord();
            gr.setWeight(c.getFloat(c.getColumnIndexOrThrow("weight")));
            gr.setHeight(c.getFloat(c.getColumnIndexOrThrow("height")));
            gr.setNutritionStatus(c.getString(c.getColumnIndexOrThrow("nutrition_status")));
            gr.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        }
        c.close();
        return gr;
    }

    public int getMalnourishedCount(String centerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(DISTINCT g.child_id) FROM " + TABLE_GROWTH + " g " +
                "JOIN " + TABLE_CHILDREN + " ch ON g.child_id=ch.id " +
                "WHERE ch.center_id=? AND g.nutrition_status != 'normal' " +
                "AND g.date = (SELECT MAX(date) FROM " + TABLE_GROWTH + " WHERE child_id=g.child_id)",
                new String[]{centerId});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ─── VACCINATION METHODS ─────────────────────────────────
    public List<Vaccination> getChildVaccinations(int childId) {
        List<Vaccination> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_VACCINATION, null, "child_id=?",
                new String[]{String.valueOf(childId)}, null, null, "due_date ASC");
        while (c.moveToNext()) {
            Vaccination v = new Vaccination();
            v.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            v.setChildId(c.getInt(c.getColumnIndexOrThrow("child_id")));
            v.setVaccineName(c.getString(c.getColumnIndexOrThrow("vaccine_name")));
            v.setDueDate(c.getString(c.getColumnIndexOrThrow("due_date")));
            v.setGivenDate(c.getString(c.getColumnIndexOrThrow("given_date")));
            v.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
            list.add(v);
        }
        c.close();
        return list;
    }

    public int getPendingVaccinations(String centerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_VACCINATION + " v " +
                "JOIN " + TABLE_CHILDREN + " ch ON v.child_id=ch.id " +
                "WHERE ch.center_id=? AND v.status='pending'",
                new String[]{centerId});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ─── SYNC QUEUE ──────────────────────────────────────────
    private void addToSyncQueue(String table, int recordId, String action, SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("table_name", table); cv.put("record_id", recordId); cv.put("action", action);
        db.insert(TABLE_SYNC_QUEUE, null, cv);
    }

    public int getPendingSyncCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SYNC_QUEUE, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ─── NOTIFICATION LOGGING ───────────────────────────────
    public void logNotification(int childId, String type, String msgType, String recipient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("child_id", childId);
        cv.put("type", type);
        cv.put("message_type", msgType);
        cv.put("recipient", recipient);
        db.insert(TABLE_NOTIFICATIONS, null, cv);
    }

    // ── NUTRITION ────────────────────────────────────────────
    public boolean addNutritionRecord(int childId, String date, String mealType, 
                                     String food, String quantity, String remarks, String givenBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("child_id", childId);
        cv.put("date", date);
        cv.put("meal_type", mealType);
        cv.put("food_items", food);
        cv.put("quantity", quantity);
        cv.put("remarks", remarks);
        cv.put("given_by", givenBy);
        cv.put("is_synced", 0);
        
        long id = db.insert(TABLE_NUTRITION, null, cv);
        if (id != -1) {
            addToSyncQueue("nutrition_records", (int) id, "INSERT", db);
            return true;
        }
        return false;
    }

    public List<NutritionRecord> getTodayNutritionRecords(String centerId, String date) {
        List<NutritionRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT n.*, c.name as child_name FROM " + TABLE_NUTRITION + " n " +
                      "JOIN " + TABLE_CHILDREN + " c ON n.child_id = c.id " +
                      "WHERE c.center_id = ? AND n.date = ? ORDER BY n.id DESC";
        Cursor c = db.rawQuery(query, new String[]{centerId, date});
        
        if (c.moveToFirst()) {
            do {
                NutritionRecord r = new NutritionRecord();
                r.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                r.setChildId(c.getInt(c.getColumnIndexOrThrow("child_id")));
                r.setChildName(c.getString(c.getColumnIndexOrThrow("child_name")));
                r.setDate(c.getString(c.getColumnIndexOrThrow("date")));
                r.setMealType(c.getString(c.getColumnIndexOrThrow("meal_type")));
                r.setFoodItems(c.getString(c.getColumnIndexOrThrow("food_items")));
                r.setQuantity(c.getString(c.getColumnIndexOrThrow("quantity")));
                r.setRemarks(c.getString(c.getColumnIndexOrThrow("remarks")));
                r.setGivenBy(c.getString(c.getColumnIndexOrThrow("given_by")));
                list.add(r);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // ── VACCINATION UPDATES ──────────────────────────────────
    public boolean markVaccinationGiven(int vaccId, String givenDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("given_date", givenDate);
        cv.put("status", "given");
        cv.put("is_synced", 0);
        
        int rows = db.update(TABLE_VACCINATION, cv, "id = ?", new String[]{String.valueOf(vaccId)});
        if (rows > 0) {
            addToSyncQueue("vaccinations", vaccId, "UPDATE", db);
            return true;
        }
        return false;
    }

    // ─── SYNC METHODS ──────────────────────────────────────────

    public void clearSyncQueue() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SYNC_QUEUE, null, null);
    }

    /**
     * Bulk update children from server.
     * Inserts if new, updates if exists.
     * Does NOT add to sync_queue to avoid loops.
     */
    public void syncChildrenFromServer(JSONArray array) throws JSONException {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            int serverId = json.getInt("id");
            ContentValues cv = new ContentValues();
            cv.put("name", json.getString("name"));
            cv.put("dob", json.getString("dob"));
            cv.put("gender", json.getString("gender"));
            cv.put("mother_name", json.optString("mother_name"));
            cv.put("father_name", json.optString("father_name"));
            cv.put("phone", json.optString("phone"));
            cv.put("address", json.optString("address"));
            cv.put("center_id", json.getString("center_id"));
            cv.put("is_synced", 1);

            // Check if exists (by id or maybe by a unique server_id if we added one)
            // For now, if serverId matches our local id, we update.
            // In a real app, you'd use a UUID or server_id column.
            int rows = db.update(TABLE_CHILDREN, cv, "id = ?", new String[]{String.valueOf(serverId)});
            if (rows == 0) {
                cv.put("id", serverId);
                db.insert(TABLE_CHILDREN, null, cv);
            }
        }
    }

    public void syncAttendanceFromServer(JSONArray array) throws JSONException {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            ContentValues cv = new ContentValues();
            cv.put("child_id", json.getInt("child"));
            cv.put("date", json.getString("date"));
            cv.put("status", json.getString("status"));
            cv.put("marked_by", json.optString("marked_by"));
            cv.put("is_synced", 1);
            db.insertWithOnConflict(TABLE_ATTENDANCE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void syncGrowthFromServer(JSONArray array) throws JSONException {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            ContentValues cv = new ContentValues();
            cv.put("child_id", json.getInt("child"));
            cv.put("date", json.getString("date"));
            cv.put("weight", json.getDouble("weight"));
            cv.put("height", json.getDouble("height"));
            cv.put("nutrition_status", json.getString("nutrition_status"));
            cv.put("is_synced", 1);
            db.insert(TABLE_GROWTH, null, cv);
        }
    }

    public void syncVaccinationsFromServer(JSONArray array) throws JSONException {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            ContentValues cv = new ContentValues();
            cv.put("child_id", json.getInt("child"));
            cv.put("vaccine_name", json.getString("vaccine_name"));
            cv.put("due_date", json.getString("due_date"));
            cv.put("given_date", json.optString("given_date", null));
            cv.put("status", json.getString("status"));
            cv.put("is_synced", 1);
            db.insertWithOnConflict(TABLE_VACCINATION, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    // Returns list of [child_id, attendance_id]
    public List<int[]> getPendingAttendance() {
        List<int[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor sq = db.query(TABLE_SYNC_QUEUE, null,
                "table_name='attendance'",
                null, null, null, null);
        while (sq.moveToNext()) {
            int recordId = sq.getInt(sq.getColumnIndexOrThrow("record_id"));
            Cursor c = db.query(TABLE_ATTENDANCE, null,
                    "id=?", new String[]{String.valueOf(recordId)}, null, null, null);
            if (c.moveToFirst()) {
                list.add(new int[]{
                    c.getInt(c.getColumnIndexOrThrow("child_id")),
                    recordId
                });
            }
            c.close();
        }
        sq.close();
        return list;
    }

    public String getAttendanceDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ATTENDANCE, new String[]{"date"}, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        String val = "";
        if (c.moveToFirst()) val = c.getString(0);
        c.close(); return val;
    }

    public String getAttendanceStatus(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ATTENDANCE, new String[]{"status"}, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        String val = "present";
        if (c.moveToFirst()) val = c.getString(0);
        c.close(); return val;
    }

    public List<int[]> getPendingGrowth() {
        List<int[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor sq = db.query(TABLE_SYNC_QUEUE, null,
                "table_name='growth_records'",
                null, null, null, null);
        while (sq.moveToNext()) {
            int recordId = sq.getInt(sq.getColumnIndexOrThrow("record_id"));
            Cursor c = db.query(TABLE_GROWTH, null,
                    "id=?", new String[]{String.valueOf(recordId)}, null, null, null);
            if (c.moveToFirst()) {
                list.add(new int[]{
                    c.getInt(c.getColumnIndexOrThrow("child_id")),
                    recordId
                });
            }
            c.close();
        }
        sq.close();
        return list;
    }

    public String getGrowthDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, new String[]{"date"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public float getGrowthWeight(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, new String[]{"weight"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        float v = 0; if (c.moveToFirst()) v = c.getFloat(0); c.close(); return v;
    }

    public float getGrowthHeight(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, new String[]{"height"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        float v = 0; if (c.moveToFirst()) v = c.getFloat(0); c.close(); return v;
    }

    public String getGrowthStatus(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_GROWTH, new String[]{"nutrition_status"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = "normal"; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public List<int[]> getPendingVaccinationUpdates() {
        List<int[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor sq = db.query(TABLE_SYNC_QUEUE, null,
                "table_name='vaccinations'", null, null, null, null);
        while (sq.moveToNext()) {
            int recordId = sq.getInt(sq.getColumnIndexOrThrow("record_id"));
            Cursor c = db.query(TABLE_VACCINATION, null,
                    "id=?", new String[]{String.valueOf(recordId)}, null, null, null);
            if (c.moveToFirst()) {
                list.add(new int[]{
                    c.getInt(c.getColumnIndexOrThrow("child_id")),
                    recordId
                });
            }
            c.close();
        }
        sq.close();
        return list;
    }

    public String getVaccineName(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_VACCINATION, new String[]{"vaccine_name"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public String getVaccineDueDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_VACCINATION, new String[]{"due_date"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public String getVaccineGivenDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_VACCINATION, new String[]{"given_date"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public String getVaccineStatus(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_VACCINATION, new String[]{"status"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        String v = "pending"; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
    }

    public List<int[]> getPendingNutrition() {
        List<int[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor sq = db.query(TABLE_SYNC_QUEUE, null,
                "table_name='nutrition_records'",
                null, null, null, null);
        while (sq.moveToNext()) {
            int recordId = sq.getInt(sq.getColumnIndexOrThrow("record_id"));
            try {
                Cursor c = db.query(TABLE_NUTRITION, null,
                        "id=?", new String[]{String.valueOf(recordId)}, null, null, null);
                if (c.moveToFirst()) {
                    list.add(new int[]{
                        c.getInt(c.getColumnIndexOrThrow("child_id")),
                        recordId
                    });
                }
                c.close();
            } catch (Exception e) {
                android.util.Log.e("DB", "Nutrition sync error: " + e.getMessage());
            }
        }
        sq.close();
        return list;
    }

    public String getNutritionDate(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.query(TABLE_NUTRITION, new String[]{"date"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
            String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
        } catch (Exception e) { return ""; }
    }

    public String getNutritionMealType(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.query(TABLE_NUTRITION, new String[]{"meal_type"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
            String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
        } catch (Exception e) { return ""; }
    }

    public String getNutritionFood(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.query(TABLE_NUTRITION, new String[]{"food_items"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
            String v = ""; if (c.moveToFirst()) v = c.getString(0); c.close(); return v;
        } catch (Exception e) { return ""; }
    }

    public String getChildPhone(int childId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CHILDREN, new String[]{"phone"}, "id = ?", 
                           new String[]{String.valueOf(childId)}, null, null, null);
        String phone = "";
        if (c.moveToFirst()) phone = c.getString(0);
        c.close();
        return phone;
    }
}
