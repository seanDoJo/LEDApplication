package com.cisco.prototype.ledsignaldetection;

import android.content.Context;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;


public class CustomView extends JavaCameraView {

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}