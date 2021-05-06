package com.epl.netweather;

import android.os.Parcel;
import android.os.Parcelable;



public class BandwidthInfo implements Parcelable {
    private double bandwidthKbps;
    private double duration;

    public static final Parcelable.Creator<BandwidthInfo> CREATOR = new Parcelable.Creator<BandwidthInfo> (){
        public BandwidthInfo createFromParcel(Parcel input){
            return new BandwidthInfo(input);
        }
        public BandwidthInfo[] newArray(int size){
            return new BandwidthInfo[size];
        }
    };

    private  BandwidthInfo(Parcel in){
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in){
        bandwidthKbps = in.readDouble();
        duration = in.readDouble();
    }

    public BandwidthInfo(double bandwidthKbps, double duration){
        this.bandwidthKbps = bandwidthKbps;
        this.duration = duration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(this.bandwidthKbps);
        parcel.writeDouble(this.duration);

    }

    public double getBandwidthKbps() {
        return bandwidthKbps;
    }

    public double getDuration() {
        return duration;
    }

    public void setBandwidthKbps(double bandwidthKbps) {
        this.bandwidthKbps = bandwidthKbps;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }
}
