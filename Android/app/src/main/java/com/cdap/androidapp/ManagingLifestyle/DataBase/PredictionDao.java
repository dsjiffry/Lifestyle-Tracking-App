package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPrediction(PredictionEntity prediction);

    @Update
    void updatePredictions(PredictionEntity prediction);

    @Delete
    void deletePredictions(PredictionEntity prediction);

    @Delete
    void deletePredictions(List<PredictionEntity> predictions);

    @Query("SELECT * FROM prediction_table")
    List<PredictionEntity> loadAllPredictions();

    @Query("SELECT * FROM prediction_table WHERE day=(:day) AND month=(:month) AND year=(:year)")
    List<PredictionEntity> loadPredictionsByDate(int day, int month, int year);



}
