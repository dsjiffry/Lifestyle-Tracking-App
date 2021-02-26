package com.cdap.androidapp.ManagingLifestyle.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bmi_table")
public class BmiEntity {

    @PrimaryKey
    public int id;

    @ColumnInfo(name = "bmi")
    public float bmi;

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    public BmiEntity(float bmi, int day, int month, int year) {
        this.id = Integer.parseInt(""+day+month+year);
        this.bmi = bmi;
        this.day = day;
        this.month = month;
        this.year = year;
    }
}
