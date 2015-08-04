package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
    private String uname = null;
    private String pass = null;
    private String log = "";
    Pattern booted1 = Pattern.compile("(?s)[^()#]*#[^#]*");
    Pattern booted2 = Pattern.compile("(?s).*[sS]{1}witch>[^>]*");
    private boolean loggedIn;

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

    public boolean loginSuccessful(){
        if (record.toLowerCase().contains("incorrect") || record.toLowerCase().contains("failed")) return false;
        else return true;
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

    public void submit(String  info){
        String[] loginInfo = info.split(",");
        uname = loginInfo[0].trim();
        pass = loginInfo[1].trim();
        mListener.writeData("");
    }

    public void readLogin(String data){
        record += data;
        switch(state){
            case 0:
                if(record.toLowerCase().contains("password:")){
                    mListener.writeData("");
                    record = "";
                }else if((record.toLowerCase().contains("login:") || record.toLowerCase().contains("user")) && uname != null){
                    mListener.writeData(uname);
                    state++;
                    record = "";
                    Log.e("LOGIN", "wrote login:");
                }else if(booted1.matcher(record).matches()){
                    mListener.setLoggedIn(true);
                }
                break;
            case 1:
                if(record.toLowerCase().contains("password:")){
                    mListener.writeData(pass);
                    state++;
                    record = "";
                    Log.e("LOGIN", "wrote password:");
                }
                break;
            case 2:
                if(record.toLowerCase().contains("failed") || record.toLowerCase().contains("incorrect")){
                    uname = null;
                    pass = null;
                    state = 0;
                    record = "";
                    Log.e("LOGIN", "login failed anus");
                    mListener.setLoggedIn(false);
                } else {
                    state++;
                    record = "";
                    mListener.setLoggedIn(true);
                }
        }
    }

}
