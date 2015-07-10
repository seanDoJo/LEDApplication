package com.cisco.prototype.ledsignaldetection;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class HomeActivity extends Activity {
    public final static String EXTRA_MESSAGE = "com.cisco.prototype.ledsignaldetection.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
    }

    public void switchScanContext(View view){
        Intent  nextIntent = new Intent(this, CameraActivity.class);
        startActivity(nextIntent);
    }

    public void switchRecognize(View view){
        Intent nextIntent = new Intent(this, RecognizeActivity.class);
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
        startActivity(nextIntent);
    }
}