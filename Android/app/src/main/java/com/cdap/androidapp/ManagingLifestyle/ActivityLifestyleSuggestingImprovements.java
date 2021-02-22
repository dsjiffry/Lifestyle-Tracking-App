package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.cdap.androidapp.MainActivity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PercentageEntity;
import com.cdap.androidapp.ManagingLifestyle.Models.Constants;
import com.cdap.androidapp.R;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;

import java.util.List;

public class ActivityLifestyleSuggestingImprovements extends AppCompatActivity implements Runnable {

    private PieChart pieChart;
    TextView chartStanding, chartSitting, chartWalking,
            chartStairs, chartJogging, chartNoOfDays,
            suggestionsTopic;
    Context context;
    private DataBaseManager dataBaseManager;
    private SharedPreferences sharedPref;
    private ConstraintLayout suggestionsCard;
    private int previousTextViewID;

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
        suggestionsCard = findViewById(R.id.suggestions_card);
        suggestionsTopic = findViewById(R.id.suggestion_topic);
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

        if (numberOfDays > 0) {
            updatePieChart(standingPercentage / numberOfDays,
                    sittingPercentage / numberOfDays,
                    walkingPercentage / numberOfDays,
                    stairsPercentage / numberOfDays,
                    joggingPercentage / numberOfDays,
                    numberOfDays);
        }
        else
        {
            emptyPieChart();
        }

        String[] suggestions = sharedPref.getString(Constants.IMPROVEMENTS, "").split(";");
        for (String suggestion : suggestions) {
            addSuggestion(suggestion);
        }
        addSuggestion(""); //empty line at end

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
        series.setColor(0xFF56B7F1);

        series.addPoint(new ValueLinePoint("Jan", 2.4f));
        series.addPoint(new ValueLinePoint("Feb", 3.4f));
        series.addPoint(new ValueLinePoint("Mar", .4f));
        series.addPoint(new ValueLinePoint("Apr", 1.2f));
        series.addPoint(new ValueLinePoint("Mai", 2.6f));
        series.addPoint(new ValueLinePoint("Jun", 1.0f));
        series.addPoint(new ValueLinePoint("Jul", 3.5f));
        series.addPoint(new ValueLinePoint("Aug", 2.4f));
        series.addPoint(new ValueLinePoint("Sep", 2.4f));
        series.addPoint(new ValueLinePoint("Oct", 3.4f));
        series.addPoint(new ValueLinePoint("Nov", .4f));
        series.addPoint(new ValueLinePoint("Dec", 1.3f));

//        lineChart.addSeries(series);
    }

    public void emptyPieChart() {
        updatePieChart(0,0,0,0,0,0);
    }

    public void addSuggestion(String message) {
        TextView textView = new TextView(context);
        textView.setId(View.generateViewId());
        textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setTextColor(Color.WHITE);
        textView.setText(message);

        suggestionsCard.addView(textView);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 150);
        params.setMargins(8, 8, 8, 8);
        textView.setLayoutParams(params);


        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(suggestionsCard);
        constraintSet.connect(textView.getId(), ConstraintSet.TOP, previousTextViewID, ConstraintSet.BOTTOM);
        constraintSet.applyTo(suggestionsCard);

        previousTextViewID = textView.getId();
    }
}





















