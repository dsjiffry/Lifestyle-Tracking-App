package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LifestyleMainActivity extends AppCompatActivity implements Runnable {

    private Context context;
    public final static String SERVER_URL = "http://192.168.8.140:8000/life";
    public TextView textView;
    private URL url = null;
    private PredictionEntity previousPredictionEntity = null;

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
     * Will send every 200 sensor readings to the server to obtain a prediction
     */
    @Override
    public void run() {
        Intent intent = new Intent(context, WearService.class);
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
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            if (!WearService.isRunning && !nodes.isEmpty()) {
                context.startService(intent);
            }

            if(!WearService.prediction.isEmpty())
            {
               setUITextFromThreads(textView, WearService.prediction);
            }

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