package com.ratewind;

import java.io.Serializable;

public class User implements Serializable {
    String phoneNumber;
    String email;
    String name;
    String userType;
    String joinDate;

    public User (){}

    public User (String phoneNumber, String email, String name, String userType, String joinDate){
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.name = name;
        this.userType = userType;
        this.joinDate = joinDate;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getPhoneNumber() {return phoneNumber;}

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }
}
