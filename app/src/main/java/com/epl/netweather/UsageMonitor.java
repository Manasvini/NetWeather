package com.epl.netweather;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.util.Log;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class UsageMonitor {

    private String TAG = "UsageMonitor";

    private UsageStatsManager usageStatsManager;

    private HashMap<String, AppUsageInfo> appUsageInfo = new HashMap<>();

    ScheduledThreadPoolExecutor threadPoolExecutor;
    int timePeriodSeconds = 600;

    public UsageMonitor(UsageStatsManager usageStatsManager){
        this.usageStatsManager = usageStatsManager;
        Log.i(TAG, "init usage monitor");
        //threadPoolExecutor = new ScheduledThreadPoolExecutor(timePeriodSeconds);
        //threadPoolExecutor.scheduleAtFixedRate(new UsageStatsTask(), 0, timePeriodSeconds, TimeUnit.SECONDS);
    }

    class AppUsageInfo implements Comparable {
        long usageTime;
        long timePeriod;
        String appName;
        double value;
        public AppUsageInfo(long usageTime, long timePeriod, String appName){
            this.usageTime = usageTime;
            this.timePeriod = timePeriod;
            this.appName = appName;
        }
        public double getNormalizedUsage(){
            return (usageTime * 1.0) / (timePeriod * 1.0);
        }

        public String getAppName(){
            return  appName;
        }

        public void setValue(double value){
            this.value = value;
        }

        public double getValue(){
            return value;
        }
        public int compareTo(AppUsageInfo o1){
            if(getValue() < o1.getValue()){
                return -1;
            }
            else if(getValue() > o1.getValue()){
                return 1;
            }
            return appName.compareTo(o1.appName);
        }
        @Override
        public int compareTo(Object o) {
            return compareTo((AppUsageInfo)o);
        }
    }


    public void computeUsages() {
        List<AppUsageInfo> appInfo = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        Log.i(TAG, "in compute usage");
            List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 3600 * 24 * 1000, currentTime);
            for (UsageStats stat : stats) {
              try{
                //  Log.i(TAG, "usage=" + stat.getPackageName() + ", time visible=" + stat.getTotalTimeVisible() / 1000 + " time in fg =" + stat.getTotalTimeInForeground() / 1000 + ", ts=" + stat.getFirstTimeStamp());
                appInfo.add(new AppUsageInfo(stat.getTotalTimeVisible() / 1000, 24 * 3600, stat.getPackageName()));
              } catch(Exception ex){
                  ex.printStackTrace();
              }
        }
        updateUsage(appInfo);
    }
    private synchronized  void updateUsage(List<AppUsageInfo> currentAppUsage){
        appUsageInfo.clear();
        for(AppUsageInfo appInfo: currentAppUsage){
            appUsageInfo.put(appInfo.getAppName(), appInfo);
        }
    }


    public synchronized  HashMap<String, AppUsageInfo> getAllAppUsageInfo(){
        return appUsageInfo;
    }

    public synchronized  AppUsageInfo getAppUsageInfo(String appName){
        computeUsages();
        if(appUsageInfo.containsKey(appName)){
            return appUsageInfo.get(appName);
        }
        return null;
    }

}
