package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Display;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.UserActivities;
import com.cdap.androidapp.ManagingLifestyle.Models.Reading;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Analyzes user's current lifestyle
 * Listens to messages from the watch and identifies user's:
 * wake-up time
 * sleep time
 * home location
 * workplace location
 * work hours
 * exercise time
 * exercise type
 * method of transport to work
 */
public class PhoneLifestyleService extends WearableListenerService implements Runnable {


    public static volatile String PREDICTION = "predicting...";
    public static Boolean isRunning = false;    // Used to check if service is already running
    public static Boolean isAnalysisPeriod = true;    // Used to check if we are still in analysis week
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
    public static boolean IS_SERVER_REACHABLE = false;

    private final ArrayList<Reading> values = new ArrayList<>();    // Stores 200 accelerometer readings
    private Context context;
    private URL url = null;
    private PredictionEntity previousPredictionEntity = null;
    private PowerManager.WakeLock wakeLock = null;
    private boolean isCharging = false;
    private boolean isUnlocked = false;
    private SharedPreferences sharedPref;
    private final Handler handler = new Handler();
    private ArrayList<Double> workLongitude;
    private ArrayList<Double> workLatitude;
    private IntentFilter intentFilter;
    private BroadcastReceiver broadcastReceiver;

    private final Object makePredictionLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        workLongitude = new ArrayList<>();
        workLatitude = new ArrayList<>();

        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
                    if (isUnlocked) {
                        setSleepingTime();
                    }
                    unregisterReceiver(this);
                }
            }
        };


    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        IS_SERVER_REACHABLE = true;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "phoneService")
                .setContentTitle("Receiving sensor readings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setOngoing(true);

        NotificationChannel channel = new NotificationChannel("phoneService", "lifestyle tracking", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("getting sensor readings from watch");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManagerCompat.notify(Constants.PHONE_SERVICE, builder.build());
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PhoneLifestyleService.class.getSimpleName());
        wakeLock.acquire();

        Thread thread = new Thread(this);
        thread.start();

        return Service.START_STICKY;
    }


    /**
     * If the message received from the watch is a sensor reading set the thread will be asked to obtain the prediction.
     * If the message received says "charging" then setSleepingTime() will be called,
     * when watch is done charging setWakingTime() will be called.
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        System.out.println("Received Message from Watch: " + messageEvent.getSourceNodeId());
        String message = new String(messageEvent.getData());

        if (message.equalsIgnoreCase("CHARGING")) //Detecting Sleep Time
        {
            isCharging = true;
            setSleepingTime();
            // The user might continue to use the phone even though watch is charging
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (!myKM.inKeyguardRestrictedInputMode()) { // Phone is unlocked
                for (Display display : dm.getDisplays()) {
                    if (display.getState() != Display.STATE_OFF && display.getState() != Display.FLAG_PRIVATE) {    // Display is on
                        isUnlocked = true;
                        registerReceiver(broadcastReceiver, intentFilter);
                    }
                }
            }
            PREDICTION = "Watch is Charging";
            return;
        }

        if (isCharging) { // When we get first sensor reading after taking off charger
            setWakingTime();
            isCharging = false;
        }

        if (message.equalsIgnoreCase("EXERCISING")) //Detecting when Exercising
        {
            PREDICTION = "Exercising";
            setExercisingTime();
            return;
        }


        String[] lines = message.split("(\\$%\\$%)");
        if (values.isEmpty()) {
            for (int i = 0; i < 200; i++) {
                Reading reading = new Reading(
                        lines[i].split("#&")[0], // x - axis
                        lines[i].split("#&")[1], // y - axis
                        lines[i].split("#&")[2] // z - axis
                );
                values.add(reading);
            }

            synchronized (makePredictionLock) { //making prediction via thread
                makePredictionLock.notifyAll();
            }
        }
    }


    @Override
    public void run() {
        sendPostMessage(); //Starting up the thread
        determineWorkHours(); //runs in separate thread
        determineWorkplaceLocation(); //runs in separate thread

        while (isAnalysisPeriod) {

            checkEndOfAnalysisPeriod();
            determineWorkplaceTravelMethod();

            try {
                Thread.sleep(600000);   // 10 min
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Will make a POST request to backend server and obtain the predicted activity
     * This will also store the Predicted Activity in the database.
     */
    public void sendPostMessage() {
        (new Thread(() -> {
            while (true) {
                try {
                    synchronized (makePredictionLock) {
                        makePredictionLock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                try {
                    if (url == null) {
                        url = new URL(SERVER_URL);
                    }

                    //Creating JSON body to send
                    JSONObject jsonObject = new JSONObject();
                    JSONArray jsonArray = new JSONArray();
                    JSONArray readingsArray = new JSONArray();
                    for (int i = 0; i < 200; i++) {
                        JSONArray temp = new JSONArray();
                        temp.put(0, values.get(i).xAxis);
                        temp.put(1, values.get(i).yAxis);
                        temp.put(2, values.get(i).zAxis);
                        readingsArray.put(temp);
                    }
                    values.clear();
                    jsonArray.put(readingsArray);
                    jsonObject.put("data", jsonArray);

                    //Making POST request
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("Content-Type", "application/json");
                    httpURLConnection.setReadTimeout(3000);
                    httpURLConnection.connect();

                    DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                    wr.writeBytes(jsonObject.toString());
                    wr.flush();
                    wr.close();

                    //Getting Response
                    InputStream response = httpURLConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    PREDICTION = stringBuilder.toString().replaceAll("\"", "").trim();

                    DataBaseManager dataBaseManager = new DataBaseManager(context);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    PredictionEntity predictionEntity = new PredictionEntity(
                            localDateTime.getDayOfMonth(),
                            localDateTime.getMonthValue(),
                            localDateTime.getYear(),
                            localDateTime.getHour(),
                            localDateTime.getMinute(),
                            PREDICTION
                    );
                    dataBaseManager.addPrediction(predictionEntity);
                    if (previousPredictionEntity != null && previousPredictionEntity.day != predictionEntity.day) {
                        LifestylePercentageManager.saveDailyPercentages(context, previousPredictionEntity);
                    }


                    previousPredictionEntity = predictionEntity;
                } catch (ConnectException e) {
                    IS_SERVER_REACHABLE = false;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        })).start();
    }

    /**
     * Assuming user plugs watch to charge before sleeping,
     * this will save sleeping time in preferences
     */
    public void setSleepingTime() {
        if (!isAnalysisPeriod) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        // Assuming people go to sleep sometime between 8pm and 3am.
        if (rightNow.getHour() >= 20 && rightNow.getHour() <= 23 ||
                rightNow.getHour() >= 0 && rightNow.getHour() <= 3) {

            int hour = sharedPref.getInt(Constants.SLEEP_TIME_HOUR, -1);
            int minute = sharedPref.getInt(Constants.SLEEP_TIME_MINUTE, -1);

            if (hour > 0 && minute > 0) {
                hour = ((hour + rightNow.getHour()) / 2);
                minute = ((hour + rightNow.getMinute()) / 2);
            } else {
                hour = rightNow.getHour();
                minute = rightNow.getMinute();
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(Constants.SLEEP_TIME_HOUR, hour);
            editor.putInt(Constants.SLEEP_TIME_MINUTE, minute);
            editor.apply();
        }
    }

    /**
     * Assuming wears watch after waking up,
     * this will save waking time in preferences
     * Also gets user's location and assumes it as their home.
     */
    private void setWakingTime() {
        if (!isAnalysisPeriod) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        // Assuming people wake up sometime between 3am and 9am.
        if (rightNow.getHour() >= 3 && rightNow.getHour() <= 9) {

            int hour = sharedPref.getInt(Constants.WAKE_TIME_HOUR, -1);
            int minute = sharedPref.getInt(Constants.WAKE_TIME_MINUTE, -1);

            if (hour > 0 && minute > 0) {
                hour = ((hour + rightNow.getHour()) / 2);
                minute = ((hour + rightNow.getMinute()) / 2);
            } else {
                hour = rightNow.getHour();
                minute = rightNow.getMinute();
            }

            Location currentLocation = getCurrentLocation();


            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(Constants.WAKE_TIME_HOUR, hour);
            editor.putInt(Constants.WAKE_TIME_MINUTE, minute);

            if (currentLocation != null) {
                editor.putString(Constants.HOME_LATITUDE, String.valueOf(currentLocation.getLatitude()));
                editor.putString(Constants.HOME_LONGITUDE, String.valueOf(currentLocation.getLongitude()));
            }

            editor.apply();
        }
    }

    /**
     * Will record the time when the watch says the user is Exercising
     */
    private void setExercisingTime() {
        if (!isAnalysisPeriod) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        int hour = sharedPref.getInt(Constants.EXERCISE_TIME_HOUR, -1);
        int minute = sharedPref.getInt(Constants.EXERCISE_TIME_MINUTE, -1);

        // Exercise time
        if (hour > 0 && minute > 0) {
            hour = ((hour + rightNow.getHour()) / 2);
            minute = ((hour + rightNow.getMinute()) / 2);
        } else {
            hour = rightNow.getHour();
            minute = rightNow.getMinute();
        }

        //Exercise days
        String days;
        if (sharedPref.contains(Constants.EXERCISE_DAYS)) {
            days = sharedPref.getString(Constants.EXERCISE_DAYS, "") + ";" + rightNow.getDayOfWeek().toString();
        } else {
            days = rightNow.getDayOfWeek().toString();
        }

        //Exercise Type
        String type;
        DataBaseManager dataBaseManager = new DataBaseManager(context);
        int typeHour = rightNow.getHour();
        if (rightNow.getMinute() < 20) {
            typeHour = rightNow.getHour() - 1;
        }

        List<PredictionEntity> predictions = dataBaseManager.getAllPredictions(typeHour, rightNow.getDayOfMonth(), rightNow.getMonthValue(), rightNow.getYear());
        int total, running;
        total = running = 0;
        for (PredictionEntity prediction : predictions) {
            total++;
            if (prediction.activity.equalsIgnoreCase(UserActivities.JOGGING)) {
                running++;
            }
        }
        if (((double) running / total) > 0.8) {
            type = Constants.RUNNING;
        } else {
            type = Constants.GYM;
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.EXERCISE_TIME_HOUR, hour);
        editor.putInt(Constants.EXERCISE_TIME_MINUTE, minute);
        editor.putString(Constants.EXERCISE_DAYS, days);
        editor.putString(Constants.EXERCISE_TYPE, type);
        editor.apply();
    }

    /**
     * Will determine the workplace location
     * by checking the location after 11am and before 3pm.
     */
    private void determineWorkplaceLocation() {
        Runnable runnable = new Runnable() {
            public void run() {
                if (!isAnalysisPeriod) {
                    handler.removeCallbacks(this);
                    return;
                }
                LocalDateTime rightNow = LocalDateTime.now();
                if (rightNow.getDayOfWeek() != DayOfWeek.SATURDAY && rightNow.getDayOfWeek() != DayOfWeek.SUNDAY) {

                    if (rightNow.getHour() >= 11 && rightNow.getHour() <= 15) //in between 11am and 3pm
                    {
                        Location currentLocation = getCurrentLocation();
                        workLongitude.add(currentLocation.getLongitude());
                        workLatitude.add(currentLocation.getLongitude());

                        handler.postDelayed(this, 3600000); //Once per hour
//                        Toast.makeText(context, "ONE Hour", Toast.LENGTH_LONG).show();

                    } else {
                        if (!workLatitude.isEmpty() && !workLongitude.isEmpty()) {
                            double latitude = Collections.max(workLatitude);
                            double longitude = Collections.max(workLongitude);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(Constants.WORK_LATITUDE, String.valueOf(latitude));
                            editor.putString(Constants.WORK_LONGITUDE, String.valueOf(longitude));
                            editor.apply();
                        }
                    }


                } else {
                    long duration = (24 - rightNow.getHour()) * 3600000; //Time to next midnight in milliseconds
                    handler.postDelayed(this, duration); //To stop use: handler.removeCallbacks(runnable);
                }
            }
        };
        runnable.run();

    }

    /**
     * Determine how the user gets to work
     */
    private void determineWorkplaceTravelMethod() {
        if (!isAnalysisPeriod) {
            return;
        }

        //Detecting how user goes to work
        if (!sharedPref.contains(Constants.WORK_TRAVEL_METHOD)) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Constants.WORK_TRAVEL_METHOD, Constants.WALKING);
            editor.apply();
        }

        int wakeHour = sharedPref.getInt(Constants.WAKE_TIME_HOUR, -1);
        int atWorkHour = sharedPref.getInt(Constants.WORK_START_TIME_HOUR, -1);
        if (wakeHour >= 0 && atWorkHour >= 0) {
            LocalDateTime rightNow = LocalDateTime.now();
            if (rightNow.getHour() > wakeHour && rightNow.getHour() < atWorkHour) {
                if (getCurrentLocation().getSpeed() > 3.0f) // 3 m/s = 10.8 km/h
                {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Constants.WORK_TRAVEL_METHOD, Constants.VEHICLE);
                    editor.apply();
                }
            }
        }
    }

    /**
     * Will monitor user's location and record the time when they enter the workplace and when they leave the workplace
     */
    private void determineWorkHours() {
        final AtomicBoolean isAtWork = new AtomicBoolean(false);
        Runnable runnable = new Runnable() {
            public void run() {
                if (!isAnalysisPeriod) {
                    handler.removeCallbacks(this);
                    return;
                }

                if (!sharedPref.contains(Constants.WORK_LATITUDE) && !sharedPref.contains(Constants.WORK_LONGITUDE)) { //Should not happen
                    return;
                }

                double workLatitude = Double.parseDouble(sharedPref.getString(Constants.WORK_LATITUDE, ""));
                double workLongitude = Double.parseDouble(sharedPref.getString(Constants.WORK_LONGITUDE, ""));
                Location currentLocation = getCurrentLocation();
                LocalDateTime rightNow = LocalDateTime.now();

                if (!isAtWork.get()) { ///////////////////////////////////// Getting Time user arrived at Workplace ///////////////////////////
                    // Allowing Error margin of 0.0005
                    if (currentLocation.getLongitude() < workLongitude + 0.0005 && currentLocation.getLongitude() > workLongitude - 0.0005 &&
                            currentLocation.getLatitude() < workLatitude + 0.0005 && currentLocation.getLatitude() > workLatitude - 0.0005) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        if (sharedPref.contains(Constants.WORK_START_TIME_HOUR) && sharedPref.contains(Constants.WORK_START_TIME_MINUTE)) {
                            if (rightNow.getHour() < sharedPref.getInt(Constants.WORK_START_TIME_HOUR, 99)) {

                                editor.putInt(Constants.WORK_START_TIME_HOUR, rightNow.getHour());
                                if (rightNow.getMinute() < sharedPref.getInt(Constants.WORK_START_TIME_MINUTE, 99)) {
                                    editor.putInt(Constants.WORK_START_TIME_MINUTE, rightNow.getMinute());
                                }
                            }
                        } else {
                            editor.putInt(Constants.WORK_START_TIME_HOUR, rightNow.getHour());
                            editor.putInt(Constants.WORK_START_TIME_MINUTE, rightNow.getMinute());
                        }
                        editor.apply();
                        isAtWork.set(true);
                    } else {
                        handler.postDelayed(this, 600000); // 10 minutes
                        //To stop use: handler.removeCallbacks(runnable);
                    }
                } else {///////////////////////////////////// Getting Time user leaves Workplace /////////////////////////////////////////////
                    currentLocation = getCurrentLocation();
                    rightNow = LocalDateTime.now();

                    // Allowing Error margin of 0.0005
                    if (currentLocation.getLongitude() > workLongitude + 0.0005 && currentLocation.getLongitude() < workLongitude - 0.0005 &&
                            currentLocation.getLatitude() > workLatitude + 0.0005 && currentLocation.getLatitude() < workLatitude - 0.0005) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        if (sharedPref.contains(Constants.WORK_END_TIME_HOUR) && sharedPref.contains(Constants.WORK_END_TIME_MINUTE)) {
                            if (rightNow.getHour() > sharedPref.getInt(Constants.WORK_END_TIME_HOUR, 99)) {
                                editor.putInt(Constants.WORK_END_TIME_HOUR, rightNow.getHour());
                                if (rightNow.getMinute() > sharedPref.getInt(Constants.WORK_END_TIME_MINUTE, 99)) {
                                    editor.putInt(Constants.WORK_END_TIME_MINUTE, rightNow.getMinute());
                                }
                            }
                        } else {
                            editor.putInt(Constants.WORK_END_TIME_HOUR, rightNow.getHour());
                            editor.putInt(Constants.WORK_END_TIME_MINUTE, rightNow.getMinute());
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

    /**
     * Once the week of analyzing is over we need to start the {@link SuggestingLifestyleImprovements} service
     */
    private void checkEndOfAnalysisPeriod() {
        LocalDate rightNow = LocalDate.now();
        LocalDate analysisStartDate = LocalDate.parse(sharedPref.getString(Constants.ANALYSIS_START_DATE, ""));

        if (rightNow.isAfter(analysisStartDate.plusWeeks(1))) {
            isAnalysisPeriod = false;
            return;
        }

        isAnalysisPeriod = true;
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


    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        isRunning = false;
        super.onDestroy();
        System.out.println("Wear OS destroy");
    }

    @Override
    public boolean stopService(Intent name) {
        isRunning = false;
        return super.stopService(name);
    }
}
