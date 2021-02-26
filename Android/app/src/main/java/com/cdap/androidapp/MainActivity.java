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
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.DataBase.BmiEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity implements Runnable, GoogleApiClient.ConnectionCallbacks {

    ////////////////////////////////////// DO NOT MODIFY THESE /////////////////////////////////////////////////
    public static final String PREFERENCES_NAME = "fitness_mobile_game_preferences";
    public static final String DB_NAME = "fitness_mobile_game_DB";
    public static final String SERVER_BASE_URL = "http://34.123.19.46" +
            ":8000";

    //Shared Preference Keys
    public static final String PREFERENCES_USERS_AGE = "user_age";
    public static final String PREFERENCES_USERS_HEIGHT = "user_height";
    public static final String PREFERENCES_USERS_WEIGHT = "user_weight";
    public static final String PREFERENCES_USERS_CURRENT_BMI = "user_bmi";
    public static final String PREFERENCES_USERS_LAST_BMI_READING = "user_last_bmi_reading";
    public static final String PREFERENCES_USERS_GENDER = "user_gender";
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    private ImageView background;
    private HandlerThread handlerThread;
    private Handler handler;
    private Context context;
    private final int NumberOfBackgroundImages = 5;
    private int currentBackgroundImageNumber = 1;
    private SharedPreferences sharedPref;
    private EditText ageInput, heightInput, weightInput;
    private RadioButton maleButton, femaleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (!getIntent().getBooleanExtra("IS_EDIT_MODE", false)) {
            if (sharedPref.contains(MainActivity.PREFERENCES_USERS_AGE)
                    && sharedPref.contains(MainActivity.PREFERENCES_USERS_HEIGHT)
                    && sharedPref.contains(MainActivity.PREFERENCES_USERS_WEIGHT)) {
                Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
                //        intent.putExtra("key", value);
                this.startActivity(intent);
                return;
            }
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
        maleButton = findViewById(R.id.maleButton);
        femaleButton = findViewById(R.id.femaleButton);


        handlerThread = new HandlerThread("slideshowThread"); //Name the handlerThread
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        // handler started in onResume()


    }

    public void toNavigationScreen(View view) {

        if (ageInput.getText().toString().trim().isEmpty()
                || heightInput.getText().toString().trim().isEmpty()
                || weightInput.getText().toString().trim().isEmpty()
                || !maleButton.isChecked() && !femaleButton.isChecked()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        int age;
        int weight;
        int height;
        try {
            age = Integer.parseInt(ageInput.getText().toString());
            weight = Integer.parseInt(weightInput.getText().toString());
            height = Integer.parseInt(heightInput.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(context, "Invalid Input", Toast.LENGTH_LONG).show();
            return;
        }

        if (age <= 0 || age > 99
                || height <= 0 || height > 300
                || weight <= 0 || weight > 500) {
            Toast.makeText(context, "Invalid Input", Toast.LENGTH_LONG).show();
            return;
        }

        double bmi = (weight / ((height / 100.0) * (height / 100.0)));

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(MainActivity.PREFERENCES_USERS_AGE, age);
        editor.putInt(MainActivity.PREFERENCES_USERS_WEIGHT, weight);
        editor.putInt(MainActivity.PREFERENCES_USERS_HEIGHT, height);
        editor.putFloat(MainActivity.PREFERENCES_USERS_CURRENT_BMI, (float) bmi);
        editor.putString(MainActivity.PREFERENCES_USERS_LAST_BMI_READING, LocalDateTime.now().toString());
        if(maleButton.isChecked())
        {
            editor.putString(MainActivity.PREFERENCES_USERS_GENDER, Constants.MALE);
        }
        else
        {
            editor.putString(MainActivity.PREFERENCES_USERS_GENDER, Constants.FEMALE);
        }
        editor.apply();

        GoogleApiClient googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(result -> System.out.println("Connection failed"))
                .addApi(Wearable.API)
                .build();

        // Sending age to watch
        (new Thread(() -> {

            //Saving BMI to Database
            LocalDateTime rightNow = LocalDateTime.now();
            BmiEntity bmiEntity = new BmiEntity((float) bmi, rightNow.getDayOfMonth(), rightNow.getMonthValue(), rightNow.getYear());
            DataBaseManager dataBaseManager = new DataBaseManager(context);
            dataBaseManager.addBmi(bmiEntity);


            List<Node> nodes;
            googleClient.connect();
            String message = "/age";
            byte[] payload = String.valueOf(age).getBytes();
            try {
                nodes = Tasks.await(Wearable.getNodeClient(getApplicationContext()).getConnectedNodes());
                for (Node node : nodes) {
                    Wearable.MessageApi.sendMessage(googleClient, node.getId(), message, payload).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            System.out.println("WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        })).start();

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
        runOnUiThread(() -> background.setImageResource(resourceID));
        background.startAnimation(fadeIn);
    }

    @Override
    public void run() {
        if (getIntent().getBooleanExtra("IS_EDIT_MODE", false)) {
            ageInput.setText(String.valueOf(sharedPref.getInt(MainActivity.PREFERENCES_USERS_AGE,0)));
            heightInput.setText(String.valueOf(sharedPref.getInt(MainActivity.PREFERENCES_USERS_HEIGHT,0)));
            weightInput.setText(String.valueOf(sharedPref.getInt(MainActivity.PREFERENCES_USERS_WEIGHT,0)));
            switch (sharedPref.getString(MainActivity.PREFERENCES_USERS_GENDER,"").toLowerCase())
            {
                case Constants.MALE:
                    maleButton.setChecked(true);
                    femaleButton.setChecked(false);
                    break;
                case Constants.FEMALE:
                    femaleButton.setChecked(true);
                    maleButton.setChecked(false);
                    break;
            }

            int imageNumber = ThreadLocalRandom.current().nextInt(1, NumberOfBackgroundImages + 1);
            int resourceID = getResources().getIdentifier("intro_background_" + imageNumber, "drawable", getPackageName());
            runOnUiThread(() -> background.setImageResource(resourceID));
            return;
        }

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
        if (handler != null) {
            handler.removeCallbacks(this);
            handlerThread.quitSafely();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null) {
            handler.post(this);
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}