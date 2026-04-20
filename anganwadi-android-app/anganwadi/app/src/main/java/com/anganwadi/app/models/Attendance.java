package com.anganwadi.app.models;

public class Attendance {
    private int id, childId; private String date, status, markedBy;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getChildId(){return childId;} public void setChildId(int c){this.childId=c;}
    public String getDate(){return date;} public void setDate(String d){this.date=d;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getMarkedBy(){return markedBy;} public void setMarkedBy(String m){this.markedBy=m;}
}
