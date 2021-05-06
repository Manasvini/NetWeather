// BandwidthScheduleCallback.aidl
package com.epl.netweather;
parcelable BandwidthInfo;
// Declare any non-default types here with import statements

oneway interface BandwidthScheduleCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   void onScheduleAvailable(in List<BandwidthInfo> bandwidthInfo);
}
