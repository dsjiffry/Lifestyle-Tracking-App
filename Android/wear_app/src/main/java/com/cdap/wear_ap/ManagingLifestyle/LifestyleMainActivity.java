package com.cdap.wear_ap.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.WindowManager;
import android.widget.TextView;

import com.cdap.wear_ap.R;

public class LifestyleMainActivity extends WearableActivity {

    private TextView mTextView;
    private Context context;
//    private float[] accelerometerReadings = {0, 0, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        context = getApplicationContext();
        mTextView = findViewById(R.id.textView);

        Intent intent = new Intent(this, MyService.class);
        context.startService(intent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        (new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final Intent batteryStatus =  registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    final float[] accelerometerReadings = MyService.accelerometerReadings;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mTextView.setText(
                                    "x-axis: " + accelerometerReadings[0] + "\n" +
                                    "y-axis: " + accelerometerReadings[1] + "\n" +
                                    "z-axis: " + accelerometerReadings[2] + "\n\n" +
                                    "Battery Level "+batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)+"%"
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


//        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }











}