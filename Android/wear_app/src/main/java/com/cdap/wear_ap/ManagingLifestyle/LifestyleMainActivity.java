package com.cdap.wear_ap.ManagingLifestyle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.cdap.wear_ap.R;

import java.time.LocalDateTime;

public class LifestyleMainActivity extends WearableActivity {

    private TextView mTextView;
    private Context context;
    private final Object displayLock = new Object();
    private PowerManager.WakeLock wakeLock;

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

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                                    "\n\n"+
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

    @SuppressLint("WakelockTimeout")
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mTextView.setTextColor(Color.GRAY);
        mTextView.getPaint().setAntiAlias(false);
        PowerManager powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LifestyleMainActivity.class.getSimpleName());
        wakeLock.acquire();

        onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mTextView.setTextColor(Color.WHITE);
        mTextView.getPaint().setAntiAlias(true);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        synchronized (displayLock) {
            displayLock.notifyAll();
        }
    }

    /**
     * Called once per minute when in ambient mode
     */
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        final Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        LocalDateTime rightNow = LocalDateTime.now();
        StringBuilder message = new StringBuilder();
        message.append("\n");// Fixing spacing


        if (rightNow.getMinute() % 2 == 0) //Preventing amoled burn in
        {
            message.append("\n\n\n\n");
        }

        String prefix;
        if(rightNow.getHour() > 12) {
            message.append(rightNow.getHour() - 12).append(":");
            prefix = " pm";
        }
        else
        {
            message.append(rightNow.getHour()).append(":");
            prefix = " am";
        }
        if(rightNow.getMinute() < 10)
        {
            message.append("0");
        }
        message.append(rightNow.getMinute());
        message.append(prefix).append("\n").append("Always-on mode").append("\n").append("Battery Level ").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).append("%");

        mTextView.setText(message.toString());


    }


}