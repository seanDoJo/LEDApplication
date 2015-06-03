package com.cisco.prototype.ledsignaldetection;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.ArrayList;


public class CustomView extends JavaCameraView {

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setFps() {
        disconnectCamera();
        setResolution();
        connectCamera(getWidth(), getHeight());
    }
    public void setResolution() {
        mMaxHeight = 240;
        mMaxWidth = 320;
        //mMaxHeight = 1080;
        //mMaxWidth = 1920;
        //mMaxHeight = 480;
        //mMaxWidth = 640;
    }
}
