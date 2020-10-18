package com.cdap.androidapp;

import android.content.Intent;
import android.os.Bundle;

import com.cdap.androidapp.ManagingLifestyle.LifestyleMainActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ////////////////////////////////////// DO NOT MODIFY THESE /////////////////////////////////////////////////
    public static final String PREFERENCES_NAME = "fitness_mobile_game_preferences";
    public static final String DB_NAME = "fitness_mobile_game_DB";
    public static final int DB_VERSION = 1;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, LifestyleMainActivity.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
    }

}