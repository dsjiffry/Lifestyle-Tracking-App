package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "bmi_table")
public class BmiEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "bmi")
    public float bmi;

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    public BmiEntity(int id, float bmi, int day, int month, int year) {
        this.id = id;
        this.bmi = bmi;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    @Ignore
    public BmiEntity(float bmi, int day, int month, int year) {
        this.bmi = bmi;
        this.day = day;
        this.month = month;
        this.year = year;
    }
}
