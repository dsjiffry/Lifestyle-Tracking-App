package com.cdap.androidapp.DailyRoutine;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.R;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class RoutineMainActivity extends AppCompatActivity {

    private ListView activityList;
    private TextView bmiText;
    private SharedPreferences sharedPref;
    private HashMap<String, Boolean> workout1 = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_main);

        activityList = findViewById(R.id.activityList);
        bmiText = findViewById(R.id.bmiText);

        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (sharedPref.contains(MainActivity.PREFERENCES_USERS_CURRENT_BMI)) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            bmiText.setText("BMI: " + df.format(sharedPref.getFloat(MainActivity.PREFERENCES_USERS_CURRENT_BMI, 0f)));
        } else {
            bmiText.setText("BMI not found");
        }

        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice,
                listItems);
        activityList.setAdapter(adapter);

        listItems.add("Workout");
        listItems.add("Workout");
        listItems.add("Workout");
        listItems.add("Workout");
        listItems.add("Workout");
        adapter.notifyDataSetChanged();


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        activityList.getCheckedItemPositions();
    }
}