package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.R;

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
import java.time.LocalDateTime;
import java.util.ArrayList;

public class LifestyleMainActivity extends AppCompatActivity implements Runnable {

    private Context context;
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
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

    }

    /**
     * Will make a POST request to backend server and obtain the predicted activity
     *
     * @param values
     */
    private String predictActivity(ArrayList<Reading> values){
        String prediction = null;

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
                temp.put(0, values.get(i).xAxis);
                temp.put(1, values.get(i).yAxis);
                temp.put(2, values.get(i).zAxis);
                readingsArray.put(temp);
            }
            jsonArray.put(readingsArray);
            jsonObject.put("data", jsonArray);

            //Making POST request
            URL url = new URL(SERVER_URL);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
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
            prediction = stringBuilder.toString();
            final String finalPrediction = prediction;
            runOnUiThread(new Runnable() {
                public void run() {
                        textView.setText(finalPrediction);
                }
            });


            DataBaseManager dataBaseManager = new DataBaseManager(context);
            LocalDateTime localDateTime = LocalDateTime.now();
            PredictionEntity predictionEntity = new PredictionEntity(
                    localDateTime.getDayOfMonth(),
                    localDateTime.getMonthValue(),
                    localDateTime.getYear(),
                    localDateTime.getHour(),
                    localDateTime.getMinute(),
                    prediction
            );
            dataBaseManager.addPrediction(predictionEntity);


        } catch (MalformedURLException e) { //url = new URL(SERVER_URL);
            e.printStackTrace();
        } catch (IOException e) { // URLConnection conn = url.openConnection();
            e.printStackTrace();
        } catch (JSONException e) { // jsonObject.put(...);
            e.printStackTrace();
        }
        return prediction;
    }



    @Override
    public void run() {
        Intent intent = new Intent(context, WearService.class);
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText("predicting...");
            }
        });

        while (true) {
            if (!WearService.isRunning) {
                context.startService(intent);
            }
            if (WearService.values.size() >= 200) {
                predictActivity(WearService.values);
                WearService.values.clear();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}