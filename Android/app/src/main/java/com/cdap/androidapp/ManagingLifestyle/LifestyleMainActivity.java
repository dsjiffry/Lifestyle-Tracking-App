package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.cdap.androidapp.R;

import androidx.appcompat.app.AppCompatActivity;

public class LifestyleMainActivity extends AppCompatActivity {

    private Context context;
    public TextView textView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        context = getApplicationContext();
        textView = findViewById(R.id.mainText);


        Intent intent = new Intent(context, WearService.class);
        context.startService(intent);

//        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
//        String[] accelerometer = prefs.getStringSet("accelerometer", new HashSet<String>()).toArray(new String[0]);
//        textView.setText(accelerometer[0] +"\n"+ accelerometer[1] +"\n"+ accelerometer[2] );
    }

    private void loadModel()
    {
        (new Thread(new Runnable(){
            public void run(){

            }
        })).start();
    }
}