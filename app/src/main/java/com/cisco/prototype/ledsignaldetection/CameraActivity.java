package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
    int blinkNum = 0;
    int state = state_start;
    int received_packet = 0;
    char received_char = 0;
    String received_string = "";

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
        if(mResult != null) mResult.release();
        boolean led = false;
        double currtime = System.currentTimeMillis();
        double tdiff = currtime - tstamp;


        if(mDisplayFrame != null) mDisplayFrame.release();
        mCurrentFrame = inputFrame.gray();
        mDisplayFrame = mCurrentFrame.clone();

        /*mColorFrame = inputFrame.rgba();
        Imgproc.cvtColor(mColorFrame, mCurrentFrame, Imgproc.COLOR_RGBA2GRAY);*/

        Imgproc.threshold(mCurrentFrame, mResult, 175, 255, Imgproc.THRESH_BINARY);
        //205
        Mat imgCopy = mResult.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Moments> myMoments = new ArrayList<Moments>();
        Imgproc.findContours(imgCopy, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        imgCopy.release();
        for(int i = 0; i < contours.size(); i++){
            myMoments.add(Imgproc.moments(contours.get(i)));
        }
        for(int i = 0; i < contours.size(); i++) {
            Moments oMoments = myMoments.get(i);
            double dArea = oMoments.get_m00();
            if(dArea >= 80 && dArea <= 768){
                double dM01 = oMoments.get_m01();
                double dM10 = oMoments.get_m10();
                int posX = (int)(dM10/dArea);
                int posY = (int)(dM01/dArea);
                Core.circle(mDisplayFrame, new Point(posX, posY), 5, new Scalar(255, 255, 255));

                if((posX <= 168 && posX >= 152) && (posY <= 128 && posY >= 112) && !ledFound && state == state_start){
                    ledFound = true;
                    led = true;
                }
                else if ((posX <= 168 && posX >= 152) && (posY <= 128 && posY >= 112) && state == state_inter && tdiff >= 500){
                    state = state_receive_data;
                    blinkNum++;
                    received_packet = received_packet | 256;
                    received_packet = received_packet >> 1;
                    received_string += Integer.toString(1);
                    if(blinkNum == 8){
                        state = state_end;
                    }
                    tstamp = currtime;
                }
                else if((posX <= 168 && posX >= 152) && (posY <= 128 && posY >= 112) && state == state_receive_data) { //240p config
                //if((posX <= 336 && posX >= 304) && (posY <= 256 && posY >= 224) && !ledOn) { //480p config
                    if(tdiff >= 500){
                        blinkNum++;
                        received_packet = received_packet | 256;
                        received_packet = received_packet >> 1;
                        received_string += Integer.toString(1);
                        if(blinkNum == 8){
                            state = state_end;
                        }
                        tstamp = currtime;
                    }
                    Core.putText(mDisplayFrame, "Receiving... " + received_string, new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
                    led = true;
                    break;
                }
                else if((posX <= 168 && posX >= 152) && (posY <= 128 && posY >= 112) && state == state_start) {
                    led = true;
                    break;
                }
            }
        }
        if(!led && ledFound){
            if(state == state_start){
                state = state_inter;
                tstamp = currtime;
            }
            else if (state == state_inter && tdiff >= 500){
                state = state_receive_data;
                tstamp = currtime;
            }
            else if(state == state_receive_data){
                Core.putText(mDisplayFrame, "Receiving... " + Integer.toString(blinkNum), new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
                if(tdiff >= 500){
                    blinkNum++;
                    received_packet = received_packet & 255;
                    received_packet = received_packet >> 1;
                    received_string += Integer.toString(0);
                    if(blinkNum == 8){
                        state = state_end;
                    }
                    tstamp = currtime;
                    Core.putText(mDisplayFrame, "Receiving... " + received_string, new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
                }
            }
            else if (state == state_end){
                received_char = (char) received_packet;
                Core.putText(mDisplayFrame, String.valueOf(received_char) + " a " + received_string, new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255), 1);
            }
        }
        Core.circle(mDisplayFrame, new Point(160, 120), 16, new Scalar(255, 255, 255)); //240p config
        //Core.circle(mResult, new Point(320, 240), 16, new Scalar(255, 255, 255)); //480p config
        mCurrentFrame.release();

        return mDisplayFrame;
    }
}
