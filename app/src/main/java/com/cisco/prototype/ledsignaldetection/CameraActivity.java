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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


public class CameraActivity extends Activity implements CvCameraViewListener2 {

    private CustomView mOpenCvCameraView;
    Mat mCurrentFrame;
    Mat mHSV;
    Mat mThresh;
    Mat mResult;

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
        mOpenCvCameraView.setFps();
        mResult = new Mat();
        mCurrentFrame = new Mat();
    }

    public void onCameraViewStopped() {
        if(mResult != null){
            mResult.release();
        }
        if(mCurrentFrame != null){
            mCurrentFrame.release();
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(mResult != null){
            mResult.release();
        }

        mCurrentFrame = inputFrame.gray();

        /*
        mCurrentFrame = inputFrame.rgba();
        mThresh = new Mat();
        mHSV = new Mat();

        Imgproc.cvtColor(mCurrentFrame, mHSV, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(mHSV,mHSV,Imgproc.COLOR_BGR2HSV);
        Core.inRange(mHSV, new Scalar(0, 0, 50), new Scalar(15, 255, 255), mThresh);
        Imgproc.cvtColor(mThresh, mThresh, Imgproc.COLOR_GRAY2RGBA);
        //Core.bitwise_and(mCurrentFrame, mThresh, mResult);
        mThresh.release();
        mHSV.release();
        */

        Imgproc.threshold(mCurrentFrame, mResult, 190, 255, Imgproc.THRESH_TOZERO);

        Moments oMoments = Imgproc.moments(mResult);

        double dM01 = oMoments.get_m01();
        double dM10 = oMoments.get_m10();
        double dArea = oMoments.get_m00();

        int posX = (int)(dM10 / dArea);
        int posY = (int)(dM01 / dArea);

        if(posX > 0 && posY > 0)Core.rectangle(mResult, new Point(posX - 50, posY - 50), new Point(posX + 50, posY + 50), new Scalar(255, 255, 255));

        mCurrentFrame.release();

        return mResult;
    }
}
