package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.UserActivities;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;

import java.time.LocalDateTime;
import java.util.List;

/**
 * By the time we start using this class we should have already run {@link PhoneLifestyleService} for a week
 * This class will monitor the predictions made and decide how to improve the user's life.
 */
public class SuggestingLifestyleImprovements extends Service implements Runnable {

    public static Boolean isRunning = false;    //Used to check if service is already running
    private Context context;
    private DataBaseManager dataBaseManager;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        dataBaseManager = new DataBaseManager(context);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Thread thread = new Thread(this);
        thread.start();
        isRunning = true;

        return Service.START_STICKY;
    }


    @Override
    public void run() {
        while (true) {
            checkTimeSeated();


            try {
                Thread.sleep(3600000 ); //one hour
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Will check if the user has been sitting continuously for an hour
     * and ask them to move about if they have.
     * <p>
     * https://newsnetwork.mayoclinic.org/discussion/infographic-sitting-vs-standing-2/
     */
    public void checkTimeSeated() {
        LocalDateTime rightNow = LocalDateTime.now();
        //Getting predictions of the last 2 hours
        List<PredictionEntity> predictions = dataBaseManager.getAllPredictions(rightNow.getHour(), rightNow.getDayOfMonth(), rightNow.getMonthValue(), rightNow.getYear());
        predictions.addAll(dataBaseManager.getAllPredictions(rightNow.getHour() - 1, rightNow.getDayOfMonth(), rightNow.getMonthValue(), rightNow.getYear()));

        int total, sitting;
        total = sitting = 0;
        for (PredictionEntity prediction : predictions) {
            if (prediction.hour == rightNow.getHour() - 1 && prediction.minute >= rightNow.getMinute() ||
                    prediction.hour == rightNow.getHour() && prediction.minute <= rightNow.getMinute()) {
                total++;
                if (prediction.activity.equalsIgnoreCase(UserActivities.SITTING)) {
                    sitting++;
                }
            }
        }

        double percentage = ((double) sitting / total);
        if (percentage > 0.85) //sitting for more than 85% of the time
        {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "sittingForTooLong")
                    .setContentTitle("You've been sitting for a long time")
                    .setContentText("We recommend moving about for a bit")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.drawable.long_sitting_icon)
                    .setOngoing(false);

            NotificationChannel channel = new NotificationChannel("sittingForTooLong", "sitting tracker", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("notify when user is sitting for long periods of time.");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManagerCompat.notify(Constants.SITTING_TRACKER, builder.build());

        }

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
