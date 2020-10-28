package com.cdap.wear_ap.ManagingLifestyle;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.cdap.wear_ap.R;

import java.time.LocalDateTime;

public class LifestyleMainActivity extends WearableActivity {

    private TextView mTextView;
    private Context context;
    private final Object displayLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BODY_SENSORS,
        }, 1);

        context = getApplicationContext();
        mTextView = findViewById(R.id.textView);

        final Intent intent = new Intent(this, WatchService.class);
        startService(intent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setAmbientEnabled();


        (new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (isAmbient()) {
                        synchronized (displayLock) {
                            try {
                                displayLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    final Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    final float[] accelerometerReadings = WatchService.accelerometerReadings;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mTextView.setText(
                                    "x-axis: " + accelerometerReadings[0] + "\n" +
                                            "y-axis: " + accelerometerReadings[1] + "\n" +
                                            "z-axis: " + accelerometerReadings[2] + "\n\n" +
                                            "Battery Level " + batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + "%"
                            );
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mTextView.setTextColor(Color.GRAY);
        mTextView.getPaint().setAntiAlias(false);
        onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mTextView.setTextColor(Color.WHITE);
        mTextView.getPaint().setAntiAlias(true);
        synchronized (displayLock) {
            displayLock.notifyAll();
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        final Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        LocalDateTime rightNow = LocalDateTime.now();
        mTextView.setText(
                rightNow.getHour() + ":" + rightNow.getMinute() + "\n" +
                        "Always-on mode" + "\n\n" +
                        "Battery Level " + batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + "%"
        );
    }




}