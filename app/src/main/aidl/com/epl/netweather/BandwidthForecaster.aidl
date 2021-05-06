// BandwidthForecaster.aidl
package com.epl.netweather;

import com.epl.netweather.BandwidthScheduleCallback;
// Declare any non-default types here with import statements

interface BandwidthForecaster {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    List<BandwidthInfo> getBandwidthSchedule(String appname);

    void registerApp(String appName, BandwidthScheduleCallback cb);

    void specifyRequirement(String appName, double requirement);

    void addRoute(String route);

    void unregisterApp(String appName);

}
