package com.anganwadi.app.models;

public class User {
    private int id, childId;
    private String name, phone, role, centerId;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getPhone(){return phone;} public void setPhone(String p){this.phone=p;}
    public String getRole(){return role;} public void setRole(String r){this.role=r;}
    public String getCenterId(){return centerId;} public void setCenterId(String c){this.centerId=c;}
    public int getChildId(){return childId;} public void setChildId(int c){this.childId=c;}
}
