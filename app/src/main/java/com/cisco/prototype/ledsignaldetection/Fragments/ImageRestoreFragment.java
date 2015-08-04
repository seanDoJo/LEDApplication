package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.HashMap;

public class ImageRestoreFragment extends Fragment {
    private BluetoothInterface mListener;
    private String record = "";
    private TextView log = null;
    private int state = 1;
    private boolean recoveryStarted = false;
    private String gw = "172.25.186.1";
    private String ip = "172.25.186.254";
    private String ftp = "171.71.15.151";
    private String ksimg =  "";
    private String sysimg = "";
    private String username = "";
    private String password = "";

    public ImageRestoreFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_restore, container, false);
        log = (TextView)view.findViewById(R.id.recoveryLog);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BluetoothInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void collectInfo(String[] information){
        String[] bits = information;
        LinearLayout startView = (LinearLayout)getActivity().findViewById(R.id.recoveryStart);
        LinearLayout logView = (LinearLayout)getActivity().findViewById(R.id.recoveryLogShell);
        startView.setVisibility(SurfaceView.GONE);
        logView.setVisibility(SurfaceView.VISIBLE);
        if(!bits[0].trim().equals(""))ip = bits[0];
        if(!bits[1].trim().equals(""))gw = bits[1];
        if(!bits[2].trim().equals(""))ftp = bits[2];
        ksimg = bits[3];
        sysimg = bits[4];
        username = bits[5];
        password = bits[6];
        recoveryStarted = true;
        mListener.writeData("");
    }

    public void step(String received){
        record += received;
        log.setText(log.getText() + received);
        if(recoveryStarted) {
            switch (state) {
                case 1:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("set ip " + ip.trim() + " 255.255.255.0");
                        state++;
                        record = "";
                    } else if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("");
                        state = 8;
                        record = "";
                    }
                    break;
                case 2:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("set gw " + gw.trim());
                        state++;
                        record = "";
                    }
                    break;
                case 3:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("boot tftp://" + ftp.trim() + "/" + ksimg.trim());
                        state++;
                        record = "";
                    }
                    break;
                case 4:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("conf t");
                        state++;
                        record = "";
                    }
                    break;
                case 5:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("int m0");
                        state++;
                        record = "";
                    }
                    break;
                case 6:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("no shut");
                        state++;
                        record = "";
                    }
                    break;
                case 7:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("exit");
                        state++;
                        record = "";
                    }
                    break;
                case 8:
                    if (!ksimg.trim().equals("")) {
                        if (record.toLowerCase().contains("boot)#")) {
                            mListener.writeData("copy ftp: bootflash:");
                            record = "";
                        } else if (record.toLowerCase().contains("filename:")) {
                            mListener.writeData(ksimg.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("server:")) {
                            mListener.writeData(ftp.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("username:")) {
                            mListener.writeData(username.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("password")) {
                            mListener.writeData(password.trim());
                            record = "";
                            state++;
                        }
                    } else {
                        state++;
                        record = "";
                        mListener.writeData("");
                    }
                    break;
                case 9:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("copy ftp: bootflash:");
                        record = "";
                    } else if (record.toLowerCase().contains("filename:")) {
                        mListener.writeData(sysimg.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("server:")) {
                        mListener.writeData(ftp.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("username:")) {
                        mListener.writeData(username.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("password")) {
                        mListener.writeData(password.trim());
                        record = "";
                        state++;
                    }
                    break;
                case 10:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("load bootflash:" + sysimg.trim());
                        state++;
                        record = "";
                    }
                    break;
                case 11:
                    if (record.toLowerCase().contains("boot)#")) {
                        state++;
                        record = "";
                    }
                    break;
            }
        } else {
            Log.e("LEDApp", "pre start reception");
        }
    }

}
