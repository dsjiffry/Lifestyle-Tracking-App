package com.cdap.androidapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Runnable {

    ////////////////////////////////////// DO NOT MODIFY THESE /////////////////////////////////////////////////
    public static final String PREFERENCES_NAME = "fitness_mobile_game_preferences";
    public static final String DB_NAME = "fitness_mobile_game_DB";
    public static final String SERVER_BASE_URL = "http://192.168.8.141:8000";
    public static final int DB_VERSION = 1;

    //Shared Preference Keys
    public static final String PREFERENCES_USERS_AGE = "user_age";
    public static final String PREFERENCES_USERS_HEIGHT = "user_height";
    public static final String PREFERENCES_USERS_WEIGHT = "user_weight";
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    private ImageView background;
    private Handler handler;
    private Context context;
    private int NumberOfBackgroundImages = 5;
    private int currentBackgroundImageNumber = 1;
    private SharedPreferences sharedPref;
    private EditText ageInput, heightInput, weightInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (sharedPref.contains(MainActivity.PREFERENCES_USERS_AGE)
                && sharedPref.contains(MainActivity.PREFERENCES_USERS_HEIGHT)
                && sharedPref.contains(MainActivity.PREFERENCES_USERS_WEIGHT)) {
            Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
//        intent.putExtra("key", value);
            this.startActivity(intent);
            return;
        }

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

        context = getApplicationContext();
        ageInput = findViewById(R.id.ageInput);
        heightInput = findViewById(R.id.heightInput);
        weightInput = findViewById(R.id.weightInput);
        background = findViewById(R.id.background_image);



        HandlerThread handlerThread = new HandlerThread("slideshowThread"); //Name the handlerThread
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        // handler started in onResume()


    }

    public void toNavigationScreen(View view) {

        if (ageInput.getText().toString().trim().isEmpty()
                || heightInput.getText().toString().trim().isEmpty()
                || weightInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        int age,weight,height;
        try {
            age = Integer.parseInt(ageInput.getText().toString());
            weight = Integer.parseInt(weightInput.getText().toString());
            height = Integer.parseInt(heightInput.getText().toString());
        }catch (NumberFormatException e)
        {
            Toast.makeText(context, "Invalid Input", Toast.LENGTH_LONG).show();
            return;
        }

        if (age < 0 || age > 99
                && height < 0 || height > 300
                && weight < 0 || weight > 500) {
            Toast.makeText(context, "Invalid Input", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(MainActivity.PREFERENCES_USERS_AGE, age);
        editor.putInt(MainActivity.PREFERENCES_USERS_WEIGHT, weight);
        editor.putInt(MainActivity.PREFERENCES_USERS_HEIGHT, height);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
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
        if (currentBackgroundImageNumber > NumberOfBackgroundImages) {
            currentBackgroundImageNumber = 1;
        }
        setBackgroundImage(currentBackgroundImageNumber);
        currentBackgroundImageNumber++;

        handler.postDelayed(this, 10000); //Stop using: handler.removeCallbacks(this);

    }


    @Override
    protected void onPause() {
        super.onPause();
        if(handler != null) {
            handler.removeCallbacks(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(handler != null) {
            handler.post(this);
        }
    }

    @Override
    public void onBackPressed() {
        return;
    }

}