package com.cdap.androidapp.ManagingLifestyle;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LifestyleMainActivity extends AppCompatActivity implements Runnable {

    private Context context;
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
    public TextView textView;
    private SharedPreferences sharedPref;
    private URL url = null;

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
        textView = findViewById(R.id.mainText);
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
     */
    @Override
    public void run() {

        SharedPreferences sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        //Will be taking one week to analyze user's current lifestyle
        if (!sharedPref.contains(Constants.IS_ANALYZING_PERIOD) || !sharedPref.contains(Constants.ANALYSIS_START_DATE)) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(Constants.IS_ANALYZING_PERIOD, true);
            editor.putString(Constants.ANALYSIS_START_DATE, LocalDate.now().toString());
            editor.apply();
        }

        Intent phoneServiceIntent = new Intent(context, PhoneLifestyleService.class);
        Intent suggestingImprovementsIntent = new Intent(context, SuggestingLifestyleImprovements.class);
        List<Node> nodes = new ArrayList<>();
        while (!checkServerAvailability()) //Wait till server is available
        {
            setUITextFromThreads(textView, "Server Unavailable");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while (isAnalysisPeriod()) {
            try {
                nodes = Tasks.await(Wearable.getNodeClient(getApplicationContext()).getConnectedNodes());

                if (!PhoneLifestyleService.isRunning && !nodes.isEmpty()) {
                    context.startService(phoneServiceIntent);
                }

                if (nodes.isEmpty()) {
                    PhoneLifestyleService.PREDICTION = "Watch not connected";
                    context.stopService(phoneServiceIntent);
                }

                setUITextFromThreads(textView, PhoneLifestyleService.PREDICTION);
                Thread.sleep(1000);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        while (true) {
            if (!PhoneLifestyleService.isRunning && !nodes.isEmpty()) {
                context.startService(phoneServiceIntent);
            }
            if (!SuggestingLifestyleImprovements.isRunning && !nodes.isEmpty()) {
                context.startService(suggestingImprovementsIntent);
            }

            if(nodes.isEmpty())
            {
                context.stopService(phoneServiceIntent);
                context.stopService(suggestingImprovementsIntent);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
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
        runOnUiThread(() -> textView.setText(message));
    }

    /**
     * @return true if server is reachable
     */
    public boolean checkServerAvailability() {
        try {
            if (url == null) {
                url = new URL(SERVER_URL);
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
            URL url = new URL(SERVER_URL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
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

    private boolean isAnalysisPeriod() {
        LocalDate rightNow = LocalDate.now();
        LocalDate analysisStartDate = LocalDate.parse(sharedPref.getString(Constants.ANALYSIS_START_DATE, ""));

        if (rightNow.isAfter(analysisStartDate.plusWeeks(1))) {
            return false;
        }

        return true;
    }
}