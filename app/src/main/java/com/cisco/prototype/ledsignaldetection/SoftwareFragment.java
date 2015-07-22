package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SoftwareFragment extends Fragment {
    private BluetoothInterface mListener;
    public String log;
    private TextView textView;
    private View view;

    public SoftwareFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_software, container, false);
        textView = (TextView) view.findViewById(R.id.soft_text);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (BluetoothInterface)activity;
            mListener.onSoftwareFragment();
        } catch(ClassCastException e){}
    }

    public void add(String data) {
        log += data;
        System.out.println(log);
        if (log.contains(">")){
            //"loader>" prompt
            log = "";
            mListener.softwareMode(1);
        } else if (log.contains("(boot)#")){
            //"(boot)# prompt
            log = "";
            mListener.softwareMode(2);
        } else if(log.contains("#")){
            //fully loaded
            log = "";
            mListener.softwareMode(3);
        }else if(log.contains("URN")){
            //Press RETURN to get started
            mListener.softwareMode(4);
        }
        else {}
    }

    public void setText(String string){ textView.setText(string); }

}
