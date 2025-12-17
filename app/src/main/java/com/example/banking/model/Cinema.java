package com.example.banking.model;

import java.io.Serializable;

public class Cinema implements Serializable {
    private String cinemaId;
    private String name;
    private String logo;
    private String address;

    public Cinema() {
    }

    public Cinema(String cinemaId, String name, String logo, String address) {
        this.cinemaId = cinemaId;
        this.name = name;
        this.logo = logo;
        this.address = address;
    }


    public String getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(String cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

