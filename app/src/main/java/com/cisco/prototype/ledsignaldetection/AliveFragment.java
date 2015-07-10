package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.R;

import org.w3c.dom.Text;

public class AliveFragment extends Fragment {
    private BluetoothInterface mListener;
    private View view;
    private TextView textView;
    public Button button;
    //TODO reference onAliveFragment()

    public AliveFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alive, container, false);
        button = (Button) view.findViewById(R.id.ok);
        button.setEnabled(false);
        textView = (TextView) view.findViewById(R.id.alive_text);
        mListener.onAliveFragment();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            Log.i("Entered", "onAttach try entered");
            mListener = (BluetoothInterface)activity;
            //Log.i("result", "" + alive);
        } catch(ClassCastException e){}
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setMessage(String message) {
        textView.setText(message);
    }

}