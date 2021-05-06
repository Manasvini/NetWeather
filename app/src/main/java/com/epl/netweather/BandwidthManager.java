package com.epl.netweather;

import android.content.ContentValues;
import android.content.Context;

import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;


import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

public class BandwidthManager {
    String TAG = "BandwidthManager";

    DeviceLocationManager mDeviceLocationManager;
    DeviceLocationChangeHandler deviceLocationChangeHandler;

    BandwidthResponseHandler locationHandler;
    BandwidthClient mClient;

    UsageMonitor usageMonitor;

    List<LocationBandwidthInfo> bandwidthInfoList = new ArrayList<>();
    List<LocationBandwidthInfo> oldBandwidthInfoList = new ArrayList<>();
    List<LatLong> mRoute;


    Location mOldLoc;
    long timeOfLocation = System.currentTimeMillis();
    long MILLIS_TO_SEC = 1000;
    double mSpeed  = 10.0;
    int mPositionUpdateInterval = 1;
    long epochSeconds = 1000;
    int currentPositionIdx = 0;

    double distanceTravledSoFar = 0.0;
    double oldDistanceTraveledSoFar = 0.0;
    double thresholdDifference = 0.0;
    double bwChangeThreshold = 0;
    int incr = 10;
    int decr = 20;


    HashMap<String, Double> appRequirements = new HashMap<>();
    HashMap<String, List<BandwidthInfo>> allocationsByDuration = new HashMap<>();


    BandwidthUpdateDispatcher dispatcher;

    final Handler bandwidthUpdateHandler = new Handler();
    final Handler routeInfoUpdateHandler = new Handler();
    final Handler positionUpdateHandler = new Handler();
    BandwidthUpdateTask bandwidthUpdateTask = new BandwidthUpdateTask();
    RouteInfoFetchTask routeInfoFetchTask = new RouteInfoFetchTask();
    PositionUpdateTask positionUpdateTask = new PositionUpdateTask();
    BandwidthScheduler bandwidthScheduler = new BandwidthScheduler();

    public interface BandwidthUpdateDispatcher{
        void onBandwidthChange(String appName);
    }

    public BandwidthManager(Context context, UsageMonitor usageMonitor, BandwidthUpdateDispatcher handler){
        Log.i(TAG, "init manager");
        locationHandler = new BandwidthResponseHandler();
        mClient = new BandwidthClient(context, locationHandler);

        deviceLocationChangeHandler = new DeviceLocationChangeHandler();
        mDeviceLocationManager = new DeviceLocationManager(context, deviceLocationChangeHandler);

        this.usageMonitor = usageMonitor;
        bandwidthScheduler.setUsageMonitor(usageMonitor);

        bandwidthInfoList = new ArrayList<>();
        mRoute = new ArrayList<>();

        this.dispatcher = handler;


    }

    private class PositionUpdateTask implements Runnable{
        @Override
        public synchronized  void run() {
            updatePosition();
            Log.i(TAG, "updated position to " + currentPositionIdx);
            if(currentPositionIdx< mRoute.size() - 1){
                positionUpdateHandler.postDelayed(this, mPositionUpdateInterval * MILLIS_TO_SEC);
            }
        }

    }
    public synchronized  void updatePosition(){
        double distanceToTravel = mSpeed * mPositionUpdateInterval;

      //  Log.i(TAG, "distance to travel= " + distanceToTravel + " and route has " + mRoute.size() + " and cur pos is " + currentPositionIdx + " pos diff " + Utils.getDistance(mRoute.get(currentPositionIdx), mRoute.get(currentPositionIdx + 1)));
        double curDist = 0;
        while(currentPositionIdx < mRoute.size() - 1 && (curDist < distanceToTravel)){
            curDist += Math.min(Utils.getDistance(mRoute.get(currentPositionIdx), mRoute.get(currentPositionIdx + 1)), distanceToTravel);
       //     Log.i(TAG, "curDist =" + curDist + " distance to travel = " + distanceToTravel + ", diff="+ Utils.getDistance(mRoute.get(currentPositionIdx), mRoute.get(currentPositionIdx + 1)));
            distanceTravledSoFar += curDist;


            if((distanceTravledSoFar - oldDistanceTraveledSoFar)  >= Utils.getDistance(mRoute.get(currentPositionIdx), mRoute.get(currentPositionIdx + 1))){
                ++currentPositionIdx;
                oldDistanceTraveledSoFar = distanceTravledSoFar;
                break;
            }
            if(Utils.getDistance(mRoute.get(currentPositionIdx), mRoute.get(currentPositionIdx + 1)) < distanceToTravel){
                ++currentPositionIdx;
            }
            else{
                break;
            }
            //Log.i(TAG, "distance is now "+ distanceTraveled);


        }
        Log.i(TAG, "cur pos is " + currentPositionIdx);

    }

