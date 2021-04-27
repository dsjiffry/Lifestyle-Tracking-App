package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.BmiEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PercentageEntity;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.PieModel;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class ActivityLifestyleSuggestingImprovements extends AppCompatActivity implements Runnable {

    private PieChart pieChart;
    private ValueLineChart lineChart;
    TextView chartStanding, chartSitting, chartWalking,
            chartStairs, chartJogging, chartNoOfDays,
            suggestionsTopic;
    Context context;
    private DataBaseManager dataBaseManager;
    private SharedPreferences sharedPref;
    private ConstraintLayout suggestionsCard;
    private ListView suggestionsList;
    private int previousTextViewID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_suggesting_improvements);
        context = getApplicationContext();
        pieChart = findViewById(R.id.piechart);
        lineChart = findViewById(R.id.cubiclinechart);
        chartStanding = findViewById(R.id.pieChart_standing_textView);
        chartSitting = findViewById(R.id.pieChart_sitting_textView);
        chartWalking = findViewById(R.id.pieChart_walking_textView);
        chartStairs = findViewById(R.id.pieChart_stairs_textView);
        chartJogging = findViewById(R.id.pieChart_jogging_textView);
        chartNoOfDays = findViewById(R.id.pieChart_noOfDays_textView);
        dataBaseManager = new DataBaseManager(context);
        suggestionsCard = findViewById(R.id.suggestions_card);
        suggestionsTopic = findViewById(R.id.suggestion_topic);
        suggestionsList = findViewById(R.id.suggestionsList);
        previousTextViewID = suggestionsTopic.getId();
        sharedPref = getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);


        Thread thread = new Thread(this);
        thread.start();

    }

    @Override
    public void run() {
        List<PercentageEntity> percentages = dataBaseManager.loadEntireHistory();
        int numberOfDays, standingPercentage, sittingPercentage, walkingPercentage, stairsPercentage, joggingPercentage;
        numberOfDays = standingPercentage = sittingPercentage = walkingPercentage = stairsPercentage = joggingPercentage = 0;


        for (PercentageEntity percentage : percentages) {
            switch (percentage.activity.toLowerCase()) {
                case Constants.STANDING:
                    standingPercentage += percentage.percentage;
                    break;
                case Constants.SITTING:
                    sittingPercentage += percentage.percentage;
                    break;
                case Constants.WALKING:
                    walkingPercentage += percentage.percentage;
                    break;
                case Constants.STAIRS:
                    stairsPercentage += percentage.percentage;
                    break;
                case Constants.JOGGING:
                    joggingPercentage += percentage.percentage;
                    break;
            }
            numberOfDays++;
        }

        numberOfDays = numberOfDays / 5;

        updateLineChart();
        if (numberOfDays > 0) {
            updatePieChart(standingPercentage / numberOfDays,
                    sittingPercentage / numberOfDays,
                    walkingPercentage / numberOfDays,
                    stairsPercentage / numberOfDays,
                    joggingPercentage / numberOfDays,
                    numberOfDays);
        } else {
            emptyPieChart();
        }

        String[] suggestions = sharedPref.getString(Constants.IMPROVEMENTS, "").split(";");
        ArrayList<String> listItems = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        suggestionsList.setAdapter(adapter);
        for (String suggestion : suggestions) {
            if (!suggestion.isEmpty()) {
                listItems.add(suggestion);
            }
        }
        adapter.notifyDataSetChanged();

    }

    public void updatePieChart(int standingPercentage, int sittingPercentage, int walkingPercentage, int stairsPercentage, int joggingPercentage, int numberOfDays) {
        pieChart.addPieSlice(
                new PieModel(
                        "Standing",
                        standingPercentage,
                        getColor(R.color.Standing)));
        pieChart.addPieSlice(
                new PieModel(
                        "Sitting",
                        sittingPercentage,
                        getColor(R.color.Sitting)));
        pieChart.addPieSlice(
                new PieModel(
                        "Walking",
                        walkingPercentage,
                        getColor(R.color.Walking)));
        pieChart.addPieSlice(
                new PieModel(
                        "Stairs",
                        stairsPercentage,
                        getColor(R.color.Stairs)));
        pieChart.addPieSlice(
                new PieModel(
                        "Jogging",
                        joggingPercentage,
                        getColor(R.color.Jogging)));
        runOnUiThread(() -> {
            pieChart.startAnimation();

            chartStanding.setText("Standing (" + standingPercentage + "%)");
            chartSitting.setText("Sitting (" + sittingPercentage + "%)");
            chartWalking.setText("Walking (" + walkingPercentage + "%)");
            chartStairs.setText("Stairs (" + stairsPercentage + "%)");
            chartJogging.setText("Jogging (" + joggingPercentage + "%)");
            chartNoOfDays.setText("Number of days: " + numberOfDays);
        });
    }

    public void updateLineChart() {
        ValueLineSeries series = new ValueLineSeries();
        series.setColor(Color.GREEN);

        DataBaseManager dataBaseManager = new DataBaseManager(context);
        List<BmiEntity> bmiEntities = dataBaseManager.loadBmiHistory();

        int count = 0;
        float endingValue = 0f;

        series.addPoint(new ValueLinePoint("starting point", 0f));
        for (BmiEntity bmiEntity : bmiEntities) {
            count++;
            String day;

            if (bmiEntity.day >= 11 && bmiEntity.day <= 13) {
                day = bmiEntity.day + "th";
            } else {
                switch (bmiEntity.day % 10) {
                    case 1:
                        day = bmiEntity.day + "st";
                        break;
                    case 2:
                        day = bmiEntity.day + "nd";
                        break;
                    case 3:
                        day = bmiEntity.day + "rd";
                        break;
                    default:
                        day = bmiEntity.day + "th";
                }
            }

            String monthString = new DateFormatSymbols().getMonths()[bmiEntity.month - 1];
            monthString = monthString.substring(0, 3);

            series.addPoint(new ValueLinePoint(day + " " + monthString, bmiEntity.bmi));
            endingValue = bmiEntity.bmi;


            if (count >= 5) {
                break;
            }
        }
        series.addPoint(new ValueLinePoint("ending point", endingValue));
        lineChart.addSeries(series);


        //ideal BMI is in the 18.5 to 24.9 range
        ValueLineSeries idealMin = new ValueLineSeries();
        idealMin.setColor(Color.BLUE);
        idealMin.addPoint(new ValueLinePoint("starting point", 18.5f));
        idealMin.addPoint(new ValueLinePoint("ending point", 18.5f));
        lineChart.addSeries(idealMin);

        ValueLineSeries idealMax = new ValueLineSeries();
        idealMax.setColor(Color.BLUE);
        idealMax.addPoint(new ValueLinePoint("starting point", 24.9f));
        idealMax.addPoint(new ValueLinePoint("ending point", 24.9f));
        lineChart.addSeries(idealMax);


        runOnUiThread(() -> lineChart.startAnimation());
    }

    public void emptyPieChart() {
        updatePieChart(0, 0, 0, 0, 0, 0);
    }

}





















