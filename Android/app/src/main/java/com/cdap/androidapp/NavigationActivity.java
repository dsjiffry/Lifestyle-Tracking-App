package com.cdap.androidapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.LifestyleNavigationActivity;
import com.cdap.androidapp.ManagingLifestyle.PhoneLifestyleService;

public class NavigationActivity extends AppCompatActivity implements Runnable{

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
//        Intent intent = new Intent(NavigationActivity.this, .class);
////        intent.putExtra("key", value);
//        this.startActivity(intent);
    }

    public void toLifestyleSection(View view) {
        Intent intent = new Intent(NavigationActivity.this, LifestyleNavigationActivity.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
    }

    public void toFoodSection(View view) {
//        Intent intent = new Intent(NavigationActivity.this, .class);
////        intent.putExtra("key", value);
//        this.startActivity(intent);
    }

    public void toDailyTaskSection(View view) {
//        Intent intent = new Intent(NavigationActivity.this, .class);
////        intent.putExtra("key", value);
//        this.startActivity(intent);
    }






    @Override
    public void onBackPressed() {
    }

    @Override
    public void run() {

        //////////////////////////////////// Starting the Lifestyle Background Service ////////////////////////////////////

        Intent phoneServiceIntent = new Intent(context, PhoneLifestyleService.class);
        context.startService(phoneServiceIntent);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
}