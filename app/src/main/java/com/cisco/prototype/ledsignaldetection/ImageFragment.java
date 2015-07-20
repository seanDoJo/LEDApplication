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
    private boolean kickstart;
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            if(kickstart) mListener.onSelectImageKick(position);
            else mListener.onSelectImageSys(position);
        }
    };

    public ImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // upgrade software image
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        mList = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.fragment_image, mList);

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

    public void populate(ArrayList<String> imagePairs, boolean kick){
        mList = imagePairs;
        mAdapter.notifyDataSetChanged();
        kickstart = kick;
    }

}