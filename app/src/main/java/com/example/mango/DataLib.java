package com.example.mango;

public class DataLib {
    public double grav;
    public double initLatitude, initLongitude, initAltitude, initAngle;
    public double initWGS84Latitude, initWGS84Longitude, initWGS84Altitude;
    public double latitude, longtitude, altitude, angle;
    public StringBuilder sensorDataString, filterDataString, positionDataString;

    public DataLib() {
        sensorDataString = new StringBuilder();
        filterDataString = new StringBuilder();
        positionDataString = new StringBuilder();

        grav = 0;
        initAngle = 0;

        initLatitude = 0;
        initLongitude = 0;
        initAltitude = 0;

        initWGS84Latitude = 0;
        initWGS84Longitude = 0;
        initWGS84Altitude = 0;

        latitude = 0;
        longtitude = 0;
        altitude = 0;
        angle = 0;
    }
}
