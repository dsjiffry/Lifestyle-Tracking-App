package com.cdap.androidapp.ManagingLifestyle;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.R;

public class LifestyleNavigationActivity extends AppCompatActivity implements Runnable{

    private ImageView background;
    private Object slideshowLock = new Object();
    private boolean pauseSlideshow = false;

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
        setContentView(R.layout.activity_lifestyle_navigation);
        background = findViewById(R.id.background_image);

        Thread thread = new Thread(this);
        thread.start();
    }

    public void openLifestyleTracking(View v)
    {
        Intent intent = new Intent(LifestyleNavigationActivity.this, LifestyleMainActivity.class);
        this.startActivity(intent);
    }

    public void openSuggestingImprovements(View v)
    {
        Intent intent = new Intent(LifestyleNavigationActivity.this, LifestyleSuggestingImprovementsActivity.class);
        this.startActivity(intent);
    }

    @Override
    public void run() {
        int NumberOfImages = 5;
        int currentImageNumber = 1;
        try {

            while (true) {
                if (currentImageNumber > NumberOfImages) {
                    currentImageNumber = 1;
                }
                setBackgroundImage(currentImageNumber);
                currentImageNumber++;
                if(pauseSlideshow)
                {
                    synchronized (slideshowLock)
                    {
                        slideshowLock.wait();
                    }
                }

                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setBackgroundImage(int imageNumber) {

        int resourceID = getResources().getIdentifier("lifestyle_background_" + imageNumber, "drawable", getPackageName());
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
    protected void onPause() {
        super.onPause();
        pauseSlideshow = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        pauseSlideshow = false;
        synchronized (slideshowLock)
        {
            slideshowLock.notifyAll();
        }
    }
}