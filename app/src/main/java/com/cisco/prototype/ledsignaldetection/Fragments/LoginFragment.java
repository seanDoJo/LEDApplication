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

import java.util.regex.Pattern;

public class LoginFragment extends Fragment {

    private BluetoothInterface mListener;
    private String record = "";
    private int state = 0;
    Pattern booted1 = Pattern.compile("(?s)[^()#]*#[^#]*");
    Pattern booted2 = Pattern.compile("(?s).*[sS]{1}witch>[^>]*");

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BluetoothInterface) activity;
            mListener.writeData("");
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

    public void readLogin(String data){
        record += data;
        String uname = "";
        String pass = "";
        switch(state){
            case 0:
                if(record.toLowerCase().contains("password:")){
                    mListener.writeData("");
                    record = "";
                }else if(record.toLowerCase().contains("login:") || record.toLowerCase().contains("user")){
                    mListener.writeData(uname);
                    state++;
                    record = "";
                }else if(booted1.matcher(record).matches()){
                    //enable logged in functions
                }
                break;
            case 1:
                if(record.toLowerCase().contains("password:")){
                    mListener.writeData(pass);
                    state++;
                    record = "";
                }
                break;
            case 2:
                if(record.toLowerCase().contains("failed") || record.toLowerCase().contains("incorrect")){
                    //wrong info
                } else {
                    //enable logged in functions
                }
        }
    }

}
