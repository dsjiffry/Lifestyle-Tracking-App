package com.cdap.androidapp.ManagingLifestyle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cdap.androidapp.R;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Listens to messages from the watch
 */
public class WearService extends WearableListenerService implements Serializable {

    public static ArrayList<Reading> values = new ArrayList<>();    // Stores last 200 accelerometer readings
    private PowerManager.WakeLock wakeLock = null;
    public static Boolean isRunning = false;    //Used to check if service is already running

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

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



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WearService.class.getSimpleName());
        wakeLock.acquire();
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
        String message = new String(messageEvent.getData());

        if (message.contains("#&") && values.size() <= 200) {
            Reading reading = new Reading(
                    message.split("#&")[0], // x - axis
                    message.split("#&")[1], // y - axis
                    message.split("#&")[2] // z - axis
            );
            values.add(reading);
//            System.out.println(reading);
        }

    }

}
