package com.example.banking.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Banner implements Serializable {

    @SerializedName("age")
    private String age;

    @SerializedName("genre")
    private String genre;

    @SerializedName("image")
    private String image;

    @SerializedName("name")
    private String name;

    @SerializedName("time")
    private String time;

    @SerializedName("year")
    private String year;

    public Banner() {}

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
