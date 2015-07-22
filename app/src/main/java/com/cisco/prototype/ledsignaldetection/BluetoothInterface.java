package com.cisco.prototype.ledsignaldetection;

import android.app.DialogFragment;

/**
 * Created by jessicalandreth on 7/6/15.
 */
public interface BluetoothInterface {
    public void onSelectionFragment(int index);
    public void onAliveFragment();
    //public void onBTMenuFragment();
    public void onImageFragment(boolean kick);
    public void onSelectImageKick(int position);
    public void onSelectImageSys(int position);
    public void onPasswordFragment(String message);
    public void disconnect();
    public void onSoftwareFragment();
    public void softwareMode(int mode);
    public void onKickstartFragment();
    public void writeData(String data);
}