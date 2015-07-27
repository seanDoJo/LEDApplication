package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

public class CommunicationFragment extends Fragment {
    private BluetoothInterface mListener;

    public CommunicationFragment(){
        // This just needs to be here
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_communication, container, false);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (BluetoothInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onSendListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.destroyFile();
        mListener = null;
    }

    /*public interface onSendListener{
        public void onSendListener(String message);
    }*/

    public void addMessage(String message) {
        TextView text = (TextView) getView().findViewById(R.id.text_view);
        if(text != null)text.append(message);
    }

    public void collapse(){
        EditText editText = (EditText)getView().findViewById(R.id.edit_message);
        editText.setText("");
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

}
