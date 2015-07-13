package com.cisco.prototype.ledsignaldetection;

import android.app.DialogFragment;
import android.inputmethodservice.KeyboardView;
import android.support.v4.app.FragmentManager;
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
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

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
    private SoftwareFragment soFrag;
    private int citer = 0;
    private boolean letsGoSoftware;
    private int passResult = 0;
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
                if(!result.contains("@")){
                    cFrag.addMessage(result);
                    ScrollView sView = (ScrollView)findViewById(R.id.scrollView);
                    sView.fullScroll(View.FOCUS_DOWN);
                }
            }
            else if(m.what == 11111111){
                Toast.makeText(getApplicationContext(), "Couldn't connect to device!", Toast.LENGTH_LONG).show();
                connection.close();
            }
            else if(m.what == 22222222){
                Toast.makeText(getApplicationContext(), "Disconnected from device", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            else if(m.what == 3){
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                passResult = pFrag.read(result);
                if(passResult > 0) citer++;
                passHit();
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

        public void write(String contentl){
            String content = contentl;
            try {
                if(content.contains("!^"))content = getSpecial(content.substring(2, content.length()));
                char ret = (char)13;
                if(content.trim().length() > 0) {
                    byte[] bytes = content.getBytes();
                    mmOutStream.write(bytes);
                }
                mmOutStream.write((byte)ret);
            }catch(IOException e){
                e.printStackTrace();
            }

        }

        private String getSpecial(String special){
            String returned = "";
            if(special.toLowerCase().equals("esc")) returned += (char)27;
            else if(special.toLowerCase().equals("tab")) returned += (char)9;
            return returned;
        }

        public void close(){
            paused = true;
            running = false;
            connectionHandler.obtainMessage(22222222, 1, -1, 1).sendToTarget();
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
                mmOutStream.write(("").getBytes());
                while (System.currentTimeMillis() - start < 2000){
                    if(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        if(bytes > 0)break;
                    }
                }
                Log.i("LedApp","past the loop");
                if(bytes > 0){
                    byte[] received = new byte[bytes];
                    for(int i = 0; i < bytes; i++)received[i] = buffer[i];
                    String mystr = new String(received);
                    while(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        received = new byte[bytes];
                        for(int i = 0; i < bytes; i++)received[i] = buffer[i];
                        mystr += new String(received);
                    }
                    Log.i("LedApp", mystr);
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
        connection.close();
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
        tran.addToBackStack("menu");
        tran.commit();
        connection.pau();
        fragIndex = 0;
        connection.res();

    }

    public void switchCommunication(View view){
        //connection.pau();
        cFrag = new CommunicationFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, cFrag);
        tran.addToBackStack(null);
        tran.commit();
        //connection.pau();
        fragIndex = 30;
        for(int i = 0; i< 5; i++) connection.write("");
        //connection.res();
    }

    public void switchAlive(View view){
        //connection.pau();
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
        citer = 0;
        pFrag = new PasswordFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, pFrag);
        tran.addToBackStack(null);
        tran.commit();
        //connection.pau();
        fragIndex = 3;
        //connection.res();
    }

    public void switchSoftware(View view){
        if(letsGoSoftware) {
            soFrag = new SoftwareFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, soFrag);
            tran.addToBackStack(null);
            tran.commit();
            connection.pau();
            fragIndex = 4;
            connection.res();
        } else{
            FragmentManager frag = getSupportFragmentManager();
            frag.popBackStack();
        }
    }

    public void onAliveFragment(){
        connection.ping();
        try {
            latch.await();
        }catch(InterruptedException e){}
        int alive = pingval[0];
        aFrag.enButton();
        if(alive > 0){
            aFrag.setMessage("It's alive");
            letsGoSoftware = true;
        } else {
            aFrag.setMessage("It's dead");
            letsGoSoftware = false;
        }
        Log.i("LedApp", "end of onalive response");
        connection.res();
    }

    public void onCommunicationFragment(View view){
        EditText editText = (EditText)findViewById(R.id.edit_message);
        connection.write(editText.getText().toString());
        cFrag.collapse();
    }

    public void onImageFragment(){
        //Do something
    }

    public void onPasswordFragment(){
        boolean result = pFrag.setMessage("Disconnect power cable from switch. Hold down Mode button and reconnect " +
                "power. Release Mode button when SYST LED blinks amber then turns solid green." +
                "Boot will then take place. I'll let you know when I've started the password " +
                "recovery script.");
    }

    private void passHit(){
        String command = null;
        switch(passResult){
            case 2: command = "n";
                    break;
            case 3: command = "en";
                    break;
            case 5: command = "";
                    break;
            case 6: command = "";
                    break;
            case 7: command = "conf t";
                    break;

            case 1: if(citer == 1) command = "flash_init";
                    else if(citer == 2) command = "load_helper";
                    else if(citer == 3) command = "rename flash:config.text flash:config.old";
                    else if(citer == 4) command = "boot";
                    break;
            case 4: if(citer == 7) command = "rename flash:config.old flash:config.text";
                    else if(citer == 9) command  = "copy flash:config.text system:running-config";
                    break;
            default: break;

        }
        if(command != null)connection.write(command);
    }

    public void disconnect(){
        if(connection != null){
            connection.close();
            activeDevice = "";
        }
    }
}