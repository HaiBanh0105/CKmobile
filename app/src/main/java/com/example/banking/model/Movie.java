package com.example.banking.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Movie implements Serializable {
    @SerializedName("MovieId")
    private String movieId;

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    @SerializedName("Title")
    private String title;

    @SerializedName("Description")
    private String description;

    @SerializedName("Poster")
    private String poster;

    @SerializedName("Time")
    private String time;

    @SerializedName("Year")
    private int year;

    @SerializedName("age")
    private String age;

    @SerializedName("price")
    private int price;

    @SerializedName("Trailer")
    private String trailer;

    @SerializedName("Genre")
    private List<String> genre;

    @SerializedName("Casts")
    private List<Cast> casts;

    public Movie() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public List<String> getGenre() {
        return genre;
    }

    public void setGenre(List<String> genre) {
        this.genre = genre;
    }

    public List<Cast> getCasts() {
        return casts;
    }

    public void setCasts(List<Cast> casts) {
        this.casts = casts;
    }

    /* ===================== INNER CLASS ===================== */

    public static class Cast implements Serializable {

        @SerializedName("Actor")
        private String actor;

        @SerializedName("PicUrl")
        private String picUrl;

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public String getPicUrl() {
            return picUrl;
        }

        public void setPicUrl(String picUrl) {
            this.picUrl = picUrl;
        }
    }
}
