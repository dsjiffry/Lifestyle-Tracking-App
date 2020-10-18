package com.cdap.androidapp.ManagingLifestyle.DataBase;

public class UserActivities
{
    public static final String STANDING = "standing";
    public static final String SITTING = "sitting";
    public static final String WALKING = "walking";
    public static final String STAIRS = "stairs";
    public static final String JOGGING = "jogging";

    public static String getActivity(String name)
    {
        if(name.trim().toLowerCase().contains(STANDING))
        {
            return STANDING;
        }
        else if(name.trim().toLowerCase().contains(SITTING))
        {
            return SITTING;
        }
        else if(name.trim().toLowerCase().contains(WALKING))
        {
            return WALKING;
        }
        else if(name.trim().toLowerCase().contains(STAIRS))
        {
            return STAIRS;
        }
        else if(name.trim().toLowerCase().contains(JOGGING))
        {
            return JOGGING;
        }
        return "unknown";
    }
}
