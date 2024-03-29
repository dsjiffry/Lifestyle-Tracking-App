package com.cdap.androidapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cdap.androidapp.ManagingLifestyle.ActivityLifestyleNavigation;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.ManagingLifestyle.PhoneLifestyleService;
import com.cdap.androidapp.ManagingLifestyle.SuggestingLifestyleImprovements;

import java.time.LocalDate;

public class NavigationActivity extends AppCompatActivity implements Runnable {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Making status bar and navigation bar transparent
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().setStatusBarColor(Color.parseColor("#42000000"));
        getWindow().setNavigationBarColor(Color.parseColor("#42000000"));

        context = getApplicationContext();
        setContentView(R.layout.activity_navigation);

        Thread thread = new Thread(this);
        thread.start();
    }

    public void toEmotionSection(View view) {
    }

    public void toLifestyleSection(View view) {
        Intent intent = new Intent(NavigationActivity.this, ActivityLifestyleNavigation.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
    }

    public void toFoodSection(View view) {
    }

    public void toDailyTaskSection(View view) {
    }


    @Override
    public void onBackPressed() {
    }

    @Override
    public void run() {

        //////////////////////////////////// Starting the Lifestyle Background Service ////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Will be taking one week to analyze user's current lifestyle
        SharedPreferences sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (!sharedPref.contains(Constants.IS_ANALYZING_PERIOD) || !sharedPref.contains(Constants.ANALYSIS_START_DATE)) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(Constants.IS_ANALYZING_PERIOD, true);
            editor.putString(Constants.ANALYSIS_START_DATE, LocalDate.now().toString());
            editor.apply();
        }

        //Getting all Permissions
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION,}, 1);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Intent phoneServiceIntent = new Intent(context, PhoneLifestyleService.class);
        Intent suggestingImprovementsIntent = new Intent(context, SuggestingLifestyleImprovements.class);

        context.startService(phoneServiceIntent);

        LocalDate rightNow = LocalDate.now();
        LocalDate analysisStartDate = LocalDate.parse(sharedPref.getString(Constants.ANALYSIS_START_DATE, ""));

        if (rightNow.isAfter(analysisStartDate.plusWeeks(1))) {
            context.startService(suggestingImprovementsIntent);
        }

    }










}