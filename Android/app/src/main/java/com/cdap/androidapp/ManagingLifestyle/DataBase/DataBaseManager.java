package com.cdap.androidapp.ManagingLifestyle.DataBase;

import android.content.Context;

import java.time.LocalDate;
import java.util.List;

public class DataBaseManager
{
    private DataBase db;

    public DataBaseManager(Context context)
    {
        db = DataBase.getInstance(context);
    }

    public void addPrediction()
    {
        PredictionEntity predictionEntity = new PredictionEntity(LocalDate.now().toString(),20,30, "jogging");
        db.predictionDao().insertPrediction(predictionEntity);
    }

    public void updatePrediction(PredictionEntity predictionEntity)
    {
        db.predictionDao().updatePredictions(predictionEntity);
    }

    public void deletePrediction(PredictionEntity predictionEntity)
    {
        db.predictionDao().deletePredictions(predictionEntity);
    }

    public void deletePrediction(List<PredictionEntity> predictionEntity)
    {
        db.predictionDao().deletePredictions(predictionEntity);
    }

    public List<PredictionEntity> getAllPredictions()
    {
       return db.predictionDao().loadAllPredictions();
    }



}
