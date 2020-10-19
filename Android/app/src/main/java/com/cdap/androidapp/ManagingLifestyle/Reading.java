package com.cdap.androidapp.ManagingLifestyle;

import java.io.Serializable;

public class Reading implements Serializable
{
    public double xAxis;
    public double yAxis;
    public double zAxis;

    public Reading(double xAxis, double yAxis, double zAxis) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
    }

    public Reading() {
        this.xAxis = 0.0;
        this.yAxis = 0.0;
        this.zAxis = 0.0;
    }
}
