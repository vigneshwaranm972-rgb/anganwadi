package com.anganwadi.app.models;

public class Vaccination {
    private int id, childId; private String vaccineName, dueDate, givenDate, status;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getChildId(){return childId;} public void setChildId(int c){this.childId=c;}
    public String getVaccineName(){return vaccineName;} public void setVaccineName(String v){this.vaccineName=v;}
    public String getDueDate(){return dueDate;} public void setDueDate(String d){this.dueDate=d;}
    public String getGivenDate(){return givenDate;} public void setGivenDate(String d){this.givenDate=d;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
}
