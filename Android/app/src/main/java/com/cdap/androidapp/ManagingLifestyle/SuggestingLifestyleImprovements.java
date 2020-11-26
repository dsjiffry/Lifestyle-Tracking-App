package com.cdap.androidapp.ManagingLifestyle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.UserActivities;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * By the time we start using this class we should have already run {@link PhoneLifestyleService} for a week
 * This class will monitor the predictions made and decide how to improve the user's life.
 */
public class SuggestingLifestyleImprovements extends Service implements Runnable {

    public static volatile Boolean isRunning = false;    //Used to check if service is already running
    private Context context;
    private SharedPreferences sharedPref;
    private DataBaseManager dataBaseManager;
    private Handler handler;
    private double hoursOfSleep = -1;

    private final String standingSuggestion = "You sit for a long time, try standing and moving about once per hour.";
    private final String sleepingSuggestion = "You aren't getting enough sleep, An adult requires at least 7 hours of sleep per day.";


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        dataBaseManager = new DataBaseManager(context);
        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!isRunning) {
            HandlerThread handlerThread = new HandlerThread("MyHandlerThread"); //Name the handlerThread
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            handler.post(this); // Start thread immediately
            isRunning = true;
        }

        return Service.START_STICKY;
    }


    @Override
    public void run() {
        checkTimeSeated();
        checkSleepHours();

        //Getting milliseconds to next hour
        LocalDateTime rightNow = LocalDateTime.now();
        LocalDateTime nextHour = rightNow.plusHours(1).truncatedTo(ChronoUnit.HOURS);
        long duration = Duration.between(rightNow, nextHour).toMillis();

        handler.postDelayed(this, duration); //fire at next hour
        // Stop using: handler.removeCallbacks(this);
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
            String improvements = sharedPref.getString(Constants.IMPROVEMENTS, "");
            if (!improvements.toLowerCase().contains("sit for a long time")) {
                SharedPreferences.Editor editor = sharedPref.edit();
                String suggestion = improvements +
                        ";" + standingSuggestion;
                editor.putString(Constants.IMPROVEMENTS, suggestion);
                editor.apply();
            }
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
            LocalDateTime rightNow = LocalDateTime.now();

            sleepHour += ((double) sleepMinute / 60);
            wakeHour += ((double) wakeMinute / 60);

            double sleepingHours;
            if (sleepHour > wakeHour) {
                sleepingHours = 24 - (sleepHour - wakeHour);
            } else {
                sleepingHours = wakeHour - sleepHour;
            }
            if (sleepingHours <= 7.0) {
                if (hoursOfSleep == -1) {
                    String improvements = sharedPref.getString(Constants.IMPROVEMENTS, "");
                    if (!improvements.toLowerCase().contains("getting enough sleep")) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(Constants.IMPROVEMENTS, improvements
                                + ";" + sleepingSuggestion);
                        editor.apply();
                    }
                    hoursOfSleep = sleepingHours;
                } else {
                    int idealSleepHour = (int) wakeHour - 7;
                    if (idealSleepHour < 0) {
                        idealSleepHour = 24 - (idealSleepHour * (-1));
                    }
                    if (rightNow.getHour() == idealSleepHour) {
                        sendANotification("Sleeping now would be better",
                                "if you sleep now you can get the required 7 hours of sleep",
                                R.drawable.ic_not_enough_sleep,
                                Constants.NOT_ENOUGH_SLEEP);
                    }
                }

            } else {
                String improvements = sharedPref.getString(Constants.IMPROVEMENTS, "");
                if (!improvements.toLowerCase().contains("getting enough sleep")) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    improvements = improvements.replace(";" + sleepingSuggestion, "");
                    editor.putString(Constants.IMPROVEMENTS, improvements);
                    editor.apply();
                }
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

    @Override
    public boolean stopService(Intent name) {
        isRunning = false;
        return super.stopService(name);
    }

}
