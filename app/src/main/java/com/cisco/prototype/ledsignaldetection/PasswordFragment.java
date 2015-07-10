package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;


public class PasswordFragment extends Fragment {
    private BluetoothInterface mListener;
    private HashMap<String, String> responses;
    private TextView textView;

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
        textView = (TextView) view.findViewById(R.id.password_text);
        mListener.onPasswordFragment();

        return view;
    }

    public void go(){
        mListener.onPasswordFragment();
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

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    /*private void constructHashMap(){
        //This only applies to the 2955 series...ignore for now
        //byte[] esc = {0x1b};

        //responses.put("Send break character to prevent autobooting.", new String(esc));

        //initialize flash
        responses.put("boot\n\nswitch:", "flash_init");


        //////// Note...the tutorial said to just automatically enter load_helper and dir flash:

        //load_helper
        responses.put("Parameter Block Filesystem (pb:) installed, fsid: 4\nswitch:", "load_helper");

        //dir flash:
        responses.put("load_helper\nswitch:", "dir flash:");

        //rename flash:config.text flash:config.old
        responses.put("bytes used)\nswitch:", "rename flash:config.text flash:config.old");

        //boot -- may need to be "reset" according to non-Cisco article
        responses.put("rename flash:config.text flash:config.old\nswitch:", "boot");

        //no to configuration
        responses.put("Continue with configuration dialog? [yes/no]:", "n");

        //return
        responses.put("Press RETURN to get started.", "\r");

        //enable -- could be enable
        responses.put("\n\n\n\nSwitch>", "en");

        //rename flash:config.old flash:config.text
        responses.put("en\nSwitch#", "rename flash:config.old flash:config.text");

        //confirm rename
        responses.put("Destination filename [config.text]?", "\r");

        //copy HOW TO DO THIS...
        responses.put("Destination filename [config.text]?\nSwitch#", "copy flash:config.text system:running-config");

        //Confirm copy
        responses.put("Destination filename [running-config]?", "\r");

        //Enter Global Configuration mode
        responses.put("bytes/sec)\nSwitch#", "conf t");

        //At this point, ask user what kind of password to overwrite, then overwrite.
    }*/

    public boolean setMessage(String message){
        textView.setText(message);
        return true;
    }

    public int read(String data){
        int result = 0;
        if (data.contains("witch:")) result = 1;
        else if(data.contains("Continue with configuration dialog? [yes/no]:")) result = 2;
        else if(data.contains("witch>")) result = 3;
        else if(data.contains("witch#")) result = 4;
        else if(data.contains("[config.text]")) result = 5;
        else if(data.contains("[running-config]")) result = 6;
        else if(data.contains("Sw") && data.contains("#")) result = 7;
        else result = -1;

        return result;
    }

}
