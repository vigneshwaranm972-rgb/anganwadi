package com.anganwadi.app.models;

public class NutritionRecord {
    private int id, childId;
    private String childName, date, mealType, foodItems, quantity, remarks, givenBy;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public int getChildId() { return childId; } public void setChildId(int c) { this.childId = c; }
    public String getChildName() { return childName; } public void setChildName(String n) { this.childName = n; }
    public String getDate() { return date; } public void setDate(String d) { this.date = d; }
    public String getMealType() { return mealType; } public void setMealType(String m) { this.mealType = m; }
    public String getFoodItems() { return foodItems; } public void setFoodItems(String f) { this.foodItems = f; }
    public String getQuantity() { return quantity; } public void setQuantity(String q) { this.quantity = q; }
    public String getRemarks() { return remarks; } public void setRemarks(String r) { this.remarks = r; }
    public String getGivenBy() { return givenBy; } public void setGivenBy(String g) { this.givenBy = g; }
}
