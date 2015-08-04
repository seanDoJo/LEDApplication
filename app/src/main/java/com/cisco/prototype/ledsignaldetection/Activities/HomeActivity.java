package com.cisco.prototype.ledsignaldetection.Activities;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.cisco.prototype.ledsignaldetection.R;

import java.io.File;


public class HomeActivity extends Activity {
    public final static String EXTRA_MESSAGE = "com.cisco.prototype.ledsignaldetection.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        boolean hasFiles = false;
        File appFolder = new File(getFilesDir().getAbsolutePath());
        File[] folderContents = appFolder.listFiles();
        if(folderContents.length > 0)hasFiles = true;
        Button currbutton = null;
        if(hasFiles) {
            currbutton = (Button) findViewById(R.id.files);
            currbutton.setEnabled(true);
            currbutton.setVisibility(SurfaceView.VISIBLE);
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        boolean hasFiles = false;
        File appFolder = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "captures");
        if(!appFolder.exists()){
            appFolder.mkdirs();
        }
        File imageFolder = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "images");
        if(!imageFolder.exists()){
            imageFolder.mkdirs();
        }
        File[] folderContents = appFolder.listFiles();
        if(folderContents.length > 0){
            hasFiles = true;
            for(File file : folderContents){
                Log.e("LEDApp", file.getAbsolutePath());
            }
        }
        Button currbutton = null;
        if(hasFiles) {
            currbutton = (Button) findViewById(R.id.files);
            currbutton.setEnabled(true);
            currbutton.setVisibility(SurfaceView.VISIBLE);
        }

    }

    public void switchScanContext(View view){
        Intent  nextIntent = new Intent(this, CameraActivity.class);
        startActivity(nextIntent);
    }

    public void switchRecognize(View view){
        Intent nextIntent = new Intent(this, LedFilterActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, 0);
        startActivity(nextIntent)
;    }

    public void switchTrain(View view){
        Intent nextIntent = new Intent(this, RecognizeActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, 1);
        startActivity(nextIntent);
    }

    public void switchConnect(View view){
        Intent nextIntent = new Intent(this, BluetoothActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, 0);
        startActivity(nextIntent);
    }

    public void switchFiles(View view){
        Intent nextIntent = new Intent(this, BluetoothActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, 1);
        startActivity(nextIntent);
    }

    public void switchImageSelection(View view){
        Intent nextIntent = new Intent(this, BluetoothActivity.class);
        nextIntent.putExtra(EXTRA_MESSAGE, 3);
        startActivity(nextIntent);
    }
}