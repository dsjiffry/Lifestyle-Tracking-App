package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PercentageEntity;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import java.util.List;

public class LifestyleSuggestingImprovementsActivity extends AppCompatActivity implements Runnable {

    private PieChart pieChart;
    TextView chartStanding, chartSitting, chartWalking, chartStairs, chartJogging, chartNoOfDays;
    Context context;
    private DataBaseManager dataBaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle_suggesting_improvements);
        context = getApplicationContext();
        pieChart = findViewById(R.id.piechart);
        chartStanding = findViewById(R.id.pieChart_standing_textView);
        chartSitting = findViewById(R.id.pieChart_sitting_textView);
        chartWalking = findViewById(R.id.pieChart_walking_textView);
        chartStairs = findViewById(R.id.pieChart_stairs_textView);
        chartJogging = findViewById(R.id.pieChart_jogging_textView);
        chartNoOfDays = findViewById(R.id.pieChart_noOfDays_textView);
        dataBaseManager = new DataBaseManager(context);


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


        updatePieChart(standingPercentage, sittingPercentage, walkingPercentage, stairsPercentage, joggingPercentage,numberOfDays);
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

            chartStanding.setText("Standing ("+standingPercentage+"%)");
            chartSitting.setText("Sitting ("+sittingPercentage+"%)");
            chartWalking.setText("Walking ("+walkingPercentage+"%)");
            chartStairs.setText("Stairs ("+stairsPercentage+"%)");
            chartJogging.setText("Jogging ("+joggingPercentage+"%)");
            chartNoOfDays.setText("Number of days: " + numberOfDays);
        });
    }
}





















