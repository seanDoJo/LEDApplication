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
    public boolean lockFps() {
        if (mStartCaptureTime > 0 && SystemClock.elapsedRealtime() - mStartCaptureTime > 2000) {
            Camera.Parameters params = mCamera.getParameters();
            params.setAutoWhiteBalanceLock(true);
            params.setAutoExposureLock(true);
            mCamera.setParameters(params);
            return true;
        }
        return false;
    }
}
