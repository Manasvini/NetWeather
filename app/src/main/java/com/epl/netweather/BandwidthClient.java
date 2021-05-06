package com.epl.netweather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class BandwidthClient {
    RequestQueue queue;
    String url;
    String annotationName;
    Context context;
    RequestCompleteCallback cb;
    String TAG = "LocationClient";
    long startTime = System.currentTimeMillis();
    List<List<LocationBandwidthInfo>> routes = new ArrayList<>();
    public interface RequestCompleteCallback{
        void onRequestComplete(JSONObject jsonResponse,  boolean isFinal);
    }

    public BandwidthClient(Context context, RequestCompleteCallback cb){
        queue = Volley.newRequestQueue(context);
        url = "http://localhost:8080/magicschoolbus/getpoints";
        annotationName = "bandwidth";
        this.context = context;
        this.cb = cb;
        readRoutes();

    }

    private void readRoutes(){
        try{
            InputStream is = context.getResources().openRawResource(R.raw.stall_routes);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            Log.i(TAG, "got " + jsonArray.length() + " routes");
            for(int i = 0; i < jsonArray.length(); ++i){
                List<LocationBandwidthInfo> routeInfo = new ArrayList<>();
                for(int j = 0; j < jsonArray.getJSONArray(i).length(); ++j){
                    JSONObject object = jsonArray.getJSONArray(i).getJSONObject(j);
                    LocationBandwidthInfo info = new LocationBandwidthInfo();
                    LatLong latLong= new LatLong();
                    latLong.setLatitude(object.getDouble("y"));
                    latLong.setLongitude(object.getDouble("x"));
                    //Log.i(TAG,"i =" + i + " +j = " + j + "bw = " + object.getDouble("bandwidth"));
                    info.setLatLong(latLong);
                    info.setBandwidth(object.getDouble("bandwidth"));
                    routeInfo.add(info);

                }
                routes.add(routeInfo);
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

    class LocationJsonRequest implements Response.Listener<JSONObject>{

        RequestCompleteCallback cb;
        boolean isFinal;
        long startTime;
        LocationJsonRequest(RequestCompleteCallback cb, boolean isFinal, long startTime){

            this.cb = cb;
            this.isFinal = isFinal;
            this.startTime = startTime;
        }
        @Override
        public void onResponse(JSONObject response) {
            Log.i(TAG, "bw req complete, calling handler, isFinal =" + isFinal);
            long endTime = System.currentTimeMillis();
            Log.i(TAG , " request completion time = " + (endTime - startTime) + " ms");
            cb.onRequestComplete(response, isFinal);
        }
    }
    void getAnnotatedRoute(List<LatLong> latLongs){
        try {

            boolean isFinal = true;
            int i = 0;
            JSONObject reqBody = new JSONObject();
            reqBody.put("annotationName", annotationName);
            JSONArray locations = new JSONArray();
            for (LatLong latLong : latLongs) {

                JSONObject locationObj = new JSONObject();
                locationObj.put("x", latLong.getLongitude());
                locationObj.put("y", latLong.getLatitude());
                locationObj.put("city", "Cork");
                locations.put(locationObj);

            }

            reqBody.put("locations", locations);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, reqBody, new LocationJsonRequest(cb, isFinal, System.currentTimeMillis()), new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    Log.d(TAG,error.toString());
                }
            });

            request.setRetryPolicy(new DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(request);


        } catch(Exception ex){
            ex.printStackTrace();
        }

    }


}
