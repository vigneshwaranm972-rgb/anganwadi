package com.anganwadi.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public class SessionManager {
    private static final String PREF = "AnganwadiSession";
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ── Server URL Configuration ─────────────────────────────
    public void saveServerUrl(String url) {
        if (!url.endsWith("/")) url += "/";
        editor.putString("serverUrl", url);
        editor.apply();
    }

    public String getServerUrl() {
        // Default to a typical local emulator IP if not set
        return prefs.getString("serverUrl", "http://10.0.2.2:8000/api/");
    }

    // ── Login session ─────────────────────────────────────────
    public void saveSession(int userId, String name, String role,
                             String centerId, int childId) {
        editor.putBoolean("isLoggedIn", true);
        editor.putInt("userId",         userId);
        editor.putString("userName",    name);
        editor.putString("userRole",    role);
        editor.putString("centerId",    centerId);
        editor.putInt("childId",        childId);
        editor.apply();
    }

    // ── Save phone + password for auto re-login during sync ───
    public void saveCredentials(String phone, String password) {
        editor.putString("userPhone",    phone);
        editor.putString("userPassword", password);
        editor.apply();
    }

    // ── JWT Token from Django ─────────────────────────────────
    public void saveToken(String token) {
        editor.putString("jwtToken", token);
        editor.apply();
    }

    public String getToken()         { return prefs.getString("jwtToken", null); }
    public boolean isLoggedIn()      { return prefs.getBoolean("isLoggedIn", false); }
    public int getUserId()           { return prefs.getInt("userId", -1); }
    public String getUserName()      { return prefs.getString("userName", ""); }
    public String getUserRole()      { return prefs.getString("userRole", ""); }
    public String getCenterId()      { return prefs.getString("centerId", "AWC_001"); }
    public int getChildId()          { return prefs.getInt("childId", 1); }
    public String getUserPhone()     { return prefs.getString("userPhone", ""); }
    public String getSavedPassword() { return prefs.getString("userPassword", ""); }

    // ── Unique device ID for sync tracking ────────────────────
    public String getDeviceId(android.content.Context ctx) {
        String stored = prefs.getString("deviceId", null);
        if (stored != null) return stored;
        String id = Settings.Secure.getString(
                ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null) id = "device_" + System.currentTimeMillis();
        editor.putString("deviceId", id);
        editor.apply();
        return id;
    }

    public void logout() {
        // Keep deviceId and credentials for re-login
        // Clear only session data
        editor.putBoolean("isLoggedIn", false);
        editor.remove("jwtToken");
        editor.remove("userId");
        editor.remove("userName");
        editor.remove("userRole");
        editor.remove("centerId");
        editor.remove("childId");
        editor.apply();
    }
}
