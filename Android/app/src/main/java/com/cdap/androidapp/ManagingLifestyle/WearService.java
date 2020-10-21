package com.cdap.androidapp.ManagingLifestyle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
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
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Listens to messages from the watch
 */
public class WearService extends WearableListenerService implements Runnable {

    private ArrayList<Reading> values = new ArrayList<>();    // Stores 200 accelerometer readings
    public static volatile String prediction = "predicting...";
    private Context context;
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
    private URL url = null;
    private PredictionEntity previousPredictionEntity = null;
    private PowerManager.WakeLock wakeLock = null;
    public static Boolean isRunning = false;    //Used to check if service is already running

    private Thread thread;
    private final Object lock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        context = getApplicationContext();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "69")
                .setContentTitle("Receiving sensor readings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.quantum_ic_clear_white_24)
                .setOngoing(true);

        NotificationChannel channel = new NotificationChannel("69", "Readings", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("getting sensor readings from watch");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManagerCompat.notify(69, builder.build());
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WearService.class.getSimpleName());
        wakeLock.acquire();

        thread = new Thread(this);
        thread.start();

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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        System.out.println("Received Message from Watch: " + messageEvent.getSourceNodeId());
        String message = new String(messageEvent.getData());
        String[] lines = message.split("(\\$%\\$%)");

        for (int i = 0; i < 200; i++) {
            Reading reading = new Reading(
                    lines[i].split("#&")[0], // x - axis
                    lines[i].split("#&")[1], // y - axis
                    lines[i].split("#&")[2] // z - axis
            );
            values.add(reading);
        }

        synchronized (lock) { //making prediction via thread
            lock.notify();
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
                synchronized (lock) {
                    lock.wait();
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
                prediction = stringBuilder.toString().replaceAll("\"", "").trim();

                DataBaseManager dataBaseManager = new DataBaseManager(context);
                LocalDateTime localDateTime = LocalDateTime.now();
                PredictionEntity predictionEntity = new PredictionEntity(
                        localDateTime.getDayOfMonth(),
                        localDateTime.getMonthValue(),
                        localDateTime.getYear(),
                        localDateTime.getHour(),
                        localDateTime.getMinute(),
                        prediction
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
}
