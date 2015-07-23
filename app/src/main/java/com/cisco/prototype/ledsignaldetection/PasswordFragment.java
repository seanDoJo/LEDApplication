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
    private boolean toAutoBootConf = false;

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
        textView = (TextView) view.findViewById(R.id.password_prompt);
        pBar = (ProgressBar)view.findViewById(R.id.progressBar);
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
        if(pBar.getProgress() >= 98)pBar.setProgress(100);
        else pBar.setProgress(pBar.getProgress() + 7);
    }

    public void startRecovery(String data){
        String[] fields = data.split(",");
        secretPw = fields[0].trim();
        enablePw = fields[1].trim();
        consolePw = fields[2].trim();
        setMessage("Waiting for switch to enter password recovery mode\n\nUnplug the switch, then press and hold the mode button while plugging back in");
        recoveryStarted = true;
    }

    public void setAutoBootConf(){
        toAutoBootConf = true;
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
                            mListener.onPasswordFragment("Recovery has started!");
                            state++;
                            record = "";
                        }
                        break;
                    case 1:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("rename flash:config.text flash:config.old");
                            mListener.onPasswordFragment("renaming config.text to config.old");
                            state++;
                            record = "";
                        }
                        break;
                    case 2:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("boot");
                            mListener.onPasswordFragment("rebooting the switch");
                            state++;
                            record = "";
                        }
                        break;
                    case 3:
                        if (record.toLowerCase().contains(">")) {
                            mListener.writeData("en");
                            mListener.onPasswordFragment("enabling the switch");
                            state++;
                            record = "";

                        }
                        break;
                    case 4:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("rename flash:config.old flash:config.text");
                            mListener.onPasswordFragment("renaming config.old back to config.text");
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
                            mListener.onPasswordFragment("copying config.text to the system running-config");
                            state++;
                            record = "";
                        }
                        break;
                    case 6:
                        if (record.toLowerCase().contains("#")) {
                            mListener.writeData("conf t");
                            mListener.onPasswordFragment("entering configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 7:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable secret " + secretPw);
                            mListener.onPasswordFragment("setting secret password");
                            state++;
                            record = "";
                        }
                        break;
                    case 8:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable password " + enablePw);
                            mListener.onPasswordFragment("setting enable password");
                            state++;
                            record = "";
                        }
                        break;
                    case 9:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("line con 0");
                            mListener.onPasswordFragment("");
                            state++;
                            record = "";
                        }
                        break;
                    case 10:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("password " + consolePw);
                            mListener.onPasswordFragment("setting console password");
                            state++;
                            record = "";
                        }
                        break;
                    case 11:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("!!!ctrlz");
                            mListener.onPasswordFragment("exiting configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 12:
                        if (record.toLowerCase().contains("config-line)") || record.toLowerCase().contains("(config)")){
                            mListener.writeData("end");
                            record = "";
                        }
                        else if (record.toLowerCase().contains("#")) {
                            mListener.writeData("write memory");
                            mListener.onPasswordFragment("saving configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 13:
                        if (record.toLowerCase().contains("#")) {
                            mListener.onPasswordFragment("deleting config.old");
                            mListener.writeData("del flash:config.old");
                            state++;
                            record = "";
                        }
                        break;
                    case 14:
                        if (record.toLowerCase().contains("#")) {
                            mListener.onPasswordFragment("Password Recovery Complete!");
                            state++;
                            record = "";
                        }
                        break;
                    case 15:
                        if (record.toLowerCase().contains("#") && toAutoBootConf) {
                            mListener.onPasswordFragment("Enabling Automatic Booting...");
                            mListener.writeData("conf t");
                            state++;
                            record = "";
                        }
                        break;
                    case 16:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("no boot manual");
                            state++;
                            record = "";
                        }
                        break;
                    case 17:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("!!!ctrlz");
                            state++;
                            record = "";
                        }
                        break;
                    case 18:
                        if (record.toLowerCase().contains("config-line)") || record.toLowerCase().contains("(config)")){
                            mListener.writeData("end");
                            record = "";
                        }
                        else if (record.toLowerCase().contains("#")) {
                            mListener.writeData("write memory");
                            state++;
                            record = "";
                        }
                        break;
                    case 19:
                        if(record.toLowerCase().contains("#")){
                            mListener.onPasswordFragment("Auto-Booting Configured!");
                            record = "";
                        }

                }
            }
        }
    }

}