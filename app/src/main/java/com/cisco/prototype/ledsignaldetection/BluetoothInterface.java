package com.cisco.prototype.ledsignaldetection;

import android.app.DialogFragment;

/**
 * Created by jessicalandreth on 7/6/15.
 */
public interface BluetoothInterface {
    public void onSelectionFragment(int index);
    public void onAliveFragment();
    public void onImageFragment();
    public void disconnect();
    public void onSoftwareFragment();
    public void softwareMode(int mode);
    public void imageStateMachine(int...arg);
    public void onPasswordFragment(String message);
    public void writeData(String data);
    public void setImageMode();
}