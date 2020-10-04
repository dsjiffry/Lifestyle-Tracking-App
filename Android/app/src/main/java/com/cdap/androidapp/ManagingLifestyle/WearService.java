package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.cdap.androidapp.MainActivity;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.Serializable;
import java.util.HashSet;

public class WearService extends WearableListenerService implements Serializable {

    public static String message = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
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
        HashSet<String> values = new HashSet<>();

        if (message.contains("#&")) {
            values.add(message.split("#&")[0]); // x - axis
            values.add(message.split("#&")[1]); // y - axis
            values.add(message.split("#&")[2]); // z - axis

            SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putStringSet("accelerometer", values);
            editor.commit();

        }

        Toast.makeText(getApplicationContext(), "Wear OS Message " + message.replaceAll("#&"," "), Toast.LENGTH_LONG).show();
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
