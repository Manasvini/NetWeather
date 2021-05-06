package com.epl.netweather;


import java.util.List;

public class Utils {
    public static double getDistance(LatLong a, LatLong b){
        double R = 6378.137; // Radius of earth in KM
        double dLat = b.getLatitude()  - a.getLatitude() ;
        double dLon = b.getLongitude() - a.getLongitude() ;
        double distsq = Math.sin(Math.toRadians(dLat/2)) * Math.sin(Math.toRadians(dLat/2)) +
                Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude() )) *
                        Math.sin(Math.toRadians(dLon/2)) * Math.sin(Math.toRadians(dLon/2));
        double c = 2 * Math.atan2(Math.sqrt(distsq), Math.sqrt(1-distsq));
        double d = R * c;
        return d * 1000; // meters
       // double x_sq = (a.getLatitude() - b.getLatitude()) *(a.getLatitude() - b.getLatitude());
       // double y_sq = (a.getLongitude() - b.getLongitude()) *(a.getLongitude() - b.getLongitude());
       // return Math.sqrt(x_sq + y_sq);
    }
}
class LatLong {
    private double latitude;
    private double longitude;

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
class LatLongList{
    private List<LatLong> points;

    public List<LatLong> getPoints() {
        return points;
    }

    public void setPoints(List<LatLong> latLongList) {
        this.points = latLongList;
    }
}

class AnnotationObject{
    String annotationName;
    String dataType;
    String value;
    LatLong latLong;
    public void setAnnotationName(String annotationName) {
        this.annotationName = annotationName;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    public LatLong getLatLong() {
        return latLong;
    }

    public void setLatLong(LatLong latLong) {
        this.latLong = latLong;
    }

}

class AnnotationObjectList{
    List<AnnotationObject> annotationObjects;
    LatLong latLong;
    public List<AnnotationObject> getAnnotationObjects() {
        return annotationObjects;
    }

    public void setAnnotationObjects(List<AnnotationObject> annotationObjects) {
        this.annotationObjects = annotationObjects;
    }
    public LatLong getLatLong() {
        return latLong;
    }

    public void setLatLong(LatLong latLong) {
        this.latLong = latLong;
    }
}

