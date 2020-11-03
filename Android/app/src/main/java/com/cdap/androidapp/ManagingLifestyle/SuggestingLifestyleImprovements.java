package com.cdap.androidapp.ManagingLifestyle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cdap.androidapp.MainActivity;
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
    private SharedPreferences sharedPref;
    private DataBaseManager dataBaseManager;
    private double hoursOfSleep = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        dataBaseManager = new DataBaseManager(context);
        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
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
        try {
            while (true) {
                checkTimeSeated();
                checkSleepHours();

                Thread.sleep(3600000); //one hour


            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            sendANotification("You've been sitting for a long time",
                    "We recommend moving about for a bit",
                    R.drawable.long_sitting_icon,
                    Constants.SITTING_TOO_LONG);
        }

    }


    /**
     * The recommended amount of sleep for a healthy adult is at least seven hours
     * <p>
     * https://www.mayoclinic.org/healthy-lifestyle/adult-health/in-depth/sleep/art-20048379?mc_id=us&utm_source=newsnetwork&utm_medium=l&utm_content=content&utm_campaign=mayoclinic&geo=national&placementsite=enterprise&cauid=100721
     */
    public void checkSleepHours() {
        if (sharedPref.contains(Constants.SLEEP_TIME_HOUR) && sharedPref.contains(Constants.SLEEP_TIME_MINUTE)
                && sharedPref.contains(Constants.WAKE_TIME_HOUR) && sharedPref.contains(Constants.WAKE_TIME_MINUTE)) {
            double sleepHour = sharedPref.getInt(Constants.SLEEP_TIME_HOUR, -1);
            int sleepMinute = sharedPref.getInt(Constants.SLEEP_TIME_MINUTE, -1);
            double wakeHour = sharedPref.getInt(Constants.WAKE_TIME_HOUR, -1);
            int wakeMinute = sharedPref.getInt(Constants.WAKE_TIME_MINUTE, -1);

            sleepHour += ((double) sleepMinute / 60);
            wakeHour += ((double) wakeMinute / 60);

            double sleepingHours = -1;
            if (sleepHour > wakeHour) {
                sleepingHours = 24 - (sleepHour - wakeHour);
            } else {
                sleepingHours = wakeHour - sleepHour;
            }
            if (sleepingHours <= 7.0) {
                if (hoursOfSleep == -1) {
                    sendANotification("You aren't getting enough sleep",
                            "An adult requires at least 7 hours of sleep. You get only " + hoursOfSleep,
                            R.drawable.ic_not_enough_sleep,
                            Constants.NOT_ENOUGH_SLEEP);
                }
                hoursOfSleep = sleepingHours;
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendANotification(String contentTitle, String contentText, int drawableIcon, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SuggestingLifestyleImprovements")
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(drawableIcon)
                .setOngoing(false);

        NotificationChannel channel = new NotificationChannel("SuggestingLifestyleImprovements", "improving Lifestyle", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("notifies the user of changes they can make to their current lifestyle");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
    }

}
