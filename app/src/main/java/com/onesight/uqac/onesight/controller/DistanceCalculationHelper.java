package com.onesight.uqac.onesight.controller;

public class DistanceCalculationHelper {

    /**
     * Converts degree coordinate to radians.
     */
    static double degreeToRadians(double degreeCoordinates)
    {
        return (degreeCoordinates * Math.PI) / 180;
    }

    /**
     * Given to pairs of coordinates (latitude, longitude), calculates the distance using the Haversine formula.
     */
    static double getDistance(double myLat, double myLon, double lat, double lon)
    {
        double earthRadius = 6371e3; // radius in meters

        double myRadLat = degreeToRadians(myLat);
        double radLat = degreeToRadians(lat);
        double deltaLat = degreeToRadians(lat - myLat);
        double deltaLong = degreeToRadians(lon - myLon);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(myRadLat) * Math.cos(radLat) * Math.sin(deltaLong / 2) * Math.sin(deltaLong / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    /**
     * Tells if the other user is close enough to the current user.
     */
    static boolean isCloseEnough(double distance)
    {
        return (distance < 100);
    }

}