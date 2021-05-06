package com.epl.netweather;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

public class BandwidthActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //super.onCreate();
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, BandwidthActivity::class.java), 0)


//        Intent notificationIntent = new Intent(this, BandwidthActivity.class);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                notificationIntent, 0);

//        Notification notification = new NotificationCompat.Builder(this)
//       //         .setSmallIcon(R.mipmap.app_icon)
//                .setContentTitle("My Awesome App")
//                .setContentText("Doing some work...")
//                .setContentIntent(pendingIntent).build();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(new Intent(this, BandwidthService.class));
        }
        else{
            startService(new Intent(this, BandwidthService.class));
        }
    }
}
