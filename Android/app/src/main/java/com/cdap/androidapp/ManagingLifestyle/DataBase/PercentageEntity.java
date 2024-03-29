package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "percentage_table")
public class PercentageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    @ColumnInfo(name = "activity")
    public String activity;

    @ColumnInfo(name = "percentage")
    public int percentage;

    @ColumnInfo(name = "calorieBurn")
    public double calorieBurn;


    public PercentageEntity(int id, int day, int month, int year, String activity, int percentage, double calorieBurn) {
        this.id = id;
        this.day = day;
        this.month = month;
        this.year = year;
        this.activity = activity;
        this.percentage = percentage;
        this.calorieBurn = calorieBurn;
    }

    @Ignore
    public PercentageEntity(int day, int month, int year, String activity, int percentage, double calorieBurn) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.activity = activity;
        this.percentage = percentage;
        this.calorieBurn = calorieBurn;
    }
}
