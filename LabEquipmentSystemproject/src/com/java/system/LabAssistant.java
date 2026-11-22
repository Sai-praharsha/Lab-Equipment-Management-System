package com.java.system;

public class LabAssistant extends User {
    public LabAssistant(int id,String name) {
        super(id, name);
    }

    @Override
    public void showProfile() {
        System.out.println("Role: Lab assistant | ID: " + getId() + " | Name: " + getName());
    }
}
