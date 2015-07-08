package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;


public class PasswordFragment extends Fragment {
    private BluetoothInterface mListener;
    private HashMap<String, String> responses;
    public PasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        responses = new HashMap<String, String>();
        constructHashMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_password, container, false);
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

    private void constructHashMap(){
        byte[] esc = {0x1b};
        responses.put("Send break character to prevent autobooting.", new String(esc));

        //load_helper
        responses.put("Parameter Block Filesystem (pb:) installed, fsid: 4\nswitch:", "load_helper");

        //dir flash:
        responses.put("load_helper\nswitch:", "dir flash:");

        //rename flash:config.text flash:config.old
        responses.put("bytes used)\nswitch:", "rename flash:config.text flash:config.old");

        //boot
        responses.put("rename flash:config.text flash:config.old\nswitch:", "boot");

        //no to configuration
        responses.put("Continue with configuration dialog? [yes/no]:", "n");

        //return
        responses.put("Press RETURN to get started.", "\r");

        //enable
        responses.put("\n\n\n\nSwitch>", "en");

        //rename flash:config.old flash:config.text
        responses.put("en\nSwitch#", "rename flash:config.old flash:config.text");

        //confirm rename
        responses.put("Destination filename [config.text]", "\r");

        //copy HOW TO DO THIS...
        //responses.put("Switch#", "copy flash:config.text system:running-config");

        //Confirm copy
        responses.put("Destination filename [running-config]?", "\r");

        //Then, overwrite the passwords you want to overwrite. Bleeeeehhhhhhhh
    }

}
