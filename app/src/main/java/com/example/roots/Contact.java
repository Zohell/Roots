package com.example.roots;

public class Contact {
    private String name;
    private String phone;
    private boolean isRegistered;
    private String uid;

    public Contact(String name, String phone, boolean isRegistered, String uid) {
        this.name = name;
        this.phone = phone;
        this.isRegistered = isRegistered;
        this.uid = uid;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public boolean isRegistered() { return isRegistered; }
    public String getUid() { return uid; }
}