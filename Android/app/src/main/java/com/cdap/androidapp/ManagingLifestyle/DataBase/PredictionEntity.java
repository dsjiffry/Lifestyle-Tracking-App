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

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    @ColumnInfo(name = "hour")  //using 24h clock
    public int hour;

    @ColumnInfo(name = "minute")
    public int minute;

    @ColumnInfo(name = "activity")
    public String activity;


    public PredictionEntity(int id, int day, int month, int year, int hour, int minute, String activity) {
        this.id = id;
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.activity = activity;
    }

    @Ignore
    public PredictionEntity(int day, int month, int year, int hour, int minute, String activity)
    {
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.activity = activity;
    }
}
