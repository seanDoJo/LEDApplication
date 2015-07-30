package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.ArrayList;

/**
 * This fragment is for displaying detected images
 */
public class ImageFragment extends Fragment {
    public String log;
    private BluetoothInterface mListener;
    private TextView textView;
    private View view;
    public int state = 31;
    public boolean kickstart;
    public boolean success;
    public boolean readOutput;
    private boolean firstLoader;
    private ArrayList<String> myArray;
    private AbsListView mListView;
    private ArrayAdapter<String> mAdapter;
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            state = 5;
            mListener.imageStateMachine(state, position);
        }
    };
    Button button;

    public ImageFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        state = 0;
        super.onCreate(savedInstanceState);
        log = "";
        mListener.onImageFragment();
        success = false;
        firstLoader = true;
        myArray = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, myArray);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_image, container, false);
        mListView = (AbsListView) view.findViewById(R.id.image_list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        mListView.setOnItemClickListener(mMessageClickedHandler);

        textView = (TextView) view.findViewById(R.id.image_text);
        button = (Button) view.findViewById(R.id.download);

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
        mAdapter.clear();
        for(int i = 0; i < imagePairs.size(); i++){
            mAdapter.add(imagePairs.get(i));
        }
    }

    public void success(String message){
        textView.setText(message);
        button.setText("OK");
    }

    public void setText(String message){textView.setText(message);}

    public void setReadOutput(boolean bool){
        readOutput = bool;
    }

    public void read(String data){
        log += data;
        Log.i("log", log);
        //mListener.onPasswordFragment(record);
        if(readOutput) {
            switch (state) {
                case 0:
                    if (log.contains("loader>")) {
                        if (firstLoader) {
                            firstLoader = false;
                            log = log.replace("loader>", "");
                        } else {
                            mListener.onFileListObtained();
                            readOutput = false;
                        }
                    }
                    break;
                case 2:
                    Log.i("case", "Case 2 entered");
                    if (log.contains("loader>")) {
                        //fail
                        state = 7;
                        mListener.imageStateMachine(state);
                        readOutput = false;
                    } else if (log.contains("(boot)#")) {
                        //success!
                        state = 9;
                        kickstart = false;
                        readOutput = false;
                        mListener.imageStateMachine(state);
                    }
                    break;
                case 4:
                    if(log.contains("Could not load")){
                        //fail
                        state = 8;
                        readOutput = false;
                        mListener.imageStateMachine(state);
                    }
                    else if(log.toLowerCase().contains("login") || log.toLowerCase().contains("user access")){
                        //success!
                        state = 6;
                        readOutput = false;
                        mListener.imageStateMachine(state);
                    }
                    break;
            }
        }

    }

}