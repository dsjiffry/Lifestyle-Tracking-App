package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * By the time we start using this class we should have already run {@link PhoneService} for a week
 * This class will monitor the predictions made and decide how to improve the user's life.
 */
public class SuggestingImprovements extends Service {

    public static Boolean isRunning = false;    //Used to check if service is already running

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Getting current location
     *
     * @return current location
     */
    @SuppressLint("MissingPermission") //Handled at start of LifestyleMainActivity
    private Location getCurrentLocation() {
        Location currentLocation = null;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) {
                continue;
            }
            if (currentLocation == null || location.getAccuracy() < currentLocation.getAccuracy()) { //Going for option with most accuracy
                currentLocation = location;
            }
        }
        return currentLocation;
    }









    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
