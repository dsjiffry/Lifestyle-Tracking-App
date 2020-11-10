package com.cdap.androidapp.ManagingLifestyle.Models;

/**
 * Constant values used throughout the code
 */
public class Constants {

    /////////////////////////////////// Shared Preference Keys ///////////////////////////////////
    public static final String WORK_LATITUDE = "lifestyle_" + "workLatitude";
    public static final String WORK_LONGITUDE = "lifestyle_" + "workLongitude";
    public static final String WORK_START_TIME_HOUR = "lifestyle_" + "workStartTimeHour";
    public static final String WORK_START_TIME_MINUTE = "lifestyle_" + "workStartTimeMinute";
    public static final String WORK_END_TIME_HOUR = "lifestyle_" + "workEndTimeHour";
    public static final String WORK_END_TIME_MINUTE = "lifestyle_" + "workEndTimeMinute";
    public static final String WORK_TRAVEL_METHOD = "lifestyle_" + "workTravelMethod";

    public static final String EXERCISE_TIME_HOUR = "lifestyle_" + "exerciseTimeHour";
    public static final String EXERCISE_TIME_MINUTE = "lifestyle_" + "exerciseTimeMinute";
    public static final String EXERCISE_DAYS = "lifestyle_" + "exerciseDays";
    public static final String EXERCISE_TYPE = "lifestyle_" + "exerciseType";

    public static final String IS_ANALYZING_PERIOD = "lifestyle_" + "isAnalyzingPeriod";
    public static final String ANALYSIS_START_DATE = "lifestyle_" + "analysisStartDate";

    public static final String SLEEP_TIME_HOUR = "lifestyle_" + "sleepTimeHour";
    public static final String SLEEP_TIME_MINUTE = "lifestyle_" + "sleepTimeMinute";

    public static final String WAKE_TIME_HOUR = "lifestyle_" + "wakeTimeHour";
    public static final String WAKE_TIME_MINUTE = "lifestyle_" + "wakeTimeMinute";

    public static final String HOME_LATITUDE = "lifestyle_" + "HomeLatitude";
    public static final String HOME_LONGITUDE = "lifestyle_" + "HomeLongitude";

    public static final String IMPROVEMENTS = "lifestyle_" + "improvements";
    //////////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////// notification IDs ///////////////////////////////////

    public static final int SITTING_TOO_LONG = 1;
    public static final int PHONE_SERVICE = 2;
    public static final int NOT_ENOUGH_SLEEP = 3;


    /////////////////////////////////// Predictions ///////////////////////////////////

    public static final String SITTING = "sitting";
    public static final String STANDING = "standing";
    public static final String WALKING = "walking";
    public static final String STAIRS = "stairs";
    public static final String JOGGING = "jogging";

    /////////////////////////////////// exercise types ///////////////////////////////////

    public static final String RUNNING = "running";
    public static final String GYM = "gym";

    /////////////////////////////////// work travel methods ///////////////////////////////////

//    public static final String WALKING = "walking";
    public static final String VEHICLE = "vehicle";

}
