package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "prediction_table")
public class PredictionEntity
{
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "hour")  //using 24h clock
    public int hour;

    @ColumnInfo(name = "minute")
    public int minute;

    @ColumnInfo(name = "activity")
    public String activity;


    public PredictionEntity(int id, String date, int hour, int minute, String activity) {
        this.id = id;
        this.date = date;
        this.hour = hour;
        this.minute = minute;
        this.activity = activity;
    }

    @Ignore
    public PredictionEntity(String date, int hour, int minute, String activity) {
        this.date = date;
        this.hour = hour;
        this.minute = minute;
        this.activity = activity;
    }
}
