package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.HashMap;

public class ImageRestoreFragment extends Fragment {
    private BluetoothInterface mListener;
    private HashMap<String, String> details;
    private String record = "";
    private int state = 0;

    public ImageRestoreFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        details = new HashMap<String, String>();
        details.put("ip", "172.25.186.254");
        details.put("gw", "172.25.186.1");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_restore, container, false);
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

    public void collectInfo(String information){
        String[] bits = information.split(",");
        if(bits[0].trim() != "")details.put("ip", bits[0]);
        if(bits[1].trim() != "")details.put("gw", bits[1]);
        details.put("ftpAddr", bits[2]);
        details.put("ksimg", bits[3]);
        details.put("sysimg", bits[4]);
    }

    public void step(String received){
        record += received;
        String toWrite = "";
        if(record.toLowerCase().contains("filename:")){
            toWrite = state == 9 ? details.get("ksimg") : details.get("sysimg");
            record = "";
        }
        else if(record.toLowerCase().contains("server:")){
            toWrite = details.get("ftpAddr");
            record = "";
        }
        else if(record.toLowerCase().contains("username:")){
            toWrite = details.get("username");
            record = "";
        }
        else if(record.toLowerCase().contains("password:")){
            toWrite = details.get("password");
            record = "";
        }
        else if(record.toLowerCase().contains("loader>") || record.toLowerCase().contains("boot)#")){
            switch (state) {
                case 1:
                    toWrite = "set ip " + details.get("ip") + " 255.255.255.0";
                    state++;
                    break;
                case 2:
                    toWrite = "set gw " + details.get("gw");
                    state++;
                    break;
                case 3:
                    toWrite = "boot tftp://" + details.get("ftpAddr") + "/" + details.get("ksimg");
                    state++;
                    break;
                case 4:
                    toWrite = "conf t";
                    state++;
                    break;
                case 5:
                    toWrite = "int m0";
                    state++;
                    break;
                case 6:
                    toWrite = "no shut";
                    state++;
                    break;
                case 7:
                    toWrite = "exit";
                    state++;
                    break;
                case 8:
                    toWrite = "copy ftp: bootflash:";
                    state++;
                    break;
                case 9:
                    toWrite = "copy ftp: bootflash:";
                    state++;
                    break;
                case 10:
                    toWrite = "load bootflash:" + details.get("sysimg");
                    state++;
                    break;
                default:
                    break;
            }
            record = "";
        }
        mListener.writeData(toWrite);
    }

}
