package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BmiDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertBmi(BmiEntity bmiEntity);

    @Update
    void updateBmi(BmiEntity bmiEntity);

    @Delete
    void deleteBmi(BmiEntity bmiEntity);

    @Delete
    void deleteBmi(List<BmiEntity> bmiEntities);

    @Query("SELECT * FROM bmi_table")
    List<BmiEntity> loadBmiHistory();


}
