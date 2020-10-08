package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class LifestyleMainActivity extends AppCompatActivity {

    private Context context;
    public TextView textView;

    private float[] results;
    private ActivityClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_main);
        context = getApplicationContext();
        textView = findViewById(R.id.mainText);
        classifier = new ActivityClassifier(getApplicationContext());


        Intent intent = new Intent(context, WearService.class);
        context.startService(intent);

        List<Float> data = new ArrayList<>();
        for(int i=0; i<600; i++)
            data.add(0f);
        predictActivity(data);

        (new Thread(new Runnable() {
            public void run() {
                while(true) {
                    final ArrayList<String> values = WearService.values;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if(!values.isEmpty()) {
                                textView.setText(values.get(0) + "\n" + values.get(1) + "\n" + values.get(2));

                                List<Float> data = new ArrayList<>();
                                data.add(Float.parseFloat(values.get(0)));
                                data.add(Float.parseFloat(values.get(1)));
                                data.add(Float.parseFloat(values.get(2)));
//                                predictActivity(data);
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

    private void predictActivity(List<Float> data)
    {
        results = classifier.predict(toFloatArray(data));
        System.out.println("predicted Activity: "+ Arrays.toString(results));
    }

    private float[] toFloatArray(List<Float> data)
    {
        int i = 0;
        float[] array = new float[data.size()];
        for(Float f : data)
        {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }
}