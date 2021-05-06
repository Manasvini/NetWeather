package com.epl.netweather;

import android.util.Log;

import java.util.HashMap;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class BandwidthScheduler {
    private String TAG = "BandwidthScheduler";
    private UsageMonitor usageMonitor;
    private HashMap<String, Double> appRequirements = new HashMap<>();


    public synchronized  void setAppRequirements(HashMap<String, Double> appRequirements){
        this.appRequirements = appRequirements;
    }

    public void setUsageMonitor(UsageMonitor usageMonitor){
        this.usageMonitor = usageMonitor;
    }

    public synchronized HashMap<String, Double> computeAllocations(double totalBandwidth){
        //Log.i(TAG, " in compute allocations");
        long startTime = System.nanoTime();

        usageMonitor.computeUsages();
        HashMap<String, UsageMonitor.AppUsageInfo> usages = usageMonitor.getAllAppUsageInfo();

        SortedSet<UsageMonitor.AppUsageInfo> usageToRequirementRatio = new TreeSet<>();
        //allocations.clear();
        HashMap<String, Double> allocations  = new HashMap<>();
        Log.i(TAG, "have " + usages.size() + " apps to compute allocations for potentially");
        double sum = 0.0;
        for(UsageMonitor.AppUsageInfo app: usages.values()){
           if(appRequirements.containsKey(app.getAppName())){
                sum += app.getNormalizedUsage();
           }
           else{
               double req = new Random().nextDouble() * 5;
               appRequirements.put(app.getAppName(), req);
               app.setValue(app.getNormalizedUsage() / (sum * req));
               usageToRequirementRatio.add(app);
           }
        }


        for(UsageMonitor.AppUsageInfo app: usages.values()){
            if(appRequirements.containsKey(app.getAppName())) {
                double req = appRequirements.get(app.getAppName());
                app.setValue(app.getNormalizedUsage() / (sum * req));
                usageToRequirementRatio.add(app);
            }

        }

        double allocatedBwSoFar = 0.0;


        int numScheduled = 0;
        for (UsageMonitor.AppUsageInfo entry : usageToRequirementRatio) {

            String key = entry.getAppName();

            if (appRequirements.containsKey(key)) {
                double req = appRequirements.get(key);

                if (req + allocatedBwSoFar >= totalBandwidth) {
                    allocations.put(key, (totalBandwidth - allocatedBwSoFar));
                    //Log.i(TAG, "app " + key + " has allocation " + req);
                    ++numScheduled;
                    break;
                } else {
                    allocations.put(key, req);
                    allocatedBwSoFar += req;
                    ++numScheduled;
                }
            }


            //Log.i(TAG, "app " + value + " has allocation " + allocations.get(key));
        }

        long endTime = System.nanoTime();
        Log.i(TAG, "input num apps = " + usageToRequirementRatio.size() + " num apps scheduled = " + numScheduled + " scheduler took " + (endTime - startTime)/1000 + " us.");
        return allocations;
    }

}
