package com.cdap.wear_ap.ManagingLifestyle;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MyService extends Service implements Runnable, SensorEventListener, MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private SensorManager sensorManager;
    private Sensor Accelerometer;
    //    private Sensor Gyroscope;
    private GoogleApiClient googleClient;
    private StringBuilder text = new StringBuilder();
    public static float[] accelerometerReadings = {0, 0, 0};
    //    private float[] GyroscopeReadings = {0, 0, 0};
    private ArrayList<SensorEvent> readings = new ArrayList<>();
    public static boolean sendMessage = false;
    private Object lock = new Object();

    private String message;
    private byte[] payload;
    public Thread thread;

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        Gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(this, Accelerometer, 50000);
//        sensorManager.registerListener(this, Gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        System.out.println("Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();

        googleClient.connect();

        thread = new Thread(this);
        thread.start();


        return START_STICKY;
    }


//  ----------------------------------------------------- Created Methods -----------------------------------------------------

    /**
     * Will store accelerometer reading and send to phone
     *
     * @param sensorEvent
     */
    @WorkerThread
    private void onNewAccelerometerValue(SensorEvent sensorEvent) {
        if (readings.size() < 200) {
            readings.add(sensorEvent);
        } else {
            for (SensorEvent reading : readings) //can separate values by splitting #& and lines by splitting $%$%
            {
                text.append(reading.values[0] + "#&" + reading.values[1] + "#&" + reading.values[2] + "$%$%");
            }

            setMessage("/accelerometer", text.toString().getBytes());
//            sendMessage = true;
            synchronized (lock) {
                lock.notify();
            }


            text.setLength(0); //emptying buffer
            readings.clear();
        }

//        text.append(sensorEvent.values[0] + "#&" + sensorEvent.values[1] + "#&" + sensorEvent.values[2]); //can separate values by splitting #&
    }

    /**
     * Create and send message to node on Seperate thread
     *
     * @param message path
     * @param payload payload to send
     */
    private void setMessage(String message, byte[] payload) {
        this.message = message;
        this.payload = payload;
    }


//  ----------------------------------------------------- Overridden Methods -----------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (Arrays.equals(accelerometerReadings, sensorEvent.values)) // No change in readings
            {
                return;
            }
            onNewAccelerometerValue(sensorEvent);
            for (int i = 0; i < 3; i++) {
                accelerometerReadings[i] = sensorEvent.values[i];
            }
        }
    }


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

            List<Node> nodes;

            try {
                nodes = Tasks.await(Wearable.getNodeClient(getApplicationContext()).getConnectedNodes());
                for (Node node : nodes) {
                    System.out.println("WEAR sending " + message + " to " + node);
                    Wearable.MessageApi.sendMessage(googleClient, node.getId(), message, payload).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            System.out.println("WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MyService.sendMessage = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
//        System.out.println("Message Received: "+messageEvent.toString());
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener(googleClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("Connection Failed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

}
