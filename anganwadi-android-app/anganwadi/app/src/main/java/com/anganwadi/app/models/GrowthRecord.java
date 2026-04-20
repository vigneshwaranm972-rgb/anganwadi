package com.anganwadi.app.models;

public class GrowthRecord {
    private int id, childId; private String date, nutritionStatus, remarks;
    private float weight, height, muac;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getChildId(){return childId;} public void setChildId(int c){this.childId=c;}
    public String getDate(){return date;} public void setDate(String d){this.date=d;}
    public float getWeight(){return weight;} public void setWeight(float w){this.weight=w;}
    public float getHeight(){return height;} public void setHeight(float h){this.height=h;}
    public float getMuac(){return muac;} public void setMuac(float m){this.muac=m;}
    public String getNutritionStatus(){return nutritionStatus;} public void setNutritionStatus(String s){this.nutritionStatus=s;}
    public String getRemarks(){return remarks;} public void setRemarks(String r){this.remarks=r;}
}
