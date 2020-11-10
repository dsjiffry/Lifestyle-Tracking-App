package com.cdap.androidapp.ManagingLifestyle;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class LifestyleMainActivity extends AppCompatActivity implements Runnable {

    private Context context;
    private SharedPreferences sharedPref;
    private URL url = null;

    public ImageView card_current_activity_icon, card_exercise_type_icon;

    public TextView card_current_activity_subtext, card_home_location_subtext, card_work_location_subtext,
            card_wake_time_subtext, card_work_hours_subtext, card_work_travel_subtext,
            card_exercise_time_subtext, card_exercise_type_subtext, card_sleep_time_subtext,
            card_basic_detail_subtext;


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

        setContentView(R.layout.activity_lifestyle_main);
        context = getApplicationContext();
        card_current_activity_icon = findViewById(R.id.card_current_activity_icon);
        card_exercise_type_icon = findViewById(R.id.card_exercise_type_icon);

        card_current_activity_subtext = findViewById(R.id.card_current_activity_subtext);
        card_home_location_subtext = findViewById(R.id.card_home_location_subtext);
        card_work_location_subtext = findViewById(R.id.card_work_location_subtext);
        card_wake_time_subtext = findViewById(R.id.card_wake_time_subtext);
        card_work_hours_subtext = findViewById(R.id.card_work_hours_subtext);
        card_work_travel_subtext = findViewById(R.id.card_work_travel_subtext);
        card_exercise_time_subtext = findViewById(R.id.card_exercise_time_subtext);
        card_exercise_type_subtext = findViewById(R.id.card_exercise_type_subtext);
        card_sleep_time_subtext = findViewById(R.id.card_sleep_time_subtext);
        card_basic_detail_subtext = findViewById(R.id.card_basic_details_subtext);

        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
        }   //Granted permission will fire onRequestPermissionsResult(...)
        else {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!ArrayUtils.contains(grantResults, PackageManager.PERMISSION_DENIED)) {
            Thread thread = new Thread(this);
            thread.start();
        } else {
            Toast.makeText(context, "Location permission is Needed", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    /**
     * Will check if server is reachable, start the {@link PhoneLifestyleService} and update UI
     * Also handles starting of {@link SuggestingLifestyleImprovements}
     */
    @Override
    public void run() {

        Intent phoneServiceIntent = new Intent(context, PhoneLifestyleService.class);
        Intent suggestingImprovementsIntent = new Intent(context, SuggestingLifestyleImprovements.class);
        List<Node> nodes;
        PhoneLifestyleService.IS_SERVER_REACHABLE = isServerAvailable();

//        context.startService(suggestingImprovementsIntent);

        while (true) {
            try {

                updateUI();
                if (!PhoneLifestyleService.isRunning) {
                    PhoneLifestyleService.IS_SERVER_REACHABLE = isServerAvailable();
                }

                if (!PhoneLifestyleService.IS_SERVER_REACHABLE) // Server becomes unreachable
                {
                    context.stopService(phoneServiceIntent);
                    PhoneLifestyleService.isRunning = false;
                    setUITextFromThreads(card_current_activity_subtext, "Server Unavailable");
                    runOnUiThread(() ->
                            card_current_activity_icon.setImageResource(R.drawable.ic_server_unavailable)
                    );
                    while (!isServerAvailable()) //Wait till server is available
                    {
                        Thread.sleep(10000);
                    }
                    PhoneLifestyleService.IS_SERVER_REACHABLE = true;
                    context.startService(phoneServiceIntent);
                    setUITextFromThreads(card_current_activity_subtext, "predicting...");
                }

                nodes = Tasks.await(Wearable.getNodeClient(getApplicationContext()).getConnectedNodes());
                if (isAnalysisPeriod()) {


                    if (!PhoneLifestyleService.isRunning && !nodes.isEmpty()) {
                        context.startService(phoneServiceIntent);
                    }

                    if (nodes.isEmpty()) {
                        PhoneLifestyleService.PREDICTION = "Watch not connected";
                        context.stopService(phoneServiceIntent);
                        PhoneLifestyleService.isRunning = false;
                    }
                } else {

                    if (!PhoneLifestyleService.isRunning && !nodes.isEmpty()) {
                        context.startService(phoneServiceIntent);
                    }
                    if (!SuggestingLifestyleImprovements.isRunning && !nodes.isEmpty()) {
                        context.startService(suggestingImprovementsIntent);
                    }
                    if (nodes.isEmpty()) {
                        context.stopService(phoneServiceIntent);
                        PhoneLifestyleService.isRunning = false;
                        context.stopService(suggestingImprovementsIntent);
                        SuggestingLifestyleImprovements.isRunning = false;
                    }

                }

                Thread.sleep(1000);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }


    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////// METHODS THAT MAKE LIFE EASIER //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Threads cannot directly modify ui, those requests need to be run on the UI thread.
     *
     * @param textView to be modified
     * @param message  to be set in the TextView
     */
    public void setUITextFromThreads(final TextView textView, final String message) {
        runOnUiThread(() ->
                textView.setText(message)
        );
    }

    /**
     * Will update all the cards and their details on the UI
     */
    void updateUI() {
        try {

            setUITextFromThreads(card_current_activity_subtext, PhoneLifestyleService.PREDICTION);

            // Basic Details
            if (sharedPref.contains(MainActivity.PREFERENCES_USERS_AGE)
                    && sharedPref.contains(MainActivity.PREFERENCES_USERS_HEIGHT)
                    && sharedPref.contains(MainActivity.PREFERENCES_USERS_WEIGHT)) {
                int age = sharedPref.getInt(MainActivity.PREFERENCES_USERS_AGE, 0);
                int height = sharedPref.getInt(MainActivity.PREFERENCES_USERS_HEIGHT, 0);
                int weight = sharedPref.getInt(MainActivity.PREFERENCES_USERS_WEIGHT, 0);

                String details = "Age: " + age + "\n" + "Height: " + height + "cm\t\t" + "Weight: " + weight + "Kg";
                setUITextFromThreads(card_basic_detail_subtext, details);
            }

            // Home Location
            if (sharedPref.contains(Constants.HOME_LATITUDE) && sharedPref.contains(Constants.HOME_LONGITUDE)) {
                double latitude = Double.parseDouble(sharedPref.getString(Constants.HOME_LATITUDE, ""));
                double longitude = Double.parseDouble(sharedPref.getString(Constants.HOME_LONGITUDE, ""));
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String address = geocoder.getFromLocation(latitude, longitude, 1).get(0).getAddressLine(0);
                setUITextFromThreads(card_home_location_subtext, address);
            }

            // Work Location
            if (sharedPref.contains(Constants.WORK_LATITUDE) && sharedPref.contains(Constants.WORK_LONGITUDE)) {
                double latitude = Double.parseDouble(sharedPref.getString(Constants.WORK_LATITUDE, ""));
                double longitude = Double.parseDouble(sharedPref.getString(Constants.WORK_LONGITUDE, ""));
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String address = geocoder.getFromLocation(latitude, longitude, 1).get(0).getAddressLine(0);
                setUITextFromThreads(card_work_location_subtext, address);
            }

            // Wake up Time
            if (sharedPref.contains(Constants.WAKE_TIME_HOUR) && sharedPref.contains(Constants.WAKE_TIME_MINUTE)) {
                int hour = sharedPref.getInt(Constants.WAKE_TIME_HOUR, -1);
                int minute = sharedPref.getInt(Constants.WAKE_TIME_MINUTE, -1);
                String prefix = "am";
                if (hour >= 12) {
                    prefix = "pm";
                    if (hour > 12) {
                        hour = hour - 12;
                    }
                }
                StringBuilder time = new StringBuilder();
                time.append(hour).append(":");
                if (minute < 10) {
                    time.append("0");
                }
                time.append(minute).append(" ").append(prefix);
                setUITextFromThreads(card_wake_time_subtext, time.toString());
            }

            // Work Hours
            if (sharedPref.contains(Constants.WORK_START_TIME_HOUR) && sharedPref.contains(Constants.WORK_START_TIME_MINUTE)
                    && sharedPref.contains(Constants.WORK_END_TIME_HOUR) && sharedPref.contains(Constants.WORK_END_TIME_MINUTE)) {
                StringBuilder workHours = new StringBuilder();
                int hour = sharedPref.getInt(Constants.WORK_START_TIME_HOUR, -1);
                int minute = sharedPref.getInt(Constants.WORK_START_TIME_MINUTE, -1);
                String prefix = "am";
                if (hour >= 12) {
                    prefix = "pm";
                    if (hour > 12) {
                        hour = hour - 12;
                    }
                }
                workHours.append("From").append(hour).append(":");
                if (minute < 10) {
                    workHours.append("0");
                }
                workHours.append(minute).append(" ").append(prefix).append(" to ");

                hour = sharedPref.getInt(Constants.WORK_END_TIME_HOUR, -1);
                minute = sharedPref.getInt(Constants.WORK_END_TIME_MINUTE, -1);
                prefix = "am";
                if (hour >= 12) {
                    prefix = "pm";
                    if (hour > 12) {
                        hour = hour - 12;
                    }
                }
                workHours.append(hour).append(":");
                if (minute < 10) {
                    workHours.append("0");
                }
                workHours.append(minute).append(" ").append(prefix);
                setUITextFromThreads(card_work_hours_subtext, workHours.toString());
            }


            //Travel to work by
            if (sharedPref.contains(Constants.WORK_TRAVEL_METHOD)) {
                String method = sharedPref.getString(Constants.WORK_TRAVEL_METHOD, "");
                setUITextFromThreads(card_work_travel_subtext, method);
            }

            //Exercise Time
            if (sharedPref.contains(Constants.EXERCISE_TIME_HOUR) && sharedPref.contains(Constants.EXERCISE_TIME_MINUTE)) {
                int hour = sharedPref.getInt(Constants.EXERCISE_TIME_HOUR, -1);
                int minute = sharedPref.getInt(Constants.EXERCISE_TIME_MINUTE, -1);
                String prefix = "am";
                if (hour >= 12) {
                    prefix = "pm";
                    if (hour > 12) {
                        hour = hour - 12;
                    }
                }
                StringBuilder time = new StringBuilder();
                time.append(hour).append(":");
                if (minute < 10) {
                    time.append("0");
                }
                time.append(minute).append(" ").append(prefix);
                setUITextFromThreads(card_exercise_time_subtext, time.toString());
            }

            // Exercise Type
            if (sharedPref.contains(Constants.EXERCISE_TYPE)) {
                String method = sharedPref.getString(Constants.EXERCISE_TYPE, "");
                setUITextFromThreads(card_exercise_type_subtext, method);
            }

            // Sleep Time
            if (sharedPref.contains(Constants.SLEEP_TIME_HOUR) && sharedPref.contains(Constants.SLEEP_TIME_MINUTE)) {
                int hour = sharedPref.getInt(Constants.SLEEP_TIME_HOUR, -1);
                int minute = sharedPref.getInt(Constants.SLEEP_TIME_MINUTE, -1);
                String prefix = "am";
                if (hour >= 12) {
                    prefix = "pm";
                    if (hour > 12) {
                        hour = hour - 12;
                    }
                }
                StringBuilder time = new StringBuilder();
                time.append(hour).append(":");
                if (minute < 10) {
                    time.append("0");
                }
                time.append(minute).append(" ").append(prefix);
                setUITextFromThreads(card_sleep_time_subtext, time.toString());
            }


            setIconFromThreads();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Threads cannot directly modify ui, those requests need to be run on the UI thread.
     */
    public void setIconFromThreads() {

        final int prediction_resID;
        switch (PhoneLifestyleService.PREDICTION.toLowerCase()) {
            case Constants.STANDING:
                prediction_resID = R.drawable.ic_standing_icon;
                break;
            case Constants.SITTING:
                prediction_resID = R.drawable.ic_sitting_icon;
                break;
            case Constants.WALKING:
                prediction_resID = R.drawable.ic_walking_icon;
                break;
            case Constants.JOGGING:
                prediction_resID = R.drawable.ic_jogging_icon;
                break;
            case Constants.STAIRS:
                prediction_resID = R.drawable.ic_stairs_icon;
                break;
            case "watch is charging":
                prediction_resID = R.drawable.ic_watch_charging;
                break;
//            case "server unavailable":
//                prediction_resID = R.drawable.ic_server_unavailable;
//                break;
            default:
                prediction_resID = R.drawable.ic_predicting;
                break;
        }

        runOnUiThread(() ->
                card_current_activity_icon.setImageResource(prediction_resID)
        );

        if (sharedPref.contains(Constants.EXERCISE_TYPE)) {
            final int exercise_type_resID;
            String exerciseType = sharedPref.getString(Constants.EXERCISE_TYPE, "");
            switch (exerciseType.toLowerCase()) {
                case Constants.RUNNING:
                    exercise_type_resID = R.drawable.ic_exercise_type_running;
                    break;
                case Constants.GYM:
                    exercise_type_resID = R.drawable.ic_exercise_type_gym;
                    break;
                default:
                    exercise_type_resID = R.drawable.ic_predicting;
                    break;
            }
            runOnUiThread(() ->
                    card_exercise_type_icon.setImageResource(exercise_type_resID)
            );
        }
    }

    /**
     * @return true if server is reachable
     */
    public boolean isServerAvailable() {
        try {
            if (url == null) {
                url = new URL(PhoneLifestyleService.SERVER_URL);
            }

            //Creating JSON body to send
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            JSONArray readingsArray = new JSONArray();
            for (int i = 0; i < 200; i++) {
                JSONArray temp = new JSONArray();
                temp.put(0, 0.0);
                temp.put(1, 0.0);
                temp.put(2, 0.0);
                readingsArray.put(temp);
            }
            jsonArray.put(readingsArray);
            jsonObject.put("data", jsonArray);

            //Making POST request
            URL url = new URL(PhoneLifestyleService.SERVER_URL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setConnectTimeout(1000);
            httpURLConnection.connect();

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(jsonObject.toString());
            wr.flush();
            wr.close();

            //Getting Response
            InputStream response = httpURLConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            if (!stringBuilder.toString().isEmpty()) {
                return true;
            }


        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * @return true if still in the first week of running the app (analysis period)
     */
    private boolean isAnalysisPeriod() {
        LocalDate rightNow = LocalDate.now();
        LocalDate analysisStartDate = LocalDate.parse(sharedPref.getString(Constants.ANALYSIS_START_DATE, ""));

        return !rightNow.isAfter(analysisStartDate.plusWeeks(1));
    }

    /**
     * will send user to Activity to edit their basic details
     */
    public void editButton(View view) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(MainActivity.PREFERENCES_USERS_WEIGHT);
        editor.remove(MainActivity.PREFERENCES_USERS_AGE);
        editor.remove(MainActivity.PREFERENCES_USERS_HEIGHT);
        editor.apply();

        Intent intent = new Intent(LifestyleMainActivity.this, MainActivity.class);
        this.startActivity(intent);
    }

}