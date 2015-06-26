package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.*;
import org.opencv.android.JavaCameraView2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.highgui.*;
import org.opencv.contrib.*;
import org.opencv.imgproc.*;
import org.opencv.utils.Converters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class RecognizeActivity extends Activity implements CvCameraViewListener2 {

    public final static String EXTRA_MESSAGE = "com.cisco.prototype.ledsignaldetection.MESSAGE";

    private ImageView imageView;
    private JavaCameraView2 mOpenCvCameraView;
    private boolean resSet = false;
    private Mat latestMat = null, saveMat = null, displayMat = null;
    private AssetManager assetManager;
    private FaceRecognizer mRecognizer;
    private boolean touched = false;
    private boolean ready = false;
    List<Mat> images = new ArrayList<Mat>();
    List<Integer> labels = new Vector<Integer>();
    int train;
    ArrayList<Mat> trainingSet;
    double startTime;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        assetManager = getAssets();

        Intent intent = getIntent();
        train = intent.getIntExtra(HomeActivity.EXTRA_MESSAGE, 0);
        if(train > 0){
            trainingSet = new ArrayList<Mat>();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView2) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(latestMat != null && train == 0) {
            if (latestMat.size().area() > 0) {
                if (!touched) {
                    touched = true;
                    //if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
                    int[] label = new int[1];
                    double[] confidence = new double[1];
                    saveMat = latestMat.clone();
                    Mat outputM = new Mat(100, 100, latestMat.type());
                    Imgproc.resize(latestMat, outputM, outputM.size(), 0, 0, Imgproc.INTER_AREA);
                    Imgproc.cvtColor(outputM, outputM, Imgproc.COLOR_RGB2GRAY);
                    mRecognizer.predict(outputM, label, confidence);
                    transition(label[0]);
                    System.out.println("Label: " + Integer.toString(label[0]) + " Confidence: " + Double.toString(confidence[0]));
                    double currtime = System.currentTimeMillis();
                    double tdiff;
                    while ((tdiff = System.currentTimeMillis() - currtime) < 2000) {
                    }
                    //if (mOpenCvCameraView != null)mOpenCvCameraView.enableView();
                }
            }
        }
        return true;
    }

    public void transition(int label){
        FileOutputStream out = null;
        File myDir = getFilesDir();
        File file = new File(myDir, "latest_capture.png");
        Bitmap toMap = Bitmap.createBitmap(saveMat.width(), saveMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(saveMat,toMap);
        try {
            out = new FileOutputStream(file.getAbsolutePath());
            toMap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent nextIntent = new Intent(this, HandleRecognizeActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, label);
        startActivity(nextIntent);
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
        if(touched){
            finish();
            return;
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mRecognizer = new LbphRecognizer(1, 8, 8, 8, 50.0);
        File myDir = getFilesDir();
        File file = new File(myDir, "machine_state.xml");

        if(file.exists()){
            mRecognizer.load(file.getAbsolutePath());
        }
        else {
            //trainFirstTime();
            install_machine_config();
            mRecognizer.load(file.getAbsolutePath());
        }
    }

    public void trainFirstTime(){
        read_csv();
        if(images.size() < 1){
            System.exit(1);
        }
        Mat myLabels = Converters.vector_int_to_Mat(labels);
        mRecognizer.train(images, myLabels);
        mRecognizer.save("/storage/emulated/0/Documents/machine_state.xml");
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        startTime = System.currentTimeMillis();
    }
    public void onCameraViewStopped() {

    }
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(latestMat != null) latestMat.release();
        if(displayMat != null) displayMat.release();
        latestMat = inputFrame.rgba().clone();
        displayMat = latestMat.clone();
        if(train > 0 && (System.currentTimeMillis() - startTime) < 10000){
            Mat outputM = new Mat(100, 100, latestMat.type());
            Imgproc.resize(latestMat, outputM, outputM.size(), 0, 0, Imgproc.INTER_AREA);
            Imgproc.cvtColor(outputM, outputM, Imgproc.COLOR_RGB2GRAY);
            trainingSet.add(outputM);
            Core.putText(displayMat, "SCANNING...", new Point(10, 10), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(250, 0, 0), 1);
        }
        else if(train > 0){
            Core.putText(displayMat, "DONE!", new Point(10, 10), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(250, 0, 0), 1);
            retrainMachine();
        }
        touched = false;
        return displayMat;
    }

    private void retrainMachine(){
        File myDir = getFilesDir();
        File file = new File(myDir, "machine_state.xml");
        List<Integer> newLabel = new Vector<Integer>();
        for(int i = 0; i < trainingSet.size(); i++){
            newLabel.add(3);
        }

        Mat myLabels = Converters.vector_int_to_Mat(newLabel);
        mRecognizer.update(trainingSet, myLabels);
        mRecognizer.save(file.getAbsolutePath());
        mRecognizer.save("/storage/emulated/0/Documents/machine_state.xml");
        for(Mat pic : trainingSet){
            pic.release();
        }
        finish();
        return;
    }

    private void install_machine_config(){
        InputStream ims = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        String line =  "";
        File myDir = getFilesDir();
        File file = new File(myDir, "machine_state.xml");
        File mfile = new File(myDir, "lookup.csv");
        try {
            ims = assetManager.open("state_save.xml");
            reader = new BufferedReader(new InputStreamReader(ims));
            if(!file.exists())file.createNewFile();
            writer = new BufferedWriter(new FileWriter(file));
            while ((line = reader.readLine()) != null) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e){} finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            }catch(IOException e){}
        }
        if(!mfile.exists()) {
            try {
                mfile.createNewFile();
                writer = new BufferedWriter(new FileWriter(mfile));

                writer.append("Catalyst 2960;0");
                writer.newLine();

                writer.append("Catalyst 3750;1");
                writer.newLine();

                writer.append("Cisco Aironet 2700;2");

            } catch (IOException e) {
            } finally {
                try {
                    if(writer != null) writer.close();
                } catch (IOException e){}
            }
        }
    }

    private void read_csv(){
        InputStream ims = null;
        BufferedReader reader = null;
        String line =  "";
        try{
            ims = assetManager.open("app_training.csv");
            reader = new BufferedReader(new InputStreamReader(ims));
            while((line = reader.readLine()) != null){
                String[] parts = line.split(";");
                String imgPath = parts[0];
                Integer label = Integer.parseInt(parts[1]);
                if(!imgPath.isEmpty()){
                    InputStream ist = null;
                    try {
                        ist = assetManager.open(imgPath);
                    } catch (IOException e){
                    }

                    Bitmap image = BitmapFactory.decodeStream(ist);
                    Bitmap myBitmap32 = image.copy(Bitmap.Config.ARGB_8888, true);
                    Mat ImageMat = new Mat (myBitmap32.getHeight(), myBitmap32.getWidth(), CvType.CV_8U, new Scalar(4));
                    Utils.bitmapToMat(myBitmap32, ImageMat);
                    Mat outputM = new Mat(100, 100, ImageMat.type());
                    Imgproc.resize(ImageMat, outputM, outputM.size(), 0, 0, Imgproc.INTER_AREA);
                    Imgproc.cvtColor(outputM,outputM,Imgproc.COLOR_RGB2GRAY);
                    if(image != null)
                    {
                        image.recycle();
                        image = null;
                    }
                    if(myBitmap32 != null)
                    {
                        myBitmap32.recycle();
                        myBitmap32 = null;
                    }

                    images.add(outputM);
                    labels.add(label);
                }
            }
        } catch (IOException e){
        } finally {
            try{
                ims.close();
            } catch (IOException e){
            }
        }
    }
}
