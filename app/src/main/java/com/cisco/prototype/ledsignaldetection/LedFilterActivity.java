package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class LedFilterActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private JavaCameraView2 mOpenCvCameraView;
    private Mat hsv;
    private Mat sElem;
    private Mat latestMat = null;
    private boolean touched, processed;
    private double tTime;

    static {
        if (!OpenCVLoader.initDebug()) {
            System.exit(2);
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(!touched) {
            touched = true;
            tTime = System.currentTimeMillis();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_filter);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView2) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        if(hsv != null)hsv.release();
        if(sElem != null)sElem.release();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        if(sElem == null)sElem = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        touched = false;
        processed = false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if(touched && !processed && System.currentTimeMillis() - tTime > 1500){
            processed = true;
            processMat();
        }
        else if(processed && System.currentTimeMillis() - tTime > 1500){
            processed = false;
            touched = false;
        }
        else if(!processed){
            if(latestMat != null) latestMat.release();
            latestMat = inputFrame.rgba();
        }

        return latestMat;
    }

    private void processMat(){
        Mat hsv = latestMat.clone();
        Mat copy = hsv.clone();

        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

        Core.inRange(hsv, new Scalar(0, 120, 50), new Scalar(120, 255, 255), hsv);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_GRAY2RGBA);
        Core.bitwise_and(hsv, copy, hsv);

        Imgproc.erode(hsv, hsv, sElem);
        Imgproc.dilate(hsv, hsv, sElem);

        Imgproc.dilate(hsv, hsv, sElem);
        Imgproc.erode(hsv, hsv, sElem);

        copy.release();
        latestMat.release();
        latestMat = hsv.clone();
        hsv.release();
        tTime = System.currentTimeMillis();
    }
}
