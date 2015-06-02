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
        Camera.Parameters params = mCamera.getParameters();
        ArrayList<int[]> camParams = (ArrayList<int[]>)mCamera.getParameters().getSupportedPreviewFpsRange();
        int maxFps = camParams.get(camParams.size() - 1)[1];
        params.setPreviewFpsRange(maxFps, maxFps);
        mCamera.setParameters(params);
        setResolution();
    }
    public void setResolution() {
        disconnectCamera();
        mMaxHeight = 240;
        mMaxWidth = 320;
        /*mMaxHeight = 1080;
        mMaxWidth = 1920;*/
        connectCamera(getWidth(), getHeight());
    }
}
