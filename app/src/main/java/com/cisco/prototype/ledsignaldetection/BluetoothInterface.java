package com.cisco.prototype.ledsignaldetection;

import java.io.File;

/**
 * Created by jessicalandreth on 7/6/15.
 */
public interface BluetoothInterface {
    public void onSelectionFragment(int index);
    public void updateFragIndex(int index);
    public void configureMenu();
    public void onImageFragment();
    public void disconnect();
    public void onSoftwareFragment();
    public void softwareMode(int mode);
    public void imageStateMachine(int...arg);
    public void onPasswordFragment(String message);
    public void writeData(String data);
    public void startFileExplorer();
    public void startImageViewer();
    public void viewFile(File file);
    public void initFileView();
    public void destroyFile();
    public void closeBluetooth();
    public void switchPasswordContext(String context);
    public void setLoggedIn(boolean success);
    public boolean firstImage();
    public void saveConfig(String rawconfig);
    public void setDataForImage();
    public void checkLogin();
}