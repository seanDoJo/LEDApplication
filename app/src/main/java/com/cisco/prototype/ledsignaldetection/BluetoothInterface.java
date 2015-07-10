package com.cisco.prototype.ledsignaldetection;

import android.app.DialogFragment;

/**
 * Created by jessicalandreth on 7/6/15.
 */
public interface BluetoothInterface {
    public void onSelectionFragment(int index);
    public void onAliveFragment();
    //public void onBTMenuFragment();
    public void onCommunicationFragment();
    public void onImageFragment();
    public void onPasswordFragment();
    public void onReplaceFragment(DialogFragment dialog);
    public void disconnect();
}