package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
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
 * This class will monitor the predictions made and suggest how to improve the user's health.
 */
public class SuggestingLifestyleImprovements extends Service implements Runnable {

    public static volatile Boolean isRunning = false;    //Used to check if service is already running
    private Context context;
    private SharedPreferences sharedPref;
    private DataBaseManager dataBaseManager;
    private Handler handler;
    private double hoursOfSleep = -1;
    private LocalDateTime meditatingTime;


    /**
     * The notification Messages
     */
    private final String standingSuggestion = "You sit for a long time, I'll remind you to move about once per hour.";
    private final String sleepingSuggestion = "You aren't getting enough sleep, Try going to sleep at about";
    private final String meditatingSuggestion = "A good time for you to meditate is ";


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
            HandlerThread handlerThread = new HandlerThread("SLI_HandlerThread"); //Name the handlerThread
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
        identifyMeditatingTime();
        updateUserDetails();

        //Getting milliseconds to next Instance
        LocalDateTime rightNow = LocalDateTime.now();
        LocalDateTime nextHour = rightNow.plusHours(2).truncatedTo(ChronoUnit.HOURS);
        long duration = Duration.between(rightNow, nextHour).toMillis();

        handler.postDelayed(this, duration); //fire in 2 hours
        // Stop using: handler.removeCallbacks(this);
    }

    /**
     * Will check if the user has been sitting continuously for an hour
     * and ask them to move about if they have.
     * <p>
     * https://newsnetwork.mayoclinic.org/discussion/infographic-sitting-vs-standing-2/
     * https://www.health.harvard.edu/heart-health/why-you-should-move-even-just-a-little-throughout-the-day
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
        if (percentage > 0.95) //sitting for more than 95% of the time
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
     * Suggests a good sleeping time to get these hours
     * <p>
     * https://www.webmd.com/sleep-disorders/sleep-requirements#:~:text=Most%20adults%20need%207%20to,hours%20of%20sleep%20than%20usual.
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
                    int idealSleepHour = (int) (wakeHour - 7);
                    String prefix = "am";
                    if(idealSleepHour > 12)
                    {
                        idealSleepHour = idealSleepHour - 12;
                        prefix = "pm";
                    }
                    improvements = improvements.replace(";" + sleepingSuggestion + idealSleepHour + prefix, "");
                    editor.putString(Constants.IMPROVEMENTS, improvements);
                    editor.apply();
                }
            }
        }
    }


    /**
     * Will try to identify a good time for the user to meditate.
     * Will check of they are at home, before sending notification
     * <p>
     * https://www.mayoclinic.org/tests-procedures/meditation/in-depth/meditation/art-20045858
     */
    public void identifyMeditatingTime() {
        if (!sharedPref.contains(Constants.HOME_LONGITUDE) && !sharedPref.contains(Constants.HOME_LATITUDE) &&
                !sharedPref.contains(Constants.WAKE_TIME_HOUR) && !sharedPref.contains(Constants.SLEEP_TIME_HOUR)) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        if (meditatingTime != null) {
            if (rightNow.getHour() == meditatingTime.getHour()) {
                sendANotification("Try Meditating",
                        "Meditating can help reduce stress.",
                        R.drawable.ic_meditating,
                        Constants.MEDITATING);
                return;
            }
        }

        int wakeHour = sharedPref.getInt(Constants.WAKE_TIME_HOUR, -1);
        int sleepHour = sharedPref.getInt(Constants.SLEEP_TIME_HOUR, -1);

        if (rightNow.getHour() < wakeHour && rightNow.getHour() > sleepHour) {
            return;
        }

        try {
            double currentLatitude = getCurrentLocation().getLatitude();
            double currentLongitude = getCurrentLocation().getLongitude();
            double homeLatitude = Double.parseDouble(sharedPref.getString(Constants.HOME_LATITUDE, ""));
            double homeLongitude = Double.parseDouble(sharedPref.getString(Constants.HOME_LONGITUDE, ""));

            // Allowing Error margin of 0.0005
            if (currentLatitude >= homeLatitude - 0.0005 && currentLatitude <= homeLatitude + 0.0005 &&
                    currentLongitude >= homeLongitude - 0.0005 && currentLongitude <= homeLongitude + 0.0005) {
                //Getting predictions of the last hour
                List<PredictionEntity> predictions = dataBaseManager.getAllPredictions(rightNow.getHour() - 1, rightNow.getDayOfMonth(), rightNow.getMonthValue(), rightNow.getYear());

                int sitting = 0;
                for (PredictionEntity prediction : predictions) {
                    if (prediction.activity.equalsIgnoreCase(UserActivities.SITTING)) {
                        sitting++;
                    }
                }

                double percentage = ((double) sitting / predictions.size());
                if (percentage > 0.85) {
                    sendANotification("Try Meditating",
                            "Meditating can help reduce stress.",
                            R.drawable.ic_meditating,
                            Constants.MEDITATING);
                    meditatingTime = LocalDateTime.now();

                    String improvements = sharedPref.getString(Constants.IMPROVEMENTS, "");
                    if (!improvements.toLowerCase().contains("good time to meditate")) {
                        String time;
                        if (meditatingTime.getHour() > 11) {
                            time = meditatingTime.getHour() + "pm";
                        } else {
                            time = meditatingTime.getHour() + "am";
                        }
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(Constants.IMPROVEMENTS, improvements
                                + ";" + meditatingSuggestion + time);
                        editor.apply();
                    }
                }

            }
        }catch(NullPointerException ignored) //can happen right after reboot.
        {}


    }

    /**
     * In order to track bmi and plot graph,
     * will request the user to update their details once a week.
     */
    private void updateUserDetails() {
        if (!sharedPref.contains(MainActivity.PREFERENCES_USERS_LAST_BMI_READING_DATE)) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        LocalDateTime lastBmiReading = LocalDateTime.parse(sharedPref.getString(MainActivity.PREFERENCES_USERS_LAST_BMI_READING_DATE, ""));

        if (lastBmiReading.plusWeeks(1).isBefore(rightNow)) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra("IS_EDIT_MODE", true);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, 0);

            sendANotification("Update Details",
                    "Tap to update your weight and height details",
                    R.drawable.ic_basic_details,
                    Constants.UPDATE_DETAILS,
                    pendingIntent);
        }
    }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendANotification(String contentTitle, String contentText, int drawableIcon, int notificationId, PendingIntent pendingIntent) {
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

        if (pendingIntent != null) {
            builder = builder.setAutoCancel(true)
                    .setContentIntent(pendingIntent);
        }

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
    }

    private void sendANotification(String contentTitle, String contentText, int drawableIcon, int notificationId) {
        sendANotification(contentTitle, contentText, drawableIcon, notificationId, null);
    }

    @Override
    public boolean stopService(Intent name) {
        isRunning = false;
        return super.stopService(name);
    }

    /**
     * Getting current location
     *
     * @return current location
     */
    @SuppressLint("MissingPermission") //Handled at start of ActivityLifestyleMain
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

}
