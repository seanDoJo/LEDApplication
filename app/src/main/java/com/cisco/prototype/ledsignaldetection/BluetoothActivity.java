package com.cisco.prototype.ledsignaldetection;

import android.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class BluetoothActivity extends FragmentActivity implements BluetoothInterface {
    private BluetoothAdapter mBluetooth;
    private ArrayList<BluetoothDevice> devices;
    private String activeDevice = "";
    private int REQUEST_ENABLE_BT = 123;
    private int fragIndex;
    private CommunicationFragment cFrag;
    private SelectionFragment sFrag;
    private BTMenuFragment btmFrag;
    private AliveFragment aFrag;
    private PasswordFragment pFrag;
    private ImageFragment iFrag;
    final int[] pingval = new int[1];
    CountDownLatch latch;
    CountDownLatch tready;
    //Where the asynchronous bluetooth actions are received
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //SelectionFragment sFrag = (SelectionFragment) getSupportFragmentManager().findFragmentById(R.id.select_fragment);
                if(device != null && device.getName() != null && !isDiscovered(device)){
                    sFrag.addDevice(device.getName() + "--> " + device.getAddress());
                    devices.add(device);
                }
            }
        }
    };
    private boolean isDiscovered(BluetoothDevice device){
        for(BluetoothDevice discovered : devices){
            if(discovered.getName().equals(device.getName())) return true;
        }
        return false;
    }
    //Where the connection thread communicates with the main activity
    Handler connectionHandler = new Handler(){
        public void handleMessage(Message m){
            if(m.what == 30){
                byte[] data = (byte[])m.obj;
                String result = new String(data);
                cFrag.addMessage(result);
            }
            else if(m.what == 11111111){
                Toast.makeText(getApplicationContext(), "Couldn't connect to device!", Toast.LENGTH_LONG).show();
                connection.close();
                finish();
                return;
            }
        }
    };
    //Class for the connection thread -- reading is done automatically, writing is done by invoking the write(String) method
    private class BTConnection extends Thread {
        private BluetoothSocket sock = null;
        private final CountDownLatch synchron;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean running = true;
        private boolean paused = false;
        public BTConnection(BluetoothDevice newDevice, CountDownLatch synchron){
            this.synchron = synchron;
            BluetoothSocket tmp = null;
            try {
                tmp = newDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                Log.i("LedApp", "Created socket with device");
            } catch (IOException e) {
                Log.e("LedApp", "Failed in socket creation");
                e.printStackTrace();
                //System.exit(1);
            }
            sock = tmp;
        }
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            try{
                sock.connect();
                Log.i("LedApp", "Connected socket with device");
                mmInStream = sock.getInputStream();
                Log.i("LedApp", "Connected inputstream");
                mmOutStream = sock.getOutputStream();
                Log.i("LedApp", "Connected outputstream");
                tready.countDown();
            } catch (IOException e){
                e.printStackTrace();
                connectionHandler.obtainMessage(11111111, 1, -1, 1).sendToTarget();
            }
            while(running) {
                while(!paused) {
                    try {
                        if (mmInStream != null && mmInStream.available() > 0) {
                            bytes = mmInStream.read(buffer);
                            connectionHandler.obtainMessage(fragIndex, bytes, -1, buffer).sendToTarget();
                            buffer = new byte[buffer.length];
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
            try{
                if(mmOutStream != null){
                    mmOutStream.close();
                    mmOutStream = null;
                }
                if(mmInStream != null){
                    mmInStream.close();
                    mmInStream = null;
                }
                if(sock != null){
                    sock.close();
                    sock = null;
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return;
        }

        public void write(String content){
            try {
                byte[] bytes = content.getBytes();
                mmOutStream.write(bytes);
            }catch(IOException e){
                e.printStackTrace();
            }

        }

        public void close(){
            paused = true;
            running = false;
        }

        public void pau() {
            paused = true;
        }

        public void res() {
            paused = false;
        }

        public void ping() {
            byte[] ret = {13};
            byte[] buffer = new byte[1024];
            this.write(new String(ret));
            double start = System.currentTimeMillis();
            double difference;
            try {
                int bytes = 0;
                mmOutStream.write(("hello!").getBytes());
                while (System.currentTimeMillis() - start < 1000){
                    if(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        if(bytes > 0)break;
                    }
                }
                Log.i("LedApp","past the loop");
                if(bytes > 0){
                    byte[] received = new byte[bytes];
                    for(int i = 0; i < bytes; i++)received[i] = buffer[i];
                    Log.i("LedApp", new String(received));
                    pingval[0] = 1;
                }
                else pingval[0] = 0;
            }catch(Exception e){
                Log.e("LedApp", "error in ping!");
                e.printStackTrace();
            }
            synchron.countDown();
        }

    }

    private BTConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        sFrag = new SelectionFragment();
        sFrag.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, sFrag).commit();

        latch = new CountDownLatch(1);

        //registering a filter allows us to catch specific actions
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(btReceiver, filter);

        //the bluetooth adapter is where we discover devices
        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        devices = new ArrayList<BluetoothDevice>();

        //this is where we ask to enable bluetooth
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);

        //start discovering devices -- this is handled in the broadcast receiver
        mBluetooth.startDiscovery();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //we need this otherwise poo flinging will ensue
        unregisterReceiver(btReceiver);
    }

    public void onSelectionFragment(int index){
        mBluetooth.cancelDiscovery();
        if(!devices.get(index).getName().equals(activeDevice)) {
            tready = new CountDownLatch(1);
            if (connection != null) connection.close();
            connection = new BTConnection(devices.get(index), latch);
            connection.start();
            try {
                tready.await();
            } catch (InterruptedException e){}
            activeDevice = devices.get(index).getName();
        }
        btmFrag = new BTMenuFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, btmFrag);
        tran.addToBackStack(null);
        tran.commit();
        connection.pau();
        fragIndex = 0;
        connection.res();

    }

    public void switchAlive(View view){
        connection.pau();
        aFrag = new AliveFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, aFrag);
        tran.addToBackStack(null);
        tran.commit();
        //connection.pau();
        fragIndex = 1;
        //connection.res();
    }

    public void switchImage(View view){
        iFrag = new ImageFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, iFrag);
        tran.addToBackStack(null);
        tran.commit();
        connection.pau();
        fragIndex = 2;
        connection.res();
    }

    public void switchPassword(View view){
        pFrag = new PasswordFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, pFrag);
        tran.addToBackStack(null);
        tran.commit();
        connection.pau();
        fragIndex = 3;
        connection.res();
    }

    public void onAliveFragment(){
        connection.ping();
        try {
            latch.await();
        }catch(InterruptedException e){}
        int alive = pingval[0];
        if(alive > 0)aFrag.setMessage("It's alive");
        else aFrag.setMessage("It's dead");
        Log.i("LedApp", "end of onalive response");
        connection.res();
    }

    public void onCommunicationFragment(){
        //Do something
    }

    public void onImageFragment(){
        //Do something
    }

    public void onPasswordFragment(){
        //Do something
    }
    public void onReplaceFragment(DialogFragment dialog){
        //go back to main activity (Home)
    }
    public void disconnect(){
        if(connection != null){
            connection.close();
            activeDevice = "";
        }
    }
}