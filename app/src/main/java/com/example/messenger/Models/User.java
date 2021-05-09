package com.example.messenger.Models;

public class User {
    private String phoneNumber,name,uid,profileImageUrl;

    public User() {

    }

    public User(String phoneNumber, String name, String uid, String profileImageUrl) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.uid = uid;
        this.profileImageUrl = profileImageUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public String getProfileImage() {
        return profileImageUrl;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setProfileImage(String profileImage) {
        this.profileImageUrl = profileImage;
    }
}
