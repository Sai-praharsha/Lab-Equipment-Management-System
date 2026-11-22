package com.java.system;

public class Student extends User {
    private String department;

    public Student(int id, String name, String department) {
        super(id, name);
        this.department = department;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public void showProfile() {
        System.out.println("Role: Student-| ID: " + getId() + " - Name: " + getName()
                + " - Department: " + department);
    }
}
