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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.contrib.FaceRecognizer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
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
import java.util.Map;
import java.util.Vector;


public class HandleRecognizeActivity extends Activity {
    private HashMap<Integer, String> lookupMap;
    private Mat latestMat;
    private String culprit;
    private ArrayList<String> values = new ArrayList<String>();
    private ArrayList<Integer> corr = new ArrayList<Integer>();
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            updateMachine(corr.get(position));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_recognize);
    }

    public void onResume(){
        super.onResume();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);

        setupMap();
        setupMat();

        Intent intent = getIntent();
        int mylabel = intent.getIntExtra(RecognizeActivity.EXTRA_MESSAGE, -1);


        if(mylabel < 0){
            for (Map.Entry<Integer, String> entry : lookupMap.entrySet()) {
                corr.add(entry.getKey());
                values.add(entry.getValue());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, values);
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(mMessageClickedHandler);
            culprit = "Couldn't find a match!\nPlease train me, or retake the picture";
        }
        else{
            culprit = lookupMap.get(mylabel);
        }

        TextView main = (TextView)findViewById(R.id.textView);
        main.setText(culprit);
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

    private void updateMachine(int label){
        FaceRecognizer mRecognizer = new LbphRecognizer(1, 8, 8, 8, 75.0);
        File myDir = getFilesDir();
        File file = new File(myDir, "machine_state.xml");
        mRecognizer.load(file.getAbsolutePath());

        Mat outputM = new Mat(100, 100, latestMat.type());
        Imgproc.resize(latestMat, outputM, outputM.size(), 0, 0, Imgproc.INTER_AREA);
        Imgproc.cvtColor(outputM, outputM, Imgproc.COLOR_RGB2GRAY);

        List<Mat> newImage = new ArrayList<Mat>();
        newImage.add(outputM);
        List<Integer> newLabel = new Vector<Integer>();
        newLabel.add(label);

        Mat myLabels = Converters.vector_int_to_Mat(newLabel);
        mRecognizer.update(newImage, myLabels);
        mRecognizer.save(file.getAbsolutePath());
        finish();
        return;
    }
}
