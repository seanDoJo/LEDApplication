package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothActivity extends Activity {
    private BluetoothAdapter mBluetooth;
    private ArrayList<BluetoothDevice> devices;
    private int REQUEST_ENABLE_BT = 123;
    //Where the asynchronous bluetooth actions are received
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
            }
        }
    };
    //Where the connection thread communicates with the main activity
    Handler connectionHandler = new Handler(){
        public void handleMessage(Message m){
            if(m.what == 1){
                byte[] data = (byte[])m.obj;
                String result = new String(data);
            }
        }
    };
    //Class for the connection thread -- reading is done automatically, writing is done by invoking the write(String) method
    private class BTConnection extends Thread {
        private BluetoothSocket sock = null;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean running = true;
        public BTConnection(BluetoothDevice newDevice){
            BluetoothSocket tmp = null;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmp = newDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                tmp.connect();
                tmpIn = tmp.getInputStream();
                tmpOut = tmp.getOutputStream();
            } catch (IOException e) {
                try {
                    tmp.close();
                } catch (IOException closeException) { }
                return;
            }
            sock = tmp;
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while(running) {
                try {
                    bytes = mmInStream.read(buffer);
                    connectionHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();
                    buffer = new byte[buffer.length];
                } catch (IOException e) {
                    break;
                }
            }
            return;
        }

        public void write(String content){
            try {
                byte[] bytes = content.getBytes();
                mmOutStream.write(bytes);
            }catch(IOException e){}

        }

        public void close(){
            try{
                sock.close();
                mmOutStream.close();
                mmInStream.close();
                running = false;
            } catch (IOException e){e.printStackTrace();}
        }

    }

    private BTConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        //registering a filter allows us to catch specific actions
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(btReceiver, filter);

        //the bluetooth adapter is where we discover devices
        mBluetooth = BluetoothAdapter.getDefaultAdapter();

        //this is where we ask to enable bluetooth
        if (!mBluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            //if there's no bluetooth on phone, go back to previous (main) screen
            finish();
            return;
        }

        //start discovering devices -- this is handled in the broadcast receiver
        mBluetooth.startDiscovery();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //we need this otherwise poo flinging will ensue
        unregisterReceiver(btReceiver);
    }
}