    private class BandwidthUpdateTask implements Runnable{
        @Override
        public void run(){
            List<BandwidthInfo> bwInfos = calculateDurations();
            if(bwInfos.size() == 0){
                return;
            }
            prepareSchedule(bwInfos);

          // bandwidthUpdateHandler.postDelayed(this,epochSeconds * MILLIS_TO_SEC);
        }
        public synchronized  List<BandwidthInfo> calculateDurations(){
            Log.i(TAG, "calculate durations cur pos " + currentPositionIdx );

            ArrayList<BandwidthInfo> bwInfos = new ArrayList<>();

            for(int i = currentPositionIdx; i < mRoute.size() -1; ++i){
                double distance = Utils.getDistance(mRoute.get(i), mRoute.get(i + 1));
                Log.i(TAG, "distance=" + distance);
                if(mSpeed > 0 && (bandwidthInfoList.size() > ( i - currentPositionIdx))) {
                    double durationOfBw = distance / mSpeed;
                    Log.i(TAG, "duration is" + durationOfBw);
                    BandwidthInfo info = new BandwidthInfo(bandwidthInfoList.get(i - currentPositionIdx).getBandwidth(), durationOfBw);
                    bwInfos.add(info);
                }
            }
            return bwInfos;
        }

    }

    private class RouteInfoFetchTask implements Runnable {
        @Override
        public void run(){
            oldBandwidthInfoList = bandwidthInfoList;
            bandwidthInfoList.clear();
            if(mRoute.size() > 0) {
                Log.i(TAG, "route now has " + mRoute.size() + " points");
                mClient.getAnnotatedRoute(mRoute);
                adjustInterval();

                routeInfoUpdateHandler.postDelayed(routeInfoFetchTask, epochSeconds * MILLIS_TO_SEC);
            }
        }
    }


    synchronized  void adjustInterval(){
        if(oldBandwidthInfoList == null){
            return;
        }
        else{
            double diff = 0.0;
            Log.i(TAG, "bw info size" + bandwidthInfoList.size() + " old info size " + oldBandwidthInfoList.size());
            if(oldBandwidthInfoList.size() == 0){
                return;
            }

            int j = oldBandwidthInfoList.size() - bandwidthInfoList.size();
            for(int i = 0; i < bandwidthInfoList.size(); ++i){


                double curDiff = bandwidthInfoList.get(i).getBandwidth() - oldBandwidthInfoList.get(j+i).getBandwidth();
                diff += curDiff * curDiff;
            }
            Log.i(TAG, "diff = " + Math.sqrt(diff));

            Log.i(TAG, "incr = " + incr + " decr= " + decr);
            if(Math.sqrt(diff)/bandwidthInfoList.size() < thresholdDifference){

                epochSeconds += incr;
            }
            else {
                epochSeconds = Math.max(10, epochSeconds/2);
            }
        }
    }

