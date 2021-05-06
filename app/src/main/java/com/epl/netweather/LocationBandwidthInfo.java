package com.epl.netweather;

public class LocationBandwidthInfo {
    private LatLong latLong;
    private double bandwidth;

    public double getBandwidth() {
        return bandwidth;
    }



    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public LatLong getLatLong() {
        return latLong;
    }

    public void setLatLong(LatLong latLong) {
        this.latLong = latLong;
    }
}
