package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.Models.SPkeys;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * By the time we start using this class we should have recorded the user's:
 * sleep time
 * wake time
 * Home location
 * Work location
 */
public class SuggestingImprovements   {
    private static SuggestingImprovements suggestingImprovements = null;

    private final Handler handler = new Handler();
    private Runnable runnable;
    private SharedPreferences sharedPref;
    private Context context;

    private SuggestingImprovements() {
    }

    public static void initialize(Context context) {
        if (suggestingImprovements == null) {
            suggestingImprovements = new SuggestingImprovements();
        }
        suggestingImprovements.startSuggestingImprovements(context);
    }

    private void startSuggestingImprovements(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);


        LocalDateTime rightNow = LocalDateTime.now();
        if (rightNow.getHour() > 6) {
            workHours();
        }
    }

    /**
     * Will monitor user's location and record the time when they enter the workplace and when they leave the workplace
     */
    private void workHours() {
        final AtomicBoolean isAtWork = new AtomicBoolean(false);
        runnable = new Runnable() {
            public void run() {
                if (!sharedPref.contains(SPkeys.WORK_LATITUDE) && !sharedPref.contains(SPkeys.WORK_LONGITUDE)) { //Should not happen
                    return;
                }

                double workLatitude = Double.parseDouble(sharedPref.getString(SPkeys.WORK_LATITUDE, ""));
                double workLongitude = Double.parseDouble(sharedPref.getString(SPkeys.WORK_LONGITUDE, ""));
                Location currentLocation = getCurrentLocation();
                LocalDateTime rightNow = LocalDateTime.now();

                if (!isAtWork.get()) { ///////////////////////////////////// Getting Time user arrived at Workplace ///////////////////////////
                    // Allowing Error margin of 0.0005
                    if (currentLocation.getLongitude() < workLongitude + 0.0005 && currentLocation.getLongitude() > workLongitude - 0.0005 &&
                            currentLocation.getLatitude() < workLatitude + 0.0005 && currentLocation.getLatitude() > workLatitude - 0.0005) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        if (sharedPref.contains(SPkeys.WORK_START_TIME_HOUR) && sharedPref.contains(SPkeys.WORK_START_TIME_MINUTE)) {
                            if (rightNow.getHour() < sharedPref.getInt(SPkeys.WORK_START_TIME_HOUR, 99)) {

                                editor.putInt(SPkeys.WORK_START_TIME_HOUR, rightNow.getHour());
                                if (rightNow.getMinute() < sharedPref.getInt(SPkeys.WORK_START_TIME_MINUTE, 99)) {
                                    editor.putInt(SPkeys.WORK_START_TIME_MINUTE, rightNow.getMinute());
                                }
                            }
                        } else {
                            editor.putInt(SPkeys.WORK_START_TIME_HOUR, rightNow.getHour());
                            editor.putInt(SPkeys.WORK_START_TIME_MINUTE, rightNow.getMinute());
                        }
                        editor.apply();
                        isAtWork.set(true);
                    } else {
                        handler.postDelayed(this, 600000); // 10 minutes
                        //To stop use: handler.removeCallbacks(runnable);
                    }
                }
                else {///////////////////////////////////// Getting Time user leaves Workplace /////////////////////////////////////////////
                    currentLocation = getCurrentLocation();
                    rightNow = LocalDateTime.now();

                    // Allowing Error margin of 0.0005
                    if (currentLocation.getLongitude() > workLongitude + 0.0005 && currentLocation.getLongitude() < workLongitude - 0.0005 &&
                            currentLocation.getLatitude() > workLatitude + 0.0005 && currentLocation.getLatitude() < workLatitude - 0.0005) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        if (sharedPref.contains(SPkeys.WORK_END_TIME_HOUR) && sharedPref.contains(SPkeys.WORK_END_TIME_MINUTE)) {
                            if (rightNow.getHour() > sharedPref.getInt(SPkeys.WORK_END_TIME_HOUR, 99)) {
                                editor.putInt(SPkeys.WORK_END_TIME_HOUR, rightNow.getHour());
                                if (rightNow.getMinute() > sharedPref.getInt(SPkeys.WORK_END_TIME_MINUTE, 99)) {
                                    editor.putInt(SPkeys.WORK_END_TIME_MINUTE, rightNow.getMinute());
                                }
                            }
                        } else {
                            editor.putInt(SPkeys.WORK_END_TIME_HOUR, rightNow.getHour());
                            editor.putInt(SPkeys.WORK_END_TIME_MINUTE, rightNow.getMinute());
                        }
                        editor.apply();
                        isAtWork.set(false);
                    } else {
                        handler.postDelayed(this, 600000); // 10 minutes
                        //To stop use: handler.removeCallbacks(runnable);
                    }
                }


//                handler.postDelayed(this, 18000000 ); // 5 hours
            }
        };
        runnable.run();
    }

    private void exerciseHours()
    {}


    /**
     * Getting current location
     *
     * @return current location
     */
    @SuppressLint("MissingPermission") //Handled at start of LifestyleMainActivity
    private Location getCurrentLocation() {
        Location currentLocation = null;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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


}
