package com.cdap.wear_ap.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.cdap.wear_ap.R;

public class LifestyleMainActivity extends WearableActivity {

    private TextView mTextView;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);

        context = getApplicationContext();
        mTextView = findViewById(R.id.textView);

        context.startService(new Intent(this, MyService.class));

//        // Enables Always-on
//        setAmbientEnabled();
    }
}