package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.opencv.android.Utils;
import org.opencv.contrib.FaceRecognizer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.utils.Converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


public class HandleRecognizeActivity extends Activity {
    private HashMap<Integer, String> lookupMap;
    private Mat latestMat;
    private String culprit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupMap();
        setupMat();

        Intent intent = getIntent();
        int mylabel = intent.getIntExtra(RecognizeActivity.EXTRA_MESSAGE, -1);

        culprit = lookupMap.get(mylabel);

        setContentView(R.layout.activity_handle_recognize);
    }

    private void setupMap(){
        BufferedReader reader = null;
        File myDir = getFilesDir();
        File file = new File(myDir, "lookup.csv");
        String line =  "";
        lookupMap = new HashMap<Integer, String>();

        try {
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {

                String[] stringParts = line.split(";");
                Integer label = Integer.parseInt(stringParts[1]);
                lookupMap.put(label, stringParts[0]);

            }
        } catch (IOException e){} finally {
            try{
                if(reader != null) reader.close();
            } catch (IOException e){}
        }

        lookupMap.put(-1, "Couldn't find a match!\nPlease train me, or retake the picture");

    }

    private void setupMat(){
        File myDir = getFilesDir();
        File file = new File(myDir, "latest_capture.png");

        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        Bitmap myBitmap32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Mat ImageMat = new Mat (myBitmap32.getHeight(), myBitmap32.getWidth(), CvType.CV_8U, new Scalar(4));

        Utils.bitmapToMat(myBitmap32, ImageMat);

        latestMat = ImageMat.clone();

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
        ImageMat.release();
    }

    public void updateMachine(int label, Mat image){
        FaceRecognizer mRecognizer = new EigenFaceRecognizer(80, 4500.0);
        File myDir = getFilesDir();
        File file = new File(myDir, "machine_state.xml");
        mRecognizer.load(file.getAbsolutePath());

        List<Mat> newImage = new ArrayList<Mat>();
        newImage.add(image);
        List<Integer> newLabel = new Vector<Integer>();
        newLabel.add(label);

        Mat myLabels = Converters.vector_int_to_Mat(newLabel);
        mRecognizer.update(newImage, myLabels);
        mRecognizer.save(file.getAbsolutePath());
    }
}
