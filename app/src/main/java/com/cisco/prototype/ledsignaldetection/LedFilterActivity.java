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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;


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
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Moments> myMoments = new ArrayList<Moments>();

        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

        Core.inRange(hsv, new Scalar(0, 120, 50), new Scalar(120, 255, 255), hsv);
        Imgproc.erode(hsv, hsv, sElem);
        Imgproc.dilate(hsv, hsv, sElem);

        Imgproc.dilate(hsv, hsv, sElem);
        Imgproc.erode(hsv, hsv, sElem);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_GRAY2RGBA);
        Core.bitwise_and(hsv, copy, hsv);

        copy.release();
        copy = hsv.clone();
        Imgproc.cvtColor(copy, copy, Imgproc.COLOR_RGBA2GRAY);

        Imgproc.findContours(copy, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        int size = contours.size();
        for(int i = 0; i < size; i++){
            myMoments.add(Imgproc.moments(contours.get(i)));
        }
        for(Moments moment : myMoments){
            double dArea = moment.get_m00();
            if(dArea >= 60 && dArea <= 610) {
                double dM01 = moment.get_m01();
                double dM10 = moment.get_m10();
                int posX = (int) (dM10 / dArea);
                int posY = (int) (dM01 / dArea);
                double[] colour = hsv.get(posY, posX);
                for (int i = 0; i < colour.length; i++) {
                    System.out.println(Double.toString(colour[i]));
                }
                System.out.println(dArea);
                double radius = Math.sqrt(dArea/3.1415926535);
                Core.circle(hsv, new Point(posX, posY), (int)radius, new Scalar(255, 255, 255));
            }
        }

        copy.release();
        latestMat.release();
        latestMat = hsv.clone();
        hsv.release();
        tTime = System.currentTimeMillis();
    }
}
