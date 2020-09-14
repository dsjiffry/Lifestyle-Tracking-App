package com.cdap.wear_ap;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

import com.cdap.wear_ap.ManagingLifestyle.LifestyleMainActivity;

public class MainActivity extends WearableActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, LifestyleMainActivity.class);
//        intent.putExtra("key", value);
        startActivity(intent);

    }
}
