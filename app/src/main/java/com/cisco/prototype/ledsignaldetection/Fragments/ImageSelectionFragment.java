package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.io.File;
import java.util.ArrayList;

public class ImageSelectionFragment extends Fragment {

    private BluetoothInterface mListener;
    private AbsListView mListView;
    private ArrayAdapter mAdapter;
    private ArrayList<String> fileNames;
    private ArrayList<File> myFiles;
    private AdapterView.OnItemClickListener mMessageHandler = new AdapterView.OnItemClickListener(){
        public void onItemClick(AdapterView parent, View v, int position, long id){

        }
    };


    public ImageSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileNames = new ArrayList<>();
        myFiles = new ArrayList<File>();
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1,fileNames);
    }
    @Override
    public void onResume(){
        super.onResume();
        myFiles.clear();
        fileNames.clear();
        mListener.startImageViewer();
    }

    public void init(File[] files){
        for(File file : files){
            myFiles.add(file);
            String currFileName = file.getName();
            mAdapter.add(currFileName);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_selection, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(R.id.imageList);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
        mListView.setOnItemClickListener(mMessageHandler);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
