package com.anganwadi.app.models;

// ── Child Model ──────────────────────────────────────────
public class Child {
    private int id; private String name, dob, gender;
    private String motherName, fatherName, phone, address, centerId, photoPath;
    private int fingerprintId; private boolean isSynced;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDob() { return dob; } public void setDob(String dob) { this.dob = dob; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public String getMotherName() { return motherName; } public void setMotherName(String n) { this.motherName = n; }
    public String getFatherName() { return fatherName; } public void setFatherName(String n) { this.fatherName = n; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public String getCenterId() { return centerId; } public void setCenterId(String centerId) { this.centerId = centerId; }
    public String getPhotoPath() { return photoPath; } public void setPhotoPath(String p) { this.photoPath = p; }
    public int getFingerprintId() { return fingerprintId; } public void setFingerprintId(int id) { this.fingerprintId = id; }

    public int getAgeInMonths() {
        try {
            String[] parts = dob.split("-");
            java.util.Calendar dob = java.util.Calendar.getInstance();
            dob.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])-1, Integer.parseInt(parts[2]));
            java.util.Calendar now = java.util.Calendar.getInstance();
            int years = now.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR);
            int months = now.get(java.util.Calendar.MONTH) - dob.get(java.util.Calendar.MONTH);
            return years * 12 + months;
        } catch (Exception e) { return 0; }
    }

    public String getAgeString() {
        int months = getAgeInMonths();
        if (months < 12) return months + " months";
        return (months / 12) + " yr " + (months % 12) + " mo";
    }
}
