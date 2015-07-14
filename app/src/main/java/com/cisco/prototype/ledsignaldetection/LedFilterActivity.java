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
import org.opencv.imgproc.Imgproc;


public class LedFilterActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private JavaCameraView2 mOpenCvCameraView;
    private Mat hsv;

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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if(hsv != null)hsv.release();
        Mat hsv = inputFrame.rgba();

        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

        Core.inRange(hsv, new Scalar(100,150,60), new Scalar(179,255,255), hsv);

        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_HSV2RGB);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2RGBA);

        return hsv;
    }
}
