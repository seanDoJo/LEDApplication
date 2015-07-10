package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.os.*;
import android.os.Process;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;


import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;


public class CameraActivity extends Activity implements CvCameraViewListener2 {

    private CustomView mOpenCvCameraView;
    int state_start = 0;
    int state_receive_data = 1;
    int state_end = 2;
    int state_inter = 3;

    double tstamp = System.currentTimeMillis();

    Mat mCurrentFrame;
    Mat mHSV;
    Mat mThresh;
    Mat mResult;
    /****** debugging *******/
    Mat mDisplayFrame;
    /****** debugging *******/

    boolean ledFound = false;
    boolean ledOn = false;
    int blinkNum = 0;
    int state = state_start;
    int received_packet = 0;
    double highArea, lowArea;
    int lastX, lastY;
    String received_string = "";
    String state_string = "";
    double bpsCounter = 0;
    double bpsCounterBegin = 0;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = (CustomView) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onCameraViewStarted(int width, int height) {
        //mOpenCvCameraView.setFps();
        mResult = new Mat();
        mCurrentFrame = new Mat();
        /****** debugging *******/
        mDisplayFrame = new Mat();
        /****** debugging *******/
    }

    public void onCameraViewStopped() {
        if(mResult != null){
            mResult.release();
        }
        if(mCurrentFrame != null){
            mCurrentFrame.release();
        }
        /****** debugging *******/
        if(mDisplayFrame != null){
            mDisplayFrame.release();
        }
        /****** debugging *******/
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        boolean led = false;
        double currtime = System.currentTimeMillis();
        double tdiff = currtime - tstamp;
        if(tdiff >= 800 && (state == state_receive_data || state == state_inter)){
            blinkNum = 0;
            ledFound = false;
            state = state_start;
            ledOn = false;
            received_string = "";
        }

        /* create a copy of the input frame to which we can draw circles */
        if(mDisplayFrame != null) mDisplayFrame.release();
        //if(mResult != null)mResult.release();
        mCurrentFrame = inputFrame.gray();
        mDisplayFrame = mCurrentFrame.clone();

        /* apply a threshold to filter out erroneous values */
        Imgproc.threshold(mCurrentFrame, mResult, 195, 255, Imgproc.THRESH_BINARY);

        /* find contours of filtered image */
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Moments> myMoments = new ArrayList<Moments>();
        //Mat copy = mResult.clone();
        Imgproc.findContours(mResult, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        //copy.release();
        mResult.release();

        /* collect the moments of each contour */
        int size = contours.size();
        for(int i = 0; i < size; i++){
            myMoments.add(Imgproc.moments(contours.get(i)));
        }
        for(int i = 0; i < size; i++) {
            Moments oMoments = myMoments.get(i);
            double dArea = oMoments.get_m00();

            /* Only act on the moment if it falls within our circle, and is the correct size */
            if(dArea >= 50 && dArea <= 768){
                double dM01 = oMoments.get_m01();
                double dM10 = oMoments.get_m10();
                int posX = (int)(dM10/dArea);
                int posY = (int)(dM01/dArea);
                if(!ledFound){
                    ledFound = true;
                    highArea = dArea + 25;
                    lowArea = dArea - 25;
                    lastX = posX;
                    lastY = posY;
                    tstamp = currtime;
                    break;
                }

                //else if((posX <= 168 && posX >= 152) && (posY <= 128 && posY >= 112) && ledFound){
                else if((dArea <= highArea && dArea >= lowArea) && (posX <= lastX + 30 && posX >= lastX - 30) && (posY <= lastY + 30 && posY >= lastY - 30) && ledFound){
                    //Core.circle(mDisplayFrame, new Point(posX, posY), 9, new Scalar(255, 255, 255));
                    //state_string = Integer.toString(state);
                    lastX = posX;
                    lastY = posY;

                    if (state == state_inter && (tdiff >= 250 || tdiff <= 150)) {
                        state = state_start;
                    }
                    else if(state == state_inter && !ledOn){
                        state = state_receive_data;
                        tstamp = currtime;
                    }

                    else if (state == state_receive_data && !ledOn){
                        if(tdiff >= 170){
                            blinkNum++;
                            received_packet = (received_packet | 4096) >> 1;
                            //received_string += "1";
                            tstamp = currtime;
                        }
                        else {
                            blinkNum++;
                            received_packet = (received_packet & 4095) >> 1;
                            //received_string += "0";
                            tstamp = currtime;
                        }


                        if(blinkNum == 12) state = state_end;
                    }

                    else if (state == state_end){
                        if(blinkNum == 12){
                            blinkNum = 0;
                            received_string += (checkParity(received_packet));
                            bpsCounter = 12 / ((currtime - bpsCounterBegin)/1000);
                            state = state_start;
                        }
                    }
                    ledOn = true;
                    led = true;
                    break;
                }
            }
        }
        if(!led && ledFound && ledOn){
            if(state == state_start){
                state = state_inter;
                tstamp = currtime;
                bpsCounterBegin = currtime;
            }
            ledOn = false;
        }
        //Core.circle(mDisplayFrame, new Point(160, 120), 16, new Scalar(255, 255, 255)); //240p config
        //state_string = ledFound ? "identified" : "searching...";
        //Core.putText(mDisplayFrame, state_string, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
        Core.putText(mDisplayFrame, "bps: " + Double.toString(bpsCounter), new Point(50, 75), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
        Core.putText(mDisplayFrame,"Received: " + received_string, new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
        mCurrentFrame.release();

        return mDisplayFrame; // return mDisplayFrame
    }

    char checkParity(int data){
        int c1, c2, c3, c4, d1, d2, d3, d4, d5, d6, d7, d8, C1, C2, C4, C8;
        int check = 0;
        int myChar = 0;
        c1 = data & 1;
        c2 = (data & 2) >> 1;
        c3 = (data & 8) >> 3;
        c4 = (data & 128) >> 7;

        d1 = (data & 4) >> 2;
        d2 = (data & 16) >> 4;
        d3 = (data & 32) >> 5;
        d4 = (data & 64) >> 6;
        d5 = (data & 256) >> 8;
        d6 = (data & 512) >> 9;
        d7 = (data & 1024) >> 10;
        d8 = (data & 2048) >> 11;

        C1 = ((c1 ^ d1 ^ d2 ^ d4 ^ d5 ^ d7)&1);
        C2 = ((c2 ^ d1 ^ d3 ^ d4 ^ d6 ^ d7)&1);
        C4 = ((c3 ^ d2 ^ d3 ^ d4 ^ d8)&1);
        C8 = ((c4 ^ d5 ^ d6 ^ d7 ^ d8)&1);

        check = (C8 << 3) + (C4 << 2) + (C2 << 1) + C1;

        //System.out.println(check);

        switch(check){
            case 3:
                d1 = d1 ^ 1;
                break;
            case 5:
                d2 = d2 ^ 1;
                break;
            case 6:
                d3 = d3 ^ 1;
                break;
            case 7:
                d4 = d4 ^ 1;
                break;
            case 9:
                d5 = d5 ^ 1;
                break;
            case 10:
                d6 = d6 ^ 1;
                break;
            case 11:
                d7 = d7 ^ 1;
                break;
            case 12:
                d8 = d8 ^ 1;
                break;
            default:
                break;
        }

        myChar = (myChar | d8) << 1;
        myChar = (myChar | d7) << 1;
        myChar = (myChar | d6) << 1;
        myChar = (myChar | d5) << 1;
        myChar = (myChar | d4) << 1;
        myChar = (myChar | d3) << 1;
        myChar = (myChar | d2) << 1;
        myChar = (myChar | d1);

        return ((char) myChar);

    }
}