package com.cdap.androidapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.LifestyleMainActivity;

public class MainActivity extends AppCompatActivity implements Runnable {

    ////////////////////////////////////// DO NOT MODIFY THESE /////////////////////////////////////////////////
    public static final String PREFERENCES_NAME = "fitness_mobile_game_preferences";
    public static final String DB_NAME = "fitness_mobile_game_DB";
    public static final int DB_VERSION = 1;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    private ImageView background;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Making status bar transparent
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);
        background = findViewById(R.id.background_image);

        thread = new Thread(this);
        thread.start();


    }

    public void temp_lifestyle(View view) {
        Intent intent = new Intent(MainActivity.this, LifestyleMainActivity.class);
//        intent.putExtra("key", value);
        this.startActivity(intent);
        thread.interrupt();
    }

    public void setBackgroundImage(int imageNumber) {

        int resourceID = getResources().getIdentifier("background_" + imageNumber, "drawable", getPackageName());
        Animation fadeIn = new AlphaAnimation(0.1f, 1);
        Animation fadeOut = new AlphaAnimation(1, 0.1f);
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
        int NumberOfImages = 4;
        int currentImageNumber = 1;
        try {

            while (true) {
                if (currentImageNumber > NumberOfImages) {
                    currentImageNumber = 1;
                }
                setBackgroundImage(currentImageNumber);
                currentImageNumber++;
                Thread.sleep(10000);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}