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
        //int maxFps = 35000;
        int maxFps = camParams.get(camParams.size() - 1)[1];
        /*int minFps = 0;
        for(int i = 0; i < camParams.size(); i++) {
            int[] element = camParams.get(i);
            if(element[1] > maxFps) maxFps = element[1];
            if(element[0] > minFps) minFps = element[0];
        }*/
        params.setPreviewFpsRange(maxFps, maxFps);
        mCamera.setParameters(params);
        setResolution();
        //return "min: " + Integer.toString(minFps) + " max: " + Integer.toString(maxFps) + "\n";
    }
    public void setResolution() {
        disconnectCamera();
        mMaxHeight = 480;
        mMaxWidth = 640;
        connectCamera(getWidth(), getHeight());
    }
}