    public synchronized  void prepareSchedule(List<BandwidthInfo> bwInfos){
        long prepStart = System.currentTimeMillis();
        //oldAllocationsByDuration = (HashMap<String, List<BandwidthInfo>>) allocationsByDuration.clone();
        Log.i(TAG, "got "+ bwInfos.size() + " bw infos to schedule");
        allocationsByDuration.clear();
        HashMap<String, Double> currentAllocations = new HashMap<>();
        for(int i = 0; i < bwInfos.size(); ++i){
            BandwidthInfo bwInfo = bwInfos.get(i);
            if(i == 0 || Math.abs(bwInfos.get(i).getBandwidthKbps() - bwInfos.get(i-1).getBandwidthKbps()) >=  bwChangeThreshold){
                Log.i(TAG, "cur total bw is " + bwInfo.getBandwidthKbps()+ " changed beyond threshold");
                currentAllocations = bandwidthScheduler.computeAllocations(bwInfo.getBandwidthKbps());
            }

            for(String app: currentAllocations.keySet()){
                Log.i(TAG, "total available =" + bwInfo.getBandwidthKbps() + " for app, " + app + " " + currentAllocations.get(app));
                if(bwInfo == null || currentAllocations.get(app) == null){
                    continue;
                }
                if(allocationsByDuration.containsKey(app)){
                    List<BandwidthInfo> bwInfoForApp = allocationsByDuration.get(app);
                    bwInfoForApp.add(new BandwidthInfo(currentAllocations.get(app), bwInfo.getDuration()));
                    allocationsByDuration.put(app, bwInfoForApp);
                }
                else{
                    List<BandwidthInfo> bwInfoForApp = new ArrayList<>();
                    bwInfoForApp.add(new BandwidthInfo(currentAllocations.get(app), bwInfo.getDuration()));
                    allocationsByDuration.put(app, bwInfoForApp);
                }

            }

        }
        long prepEnd = System.currentTimeMillis();
        Log.i(TAG, "schedule prep took " + (prepEnd - prepStart) + " ms  for epoch size " + epochSeconds + " and durations " + bwInfos.size() +  " for numapps " + allocationsByDuration.size()  );
        for(String app: allocationsByDuration.keySet()){
//            for(BandwidthInfo val: allocationsByDuration.get(app)){
//                Log.i(TAG,"app= " + app + " has " + val.getBandwidthKbps() + " allotted kbps");
//            }
            if(dispatcher==null){
                Log.i(TAG, "oops no handler");
            }
            dispatcher.onBandwidthChange(app);
        }
    }

    public void registerAppRequirement(String appName, double requirement){
        appRequirements.put(appName, requirement);
        bandwidthScheduler.setAppRequirements(appRequirements);
        Log.i(TAG, "got app " + appName + "req = " + requirement);
    }

    public synchronized List<BandwidthInfo> getAllocation(String appName){

        Log.i(TAG, "app " + appName + " has " +allocationsByDuration.get(appName)+ "  infors");
        int i =0;
        for(BandwidthInfo val: allocationsByDuration.get(appName)){
            Log.i(TAG + appName,  (i + currentPositionIdx) + "," + val.getBandwidthKbps() + "," + val.getDuration());
            ++i;
        }


        if(allocationsByDuration.containsKey(appName)){
            return allocationsByDuration.get(appName);
        }
        return null;
    }


