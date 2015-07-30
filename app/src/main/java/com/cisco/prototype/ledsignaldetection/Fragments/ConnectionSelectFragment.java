package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

public class ConnectionSelectFragment extends Fragment {

    private BluetoothInterface mListener;

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
        return inflater.inflate(R.layout.fragment_connection_select, container, false);
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
