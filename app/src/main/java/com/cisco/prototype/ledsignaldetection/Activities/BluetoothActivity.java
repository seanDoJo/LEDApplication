package com.cisco.prototype.ledsignaldetection.Activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.Fragments.BTMenuFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.CommunicationFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ConfigBackupFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ConnectionSelectFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.FileExplorerFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ImageFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ImageRestoreFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ImageSelectionFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.LoginFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.PasswordFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.SelectionFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.SoftwareFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.TFTPFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.TFTPPullFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ViewFileFragment;
import com.cisco.prototype.ledsignaldetection.R;
import com.cisco.prototype.ledsignaldetection.TFTPUtil;
import com.cisco.prototype.ledsignaldetection.imagePair;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BluetoothActivity extends FragmentActivity implements BluetoothInterface {
    private BluetoothAdapter mBluetooth;
    private File currConfig;
    private ArrayList<BluetoothDevice> devices;
    private ImageSelectionFragment isFrag = null;
    private TFTPPullFragment tpFrag = null;
    private String activeDevice = "";
    private int currMode = 0;
    private boolean stepped = false;
    private int REQUEST_ENABLE_BT = 123;
    private int fragIndex = -1;
    private CommunicationFragment cFrag;
    private SelectionFragment sFrag;
    private BTMenuFragment btmFrag;
    private PasswordFragment pFrag;
    private ImageFragment iFrag;
    private SoftwareFragment soFrag;
    private ImageRestoreFragment imgRestore;
    private FileExplorerFragment fileFrag;
    private ViewFileFragment fileViewer;
    private TFTPFragment tFrag;
    private ConnectionSelectFragment csFrag;
    private LoginFragment loginFrag;
    private ConfigBackupFragment cbFrag;
    private int citer = 0;
    private boolean letsGoSoftware;
    private int passResult = 0;
    final int[] pingval = new int[1];
    CountDownLatch latch;
    CountDownLatch tready;
    public boolean locked = false;
    public final Lock mLock = new ReentrantLock(true);
    private int mode;
    private ArrayList<String> files;
    private ArrayList<imagePair> imageList = new ArrayList<imagePair>();
    private ArrayList<String> kickstartImageList = new ArrayList<String>();
    private ArrayList<String> kickstartFinal = new ArrayList<String>();
    private int state;
    private String message = "";
    private boolean kick = false;
    private File outputFile;
    private BufferedWriter writer = null;
    private boolean captureEnabled = false;
    private File viewedFile = null;
    private String imageLog = "";
    private boolean first_image_open;
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
            Log.i("LEDApp", "Handler received message for: " + Integer.toString(m.what));
            if(m.what == 30){
                //Communication
                byte[] data = (byte[])m.obj;
                String result = new String(data);
                if(!result.contains("@")){
                    cFrag.addMessage(result);
                    if(captureEnabled){
                        try {
                            writer.write(result);
                        }catch(IOException e){e.printStackTrace();}
                    }
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
                iFrag.read(result);
            }
            else if(m.what == 3){
                //Password
                mLock.lock();
                locked = true;
                mLock.unlock();
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                if (captureEnabled) {
                    try {
                        writer.write(result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!result.contains("@")) pFrag.read(result);
                mLock.lock();
                locked = false;
                mLock.unlock();
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
                mLock.lock();
                locked = true;
                mLock.unlock();
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                imgRestore.step(result);
                mLock.lock();
                locked = false;
                mLock.unlock();
            }
            else if (m.what == 10){
                mLock.lock();
                locked = true;
                mLock.unlock();
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                loginFrag.readLogin(result);
                mLock.lock();
                locked = false;
                mLock.unlock();
            }
            else if(m.what == 20){
                mLock.lock();
                locked = true;
                mLock.unlock();
                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                cbFrag.read(result);
                mLock.lock();
                locked = false;
                mLock.unlock();
            }
            else if(m.what == 56){
                int status = (int)m.obj;
                Log.i("LEDApp", "called handler with 56");
                Button terminal = null;
                Button currbutton = null;
                RelativeLayout lin = null;
                TextView statlabel = (TextView)findViewById(R.id.status_label);
                View circle = (View)findViewById(R.id.status_indc);
                Drawable resid = null;
                clearBtMenu();
                switch(status){
                    case -1:
                        statlabel.setText("Device Status: unreachable");
                        resid = getResources().getDrawable(R.drawable.circle_red);
                        circle.setBackground(resid);
                        break;
                    case 0:
                        statlabel.setText("Device Status: booting...");
                        resid = getResources().getDrawable(R.drawable.circle_yellow);
                        circle.setBackground(resid);
                        lin = (RelativeLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);

                        terminal = (Button) findViewById(R.id.terminalSwitch);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        terminal.setBackground(resid);
                        terminal.setTextColor(Color.parseColor("#ffaa052a"));
                        terminal.setEnabled(true);
                        break;
                    case 1:
                        statlabel.setText("Device Status: bootloader");
                        resid = getResources().getDrawable(R.drawable.circle_yellow);
                        circle.setBackground(resid);
                        lin = (RelativeLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);

                        currbutton = (Button) findViewById(R.id.password);
                        currbutton.setEnabled(true);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        currbutton.setBackground(resid);
                        currbutton.setTextColor(Color.parseColor("#ffaa052a"));
                        currbutton.setEnabled(true);

                        currbutton = (Button) findViewById(R.id.imageRec);
                        currbutton.setEnabled(true);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        currbutton.setBackground(resid);
                        currbutton.setTextColor(Color.parseColor("#ffaa052a"));
                        currbutton.setEnabled(true);

                        terminal = (Button) findViewById(R.id.terminalSwitch);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        terminal.setBackground(resid);
                        terminal.setTextColor(Color.parseColor("#ffaa052a"));
                        terminal.setEnabled(true);
                        break;
                    case 2:
                        statlabel.setText("Device Status: operational");
                        resid = getResources().getDrawable(R.drawable.circle_green);
                        circle.setBackground(resid);
                        lin = (RelativeLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);

                        currbutton = (Button) findViewById(R.id.password);
                        currbutton.setEnabled(true);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        currbutton.setBackground(resid);
                        currbutton.setTextColor(Color.parseColor("#ffaa052a"));
                        currbutton.setEnabled(true);

                        currbutton = (Button) findViewById(R.id.login);
                        currbutton.setEnabled(true);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        currbutton.setBackground(resid);
                        currbutton.setTextColor(Color.parseColor("#ffaa052a"));
                        currbutton.setEnabled(true);

                        terminal = (Button) findViewById(R.id.terminalSwitch);
                        resid = getResources().getDrawable(R.drawable.button_home);
                        terminal.setBackground(resid);
                        terminal.setTextColor(Color.parseColor("#ffaa052a"));
                        terminal.setEnabled(true);
                        break;
                }
            }
        }
    };
    //Class for the connection thread -- reading is done automatically, writing is done by invoking the write(String) method
    private class BTConnection extends Thread {
        private BluetoothSocket sock = null;
        private CountDownLatch synchron = null;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean running = true;
        private boolean ok = true;
        private boolean paused = false;
        private boolean inHelp = false;
        private boolean telnet = false;
        private String lrecord = "";
        private boolean destroyerOfLoops = false;
        private TelnetClient tc = null;
        private Pattern booted = Pattern.compile("(?s).*[^()#]+#[^#]*");
        private Pattern boot = Pattern.compile("(?s).*[sS]witch\\(boot\\)#[^#]*");
        private Pattern bootconfig = Pattern.compile("(?s).*[sS]witch\\(boot-config\\)#[^#]*");
        private Pattern altbootconfig = Pattern.compile("(?s).*[sS]witch\\(boot\\)\\(config\\)#[^#]*");
        private Pattern loader = Pattern.compile("(?s).*[lL]{1}oader>[^>]*");
        public BTConnection(String ipaddr, int portNum, String username, String password, CountDownLatch synchron){
            telnet = true;
            StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
            StrictMode.setThreadPolicy(tp);
            tc = new TelnetClient();
            byte[] buffer = new byte[1024];
            byte[] readB;
            int bytes;
            try {
                tc.connect(InetAddress.getByName(ipaddr), portNum);
                String record = "";
                mmInStream = tc.getInputStream();
                mmOutStream = tc.getOutputStream();
                while(!record.toLowerCase().contains("username:") || record.toLowerCase().contains("user:") || record.toLowerCase().contains("login:")){
                    if(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        readB = new byte[bytes];
                        for(int i = 0; i < bytes; i++)readB[i] = buffer[i];
                        for(int i = 0; i < 1024; i++)buffer[i] = 0;
                        record += (new String(readB));
                    }
                }
                record = "";
                byte[] uname = (username != null) ? (username + "\n").getBytes() : "bland\n".getBytes();
                mmOutStream.write(uname, 0, uname.length);
                mmOutStream.flush();
                while(!record.contains("Password:")){
                    if(mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);
                        readB = new byte[bytes];
                        for(int i = 0; i < bytes; i++)readB[i] = buffer[i];
                        for(int i = 0; i < 1024; i++)buffer[i] = 0;
                        record += (new String(readB));
                        if(record.toLowerCase().contains("failed")){
                            connectionHandler.obtainMessage(22222222, 1, -1, 1).sendToTarget();
                            break;
                        }
                    }
                }
                uname = (password != null) ? (password + "\n\n").getBytes() : "yOuShOuLdUsEtHeScRiPt\n\n".getBytes();
                mmOutStream.write(uname, 0, uname.length);
                mmOutStream.flush();

                uname = "".getBytes();
                mmOutStream.write(uname, 0, uname.length);
                mmOutStream.flush();

            }catch(UnknownHostException e){
                e.printStackTrace();
                ok = false;
            }catch(IOException e){
                e.printStackTrace();
                ok = false;
            }
            if(synchron != null)synchron.countDown();
        }
        public BTConnection(BluetoothDevice newDevice, CountDownLatch synchron){
            telnet = false;
            if(!telnet) {
                BluetoothSocket tmp = null;
                try {
                    tmp = newDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                    Log.i("LedApp", "Created socket with device");
                } catch (IOException e) {
                    Log.e("LedApp", "Failed in socket creation");
                    e.printStackTrace();
                    ok = false;
                    //System.exit(1);
                }
                sock = tmp;
            }
            if(synchron != null)synchron.countDown();
        }
        public void run(){
            byte[] buffer = new byte[1024];
            byte[] readB;
            int bytes;
            double last = System.currentTimeMillis();
            try{
                if(!telnet) {
                    sock.connect();
                    Log.i("LedApp", "Connected socket with device");
                    mmInStream = sock.getInputStream();
                    Log.i("LedApp", "Connected inputstream");
                    mmOutStream = sock.getOutputStream();
                    Log.i("LedApp", "Connected outputstream");
                }
                if(ok)tready.countDown();
            } catch (IOException e){
                e.printStackTrace();
                connectionHandler.obtainMessage(11111111, 1, -1, 1).sendToTarget();
            }
            while(running) {
                while(!paused) {
                    try {
                        mLock.lock();
                        if (mmInStream != null && mmInStream.available() > 0 && locked == false) {
                            bytes = mmInStream.read(buffer);
                            readB = new byte[bytes];
                            //Log.e("LEDapp", new String(readB));
                            for(int i = 0; i < bytes; i++)readB[i] = buffer[i];
                            if(destroyerOfLoops){
                                if(loader.matcher(lrecord).matches() || altbootconfig.matcher(lrecord).matches() ||
                                        bootconfig.matcher(lrecord).matches() || boot.matcher(lrecord).matches() ||
                                        booted.matcher(lrecord).matches()){
                                    lrecord = "";
                                    destroyerOfLoops = false;
                                }
                                else {
                                    String latest = new String(readB);
                                    lrecord += latest;
                                    write("!!!break");
                                    writeData("!!!ctrl]");
                                }
                            }
                            connectionHandler.obtainMessage(fragIndex, bytes, -1, readB).sendToTarget();
                            for(int i = 0; i < 1024; i++)buffer[i] = 0;
                        }
                        mLock.unlock();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try{
                if(tc != null){
                    try {
                        tc.disconnect();
                    }catch(Exception e){
                        Log.e("LEDApp", "weiner farts");
                    }
                }
                if(!telnet) {
                    if (mmOutStream != null) {
                        mmOutStream.close();
                        mmOutStream = null;
                    }
                    if (mmInStream != null) {
                        mmInStream.close();
                        mmInStream = null;
                    }
                    if (sock != null) {
                        sock.close();
                        sock = null;
                    }
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
                    if(specComm.replace("-","").contains("ctrlz")){
                        char stop = (char)0x1A;
                        byte[] bytes = ("" + stop).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                        Log.i("LEDApp", "Special esc char sent");
                    }
                    else if(specComm.replace("-", "").contains("ctrl]")){
                        char inter = (char)0x1D;
                        byte[] bytes = ("" + inter).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                        Log.i("LEDApp", "Special break char sent");
                    }
                    else if(specComm.contains("break")){
                        char breakC = (char)0x03;
                        byte[] bytes = ("" + breakC).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                    }
                    else if(specComm.contains("space")){
                        char spaceC = (char)0x20;
                        byte[] bytes = ("" + spaceC).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                    }
                    else if(specComm.contains("freedomrings")){
                        destroyerOfLoops = true;
                    }
                } else {
                    if (content.trim().length() > 0) {
                        byte[] bytes = content.getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                    }
                    if (!content.contains("?") && !inHelp) {
                        byte[] bytes = ("" + ret).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
                    } else if (!inHelp && content.contains("?")) {
                        inHelp = true;
                    } else if (inHelp && content.toLowerCase().equals("q")) {
                        inHelp = false;
                    } else if (inHelp && content.equals("")) {
                        char spaceC = (char)0x20;
                        byte[] bytes = ("" + spaceC).getBytes();
                        if(telnet){
                            mmOutStream.write(bytes, 0, bytes.length);
                            mmOutStream.flush();
                        }
                        else {
                            mmOutStream.write(bytes);
                        }
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

        public boolean isOk(){
            return this.ok;
        }

        public void close(){
            paused = true;
            running = false;
            connectionHandler.obtainMessage(22222222, 1, -1, 1).sendToTarget();
        }
        public String sendCont(String command){
            double timeout = System.currentTimeMillis();
            byte[] buffer = new byte[1024];
            byte[] readB;
            int bytes;
            String output =  "";
            write(command);
            try {
                while ((System.currentTimeMillis() - timeout) < 5000) {
                    if(mmInStream.available() > 0)break;
                }
                if (mmInStream != null && mmInStream.available() > 0) {
                    double latest = System.currentTimeMillis();
                    while(mmInStream.available() > 0 || (System.currentTimeMillis() - latest) < 500) {
                        if(mmInStream.available() > 0) {
                            bytes = mmInStream.read(buffer);
                            readB = new byte[bytes];
                            //Log.e("LEDapp", new String(readB));
                            for (int i = 0; i < bytes; i++) readB[i] = buffer[i];
                            String newStr = new String(readB);
                            output += newStr;
                            for (int i = 0; i < 1024; i++) buffer[i] = 0;
                            latest = System.currentTimeMillis();
                        }
                    }
                }
            }catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            return output;
        }

        public void pau() {
            paused = true;
        }

        public void res() {
            paused = false;
        }

        public void ping(CountDownLatch pingLatch) {
            Pattern booted1 = Pattern.compile("(?s)[^()#]*#[^#]*");
            Pattern booted2 = Pattern.compile("(?s).*[sS]{1}witch>[^>]*");
            Pattern booted3 = Pattern.compile("(?s).*[pP]{1}assword:.*");
            Pattern booted4 = Pattern.compile("(?s).*[lL]{1}ogin:.*");

            Pattern ldr1 = Pattern.compile("(?s)[^()#]*\\(.*\\)#[^#]*");
            Pattern ldr2 = Pattern.compile("(?s).*[sS]{1}witch:.*");
            Pattern ldr3 = Pattern.compile("(?s).*[lL]{1}oader>[^>]*");
            paused = true;
            byte[] buffer = new byte[1024];
            double time = 0;
            int pingcounter = 0;
            int[] pingcount = {1, 0, 0};
            int found = 0;
            double start = System.currentTimeMillis();
            try {
                int bytes = 0;
                this.write("");
                while ((time = System.currentTimeMillis() - start) < 3000){
                    if(mmInStream.available() > 0){
                        found = 1;
                        break;
                    }
                    else if(pingcount[(int)Math.floor(time) / 1000] == 0){
                        pingcount[(int)Math.floor(time) / 1000] = 1;
                        this.write("");
                    }
                }
                String returned = "";
                boolean booting = false;
                if(mmInStream.available() > 0) {
                    double first = System.currentTimeMillis();
                    double latest = System.currentTimeMillis();
                    byte[] readB;
                    while (mmInStream.available() > 0 || (System.currentTimeMillis() - latest) < 500) {
                        if (mmInStream.available() > 0) {
                            bytes = mmInStream.read(buffer);
                            readB = new byte[bytes];
                            //Log.e("LEDapp", new String(readB));
                            for (int i = 0; i < bytes; i++) readB[i] = buffer[i];
                            String newStr = new String(readB);
                            returned += newStr;
                            for (int i = 0; i < 1024; i++) buffer[i] = 0;
                            latest = System.currentTimeMillis();
                        }
                        if(System.currentTimeMillis() - first > 5000){
                            booting = true;
                            break;
                        }
                    }
                    Log.i("LEDApp", returned);
                    if(!booting) {
                        if (booted1.matcher(returned).matches() || booted2.matcher(returned).matches()) {
                            connectionHandler.obtainMessage(56, 1, -1, 2).sendToTarget();
                            Log.i("LEDApp", "message sent to handler");
                        }else if(booted3.matcher(returned).matches() || booted4.matcher(returned).matches()){
                            connectionHandler.obtainMessage(56, 1, -1, 2).sendToTarget();
                        }
                        else if(ldr3.matcher(returned).matches()){
                            kick = true;
                            connectionHandler.obtainMessage(56, 1, -1, 1).sendToTarget();
                        } else if (ldr2.matcher(returned).matches() || ldr1.matcher(returned).matches()) {
                            connectionHandler.obtainMessage(56, 1, -1, 1).sendToTarget();
                            Log.i("LEDApp", "message sent to handler");
                        } else {
                            connectionHandler.obtainMessage(56, 1, -1, 0).sendToTarget();
                        }
                    }
                    else {
                        connectionHandler.obtainMessage(56, 1, -1, 0).sendToTarget();
                        Log.i("LEDApp", "message sent to handler");
                    }
                }
                else{
                    connectionHandler.obtainMessage(56, 1, -1, -1).sendToTarget();
                    Log.i("LEDApp", "message sent to handler");
                }
                pingval[0] = found > 0 ? 1 : 0;
            }catch(Exception e){
                Log.e("LedApp", "error in ping!");
                e.printStackTrace();
            }
            if (pingLatch != null) pingLatch.countDown();
            Log.i("LEDApp", "ping executed");
            paused = false;
        }

    }

    private BTConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        first_image_open = true;
        setContentView(R.layout.activity_bluetooth);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        currMode = intent.getIntExtra(HomeActivity.EXTRA_MESSAGE, 0);
        if(currMode == 0) {
            csFrag = new ConnectionSelectFragment();
            csFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, csFrag).commit();
        }else if(currMode == 3){
            isFrag = new ImageSelectionFragment();
            isFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, isFrag).commit();
        }
        else{
            fileFrag = new FileExplorerFragment();
            fileFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fileFrag).commit();
        }
    }
    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //we need this otherwise poo flinging will ensue
        if(currMode == 0) {
            if (connection != null) connection.close();
            if (mBluetooth != null) mBluetooth.cancelDiscovery();
        }
    }

    public void closeBluetooth(){
        if(currMode == 0) {
            if (connection != null) connection.close();
            if (mBluetooth != null) mBluetooth.cancelDiscovery();
            unregisterReceiver(btReceiver);
        }
    }

    public void switchBluetooth(View view){
        sFrag = new SelectionFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, sFrag);
        tran.addToBackStack(null);
        tran.commit();

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

    public void onSelectionFragment(int index){
        mBluetooth.cancelDiscovery();
        if(!devices.get(index).getName().equals(activeDevice)) {
            tready = new CountDownLatch(1);
            latch = new CountDownLatch(1);
            if (connection != null) connection.close();
            connection = new BTConnection(devices.get(index), latch);
            try {
                latch.await();
            }catch(InterruptedException e){e.printStackTrace();}
            if(!connection.isOk())connection.close();
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


    public void switchTelnet(View view){
        LinearLayout rel = (LinearLayout)findViewById(R.id.telMenu);
        rel.setVisibility(SurfaceView.VISIBLE);
        RelativeLayout lin = (RelativeLayout)findViewById(R.id.connectButtons);
        lin.setVisibility(SurfaceView.GONE);
        Button button = (Button)findViewById(R.id.bluetoothSwitch);
        button.setEnabled(false);
        button.setVisibility(SurfaceView.GONE);
        button = (Button)findViewById(R.id.telnetSwitch);
        button.setEnabled(false);
        button.setVisibility(SurfaceView.GONE);
    }

    public void onTelnetStart(View view){
        EditText address = (EditText)findViewById(R.id.telAddress);
        EditText port = (EditText)findViewById(R.id.telPort);
        EditText username = (EditText)findViewById(R.id.telPortComU);
        EditText password = (EditText)findViewById(R.id.telPortComP);
        File preFile = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "misc" + File.separator + "connectionLogins.txt");
        if(preFile.exists()){
            preFile.delete();
        }
        try {
            preFile.createNewFile();
        }catch(IOException e){e.printStackTrace();}
        BufferedWriter writer = null;
        try{
            writer = new BufferedWriter(new FileWriter(preFile));
            writer.write(address.getText().toString() + ","+port.getText().toString() + ","+username.getText().toString() + ","+password.getText().toString());
            writer.close();
        }catch(IOException e){e.printStackTrace();}
        csFrag.collapse();
        tready = new CountDownLatch(1);
        latch = new CountDownLatch(1);
        if (connection != null) connection.close();
        connection = new BTConnection(address.getText().toString().trim(),Integer.parseInt(port.getText().toString().trim()),
                username.getText().toString(), password.getText().toString(), latch);
        try {
            latch.await();
        }catch(InterruptedException e){e.printStackTrace();}
        if(!connection.isOk())connection.close();
        connection.start();
        try {
            tready.await();
        } catch (InterruptedException e){}
        btmFrag = new BTMenuFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, btmFrag);
        tran.addToBackStack("menu");
        tran.commit();
        connection.pau();
        fragIndex = 0;
        connection.res();
    }

    public void configureMenu(){
        //CountDownLatch configL = new CountDownLatch(1);
        connection.ping(null);
        /*try {
            configL.await();
        }catch(InterruptedException e){e.printStackTrace();}*/
        Log.i("LEDApp", "returned from ping");

    }

    public void refreshStatus(View view){
        clearBtMenu();
        connection.ping(null);
    }

    private void clearBtMenu(){
        Button currbutton = null;
        Button terminal = null;
        Drawable resid = null;

        terminal = (Button) findViewById(R.id.terminalSwitch);
        resid = getResources().getDrawable(R.drawable.button_gray);
        terminal.setBackground(resid);
        terminal.setTextColor(Color.parseColor("#85666666"));
        terminal.setEnabled(false);

        currbutton = (Button)findViewById(R.id.password);
        resid = getResources().getDrawable(R.drawable.button_gray);
        currbutton.setBackground(resid);
        currbutton.setTextColor(Color.parseColor("#85666666"));
        currbutton.setEnabled(false);


        currbutton = (Button)findViewById(R.id.imageRec);
        resid = getResources().getDrawable(R.drawable.button_gray);
        currbutton.setBackground(resid);
        currbutton.setTextColor(Color.parseColor("#85666666"));
        currbutton.setEnabled(false);
    }

    public void switchImage(View view){
        iFrag = new ImageFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, iFrag);
        tran.addToBackStack(null);
        tran.commit();
        connection.pau();
        fragIndex = 2;
        writeData("");
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
        writeData("");
        //connection.res();
    }

    public void switchLogin(){
        loginFrag = new LoginFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, loginFrag);
        tran.addToBackStack(null);
        mLock.lock();
        tran.commit();
        fragIndex = 10;
        Log.e("LEDApp", "changed to index 10");
        mLock.unlock();
    }

    public void checkLogin(){
        mLock.lock();
        locked = true;
        mLock.unlock();
        String line = connection.sendCont("");
        mLock.lock();
        locked = false;
        mLock.unlock();
        if(line.toLowerCase().contains("login") || line.toLowerCase().contains("user") || line.toLowerCase().contains("password")){
            loginFrag = new LoginFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, loginFrag);
            tran.addToBackStack(null);
            mLock.lock();
            tran.commit();
            fragIndex = 10;
            Log.e("LEDApp", "changed to index 10");
            mLock.unlock();
        }
    }

    public void switchBackup(View view){
        cbFrag = new ConfigBackupFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, cbFrag);
        tran.addToBackStack(null);
        mLock.lock();
        tran.commit();
        fragIndex = 20;
        mLock.unlock();
    }

    public void backupConfig(View view){
        EditText filename = (EditText)findViewById(R.id.configfn);
        Button buttn = (Button)findViewById(R.id.configbtn);
        TextView fnTitle = (TextView)findViewById(R.id.configfnt);
        RelativeLayout radios = (RelativeLayout)findViewById(R.id.radiobuttons);
        currConfig = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "misc" + File.separator + "config" + File.separator + filename.getText().toString());
        if(currConfig.exists())currConfig.delete();
        try {
            currConfig.createNewFile();
        }catch(IOException e){e.printStackTrace();}
        buttn.setEnabled(false);
        buttn.setVisibility(SurfaceView.GONE);
        fnTitle.setText("Backing up configuration...");
        radios.setVisibility(SurfaceView.GONE);
        filename.setVisibility(SurfaceView.GONE);
        cbFrag.setEnabled();
    }

    public void setLoggedIn(boolean success){
        if(success){
            Log.i("login", "Login success");
            FragmentManager fm = getSupportFragmentManager();
            mLock.lock();
            fm.popBackStack();
            mLock.unlock();
        } else {
            Toast.makeText(BluetoothActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
        }
    }
    public void loginSubmit(View view){
        EditText user = (EditText)findViewById(R.id.loginuname);
        EditText password = (EditText)findViewById(R.id.loginpw);
        String data = user.getText().toString() + "," + password.getText().toString();
        loginFrag.submit(data);

    }

    public void switchFileExplorer(View view){
        //connection.pau();
        fileFrag = new FileExplorerFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, fileFrag);
        tran.addToBackStack(null);
        tran.commit();
        if(connection != null) {
            connection.pau();
            fragIndex = 7;
            connection.res();
        }
    }

    public void startFileExplorer(){
        File appFolder = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "captures");
        File configFolder = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "misc" + File.separator + "config");
        File[] folderContents = appFolder.listFiles();
        File[] configContents = configFolder.listFiles();
        File[] files = new File[folderContents.length + configContents.length];
        int v = 0;
        for(int i = 0; i < folderContents.length; i ++, v++){
            files[v] = folderContents[i];
        }
        for(int i = 0; i < configContents.length; i ++, v++){
            files[v] = configContents[i];
        }
        fileFrag.init(files);
    }
    public void startImageViewer(){
        File appFolder = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "images");
        File[] folderContents = appFolder.listFiles();
        isFrag.init(folderContents);
    }

    public void viewFile(File file){
        viewedFile = file;
        fileViewer = new ViewFileFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, fileViewer);
        tran.addToBackStack(null);
        tran.commit();
        if(connection != null) {
            connection.pau();
            fragIndex = 8;
            connection.res();
        }
    }

    public void initFileView(){
        fileViewer.viewCurrentFile(viewedFile);
    }

    public void switchEmail(View view){
        if(true){
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");

            Uri uri = Uri.fromFile(viewedFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));


        } else {
            Toast.makeText(BluetoothActivity.this, "Sorry, couldn't make a file.", Toast.LENGTH_SHORT).show();
        }
    }

    public void switchFtp(View view){
        tFrag = new TFTPFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, tFrag);
        tran.addToBackStack(null);
        tran.commit();
        if(connection != null) {
            connection.pau();
            fragIndex = 9;
            connection.res();
        }
    }

    public void switchPullFtp(View view){
        tpFrag = new TFTPPullFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, tpFrag);
        tran.addToBackStack(null);
        tran.commit();
    }

    public void sendFtp(View view){
        CountDownLatch latch = new CountDownLatch(1);
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
        TFTPUtil sendUtil = new TFTPUtil();

        EditText remote = (EditText)findViewById(R.id.tftRemote);
        EditText address = (EditText)findViewById(R.id.tftIP);
        EditText username = (EditText)findViewById(R.id.tftuname);
        EditText password = (EditText)findViewById(R.id.tftpass);
        Button butt = (Button)findViewById(R.id.tftSend);

        sendUtil.start();
        sendUtil.transfer(viewedFile.getAbsolutePath(), remote.getText().toString(), address.getText().toString(), username.getText().toString(), password.getText().toString(), null);
        sendUtil.close();

        FragmentManager frag = getSupportFragmentManager();
        frag.popBackStack();
    }

    public void deleteFile(View view){
        destroyFile();
        viewedFile.delete();
        FragmentManager frag = getSupportFragmentManager();
        frag.popBackStack();
    }

    public void pullFtp(View view){
        Pattern filename = Pattern.compile("^.*/([^/]+)$");
        CountDownLatch latch = new CountDownLatch(1);
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
        TFTPUtil sendUtil = new TFTPUtil();

        EditText remote = (EditText)findViewById(R.id.tftpullRemote);
        String localFile = remote.getText().toString();
        Matcher myMatch = filename.matcher(localFile);
        if(myMatch.find()){
            localFile = myMatch.group(1).trim();
        }
        EditText address = (EditText)findViewById(R.id.tftpullIP);
        EditText username = (EditText)findViewById(R.id.tftpulluname);
        EditText password = (EditText)findViewById(R.id.tftpullpass);
        Button butt = (Button)findViewById(R.id.tftPull);
        File newf = new File(Environment.getExternalStorageDirectory() + File.separator + "SwitchArmyKnife" + File.separator + "images" + File.separator + localFile);
        try {
            newf.createNewFile();
        }catch(IOException e){e.printStackTrace();}

        sendUtil.start();
        sendUtil.receive(newf.getAbsolutePath(), remote.getText().toString(), address.getText().toString(), username.getText().toString(), password.getText().toString(), null);
        sendUtil.close();

        FragmentManager frag = getSupportFragmentManager();
        frag.popBackStack();
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

    public void onCommunicationFragment(View view){
        EditText editText = (EditText)findViewById(R.id.edit_message);
        writeData(editText.getText().toString());
        cFrag.collapse();
    }

    public void onCheckClick(View view){
        EditText editText = (EditText)findViewById(R.id.file_edit_text);
        Button button = (Button)findViewById(R.id.file_ok_button);
        TextView textView = (TextView)findViewById(R.id.save_output);

        CheckBox checkBox = (CheckBox)findViewById(R.id.checkbox);
        if(checkBox.isChecked()){
            textView.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            editText.setEnabled(true);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
        } else {
            //clear up UI stuff
            editText.setEnabled(false);
            editText.setVisibility(View.GONE);
            button.setEnabled(false);
            button.setVisibility(View.GONE);
            textView.setText("Save console output?");
            textView.setVisibility(View.VISIBLE);
            if(!(editText.isShown() || button.isShown())){destroyFile();}
        }
    }

    public void destroyFile(){
        //toast with "filename saved"
        captureEnabled = false;
        try{
            if(writer != null)writer.close();
        } catch (IOException e){e.printStackTrace();}
    }

    private void createFile(String filenamel){
        String filename = filenamel.replaceAll("\\s+", "");
        outputFile = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "captures" + File.separator + filename + ".txt");
        try {
            if(outputFile.exists())outputFile.delete();
            outputFile.createNewFile();
            writer = new BufferedWriter(new FileWriter(outputFile));
            captureEnabled = true;
        }catch(IOException e){e.printStackTrace();}
    }

    public void onOKClick(View view){
        EditText editText = (EditText)findViewById(R.id.file_edit_text);
        Button button = (Button)findViewById(R.id.file_ok_button);
        TextView textView = (TextView)findViewById(R.id.save_output);

        String fileName = editText.getText().toString().trim();
        editText.setEnabled(false);
        editText.setVisibility(View.GONE);
        button.setEnabled(false);
        button.setVisibility(View.GONE);
        textView.setText("Current file name: " + fileName);
        textView.setVisibility(View.VISIBLE);

        createFile(fileName.replaceAll("^(.*)\\..*$", "$1"));
        //Open file
    }

    public void onCheckView(View view){
        CheckBox check = (CheckBox)findViewById(R.id.view_check);
        TextView fileText = (TextView)findViewById(R.id.fileText);
        TextView lineNumber = (TextView)findViewById(R.id.line_numbers);

        if(check.isChecked()) {
            lineNumber.setText("");
            int lines = fileText.getLineCount();
            for (int i = 1; i <= lines; i++) {
                lineNumber.setText(lineNumber.getText() + "\n" + Integer.toString(i));
            }
            lineNumber.setVisibility(View.VISIBLE);
        } else {
            lineNumber.setVisibility(View.GONE);
        }
    }

    public boolean firstImage(){return first_image_open;}

    public void onImageFragment(){
        //if(first_image_open){
            iFrag.setReadOutput(false);
            Log.i("image", "onImage entered");
            kickstartImageList = new ArrayList<String>();
            iFrag.kickstart = kick;

            first_image_open = false;
            //check for concurrent images
            writeData("dir");
            iFrag.log = "";
            iFrag.setReadOutput(true);
            Log.i("image1", "end of onImage: " + iFrag.readOutput);
            //fragment will call onFileListObtained to continue once prompt is reached.
        //} else imageStateMachine(0);
    }

    public void enableSubmit(){
        //disable grey button
        Button submit = (Button)findViewById(R.id.submit_image);
        Drawable resid = null;

        resid = getResources().getDrawable(R.drawable.button_home);
        submit.setBackground(resid);
        submit.setTextColor(Color.parseColor("#ffaa052a"));
        submit.setEnabled(true);
    }

    public void greyButtonImage(){
        Button submit = (Button) findViewById(R.id.submit_image);
        Drawable resid = null;

        resid = getResources().getDrawable(R.drawable.button_gray);
        submit.setBackground(resid);
        submit.setTextColor(Color.parseColor("#85666666"));
        submit.setEnabled(false);
    }

    public void onGuessedImage(View view){
        RadioButton guess = (RadioButton)findViewById(R.id.guess_button);
        RadioButton allFiles = (RadioButton)findViewById(R.id.file_button);
        RadioButton download = (RadioButton)findViewById(R.id.download);
        Spinner sys = (Spinner)findViewById(R.id.sysImages);
        Spinner ks = (Spinner)findViewById(R.id.kickImages);
        Spinner fs = (Spinner) findViewById(R.id.file_spinner);

        fs.setVisibility(View.GONE);
        guess.setChecked(true);
        allFiles.setChecked(false);
        download.setChecked(false);

        if(iFrag.kickstart) ks.setVisibility(View.VISIBLE);
        else sys.setVisibility(View.VISIBLE);
        enableSubmit();
    }

    public void onFileImage(View view){
        RadioButton guess = (RadioButton)findViewById(R.id.guess_button);
        RadioButton allFiles = (RadioButton)findViewById(R.id.file_button);
        RadioButton download = (RadioButton)findViewById(R.id.download);
        Spinner sys = (Spinner)findViewById(R.id.sysImages);
        Spinner ks = (Spinner)findViewById(R.id.kickImages);
        Spinner fs = (Spinner) findViewById(R.id.file_spinner);

        ks.setVisibility(View.GONE);
        sys.setVisibility(View.GONE);

        guess.setChecked(false);
        download.setChecked(false);

        allFiles.setChecked(true);
        fs.setVisibility(View.VISIBLE);
        enableSubmit();
    }

    public void onDownload(View view){
        RadioButton guess = (RadioButton)findViewById(R.id.guess_button);
        RadioButton allFiles = (RadioButton)findViewById(R.id.file_button);
        RadioButton download = (RadioButton)findViewById(R.id.download);
        Spinner sys = (Spinner)findViewById(R.id.sysImages);
        Spinner ks = (Spinner)findViewById(R.id.kickImages);
        Spinner fs = (Spinner) findViewById(R.id.file_spinner);

        ks.setVisibility(View.GONE);
        sys.setVisibility(View.GONE);
        fs.setVisibility(View.GONE);

        guess.setChecked(false);
        allFiles.setChecked(false);

        download.setChecked(true);
        enableSubmit();
    }

    public void showTerminalOutput(View view){
        CheckBox check = (CheckBox) findViewById(R.id.terminal_checkbox);
        ScrollView scroll = (ScrollView) findViewById(R.id.image_output);

        if(check.isChecked()) scroll.setVisibility(View.VISIBLE);
        else scroll.setVisibility(View.GONE);
    }

    public void imageSetImages(View view){
        Spinner sys = (Spinner)findViewById(R.id.sysImages);
        Spinner ks = (Spinner)findViewById(R.id.kickImages);
        Spinner fs = (Spinner) findViewById(R.id.file_spinner);

        greyButtonImage();

        RadioButton guessButt = (RadioButton)findViewById(R.id.guess_button);
        RadioButton fileButt = (RadioButton)findViewById(R.id.file_button);
        RadioButton downButt = (RadioButton)findViewById(R.id.download);

        if (iFrag.success){
            FragmentManager frag = getSupportFragmentManager();
            frag.popBackStack();
        }

        if(guessButt.isChecked()){
            if(ks.isShown()){
                iFrag.kickImage = ks.getSelectedItem().toString().trim();
                imageStateMachine(2);
            } else if(sys.isShown()){
                iFrag.sysImage = sys.getSelectedItem().toString().trim();
                imageStateMachine(4);
            }
        } else if(fileButt.isChecked()){
            if(iFrag.kickstart){
                iFrag.kickImage = fs.getSelectedItem().toString().trim();
                imageStateMachine(2);
            } else {
                iFrag.sysImage = sys.getSelectedItem().toString().trim();
                imageStateMachine(2);
            }
        } else if(downButt.isChecked()){
            onDownClick();
        }

    }

    public void setDataForImage(){
        fragIndex = 2;
        imageStateMachine(0);
    }

    public void imageStateMachine(int...arg) {
        RadioButton guess = (RadioButton)findViewById(R.id.guess_button);
        RadioButton allFiles = (RadioButton)findViewById(R.id.file_button);
        RadioButton download = (RadioButton)findViewById(R.id.download);
        Spinner sys = (Spinner)findViewById(R.id.sysImages);
        Spinner ks = (Spinner)findViewById(R.id.kickImages);
        Spinner fs = (Spinner) findViewById(R.id.file_spinner);
        state = arg[0];
        int position = 0;
        if(arg.length > 1){ position= arg[1];}
        while(true){
            switch (state){
                case 0: //home
                    state = 3;
                    iFrag.state = state;
                    String imageType = iFrag.kickstart ? "kickstart": "system";
                    findViewById(R.id.image_options).setVisibility(View.VISIBLE);
                    String additional = "";
                    additional = iFrag.kickstart ? "": "\nYou need to match this version: " + iFrag.kickstartImageName;
                    iFrag.setAdditional(additional);
                    message = "In order to boot, the device first needs a " + imageType +
                            " image. See options for choosing an image below.";
                    break;
                case 1://only one set of concurrent images detected
                    Log.i("state", Integer.toString(state));
                    state = iFrag.kickstart ? 2 : 4;
                    iFrag.state = state;
                    break;
                case 2: //try to boot kickstart image
                    findViewById(R.id.image_options).setVisibility(View.GONE);
                    findViewById(R.id.kickImages).setVisibility(View.GONE);
                    findViewById(R.id.sysImages).setVisibility(View.GONE);
                    Log.i("state", Integer.toString(state));
                    iFrag.log = "";
                    iFrag.kickstart = true;
                    iFrag.readOutput = true;
                    writeData("boot " + iFrag.kickImage);
                    //return to wait for boot.
                    iFrag.setAdditional("");
                    iFrag.setText("Booting\n" + iFrag.kickImage + ". This may take a bit...");

                    iFrag.state = state;
                    return;
                case 3: //Display image options with associated message
                    guess.setChecked(false);
                    allFiles.setChecked(false);
                    download.setChecked(false);
                    findViewById(R.id.image_options).setVisibility(View.VISIBLE);

                    Log.i("state", Integer.toString(state));
                    iFrag.setText(message);
                    // Exit to wait for new input
                    iFrag.state = state;
                    return;
                case 4://Try to load system image
                    findViewById(R.id.image_options).setVisibility(View.GONE);
                    findViewById(R.id.kickImages).setVisibility(View.GONE);
                    findViewById(R.id.sysImages).setVisibility(View.GONE);
                    Log.i("state", Integer.toString(state));
                    iFrag.log = "";
                    iFrag.kickstart = false;
                    iFrag.readOutput = true;
                    writeData("load " + iFrag.sysImage);
                    iFrag.setAdditional("");
                    iFrag.setText("Booting\n " + iFrag.sysImage + ". This may take a bit...");
                    iFrag.state = state;
                    return;
                case 6: //System booted!
                    Log.i("state", Integer.toString(state));
                    enableSubmit();
                    iFrag.success = true;
                    String kickKick = iFrag.kickImage.equals("") ? "" : "\n\nKickstart:\n" + iFrag.kickImage;
                    String sysSys = iFrag.sysImage.equals("") ? "" : "\n\nSystem:\n" + iFrag.sysImage;
                    iFrag.success("Yes! The switch booted! The following have booted successfully:"
                            + kickKick + sysSys);
                    iFrag.state = state;
                    return;
                case 7: //failed to boot ks
                    Log.i("state", Integer.toString(state));
                    message = "It looks like there was something wrong with the following " +
                    "image: " + iFrag.kickImage + ". Please select a " +
                            "kickstart image to boot from below or choose to download totally" +
                            " new images.";
                    state = 3;
                    iFrag.state = state;
                    break;
                case 8: //failed to load system
                    Log.i("state", Integer.toString(state));
                    state = 3;
                    message = "Looks like the following system image didn't load properly: " +
                            iFrag.sysImage + ". Please select a system image to boot from " +
                            "below or choose to download totally new images.";
                    iFrag.state = state;
                    break;
                case 9://ks booted!
                    Log.i("state", Integer.toString(state));
                    state = 3;
                    message = "Ok, " + iFrag.kickImage + " booted! Now let's try to load a system" +
                            " image. Please select from the options below.";
                    iFrag.state = state;
                    break;
                case 10: //
                default:break;
            }
        }
    }

    public void onDownClick(){
        imgRestore = new ImageRestoreFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, imgRestore);
        tran.addToBackStack(null);
        tran.commit();
        connection.pau();
        fragIndex = 6;
        connection.res();
    }

    public void saveConfig(String rawconfig){
        BufferedWriter writer = null;
        Pattern booted1 = Pattern.compile("^[^()#]+#[^#]*$");
        String[] configLines = rawconfig.split("\n");
        try {
            writer = new BufferedWriter(new FileWriter(currConfig));
            for (String line : configLines) {
                if(!booted1.matcher(line).matches()){
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch(IOException e){e.printStackTrace();}finally{
            try{
                if(writer!=null)writer.close();
            }catch(IOException e){e.printStackTrace();}
        }
        Toast.makeText(BluetoothActivity.this, "Made File " + currConfig.getName(), Toast.LENGTH_SHORT).show();
        FragmentManager fm = getSupportFragmentManager();
        mLock.lock();
        fragIndex = 0;
        fm.popBackStack();
        mLock.unlock();
    }

    public void changeConfigBack(View view){
        RadioButton backupEnabled = (RadioButton)findViewById(R.id.configBackupRadio);
        RadioButton restoreEnabled = (RadioButton)findViewById(R.id.configRestoreRadio);
        LinearLayout restoreMenu = (LinearLayout)findViewById(R.id.configRestoreMenu);
        LinearLayout backupMenu = (LinearLayout)findViewById(R.id.configBackupMenu);
        TextView title = (TextView)findViewById(R.id.configtitle);
        restoreMenu.setVisibility(SurfaceView.GONE);
        backupMenu.setVisibility(SurfaceView.VISIBLE);
        title.setText("Configuration\nFile Backup");
        restoreEnabled.setChecked(false);
    }

    public void changeConfigRest(View view){
        RadioButton backupEnabled = (RadioButton)findViewById(R.id.configBackupRadio);
        RadioButton restoreEnabled = (RadioButton)findViewById(R.id.configRestoreRadio);
        LinearLayout restoreMenu = (LinearLayout)findViewById(R.id.configRestoreMenu);
        LinearLayout backupMenu = (LinearLayout)findViewById(R.id.configBackupMenu);
        TextView title = (TextView)findViewById(R.id.configtitle);
        backupMenu.setVisibility(SurfaceView.GONE);
        restoreMenu.setVisibility(SurfaceView.VISIBLE);
        title.setText("Configuration\nFile Restore");
        backupEnabled.setChecked(false);
    }

    public void onPasswordFragment(String message){
        if(!message.toLowerCase().contains("automatic") && !message.toLowerCase().contains("auto-booting"))pFrag.updateProgress(7);
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
    public void switchPasswordContext(String context){
        EditText secret = (EditText)findViewById(R.id.secretpw);
        EditText enable = (EditText)findViewById(R.id.enablepw);
        EditText console = (EditText)findViewById(R.id.consolepw);
        TextView secretV = (TextView)findViewById(R.id.secretpwh);
        TextView enableV = (TextView)findViewById(R.id.enablepwh);
        TextView consoleV = (TextView)findViewById(R.id.consolepwh);
        EditText admin = (EditText)findViewById(R.id.adminpw);
        TextView adminV = (TextView)findViewById(R.id.adminpwh);
        if(context.trim().contains("IOS")){
            admin.setEnabled(false);
            admin.setVisibility(SurfaceView.GONE);
            adminV.setVisibility(SurfaceView.GONE);

            secret.setEnabled(true);
            secret.setVisibility(SurfaceView.VISIBLE);
            enable.setEnabled(true);
            enable.setVisibility(SurfaceView.VISIBLE);
            console.setEnabled(true);
            console.setVisibility(SurfaceView.VISIBLE);
            secretV.setVisibility(SurfaceView.VISIBLE);
            enableV.setVisibility(SurfaceView.VISIBLE);
            consoleV.setVisibility(SurfaceView.VISIBLE);
        }
        else{
            admin.setEnabled(true);
            admin.setVisibility(SurfaceView.VISIBLE);
            adminV.setVisibility(SurfaceView.VISIBLE);

            secret.setEnabled(false);
            secret.setVisibility(SurfaceView.GONE);
            enable.setEnabled(false);
            enable.setVisibility(SurfaceView.GONE);
            console.setEnabled(false);
            console.setVisibility(SurfaceView.GONE);
            secretV.setVisibility(SurfaceView.GONE);
            enableV.setVisibility(SurfaceView.GONE);
            consoleV.setVisibility(SurfaceView.GONE);
        }
    }

    public void passwordStart(View view){

        TextView outputV = (TextView)findViewById(R.id.outputpwh);
        RelativeLayout writeV = (RelativeLayout)findViewById(R.id.password_text);
        CheckBox check = (CheckBox)findViewById(R.id.pwcheck);
        EditText fname = (EditText)findViewById(R.id.outputpw);
        Button start = (Button)findViewById(R.id.recoverpw);
        Spinner spin = (Spinner)findViewById(R.id.selectpw);
        String OS = spin.getSelectedItem().toString();
        String data = "";
        if (check.isChecked()) {
            String file = fname.getText().toString().trim();
            if (file.length() > 0) {
                createFile(file);
            } else {
                fname.setBackgroundColor(Color.RED);
                ScrollView sView = (ScrollView) findViewById(R.id.pwscroll);
                sView.fullScroll(View.FOCUS_DOWN);
            }
        }
        if(OS.trim() == "IOS") {
            EditText secret = (EditText)findViewById(R.id.secretpw);
            EditText enable = (EditText)findViewById(R.id.enablepw);
            EditText console = (EditText)findViewById(R.id.consolepw);
            TextView secretV = (TextView)findViewById(R.id.secretpwh);
            TextView enableV = (TextView)findViewById(R.id.enablepwh);
            TextView consoleV = (TextView)findViewById(R.id.consolepwh);
            data += secret.getText().toString() + "," + enable.getText().toString() + "," + console.getText().toString() + "," + "IOS";
            secret.setEnabled(false);
            secret.setVisibility(SurfaceView.GONE);
            enable.setEnabled(false);
            enable.setVisibility(SurfaceView.GONE);
            console.setEnabled(false);
            console.setVisibility(SurfaceView.GONE);
            secretV.setVisibility(SurfaceView.GONE);
            enableV.setVisibility(SurfaceView.GONE);
            consoleV.setVisibility(SurfaceView.GONE);
        } else{
            EditText admin = (EditText)findViewById(R.id.adminpw);
            TextView adminV = (TextView)findViewById(R.id.adminpwh);
            connection.pau();
            String line = connection.sendCont("");
            connection.res();
            Log.i("LINE", line);
            data += admin.getText().toString() + "," + "null,null,NXOS,";
            if (line.toLowerCase().contains("login:") || line.toLowerCase().contains("password:")) {
                data += "T";
            } else {
                data += "F";
            }
            admin.setEnabled(false);
            admin.setVisibility(SurfaceView.GONE);
            adminV.setVisibility(SurfaceView.GONE);
        }

        start.setEnabled(false);
        start.setVisibility(SurfaceView.GONE);
        spin.setEnabled(false);
        spin.setVisibility(SurfaceView.GONE);
        check.setEnabled(false);
        check.setVisibility(SurfaceView.GONE);
        fname.setEnabled(false);
        fname.setVisibility(SurfaceView.GONE);
        outputV.setVisibility(SurfaceView.GONE);
        writeV.setVisibility(SurfaceView.VISIBLE);

        pFrag.startRecovery(data);
    }

    public void togglePasswordOutput(View view){
        pFrag.toggleOuput();
    }

    public void passwordCheck(View view){
        CheckBox check = (CheckBox)findViewById(R.id.pwcheck);
        EditText fname = (EditText)findViewById(R.id.outputpw);
        if(check.isChecked()) {
            fname.setEnabled(true);
            fname.setVisibility(SurfaceView.VISIBLE);
            ScrollView sView = (ScrollView)findViewById(R.id.pwscroll);
            sView.fullScroll(View.FOCUS_DOWN);
        }
        else{
            fname.setEnabled(false);
            fname.setVisibility(SurfaceView.GONE);
        }
    }

    public void passwordSetImages(View view){
        Spinner sys = (Spinner)findViewById(R.id.selectSys);
        Spinner ks = (Spinner)findViewById(R.id.selectKickstart);
        int mode = sys.getVisibility();
        if(mode == SurfaceView.VISIBLE){
            String sysImage = sys.getSelectedItem().toString().trim();
            String kickImage = ks.getSelectedItem().toString().trim();
            pFrag.setImages(kickImage, sysImage);
            writeData("boot " + kickImage);
            pFrag.setMessage("booting kickstart image " + kickImage);
        }
        else{
            String[] images = ks.getSelectedItem().toString().split(":");
            pFrag.setImages(images[0].trim(), images[1].trim());
            writeData("boot " + images[0].trim());
            pFrag.setMessage("booting kickstart image " + images[0].trim());
        }
        RelativeLayout old = (RelativeLayout)findViewById(R.id.password_text);
        old.setVisibility(SurfaceView.VISIBLE);
        RelativeLayout imageSelection = (RelativeLayout)findViewById(R.id.passwordImages);
        imageSelection.setVisibility(SurfaceView.GONE);
    }
    public void passwordSwitchBootOpts(View view){
        pFrag.abandonHope();
    }
    public void passwordLoadSys(View view){
        Spinner sys = (Spinner)findViewById(R.id.selectSys);
        String sysImage = sys.getSelectedItem().toString().trim();
        writeData("load " + sysImage);
        RelativeLayout imageSelection = (RelativeLayout)findViewById(R.id.passwordImages);
        imageSelection.setVisibility(SurfaceView.GONE);
    }

    public void onSoftwareFragment(){
        writeData("");
    }

    public void softwareMode(int mode){
        boolean ready = false;

        if(mode == 1){
            //loader
            soFrag.setText("kickstart loader");
            kick = true;
            ready = true;
        } else if(mode == 2){
            //(boot)#
            soFrag.setText("system loader");
            kick = false;
            ready = true;
        } else if(mode == 3){
            //<switch name>#
            soFrag.setText("Booted!!");
        }
        else if(mode == 4){
            //Press RETURN to get started
            writeData("");
        }

        if(ready){
            iFrag = new ImageFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, iFrag);
            tran.addToBackStack(null);
            tran.commit();
            fragIndex = 2;
        }
    }

    public void onImgRestoreCollection(View view){
        String[] passed = new String[7];
        passed[0] = ((EditText)findViewById(R.id.ipaddr)).getText().toString();
        passed[1] = ((EditText)findViewById(R.id.gwaddr)).getText().toString();
        passed[2] = ((EditText)findViewById(R.id.ftpaddr)).getText().toString();
        passed[3] = ((EditText)findViewById(R.id.ksimg)).getText().toString();
        passed[4] = ((EditText) findViewById(R.id.sysimg)).getText().toString();
        passed[5] = ((EditText) findViewById(R.id.unamerec)).getText().toString();
        passed[6] = ((EditText) findViewById(R.id.pwrec)).getText().toString();

        imgRestore.collectInfo(passed);
    }



    public void writeData(String data){
        connection.write(data);
    }

    public void updateFragIndex(int index){
        mLock.lock();
        fragIndex = index;
        mLock.unlock();
    }

    public void disconnect(){
        if(connection != null){
            connection.close();
            activeDevice = "";
        }
    }
}