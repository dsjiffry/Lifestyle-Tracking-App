package com.cdap.androidapp.ManagingLifestyle.DataBase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.cdap.androidapp.MainActivity;

@Database(entities = {PredictionEntity.class, PercentageEntity.class, BmiEntity.class}, exportSchema = false, version = 1)
public abstract class DataBase extends RoomDatabase {
    private static DataBase instance;

    public static synchronized DataBase getInstance(Context context)
    {
        if(instance == null)
        {
            instance = Room.databaseBuilder(context.getApplicationContext(), DataBase.class, MainActivity.DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract PredictionDao predictionDao();
    public abstract PercentageDao percentageDao();
    public abstract BmiDao bmiDao();




    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @Override
    public void clearAllTables() {

    }
}
