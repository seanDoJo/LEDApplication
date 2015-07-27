package com.cisco.prototype.ledsignaldetection.Activities;

import android.graphics.Color;
import android.net.Uri;
import android.graphics.drawable.Drawable;
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
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cisco.prototype.ledsignaldetection.Fragments.BTMenuFragment;
import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.Fragments.CommunicationFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.EmailFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.FileExplorerFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ImageFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ImageRestoreFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.PasswordFragment;
import com.cisco.prototype.ledsignaldetection.R;
import com.cisco.prototype.ledsignaldetection.Fragments.SelectionFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.SoftwareFragment;
import com.cisco.prototype.ledsignaldetection.Fragments.ViewFileFragment;
import com.cisco.prototype.ledsignaldetection.imagePair;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


public class BluetoothActivity extends FragmentActivity implements BluetoothInterface {
    private BluetoothAdapter mBluetooth;
    private ArrayList<BluetoothDevice> devices;
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
    private EmailFragment eFrag;
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
    private int state;
    private String message = "";
    private String currentKickImage = "";
    private String currentSysImage = "";
    private boolean kick;
    private File outputFile;
    private BufferedWriter writer = null;
    private boolean captureEnabled = false;
    private File viewedFile = null;
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
                iFrag.log += result;
            }
            else if(m.what == 3){
                //Password

                byte[] data = (byte[]) m.obj;
                String result = new String(data);
                if(captureEnabled){
                    try {
                        writer.write(result);
                    }catch(IOException e){e.printStackTrace();}
                }
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
            else if(m.what == 56){
                int status = (int)m.obj;
                Log.i("LEDApp", "called handler with 56");
                LinearLayout terminal = null;
                Button currbutton = null;
                LinearLayout lin = null;
                TextView statlabel = (TextView)findViewById(R.id.status_label);
                View circle = (View)findViewById(R.id.status_indc);
                Drawable resid = null;
                clearBtMenu();
                switch(status){
                    case -1:
                        statlabel.setText("Device Status: unreachable");
                        resid = getResources().getDrawable(R.drawable.circle_red);
                        terminal = (LinearLayout) findViewById(R.id.terminal);
                        terminal.setClickable(false);
                        circle.setBackground(resid);
                        break;
                    case 0:
                        statlabel.setText("Device Status: booting...");
                        resid = getResources().getDrawable(R.drawable.circle_yellow);
                        circle.setBackground(resid);
                        lin = (LinearLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);
                        terminal = (LinearLayout) findViewById(R.id.terminal);
                        terminal.setClickable(true);
                        terminal.setVisibility(SurfaceView.VISIBLE);
                        break;
                    case 1:
                        statlabel.setText("Device Status: bootloader");
                        resid = getResources().getDrawable(R.drawable.circle_yellow);
                        circle.setBackground(resid);
                        lin = (LinearLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);
                        currbutton = (Button) findViewById(R.id.password);
                        currbutton.setEnabled(true);
                        currbutton.setVisibility(SurfaceView.VISIBLE);
                        terminal = (LinearLayout) findViewById(R.id.terminal);
                        terminal.setClickable(true);
                        terminal.setVisibility(SurfaceView.VISIBLE);
                        break;
                    case 2:
                        statlabel.setText("Device Status: booted");
                        resid = getResources().getDrawable(R.drawable.circle_green);
                        circle.setBackground(resid);
                        lin = (LinearLayout)findViewById(R.id.bt_lin);
                        lin.setVisibility(SurfaceView.VISIBLE);
                        currbutton = (Button) findViewById(R.id.password);
                        currbutton.setEnabled(true);
                        currbutton.setVisibility(SurfaceView.VISIBLE);
                        terminal = (LinearLayout) findViewById(R.id.terminal);
                        terminal.setClickable(true);
                        terminal.setVisibility(SurfaceView.VISIBLE);
                        break;
                }
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
                    if(specComm.replace("-","").contains("ctrlz")){
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
                        if (returned.contains("#") || returned.toLowerCase().contains("switch>") || returned.toLowerCase().contains("password:")) {
                            connectionHandler.obtainMessage(56, 1, -1, 2).sendToTarget();
                            Log.i("LEDApp", "message sent to handler");
                        } else if (returned.toLowerCase().contains("(boot)#")) {
                            connectionHandler.obtainMessage(56, 1, -1, 1).sendToTarget();
                            Log.i("LEDApp", "message sent to handler");
                        } else if (returned.toLowerCase().contains("switch:") || returned.toLowerCase().contains("loader>")) {
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
            synchron.countDown();
            if(pingLatch != null)pingLatch.countDown();
            Log.i("LEDApp", "ping executed");
            paused = false;
        }

    }

    private BTConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        currMode = intent.getIntExtra(HomeActivity.EXTRA_MESSAGE, 0);
        if(currMode == 0) {
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
        else{
            fileFrag = new FileExplorerFragment();
            fileFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fileFrag).commit();
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        Log.i("LEDApp", "resumed");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //we need this otherwise poo flinging will ensue
        if(currMode == 0) {
            if (connection != null) connection.close();
            if (mBluetooth != null) mBluetooth.cancelDiscovery();
            unregisterReceiver(btReceiver);
        }
    }

    public void onSelectionFragment(int index){
        mBluetooth.cancelDiscovery();
        if(!devices.get(index).getName().equals(activeDevice)) {
            tready = new CountDownLatch(1);
            latch = new CountDownLatch(1);
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

    public void configureMenu(){
        //CountDownLatch configL = new CountDownLatch(1);
        connection.ping(null);
        /*try {
            configL.await();
        }catch(InterruptedException e){e.printStackTrace();}*/
        Log.i("LEDApp", "returned from ping");

    }

    private void clearBtMenu(){
        Button currbutton = null;
        LinearLayout terminal = null;

        currbutton = (Button)findViewById(R.id.password);
        currbutton.setEnabled(false);
        currbutton.setVisibility(SurfaceView.GONE);

        terminal = (LinearLayout)findViewById(R.id.terminal);
        currbutton.setEnabled(false);
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
        File appFolder = new File(getFilesDir().getAbsolutePath());
        File[] folderContents = appFolder.listFiles();
        fileFrag.init(folderContents);
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
        eFrag = new EmailFragment();
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(R.id.fragment_container, eFrag);
        tran.addToBackStack(null);
        tran.commit();
        if(connection != null) {
            connection.pau();
            fragIndex = 8;
            connection.res();
        }
    }

    public void sendEmail(View view){
        TextView email = (TextView) view.findViewById(R.id.email_address);
        TextView subject = (TextView) view.findViewById(R.id.subject);
        TextView body = (TextView) view.findViewById(R.id.body);

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");

        Uri uri = Uri.fromFile(viewedFile);
        i.putExtra(Intent.EXTRA_STREAM, uri);

        /*i.putExtra(Intent.EXTRA_EMAIL  , email.getText());
        i.putExtra(Intent.EXTRA_SUBJECT, subject.getText());*/
        i.putExtra(Intent.EXTRA_TEXT   , viewedFile.getAbsolutePath());
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(BluetoothActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteFile(View view){
        destroyFile();
        viewedFile.delete();
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
        connection.write(editText.getText().toString());
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
        outputFile = new File(getFilesDir(), filename + ".capture");
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

    public void setImageMode(){
        iFrag.kickstart = kick;
    }

    public void onImageFragment(){
        String identifier = "kickstart";
        kickstartImageList = new ArrayList<String>();
        ArrayList<String> fileNames;

        //check for concurrent images
        connection.write("dir");
        while(!(iFrag.log.contains(">") || iFrag.log.contains("#"))){}
        files = iFrag.log.split("\n");
        Arrays.copyOf(files, files.length - 1); // get rid of last line, which is loader> prompt
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

        state = 0;
        imageStateMachine(0);
    }

    public void imageStateMachine(int...arg) {
        state = arg[0];
        int position = arg[1];
        while(true){
            switch (state){
                case 0: //home
                    if(imageList.size() == 1) state = 1;
                    else{
                        state = 3;
                        message = "I'm not sure what to try to boot. Please help me out by either" +
                                "selecting an image from the list below or choosing to download" +
                                "a new set of images.";
                    }
                    break;
                case 1://only one set of concurrent images detected
                    state = iFrag.kickstart ? 2 : 4;
                    currentKickImage = imageList.get(0).kickstartImage;
                    currentSysImage = imageList.get(0).systemImage;
                    break;
                case 2: //try to boot kickstart image
                    iFrag.log = "";
                    iFrag.kickstart = true;
                    connection.write("boot " + currentKickImage);
                    while(!(iFrag.log.contains(">") || iFrag.log.contains("#"))){}
                    if(iFrag.log.contains(">")){
                        state = 3;
                        message = "It looks like there was something wrong with the following" +
                                "image: " + currentKickImage + " Please select a" +
                                "kickstart image to boot from below or choose to download totally" +
                                " new images.";
                    } else state = 4;
                    break;
                case 3: //Display image options with associated message
                    iFrag.setText(message);
                    iFrag.populate(new ArrayList<String>(Arrays.asList(files)));
                    // Exit to wait for new input
                    return;
                case 4://Try to load system image
                    iFrag.log = "";
                    iFrag.kickstart = false;
                    connection.write("load " + currentSysImage);
                    while(!iFrag.log.contains("#")){}
                    if(iFrag.log.contains("(boot)")){
                        state = 3;
                        message = "Looks like the following system image didn't load properly: " +
                                currentSysImage + " Please select a system image to boot from " +
                                "below or choose to download totally new images.";
                    } else state = 6;
                    break;
                case 5: //set new kickstart or system variable after getting user input
                    if(iFrag.kickstart)currentKickImage = imageList.get(position).kickstartImage;
                    else currentSysImage = imageList.get(position).systemImage;
                    iFrag.populate(new ArrayList<String>()); //clear out file list
                case 6: //System booted!
                    iFrag.success = true;
                    iFrag.success("Yes! The switch booted! The following image names will be" +
                            "displayed if they were changed during the troubleshooting process. " +
                            "Kickstart: " + currentKickImage + " System: " + currentSysImage +
                            " Please update the configuration accordingly.");
                    return;
                default:break;
            }
        }
    }

    public void onImageClick(View view){
        if(iFrag.success){
            //go back to BT menu
            FragmentManager frag = getSupportFragmentManager();
            frag.popBackStack("menu", 0);
        } else {
            imgRestore = new ImageRestoreFragment();
            FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
            tran.replace(R.id.fragment_container, imgRestore);
            tran.addToBackStack(null);
            tran.commit();
            connection.pau();
            fragIndex = 6;
            connection.res();
        }
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
        TextView outputV = (TextView)findViewById(R.id.outputpwh);
        RelativeLayout writeV = (RelativeLayout)findViewById(R.id.password_text);
        CheckBox check = (CheckBox)findViewById(R.id.pwcheck);
        EditText fname = (EditText)findViewById(R.id.outputpw);
        Button start = (Button)findViewById(R.id.recoverpw);
        String data = secret.getText().toString() + "," + enable.getText().toString() + "," + console.getText().toString();
        if(check.isChecked()){
            String file = fname.getText().toString().trim();
            if(file.length() > 0){
                createFile(file);
            }
            else{
                fname.setBackgroundColor(Color.RED);
                ScrollView sView = (ScrollView)findViewById(R.id.pwscroll);
                sView.fullScroll(View.FOCUS_DOWN);
            }
        }
        secret.setEnabled(false);
        secret.setVisibility(SurfaceView.GONE);
        enable.setEnabled(false);
        enable.setVisibility(SurfaceView.GONE);
        console.setEnabled(false);
        console.setVisibility(SurfaceView.GONE);
        start.setEnabled(false);
        start.setVisibility(SurfaceView.GONE);
        check.setEnabled(false);
        check.setVisibility(SurfaceView.GONE);
        fname.setEnabled(false);
        fname.setVisibility(SurfaceView.GONE);
        secretV.setVisibility(SurfaceView.GONE);
        enableV.setVisibility(SurfaceView.GONE);
        consoleV.setVisibility(SurfaceView.GONE);
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

    public void onSoftwareFragment(){
        connection.write("");
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
            connection.write("");
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

    public void updateFragIndex(int index){
        connection.pau();
        fragIndex = index;
        connection.res();
    }

    public void disconnect(){
        if(connection != null){
            connection.close();
            activeDevice = "";
        }
    }
}