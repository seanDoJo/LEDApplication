package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;


public class PasswordFragment extends Fragment {
    private BluetoothInterface mListener;
    private Boolean recoveryStarted = false;
    private HashMap<String, String> responses;
    private TextView textView;
    private ProgressBar pBar;
    private int state = 0;
    private String record = "";
    private String secretPw = "";
    private String consolePw = "";
    private String enablePw = "";

    public PasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //responses = new HashMap<String, String>();
        //constructHashMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_password, container, false);
        //textView = (TextView) view.findViewById(R.id.password_text);
        pBar = (ProgressBar)view.findViewById(R.id.progressBar);
        /*setMessage("Disconnect power cable from switch. Hold down Mode button and reconnect " +
                "power. Release Mode button when SYST LED blinks amber then turns solid green." +
                "Boot will then take place. I'll let you know when I've started the password " +
                "recovery script.");*/
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (BluetoothInterface)activity;
        } catch(ClassCastException e){}
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public boolean setMessage(String message){
        textView.setText(message);
        return true;
    }

    public void updateProgress(){
        if(pBar.getProgress() >= 98)scrollProgress(100);
        else scrollProgress(pBar.getProgress() + 7);
    }
    private void scrollProgress(int addition){
        double currTime = 0;
        int difference = addition - pBar.getProgress();
        for(int i = 0; i < difference; i++){
            pBar.setProgress(pBar.getProgress() + 1);
            currTime = System.currentTimeMillis();
            while((System.currentTimeMillis() - currTime) < 1000){};
        }
    }

    public void startRecovery(String data){
        String[] fields = data.split(",");
        secretPw = fields[0].trim();
        enablePw = fields[1].trim();
        consolePw = fields[2].trim();
        //mListener.onPasswordFragment("Starting Recovery -- Waiting For System to Enter Recovery Mode");
        recoveryStarted = true;
    }

    public void read(String data){
        record += data;
        //mListener.onPasswordFragment(record);
        if(recoveryStarted) {
            if (record.contains("no]")) {
                mListener.writeData("n");
                record = "";
            } else if (record.contains("MORE") || record.contains("RETURN") || record.contains("]")) {
                mListener.writeData("");
                record = "";
            }
            else {
                switch (state) {
                    case 0:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("flash_init");
                            mListener.onPasswordFragment("flash_init executed!");
                            state++;
                            record = "";
                        }
                        break;
                    case 1:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("rename flash:config.text flash:config.old");
                            mListener.onPasswordFragment("renamed config.txt!");
                            state++;
                            record = "";
                        }
                        break;
                    case 2:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("boot");
                            mListener.onPasswordFragment("booting!");
                            state++;
                            record = "";
                        }
                        break;
                    case 3:
                        if (record.toLowerCase().contains(">")) {
                            mListener.writeData("en");
                            mListener.onPasswordFragment("enable");
                            state++;
                            record = "";

                        }
                        break;
                    case 4:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("rename flash:config.old flash:config.text");
                            mListener.onPasswordFragment("rename config.old to config.text");
                            state++;
                            record = "";
                        }
                        else if(record.toLowerCase().contains("connection") || record.contains(">")){
                            mListener.writeData("en");
                            record = "";
                        }
                        else if(record.toLowerCase().contains("password")){
                            //wat
                            Log.e("LEDApp", "password detected in password recovery");
                        }
                        break;
                    case 5:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("copy flash:config.text system:running-config");
                            mListener.onPasswordFragment("copy to running-config");
                            state++;
                            record = "";
                        }
                        break;
                    case 6:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("conf t");
                            mListener.onPasswordFragment("entered conf");
                            state++;
                            record = "";
                        }
                        break;
                    case 7:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable secret " + secretPw);
                            mListener.onPasswordFragment("set secret password");
                            state++;
                            record = "";
                        }
                        break;
                    case 8:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable password " + enablePw);
                            mListener.onPasswordFragment("set enable password");
                            state++;
                            record = "";
                        }
                        break;
                    case 9:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("line con 0");
                            mListener.onPasswordFragment("entered line conf");
                            state++;
                            record = "";
                        }
                        break;
                    case 10:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("password " + consolePw);
                            mListener.onPasswordFragment("set console password");
                            state++;
                            record = "";
                        }
                        break;
                    case 11:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("!!!esc");
                            mListener.onPasswordFragment("exited conf");
                            state++;
                            record = "";
                        }
                        break;
                    case 12:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("write memory");
                            mListener.onPasswordFragment("wrote memory");
                            state++;
                            record = "";
                        }
                        break;
                    case 13:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("write memory");
                            state++;
                            record = "";
                        }
                        break;
                    case 14:
                        if (record.toLowerCase().contains("#")) {
                            mListener.onPasswordFragment("delete old config");
                            mListener.writeData("del flash:config.old");
                            state++;
                            record = "";
                        }
                        break;
                    case 15:
                        if (record.toLowerCase().contains("#")) {
                            mListener.onPasswordFragment("Password Recovery Complete!");
                            state++;
                            record = "";
                        }
                        break;
                }
            }
        }
    }

}
