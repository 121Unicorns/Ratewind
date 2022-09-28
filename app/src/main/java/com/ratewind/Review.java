package com.ratewind;

import java.io.Serializable;

public class Review implements Serializable {
    String phoneNumber;
    String name;
    String ratedBy;
    String ratedName;
    String review;
    float rating;
    String ratingDate;
    String userType;

    public Review() {}

    public Review(String name, String phoneNumber, String ratedBy, String ratedName, String review, float rating, String ratingDate, String userType) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.ratedBy = ratedBy;
        this.ratedName = ratedName;
        this.review = review;
        this.rating = rating;
        this.ratingDate = ratingDate;
        this.userType = userType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRatedBy() {
        return ratedBy;
    }

    public void setRatedBy(String ratedBy) {
        this.ratedBy = ratedBy;
    }

    public String getRatedName() {
        return ratedName;
    }

    public void setRatedName(String ratedName) {
        this.ratedName = ratedName;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getRatingDate() {
        return ratingDate;
    }

    public void setRatingDate(String ratingDate) {
        this.ratingDate = ratingDate;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}