package com.cdap.androidapp.ManagingLifestyle;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.ManagingLifestyle.Models.Reading;
import com.cdap.androidapp.ManagingLifestyle.Models.SPkeys;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listens to messages from the watch
 */
public class PhoneService extends WearableListenerService implements Runnable {


    public static volatile String PREDICTION = "predicting...";
    public static Boolean isRunning = false;    //Used to check if service is already running
    public static Boolean isAnalysisPeriod = true;    //Used to check if we are still in analysis week
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";

    private ArrayList<Reading> values = new ArrayList<>();    // Stores 200 accelerometer readings
    private Context context;
    private URL url = null;
    private PredictionEntity previousPredictionEntity = null;
    private PowerManager.WakeLock wakeLock = null;
    private boolean isCharging = false;
    private SharedPreferences sharedPref;
    private final Handler handler = new Handler();
    private Runnable runnable;
    private ArrayList<Double> workLongitude = new ArrayList<>();
    private ArrayList<Double> workLatitude = new ArrayList<>();

    private Thread thread;
    private final Object makePredictionLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        context = getApplicationContext();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "69")
                .setContentTitle("Receiving sensor readings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setOngoing(true);

        NotificationChannel channel = new NotificationChannel("69", "Readings", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("getting sensor readings from watch");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManagerCompat.notify(69, builder.build());
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PhoneService.class.getSimpleName());
        wakeLock.acquire();

        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        thread = new Thread(this);
        thread.start();

    }


    /**
     * If the message received from the watch is a sensor reading set the thread will be asked to obtain the prediction.
     * If the message received says "charging" then setSleepingTime() will be called,
     * when watch is done charging setWakingTime() will be called.
     *
     * @param messageEvent
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
            PREDICTION = "Watch is Charging";
            return;
        }

        if (isCharging) {
            setWakingTime();
            isCharging = false;
        }

        if (message.equalsIgnoreCase("EXERCISING")) //Detecting when Exercising
        {
            PREDICTION = "Exercising";
            LocalDateTime rightNow = LocalDateTime.now();
            int hour = sharedPref.getInt(SPkeys.EXERCISE_TIME_HOUR, -1);
            int minute = sharedPref.getInt(SPkeys.EXERCISE_TIME_MINUTE, -1);

            if (hour > 0 && minute > 0) {
                hour = ((hour + rightNow.getHour()) / 2);
                minute = ((hour + rightNow.getMinute()) / 2);
            } else {
                hour = rightNow.getHour();
                minute = rightNow.getMinute();
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(SPkeys.EXERCISE_TIME_HOUR, hour);
            editor.putInt(SPkeys.EXERCISE_TIME_MINUTE, minute);
            editor.apply();
            return;
        }


        String[] lines = message.split("(\\$%\\$%)");

        for (int i = 0; i < 200; i++) {
            Reading reading = new Reading(
                    lines[i].split("#&")[0], // x - axis
                    lines[i].split("#&")[1], // y - axis
                    lines[i].split("#&")[2] // z - axis
            );
            values.add(reading);
        }

        synchronized (makePredictionLock) { //making prediction via thread
            makePredictionLock.notify();
        }
    }


    /**
     * Will make a POST request to backend server and obtain the predicted activity
     * This will also store the Predicted Activity in the database.
     */
    @Override
    public void run() {
        while (true) {
            try {
                synchronized (makePredictionLock) {
                    makePredictionLock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            checkEndOfAnalysisPeriod();

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
                jsonArray.put(readingsArray);
                jsonObject.put("data", jsonArray);

                //Making POST request
                URL url = new URL(SERVER_URL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(jsonObject.toString());
                wr.flush();
                wr.close();

                //Getting Response
                InputStream response = httpURLConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
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
                    PercentageManager.saveDailyPercentages(context, previousPredictionEntity);
                }


                previousPredictionEntity = predictionEntity;
            } catch (MalformedURLException e) { //url = new URL(SERVER_URL);
                e.printStackTrace();
            } catch (IOException e) { // URLConnection conn = url.openConnection();
                e.printStackTrace();
            } catch (JSONException e) { // jsonObject.put(...);
                e.printStackTrace();
            }


        }

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

            int hour = sharedPref.getInt(SPkeys.SLEEP_TIME_HOUR, -1);
            int minute = sharedPref.getInt(SPkeys.SLEEP_TIME_MINUTE, -1);

            if (hour > 0 && minute > 0) {
                hour = ((hour + rightNow.getHour()) / 2);
                minute = ((hour + rightNow.getMinute()) / 2);
            } else {
                hour = rightNow.getHour();
                minute = rightNow.getMinute();
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(SPkeys.SLEEP_TIME_HOUR, hour);
            editor.putInt(SPkeys.SLEEP_TIME_MINUTE, minute);
            editor.apply();
        }
    }


    /**
     * Assuming wears watch after waking up,
     * this will save waking time in preferences
     * Also gets user's location and assumes it as their home.
     */
    public void setWakingTime() {
        if (!isAnalysisPeriod) {
            return;
        }

        LocalDateTime rightNow = LocalDateTime.now();
        // Assuming people wake up sometime between 3am and 9am.
        if (rightNow.getHour() >= 3 && rightNow.getHour() <= 9) {

            int hour = sharedPref.getInt(SPkeys.WAKE_TIME_HOUR, -1);
            int minute = sharedPref.getInt(SPkeys.WAKE_TIME_MINUTE, -1);

            if (hour > 0 && minute > 0) {
                hour = ((hour + rightNow.getHour()) / 2);
                minute = ((hour + rightNow.getMinute()) / 2);
            } else {
                hour = rightNow.getHour();
                minute = rightNow.getMinute();
            }

            Location currentLocation = getCurrentLocation();


            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(SPkeys.WAKE_TIME_HOUR, hour);
            editor.putInt(SPkeys.WAKE_TIME_MINUTE, minute);

            if (currentLocation != null) {
                editor.putString(SPkeys.HOME_LATITUDE, String.valueOf(currentLocation.getLatitude()));
                editor.putString(SPkeys.HOME_LONGITUDE, String.valueOf(currentLocation.getLongitude()));
            }

            editor.apply();

        } else if (rightNow.getHour() > 9) {
            determineWorkplaceLocation();
        }
    }


    /**
     * Will determine the workplace location
     * by checking the location after 11am and before 3pm.
     */
    public void determineWorkplaceLocation() {
        runnable = new Runnable() {
            public void run() {
                if (!isAnalysisPeriod) {
                    handler.removeCallbacks(runnable);
                    return;
                }
                LocalDateTime rightNow = LocalDateTime.now();
                if (rightNow.getDayOfWeek() != DayOfWeek.SATURDAY && rightNow.getDayOfWeek() != DayOfWeek.SUNDAY) {

                    if (rightNow.getHour() >= 11 && rightNow.getHour() <= 15) //in between 11am and 3pm
                    {
                        Location currentLocation = getCurrentLocation();

                        workLongitude.add(currentLocation.getLongitude());
                        workLatitude.add(currentLocation.getLongitude());
                    } else {
                        double latitude = Collections.max(workLatitude);
                        double longitude = Collections.max(workLongitude);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(SPkeys.WORK_LATITUDE, String.valueOf(latitude));
                        editor.putString(SPkeys.WORK_LONGITUDE, String.valueOf(longitude));
                    }

                    Toast.makeText(context, "ONE Hour", Toast.LENGTH_LONG).show();
                    handler.postDelayed(this, 3600000); //Once per hour

                } else {
                    long duration = (24 - rightNow.getHour()) * 3600000; //Time to next midnight in milliseconds
                    handler.postDelayed(this, duration); //To stop use: handler.removeCallbacks(runnable);
                }
            }
        };
        runnable.run();

    }

    /**
     * Once the week of analyzing is over we need to start the {@link SuggestingImprovements} service
     */
    public void checkEndOfAnalysisPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate analysisStartDate = LocalDate.parse(LifestyleMainActivity.ANALYSIS_START_DATE);

        if (today.isAfter(analysisStartDate.plusWeeks(1))) {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
        System.out.println("Wear OS destroy");
    }

}
