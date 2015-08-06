package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigBackupFragment extends Fragment {

    private BluetoothInterface mListener;
    private String record = "";
    private int state = 1;
    Pattern booted1 = Pattern.compile("(?s).*[^()#]+#[^#]*");
    Pattern runningConfigShape = Pattern.compile("(?s).*(version.*)");

    public ConfigBackupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_config_backup, container, false);
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
    public void onResume(){
        super.onResume();
        mListener.writeData("");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void read(String data){
        record += data;
        if(record.contains("--More")){
            record = record.replaceAll("\\[\\d+m?--More-- \\[\\d+m?", "");
            mListener.writeData("");
        }
        else {
            switch (state) {
                case 1:
                    if (booted1.matcher(record).matches()) {
                        mListener.writeData("show running-config");
                        state++;
                        record = "";
                    }
                    break;
                case 2:
                    if (runningConfigShape.matcher(record).matches()) {
                        Matcher configMatcher = runningConfigShape.matcher(record);
                        String config = "";
                        if (configMatcher.find()) {
                            config = configMatcher.group(1);
                        }
                        config = config.replaceAll("\\[.{1}", "");
                    }
            }
        }
    }

}
