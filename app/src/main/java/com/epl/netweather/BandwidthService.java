package com.epl.netweather;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class BandwidthService extends Service {


    private String TAG = "BandwidthService";

    private UsageMonitor usageMonitor;
    private BandwidthManager bandwidthManager;


    private HashMap<Integer,String> mAppIds;
    private ArrayList<BandwidthScheduleCallback> mCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;

        //return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy(){
        mCallbacks.clear();
        super.onDestroy();
    }

    @Override
    public  void onCreate(){
        Log.i(TAG, "creating");

        super.onCreate();
       // startForeground(1, new Notification());
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
          //  startForegroundService(new Intent(this, BandwidthService.class));
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
        else{
            startService(new Intent(this, BandwidthService.class));
        }
        mCallbacks = new ArrayList<>();
        mAppIds = new HashMap<>();
        usageMonitor = new UsageMonitor((UsageStatsManager)getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE));
        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        bandwidthManager = new BandwidthManager(getApplicationContext(), usageMonitor, mHandler);

    }

    private final BandwidthForecaster.Stub mBinder = new BandwidthForecaster.Stub() {
        @Override
        public List<BandwidthInfo> getBandwidthSchedule(String appname) throws RemoteException {
            return bandwidthManager.getAllocation(appname);
        }

        @Override
        public void registerApp(String appName, BandwidthScheduleCallback cb){
            Log.i(TAG, "got app" + appName);
            mCallbacks.add(cb);
            mAppIds.put(mCallbacks.size()-1, appName);
        }

        @Override
        public void unregisterApp(String appName){
            for(int key : mAppIds.keySet()){
                if(mAppIds.get(key) == appName){
                    mCallbacks.remove(key);
                    mAppIds.remove(key);
                }
            }
        }
        @Override
        public void specifyRequirement(String appName, double requirement){
            bandwidthManager.registerAppRequirement(appName, requirement);
        }

        @Override
        public void addRoute(String route) throws RemoteException{
         //
            Log.i(TAG, "route is "+route );
            bandwidthManager.getBandwidthInfoForRoute(route);
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {



        return mBinder;
    }


    private class ServiceHandler extends Handler implements BandwidthManager.BandwidthUpdateDispatcher {

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void onBandwidthChange(String appname){
            Log.i(TAG, "notifying app " + appname);
            Message message = obtainMessage();
            for(int appid: mAppIds.keySet()) {
                if (appname.equals(mAppIds.get(appid))) {
                    message.arg1 = appid;
                    mHandler.sendMessage(message);
                    break;
                }
            }
        }

        @Override
        public void handleMessage(Message message){
            int appId = message.arg1;
            String appName = mAppIds.get(appId);
            try{
                Log.i(TAG, "sending cb to " + appName);
                List<BandwidthInfo> infos = bandwidthManager.getAllocation(appName);
                if(infos != null && mCallbacks != null && mCallbacks.size() >= appId){
                    Log.i(TAG, "have " + infos.size() + "values for " + appName);
                    mCallbacks.get(appId).onScheduleAvailable(infos);
                }
            }
            catch (RemoteException ex){
                ex.printStackTrace();
            }
        }

    }
}
