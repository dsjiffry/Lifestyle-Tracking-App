package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PercentageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertPercentage(PercentageEntity percentageEntity);

    @Update
    void updatePercentage(PercentageEntity percentageEntity);

    @Delete
    void deletePercentage(PercentageEntity percentageEntity);

    @Delete
    void deletePercentage(List<PercentageEntity> percentageEntity);

    @Query("SELECT * FROM percentage_table")
    List<PercentageEntity> loadEntireHistory();



}
