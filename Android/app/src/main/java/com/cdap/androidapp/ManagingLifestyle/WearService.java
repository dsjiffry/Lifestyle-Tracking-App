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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WearService extends WearableListenerService implements Serializable {

    public static String message = "";
    public static ArrayList<String> values = new ArrayList<>();


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
        if(values.size() == 0) {
            values.add("0");
            values.add("0");
            values.add("0");
        }
        message = new String(messageEvent.getData());
//        HashSet<String> values = new HashSet<>();

        if (message.contains("#&")) {
            values.set(0, message.split("#&")[0]); // x - axis
            values.set(1,message.split("#&")[1]); // y - axis
            values.set(2,message.split("#&")[2]); // z - axis
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
