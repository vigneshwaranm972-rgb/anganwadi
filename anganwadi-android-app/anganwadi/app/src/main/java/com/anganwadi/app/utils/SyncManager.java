package com.anganwadi.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.anganwadi.app.database.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * SyncManager — Connects Android app to Django backend.
 *
 * SETUP:
 *   1. Change SERVER_URL to your PC IP (run `ipconfig` on Windows)
 *      Example: "http://10.100.48.52/:8000/api/"
 *   2. Add to build.gradle:
 *      implementation 'com.squareup.okhttp3:okhttp:4.12.0'
 *   3. Add to AndroidManifest.xml:
 *      <uses-permission android:name="android.permission.INTERNET"/>
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    // ── CHANGE THIS TO YOUR PC/SERVER IP ─────────────────────
    // While testing: use your PC IP address on same WiFi
    // After deploying: use Railway/Render public URL
    private static final String SERVER_URL = "http://127.0.0.1:8000/api/";

    private final Context context;
    private final DatabaseHelper db;
    private final SessionManager session;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public SyncManager(Context context) {
        this.context = context;
        this.db      = DatabaseHelper.getInstance(context);
        this.session = new SessionManager(context);
        this.server_url = session.getServerUrl();
    }

    private final String server_url;

    // ── Check network ─────────────────────────────────────────
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    // ── Schedule background sync with WorkManager ─────────────
    public void scheduleBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 1, TimeUnit.HOURS) // Every hour
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "AnganwadiBackgroundSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
        Log.d(TAG, "Background sync scheduled");
    }

    // ── Call this from WorkerDashboard onResume() ─────────────
    public void syncIfOnline(SyncCallback callback) {
        if (!isOnline()) {
            Log.d(TAG, "Offline — sync skipped");
            if (callback != null) callback.onResult(false, "No internet connection");
            return;
        }
        
        // Always attempt pull if online, or only if pending?
        // Let's do a full sync: Push first, then Pull.
        Log.d(TAG, "Online — initiating full sync");
        new SyncTask(callback).execute();
    }

    // ══════════════════════════════════════════════════════════
    //  LOGIN — Get JWT token from Django
    // ══════════════════════════════════════════════════════════
    public void loginAndGetToken(String phone, String password,
                                 String role, LoginCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("phone",    phone);
                    body.put("password", password);
                    body.put("role",     role);

                    Request req = new Request.Builder()
                            .url(server_url + "auth/login/")
                            .post(RequestBody.create(body.toString(), JSON))
                            .build();

                    try (Response res = client.newCall(req).execute()) {
                        if (res.isSuccessful() && res.body() != null) {
                            JSONObject json = new JSONObject(res.body().string());
                            // Save token to SharedPreferences
                            String token = json.getString("access");
                            session.saveToken(token);
                            return token;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Login error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(String token) {
                if (callback != null) callback.onResult(token != null, token);
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════
    //  BULK SYNC TASK
    // ══════════════════════════════════════════════════════════
    private class SyncTask extends AsyncTask<Void, String, SyncResult> {
        final SyncCallback callback;
        SyncTask(SyncCallback cb) { this.callback = cb; }

        @Override
        protected SyncResult doInBackground(Void... voids) {
            SyncResult result = new SyncResult();
            try {
                // Build sync payload from local DB
                JSONObject payload = buildSyncPayload();

                // Get stored JWT token
                String token = session.getToken();

                // If no token — try auto login
                if (token == null || token.isEmpty()) {
                    publishProgress("Logging in to server...");
                    token = tryAutoLogin();
                    if (token == null) {
                        result.success = false;
                        result.message = "Login failed — check server URL and credentials";
                        return result;
                    }
                }

                // 1. PUSH local changes
                if (db.getPendingSyncCount() > 0) {
                    publishProgress("Uploading " + db.getPendingSyncCount() + " records...");
                    pushLocalChanges(token);
                }

                // 2. PULL updates from server
                publishProgress("Downloading updates from server...");
                pullServerUpdates(token);

                result.success  = true;
                result.message  = "Sync completed successfully";
                Log.d(TAG, "Full sync success");

            } catch (IOException e) {
                result.success = false;
                result.message = "Cannot reach server. Check WiFi or URL.";
                Log.e(TAG, "Network error: " + e.getMessage());
            } catch (JSONException e) {
                result.success = false;
                result.message = "Data error: " + e.getMessage();
                Log.e(TAG, "JSON error: " + e.getMessage());
            } catch (Exception e) {
                result.success = false;
                result.message = "Error: " + e.getMessage();
                Log.e(TAG, "General error: " + e.getMessage());
            }
            return result;
        }

        private void pushLocalChanges(String token) throws IOException, JSONException {
            JSONObject payload = buildSyncPayload();
            Request req = new Request.Builder()
                    .url(server_url + "sync/")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(RequestBody.create(payload.toString(), JSON))
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful()) {
                    db.clearSyncQueue();
                } else {
                    throw new IOException("Push failed: " + res.code());
                }
            }
        }

        private void pullServerUpdates(String token) throws IOException, JSONException {
            Request req = new Request.Builder()
                    .url(server_url + "sync/?center_id=" + session.getCenterId())
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject data = new JSONObject(res.body().string());
                    
                    // Update local tables from server response
                    if (data.has("children"))     db.syncChildrenFromServer(data.getJSONArray("children"));
                    if (data.has("attendance"))   db.syncAttendanceFromServer(data.getJSONArray("attendance"));
                    if (data.has("growth"))       db.syncGrowthFromServer(data.getJSONArray("growth"));
                    if (data.has("vaccinations")) db.syncVaccinationsFromServer(data.getJSONArray("vaccinations"));
                }
            }
        }

        @Override
        protected void onPostExecute(SyncResult result) {
            if (callback != null) callback.onResult(result.success, result.message);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Build sync payload from local SQLite database
    // ══════════════════════════════════════════════════════════
    private JSONObject buildSyncPayload() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("device_id",   session.getDeviceId(context));
        payload.put("device_type", "app");
        payload.put("center_id",   session.getCenterId());

        // ── Attendance records pending sync
        JSONArray attendanceArr = new JSONArray();
        for (int[] record : db.getPendingAttendance()) {
            JSONObject a = new JSONObject();
            a.put("child",      record[0]);
            a.put("date",       db.getAttendanceDate(record[1]));
            a.put("status",     db.getAttendanceStatus(record[1]));
            a.put("marked_by",  session.getUserName());
            a.put("source",     "app");
            attendanceArr.put(a);
        }
        payload.put("attendance", attendanceArr);

        // ── Growth records pending sync
        JSONArray growthArr = new JSONArray();
        for (int[] record : db.getPendingGrowth()) {
            JSONObject g = new JSONObject();
            g.put("child",            record[0]);
            g.put("date",             db.getGrowthDate(record[1]));
            g.put("weight",           db.getGrowthWeight(record[1]));
            g.put("height",           db.getGrowthHeight(record[1]));
            g.put("nutrition_status", db.getGrowthStatus(record[1]));
            g.put("source",           "app");
            growthArr.put(g);
        }
        payload.put("growth", growthArr);

        // ── Vaccination updates pending sync
        JSONArray vaccArr = new JSONArray();
        for (int[] record : db.getPendingVaccinationUpdates()) {
            JSONObject v = new JSONObject();
            v.put("child",        record[0]);
            v.put("vaccine_name", db.getVaccineName(record[1]));
            v.put("due_date",     db.getVaccineDueDate(record[1]));
            v.put("given_date",   db.getVaccineGivenDate(record[1]));
            v.put("status",       db.getVaccineStatus(record[1]));
            vaccArr.put(v);
        }
        payload.put("vaccinations", vaccArr);

        // ── Nutrition records pending sync
        JSONArray nutritionArr = new JSONArray();
        for (int[] record : db.getPendingNutrition()) {
            JSONObject n = new JSONObject();
            n.put("child",      record[0]);
            n.put("date",       db.getNutritionDate(record[1]));
            n.put("meal_type",  db.getNutritionMealType(record[1]));
            n.put("food_items", db.getNutritionFood(record[1]));
            n.put("given_by",   session.getUserName());
            nutritionArr.put(n);
        }
        payload.put("nutrition", nutritionArr);

        return payload;
    }

    // ══════════════════════════════════════════════════════════
    //  Auto login using saved credentials
    // ══════════════════════════════════════════════════════════
    private String tryAutoLogin() {
        try {
            JSONObject body = new JSONObject();
            body.put("phone",    session.getUserPhone());
            body.put("password", session.getSavedPassword());
            body.put("role",     session.getUserRole());

            Request req = new Request.Builder()
                    .url(server_url + "auth/login/")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response res = client.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONObject json = new JSONObject(res.body().string());
                    String token = json.getString("access");
                    session.saveToken(token);
                    return token;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Auto login failed: " + e.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  TEST CONNECTION — call this to verify server is reachable
    // ══════════════════════════════════════════════════════════
    public void testConnection(SyncCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            String message = "";

            @Override
            protected Boolean doInBackground(Void... v) {
                try {
                    Request req = new Request.Builder()
                            .url(server_url + "health/")
                            .get()
                            .build();
                    try (Response res = client.newCall(req).execute()) {
                        if (res.isSuccessful()) {
                            message = "Server connected! " + server_url;
                            return true;
                        } else {
                            message = "Server returned error " + res.code();
                            return false;
                        }
                    }
                } catch (IOException e) {
                    message = "Cannot reach server.\nURL: " + server_url +
                            "\nCheck WiFi and server IP address.";
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) callback.onResult(success, message);
            }
        }.execute();
    }

    // ── Callbacks ─────────────────────────────────────────────
    public interface SyncCallback { void onResult(boolean success, String message); }
    public interface LoginCallback { void onResult(boolean success, String token); }

    // ── Result holder ─────────────────────────────────────────
    private static class SyncResult {
        boolean success = false;
        String  message = "";
        int     synced  = 0;
    }
}
