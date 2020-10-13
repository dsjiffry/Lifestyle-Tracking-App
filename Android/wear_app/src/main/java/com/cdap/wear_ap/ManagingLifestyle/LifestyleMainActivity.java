package com.cdap.wear_ap.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
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


        (new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final float[] accelerometerReadings = MyService.accelerometerReadings;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mTextView.setText(
                                    "x-axis: " + accelerometerReadings[0] + "\n" +
                                    "y-axis: " + accelerometerReadings[1] + "\n" +
                                    "z-axis: " + accelerometerReadings[2] + "\n");
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
//        setAmbientEnabled();
    }
}