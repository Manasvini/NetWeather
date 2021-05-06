package com.epl.netweather;

import android.content.Context;

import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;



public class DeviceLocationManager {


    private String TAG ="DeviceLocationManager";

    private FusedLocationProviderClient mFusedLocationClient;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds
    LocationCallback cb;

    public DeviceLocationManager(Context context, LocationCallback cb){

        mFusedLocationClient= LocationServices.getFusedLocationProviderClient(context);

        this.cb = cb;
        startLocationUpdates();

    }


    private void startLocationUpdates() {
        LocationRequest locationRequest;
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);


        mFusedLocationClient.requestLocationUpdates(locationRequest, cb, Looper.getMainLooper());

    }


}