package com.cdap.androidapp;

import android.content.Intent;
import android.os.Bundle;

import com.cdap.androidapp.ManagingLifestyle.LifestyleMainActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String PREFERENCES_NAME = "preferences_file";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, LifestyleMainActivity.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
    }

}