package com.cisco.prototype.ledsignaldetection;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class ViewFileFragment extends Fragment {
    private BluetoothInterface mListener;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    private File viewedFile = null;
    private TextView fileText = null;


    public ViewFileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_file, container, false);
        fileText = (TextView)view.findViewById(R.id.fileText);
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
    public void onStart(){
        super.onStart();
        mListener.initFileView();
    }

    @Override
    public void onPause(){
        super.onPause();
        try{
            if(reader != null)reader.close();
        }catch(IOException e){e.printStackTrace();}
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try{
            if(reader != null)reader.close();
        }catch(IOException e){e.printStackTrace();}
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void viewCurrentFile(File file){
        viewedFile = file;
        String line;
        try {
            reader = new BufferedReader(new FileReader(viewedFile));
            while((line = reader.readLine()) != null){
                fileText.setText(fileText.getText() + "\n" + line);
            }
        }catch(FileNotFoundException e){e.printStackTrace();} catch (IOException e){ e.printStackTrace();}

    }

}
