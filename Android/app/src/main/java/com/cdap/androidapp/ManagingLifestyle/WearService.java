package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.Serializable;
import java.util.ArrayList;

public class WearService extends WearableListenerService implements Serializable {

    public static String message = "";
    public static ArrayList<Reading> values = new ArrayList<>();
    private static final String TAG = WearService.class.getSimpleName();
    private PowerManager.WakeLock wakeLock = null;
    public static Boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
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
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        System.out.println("Wear OS Data changed ");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        message = new String(messageEvent.getData());

        if (message.contains("#&") && values.size() <= 200) {
            Reading reading = new Reading(
                    Double.valueOf(message.split("#&")[0]), // x - axis
                    Double.valueOf(message.split("#&")[1]), // y - axis
                    Double.valueOf(message.split("#&")[2]) // z - axis
            );
            values.add(reading);
        }

//        Toast.makeText(getApplicationContext(), "Wear OS Message " + message.replaceAll("#&"," "), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        System.out.println("Wear OS Connected ");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        System.out.println("Wear OS Disconnected");
    }

}
