package com.example.pch.capstone_design;

/**
 * Created by 15U560 on 2017-10-19.
 */

public class Beacon {
    private String address;
    private int rssi;
    private String value;

    public Beacon(String address, int rssi, String value) {
        this.address = address;
        this.rssi = rssi;
        this.value = value;
    }

    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    public String getNow() {
        return value;
    }
}
