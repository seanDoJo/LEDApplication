package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * This fragment is for displaying detected images
 */
public class ImageFragment extends Fragment {
    public String log;
    private ArrayAdapter<String> mAdapter;
    private BluetoothInterface mListener;
    private ArrayList<String> mList;
    private TextView textView;
    private View view;
    private int state = 31;
    public boolean kickstart;
    public boolean success;
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            state = 5;
            mListener.imageStateMachine(state);
        }
    };
    Button button;

    public ImageFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = "";
        button = (Button) getView().findViewById(R.id.image_button);
        mListener.setImageMode();
        mListener.onImageFragment();
        success = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_image, container, false);
        mList = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.fragment_image, mList);
        textView = (TextView) view.findViewById(R.id.image_text);

        return view;
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

    public void populate(ArrayList<String> imagePairs){
        mList = imagePairs;
        mAdapter.notifyDataSetChanged();
    }

    public void success(String message){
        textView.setText(message);
        button.setText("OK");
    }

    public void setText(String message){textView.setText(message);}

    public int checkState(){return state;}

}