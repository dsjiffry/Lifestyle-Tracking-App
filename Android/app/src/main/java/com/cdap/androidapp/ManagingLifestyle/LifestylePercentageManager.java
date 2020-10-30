package com.cdap.androidapp.ManagingLifestyle;

import android.content.Context;

import com.cdap.androidapp.ManagingLifestyle.DataBase.DataBaseManager;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PercentageEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.PredictionEntity;
import com.cdap.androidapp.ManagingLifestyle.DataBase.UserActivities;

import java.util.List;

public class LifestylePercentageManager {

    public static void saveDailyPercentages(final Context context, final PredictionEntity previousPredictionEntity) {
        (new Thread(new Runnable() {
            public void run() {

                DataBaseManager dataBaseManager = new DataBaseManager(context);
                int day = previousPredictionEntity.day;
                int month = previousPredictionEntity.month;
                int year = previousPredictionEntity.year;

                List<PredictionEntity> predictions = dataBaseManager.getAllPredictions(day, month, year);
                int total = predictions.size();
                int standing, sitting, walking, stairs, jogging, unknown;
                standing = sitting = walking = stairs = jogging = unknown = 0;

                for (PredictionEntity predictionEntity : predictions) {
                    switch (predictionEntity.activity) {
                        case UserActivities.STANDING:
                            standing++;
                            break;
                        case UserActivities.SITTING:
                            sitting++;
                            break;
                        case UserActivities.WALKING:
                            walking++;
                            break;
                        case UserActivities.STAIRS:
                            stairs++;
                            break;
                        case UserActivities.JOGGING:
                            jogging++;
                            break;
                        default:
                            unknown++;
                            break;
                    }
                }

                int percentage = (int) (((double) standing / total) * 100);
                PercentageEntity percentageEntity = new PercentageEntity(day, month, year, UserActivities.STANDING, percentage);
                dataBaseManager.addPercentage(percentageEntity);

                percentage = (int) (((double) sitting / total) * 100);
                percentageEntity = new PercentageEntity(day, month, year, UserActivities.SITTING, percentage);
                dataBaseManager.addPercentage(percentageEntity);

                percentage = (int) (((double) walking / total) * 100);
                percentageEntity = new PercentageEntity(day, month, year, UserActivities.WALKING, percentage);
                dataBaseManager.addPercentage(percentageEntity);

                percentage = (int) (((double) stairs / total) * 100);
                percentageEntity = new PercentageEntity(day, month, year, UserActivities.STAIRS, percentage);
                dataBaseManager.addPercentage(percentageEntity);

                percentage = (int) (((double) jogging / total) * 100);
                percentageEntity = new PercentageEntity(day, month, year, UserActivities.JOGGING, percentage);
                dataBaseManager.addPercentage(percentageEntity);

                if (unknown > 0) {
                    percentage = (int) (((double) unknown / total) * 100);
                    percentageEntity = new PercentageEntity(day, month, year, "unknown", percentage);
                    dataBaseManager.addPercentage(percentageEntity);
                }

                dataBaseManager.deletePrediction(predictions);

            }
        })).start();
    }


}
