package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConnectionSelectFragment extends Fragment {

    private BluetoothInterface mListener;
    private EditText ip = null;
    private EditText port = null;
    private EditText uname = null;
    private EditText pass = null;

    public ConnectionSelectFragment() {
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
        View view = inflater.inflate(R.layout.fragment_connection_select, container, false);
        ip = (EditText)view.findViewById(R.id.telAddress);
        port = (EditText)view.findViewById(R.id.telPort);
        uname = (EditText)view.findViewById(R.id.telPortComU);
        pass = (EditText)view.findViewById(R.id.telPortComP);
        File preFile = new File(Environment.getExternalStorageDirectory()+File.separator + "SwitchArmyKnife" + File.separator + "misc" + File.separator + "connectionLogins.txt");
        if(!preFile.exists()){
            try{
                preFile.createNewFile();
            }catch(IOException e){e.printStackTrace();}
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(preFile));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        String line = null;
        try{
            line = reader.readLine();
        }catch(IOException e){e.printStackTrace();}
        if(line != null && !line.trim().equals("")) {
            String[] data = line.split(",");
            if(data.length >= 4) {
                ip.setText(data[0].trim());
                port.setText(data[1].trim());
                uname.setText(data[2].trim());
                pass.setText(data[3].trim());
            }
        }
        try{
            if(reader != null)reader.close();
        }catch(IOException e){e.printStackTrace();}
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
    public void onResume(){
        super.onResume();
        mListener.disconnect();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public void collapse(){
        EditText editText = (EditText)getView().findViewById(R.id.telPort);
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

}