    public synchronized  void getBandwidthInfoForRoute(String route){

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            bandwidthInfoList.clear();
            oldBandwidthInfoList.clear();
            currentPositionIdx = 0;

            allocationsByDuration.clear();
            LatLongList latLongList = objectMapper.readValue(route, LatLongList.class);
            mRoute = latLongList.getPoints();

            Log.i(TAG, "route is " + route);
            Log.i(TAG, "num latlngs= " + mRoute.size());

            routeInfoUpdateHandler.postDelayed(routeInfoFetchTask, 0);

            positionUpdateHandler.removeCallbacks(positionUpdateTask);
            positionUpdateHandler.postDelayed(positionUpdateTask, mPositionUpdateInterval * MILLIS_TO_SEC);

            mClient.getAnnotatedRoute(latLongList.getPoints());

        } catch (JsonProcessingException ex) {
            Log.i(TAG, ex.getMessage());
        }
    }
    private class BandwidthResponseHandler implements BandwidthClient.RequestCompleteCallback{

        public synchronized  void onRequestComplete(JSONObject response, boolean isFinal){
            try {

                //Log.i(TAG, "response is " + response.toString());
                JSONArray array = response.getJSONArray("annotationsByLocation");
                Log.i(TAG, "got "+ array.length() + " ann lists");
                for(int j = 0; j < array.length(); ++j) {
                    LatLong refLatlong = new LatLong();
                    refLatlong.setLongitude(array.getJSONObject(j).getJSONObject("refLocation").getDouble("x"));
                    refLatlong.setLatitude(array.getJSONObject(j).getJSONObject("refLocation").getDouble("y"));
                    LocationBandwidthInfo locationBandwidthInfo = new LocationBandwidthInfo();
                    locationBandwidthInfo.setLatLong(refLatlong);
                    double bw = 0.0;
                    JSONArray annotations = array.getJSONObject(j).getJSONArray("annotations");
                    Log.i(TAG, " for point " +array.getJSONObject(j).getJSONObject("refLocation").toString()  + " got " + annotations.length() + " bw values");
                    for (int i = 0; i < annotations.length(); ++i) {
                        JSONObject ann =
                                annotations.getJSONObject(i);
                        bw += Double.parseDouble(ann.getString("value"));
                    }

                    if (array.length() > 0) {
                        bw /= array.length();
                    }
                    locationBandwidthInfo.setBandwidth(bw);
                    bandwidthInfoList.add(locationBandwidthInfo);
                    Log.i(TAG, j + ":adding " + locationBandwidthInfo.getBandwidth());
                }
                if(isFinal){
                    Log.i(TAG, "got all annotations (total = " + bandwidthInfoList.size() + ")"+ "calculating new schedule");
                    bandwidthUpdateHandler.removeCallbacks(bandwidthUpdateTask);

                    bandwidthUpdateHandler.postDelayed(bandwidthUpdateTask, 0);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            catch (CancellationException e){
                e.printStackTrace();
            }

        }
    }

    private class DeviceLocationChangeHandler extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult result){

            Location newLoc = result.getLastLocation();
            Log.i(TAG, "location is" + "Latitude:" + newLoc.getLatitude() + ", Longitude:" + newLoc.getLongitude()  );
            long currentTime = System.currentTimeMillis();
            long timeDiff = (currentTime - timeOfLocation) / MILLIS_TO_SEC;

            LatLong latLong = new LatLong();
            if(mOldLoc == null){
                mOldLoc = newLoc;
            }
            latLong.setLatitude(mOldLoc.getLatitude());
            latLong.setLongitude(mOldLoc.getLongitude());
            LatLong latLong1 = new LatLong();
            latLong1.setLatitude(newLoc.getLatitude());
            latLong1.setLongitude(newLoc.getLongitude());
            mSpeed = Utils.getDistance(latLong, latLong1) / (double)timeDiff;

            double minDist = Double.MAX_VALUE;
            int minIdx = -1;
            Log.i(TAG, "route len = " + mRoute.size());
            for(int i = 0; i < mRoute.size(); ++i){
                LatLong routePoint = mRoute.get(i);
                double dist = Utils.getDistance(latLong1, routePoint);
                if(dist < minDist){
                    minDist = dist;
                    minIdx = i;
                }
            }
            Log.i(TAG, "closest poiint idx = " + minIdx);
            // starting point
            if(minIdx >=0){
                currentPositionIdx = minIdx;

            }

        }
    }




}
