package com.cdap.androidapp.ManagingLifestyle;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.Models.SPkeys;
import com.cdap.androidapp.R;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LifestyleMainActivity extends AppCompatActivity implements Runnable {

    private Context context;
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
    public static String ANALYSIS_START_DATE = null;
    public TextView textView;
    private URL url = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        context = getApplicationContext();
        textView = findViewById(R.id.mainText);

        Thread thread = new Thread(this);
        thread.start();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
        }

        SharedPreferences sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);

        //Will be taking one week to analyze user's current lifestyle
        if (!sharedPref.contains("isAnalyzingPeriod") || !sharedPref.contains("analysisStartDate") || ANALYSIS_START_DATE == null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(SPkeys.IS_ANALYZING_PERIOD, true);
            editor.putString(SPkeys.ANALYSIS_START_DATE, LocalDate.now().toString());
            ANALYSIS_START_DATE =  LocalDate.now().toString();
            editor.apply();
        }

        SuggestingImprovements.initialize(context);

    }


    /**
     * Will check if server is reachable, start the {@link PhoneService} and update UI
     */
    @Override
    public void run() {
        Intent intent = new Intent(context, PhoneService.class);
        List<Node> nodes = new ArrayList<>();
        while (!checkServerAvailability()) //Wait till server is available
        {
            setUITextFromThreads(textView, "Server Unavailable");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            nodes = Tasks.await(Wearable.getNodeClient(getApplicationContext()).getConnectedNodes());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            if (!PhoneService.isRunning && !nodes.isEmpty()) {
                context.startService(intent);
            }

            setUITextFromThreads(textView, PhoneService.PREDICTION);

            try {
                Thread.sleep(500);
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
     * @param textView
     * @param message
     */
    public void setUITextFromThreads(final TextView textView, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
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
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }

            if (!stringBuilder.toString().isEmpty()) {
                return true;
            }


        } catch (MalformedURLException e) { //url = new URL(SERVER_URL);
            e.printStackTrace();
        } catch (IOException e) { // URLConnection conn = url.openConnection();
            e.printStackTrace();
        } catch (JSONException e) { // jsonObject.put(...);
            e.printStackTrace();
        }

        return false;
    }


}