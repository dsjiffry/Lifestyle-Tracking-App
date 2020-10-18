package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class LifestyleMainActivity extends AppCompatActivity {

    private Context context;
    public final static String SERVER_URL = "https://en2ziscd63aas.x.pipedream.net";
    public TextView textView;
    URL url = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        context = getApplicationContext();
        textView = findViewById(R.id.mainText);


        Intent intent = new Intent(context, WearService.class);
        context.startService(intent);

//        predictActivity(WearService.values);


        (new Thread(new Runnable() {
            public void run() {
//                if(WearService.values.size() == 200) {
                    predictActivity(WearService.values);
                    WearService.values.clear();
//                }
                while (true) {
                    final ArrayList<Double> values = WearService.values;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!values.isEmpty()) {
                                textView.setText(values.get(0) + "\n" + values.get(1) + "\n" + values.get(2));
                            }
                        }
                    });
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        })).start();

    }

    /**
     * Will make a POST request to backend server and obtain the predicted activity
     *
     * @param values
     */
    private String predictActivity(ArrayList<Double> values) {
        String prediction = null;

        try {
            //Making POST request
            if (url == null) {
                url = new URL(SERVER_URL);
            }
            URLConnection conn = url.openConnection();

            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(values.get(0));
            jsonObject.put("data",jsonArray);


            conn.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
            outputStreamWriter.write( jsonObject.toString() );
            outputStreamWriter.flush();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            while((prediction = bufferedReader.readLine()) != null)
            {
                stringBuilder.append((prediction +"\n"));
            }
            prediction = stringBuilder.toString();
            System.out.println(prediction);

            DataBaseManager dataBaseManager = new DataBaseManager(context);
            dataBaseManager.addPrediction();


        } catch (MalformedURLException e) { //url = new URL(SERVER_URL);
            e.printStackTrace();
        } catch (IOException e) { // URLConnection conn = url.openConnection();
            e.printStackTrace();
        } catch (JSONException e) { // jsonObject.put(...);
            e.printStackTrace();
        }
        return prediction;
    }


}