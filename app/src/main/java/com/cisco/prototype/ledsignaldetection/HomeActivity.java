package com.cisco.prototype.ledsignaldetection;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class HomeActivity extends Activity {

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
        startActivity(nextIntent)
;    }
}
