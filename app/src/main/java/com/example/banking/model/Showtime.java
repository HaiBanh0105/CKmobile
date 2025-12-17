package com.example.banking.model;

import java.util.List;

public class Showtime {
    private String showtimeId;
    private String movieId;
    private String cinemaId;
    private String roomId;
    private String date;
    private String time;
    private int price;
    private List<String> bookedSeats;

    public Showtime() {
    }

    public Showtime(String showtimeId, String movieId, String cinemaId, String roomId, String date, String time, int price, List<String> bookedSeats) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.cinemaId = cinemaId;
        this.roomId = roomId;
        this.date = date;
        this.time = time;
        this.price = price;
        this.bookedSeats = bookedSeats;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(String cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public List<String> getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(List<String> bookedSeats) {
        this.bookedSeats = bookedSeats;
    }
}

