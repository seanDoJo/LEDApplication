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
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
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
    private KickstartFragment kFrag;
    private SystemBootFragment syFrag;
    private ImageRestoreFragment imgRestore;
    private int citer = 0;
    private boolean letsGoSoftware;
    private int passResult = 0;
    final int[] pingval = new int[1];
    CountDownLatch latch;
    CountDownLatch tready;
    private int mode;
    private String[] files;
    private ArrayList<imagePair> imageList;
    private ArrayList<String> kickstartImageList;
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
                //Communication
                byte[] data = (byte[])m.obj;
                String result = new String(data);
                if(!result.contains("@")){
                    cFrag.addMessage(result);
                    ScrollView sView = (ScrollView)findViewById(R.id.scrollView);
                    sView.fullScroll(View.FOCUS_DOWN);
                }
            }
            else if(m.what == 11111111){
                //couldn't connect
                Toast.makeText(getApplicationContext(), "Couldn't connect to device!", Toast.LENGTH_LONG).show();
                if(connection != null)connection.close();
            }
            else if(m.what == 22222222){
                //disconnect
                Toast.makeText(getApplicationContext(), "Disconnected from device", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            else if(m.what == 2){
                //Image
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                iFrag.log += result;
            }
            else if(m.what == 3){
                //Password
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                if(!result.contains("@"))pFrag.read(result);
            }
            else if(m.what == 4){
                //Software
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                soFrag.add(result);
            }
            else if(m.what == 5){
                //Kickstart
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
            }
            else if(m.what == 6){ //img recovery
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                imgRestore.step(result);
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
        private boolean inHelp = false;
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
            byte[] readB;
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
                            readB = new byte[bytes];
                            //Log.e("LEDapp", new String(readB));
                            for(int i = 0; i < bytes; i++)readB[i] = buffer[i];
                            connectionHandler.obtainMessage(fragIndex, bytes, -1, readB).sendToTarget();
                            for(int i = 0; i < 1024; i++)buffer[i] = 0;
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
                char ret = (char)13;
                if(content.contains("!!!")){
                    int i = 0;
                    while(content.charAt(i) == '!'){ i++; };
                    String specComm = content.substring(i, content.length());
                    Log.i("LEDApp", specComm);
                    if(specComm.contains("esc")){
                        char stop = (char)0x1A;
                        mmOutStream.write((byte)stop);
                        Log.i("LEDApp", "Special esc char sent");
                    }
                    else if(specComm.contains("break")){
                        char breakC = (char)0x03;
                        mmOutStream.write((byte)breakC);
                        Log.i("LEDApp", "Special break char sent");
                    }
                }
                else {
                    if (content.trim().length() > 0) {
                        byte[] bytes = content.getBytes();
                        mmOutStream.write(bytes);
                    }
                    if (!content.contains("?") && !inHelp) {
                        mmOutStream.write((byte) ret);
                    } else if (!inHelp && content.contains("?")) {
                        inHelp = true;
                    } else if (inHelp && content.toLowerCase().equals("q")) {
                        inHelp = false;
                    } else if (inHelp && content.equals("")) {
                        mmOutStream.write((byte) ret);
                    } else {
                        inHelp = false;
                    }
                }
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
            paused = true;
            byte[] buffer = new byte[1024];
            double start = System.currentTimeMillis();
            try {
                int bytes = 0;
                for(int i = 0; i<3; i++){
                    this.write("");
                }
                while (System.currentTimeMillis() - start < 3000){
                    if(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        if(bytes > 0)break;
                    }
                }
                pingval[0] = bytes > 0 ? 1 : 0;
            }catch(Exception e){
                Log.e("LedApp", "error in ping!");
                e.printStackTrace();
            }
            synchron.countDown();
            paused = false;
        }

    }

    private BTConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        if (connection != null)connection.close();
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
        //for(int i = 0; i< 5; i++)
        connection.write("");
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
            //soFrag.startSoft();
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

    public void onImageFragment(boolean kick){
        String identifier = "kickstart";
        kickstartImageList = new ArrayList<String>();

        //check for concurrent images
        iFrag.log = "";
        connection.write("dir");
        while(!iFrag.log.contains(">")){}
        files = iFrag.log.split("\n");
        for(int i = 0; i < files.length; i++){
            if (files[i].contains(identifier)){
                kickstartImageList.add(files[i]);
            }
        }
        for(int i = 0; i < kickstartImageList.size(); i++){
            String[]subStrings = kickstartImageList.get(i).split("-kickstart");
            for(int k = 0; k < files.length; k++){
                if(files[k].contains(subStrings[0]) && files[k].contains(subStrings[1])){
                    imageList.add(new imagePair(kickstartImageList.get(i), files[k]));
                }
            }
        }
        iFrag.populate(kickstartImageList, kick);
    }

    public void onSelectImageKick(int position){
        // try to boot kickstart
        connection.write("boot " + imageList.get(position).kickstartImage);

    }

    public void onSelectImageSys(int position){

    }

    public void onPasswordFragment(String message){
        if(!message.toLowerCase().contains("automatic") && !message.toLowerCase().contains("auto-booting"))pFrag.updateProgress();
        if(message != "")pFrag.setMessage(message);
        if(message.toLowerCase().contains("recovery complete!")){
            Button boot = (Button)findViewById(R.id.confautoboot);
            ProgressBar pBar = (ProgressBar)findViewById(R.id.progressBar);
            boot.setEnabled(true);
            boot.setVisibility(SurfaceView.VISIBLE);
            pBar.setEnabled(true);
            pBar.setVisibility(SurfaceView.GONE);
        }
    }

    public void configureAutoBoot(View view){
        pFrag.setAutoBootConf();
        writeData("");
        Button boot = (Button)findViewById(R.id.confautoboot);
        boot.setEnabled(false);
        boot.setVisibility(SurfaceView.GONE);
    }

    public void passwordStart(View view){
        EditText secret = (EditText)findViewById(R.id.secretpw);
        EditText enable = (EditText)findViewById(R.id.enablepw);
        EditText console = (EditText)findViewById(R.id.consolepw);
        TextView secretV = (TextView)findViewById(R.id.secretpwh);
        TextView enableV = (TextView)findViewById(R.id.enablepwh);
        TextView consoleV = (TextView)findViewById(R.id.consolepwh);
        RelativeLayout writeV = (RelativeLayout)findViewById(R.id.password_text);
        Button start = (Button)findViewById(R.id.recoverpw);
        String data = secret.getText().toString() + "," + enable.getText().toString() + "," + console.getText().toString();

        secret.setEnabled(false);
        secret.setVisibility(SurfaceView.GONE);
        enable.setEnabled(false);
        enable.setVisibility(SurfaceView.GONE);
        console.setEnabled(false);
        console.setVisibility(SurfaceView.GONE);
        start.setEnabled(false);
        start.setVisibility(SurfaceView.GONE);
        secretV.setVisibility(SurfaceView.GONE);
        enableV.setVisibility(SurfaceView.GONE);
        consoleV.setVisibility(SurfaceView.GONE);
        writeV.setVisibility(SurfaceView.VISIBLE);

        pFrag.startRecovery(data);
    }

    public void onSoftwareFragment(){
        connection.write("");
    }

    public void softwareMode(int mode){
        if(mode == 1){
            //loader
            kFrag = new KickstartFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, kFrag);
            tran.addToBackStack(null);
            tran.commit();
            connection.pau();
            fragIndex = 5;
            connection.res();
        } else if(mode == 2){
            //(boot)#
            syFrag = new SystemBootFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, syFrag);
            tran.addToBackStack(null);
            tran.commit();
            connection.pau();
            fragIndex = 6;
            connection.res();
        } else if(mode == 3){
            //<switch name>#
        }
    }

    public void onKickstartFragment(){
        // check for config issues

        // if config is ok, then look for image concurrency-- swap to image fragment.
        onImageFragment(true);
    }

    public void onImgRestoreCollection(View view){
        String passed = "";
        passed += ((EditText)findViewById(R.id.ipaddr)).getText();
        passed += ",";
        passed += ((EditText)findViewById(R.id.gwaddr)).getText();
        passed += ",";
        passed += ((EditText)findViewById(R.id.ftpaddr)).getText();
        passed += ",";
        passed += ((EditText)findViewById(R.id.ksimg)).getText();
        passed += ",";
        passed += ((EditText) findViewById(R.id.sysimg)).getText();

        imgRestore.collectInfo(passed);
    }

    public void writeData(String data){
        connection.write(data);
    }

    public void disconnect(){
        if(connection != null){
            connection.close();
            activeDevice = "";
        }
    }
}