package com.cdap.androidapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.LifestyleNavigationActivity;

public class MainActivity extends AppCompatActivity implements Runnable {

    ////////////////////////////////////// DO NOT MODIFY THESE /////////////////////////////////////////////////
    public static final String PREFERENCES_NAME = "fitness_mobile_game_preferences";
    public static final String DB_NAME = "fitness_mobile_game_DB";
    public static final String SERVER_BASE_URL = "http://192.168.8.141:8000";
    public static final int DB_VERSION = 1;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    private ImageView background;
    private Handler handler;
    private int NumberOfImages = 5;
    private int currentImageNumber = 1;

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


        setContentView(R.layout.activity_main);
        background = findViewById(R.id.background_image);

        HandlerThread handlerThread = new HandlerThread("slideshowThread"); //Name the handlerThread
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        // handler started in onResume()


    }

    public void temp_lifestyle(View view) {
        Intent intent = new Intent(MainActivity.this, LifestyleNavigationActivity.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
    }

    public void setBackgroundImage(int imageNumber) {

        int resourceID = getResources().getIdentifier("intro_background_" + imageNumber, "drawable", getPackageName());
        Animation fadeIn = new AlphaAnimation(0.01f, 1);
        Animation fadeOut = new AlphaAnimation(1, 0.01f);
        fadeIn.setDuration(2000);
        fadeOut.setDuration(2000);
        fadeIn.reset();
        fadeOut.reset();

        background.startAnimation(fadeOut);
        try {
            Thread.sleep(1900);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runOnUiThread(() -> {
            background.setImageResource(resourceID);
        });
        background.startAnimation(fadeIn);
    }

    @Override
    public void run() {
        if (currentImageNumber > NumberOfImages) {
            currentImageNumber = 1;
        }
        setBackgroundImage(currentImageNumber);
        currentImageNumber++;

        handler.postDelayed(this, 10000); //Stop using: handler.removeCallbacks(this);

    }


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(this);
    }
}