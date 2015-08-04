package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.HashMap;

public class ImageRestoreFragment extends Fragment {
    private BluetoothInterface mListener;
    private HashMap<String, String> details;
    private String record = "";
    private TextView log = null;
    private int state = 0;
    private boolean recoveryStarted = false;

    public ImageRestoreFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        details = new HashMap<String, String>();
        details.put("ip", "172.25.186.254");
        details.put("gw", "172.25.186.1");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_restore, container, false);
        log = (TextView)view.findViewById(R.id.recoveryLog);
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

    public void collectInfo(String information){
        String[] bits = information.split(",");
        LinearLayout startView = (LinearLayout)getActivity().findViewById(R.id.recoveryStart);
        LinearLayout logView = (LinearLayout)getActivity().findViewById(R.id.recoveryLogShell);
        startView.setVisibility(SurfaceView.GONE);
        logView.setVisibility(SurfaceView.VISIBLE);
        if(bits[0].trim() != "")details.put("ip", bits[0]);
        if(bits[1].trim() != "")details.put("gw", bits[1]);
        details.put("ftpAddr", bits[2]);
        details.put("ksimg", bits[3]);
        details.put("sysimg", bits[4]);
        recoveryStarted = true;
        mListener.writeData("");
    }

    public void step(String received){
        record += received;
        log.setText(log.getText() + received);
        if(recoveryStarted) {
            switch (state) {
                case 1:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("set ip " + details.get("ip") + " 255.255.255.0");
                        state++;
                        record = "";
                    } else if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("");
                        state = 8;
                        record = "";
                    }
                    break;
                case 2:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("set gw " + details.get("gw"));
                        state++;
                        record = "";
                    }
                    break;
                case 3:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("boot tftp://" + details.get("ftpAddr") + "/" + details.get("ksimg"));
                        state++;
                        record = "";
                    }
                    break;
                case 4:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("conf t");
                        state++;
                        record = "";
                    }
                    break;
                case 5:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("int m0");
                        state++;
                        record = "";
                    }
                    break;
                case 6:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("no shut");
                        state++;
                        record = "";
                    }
                    break;
                case 7:
                    if (record.toLowerCase().contains("config)#")) {
                        mListener.writeData("exit");
                        state++;
                        record = "";
                    }
                    break;
                case 8:
                    if (!details.get("ksimg").trim().equals("")) {
                        if (record.toLowerCase().contains("boot)#")) {
                            mListener.writeData("copy ftp: bootflash:");
                            record = "";
                        } else if (record.toLowerCase().contains("filename:")) {
                            mListener.writeData(details.get("ksimg"));
                            record = "";
                        } else if (record.toLowerCase().contains("server:")) {
                            mListener.writeData(details.get("ftpAddr"));
                            record = "";
                        } else if (record.toLowerCase().contains("username:")) {
                            mListener.writeData(details.get("username"));
                            record = "";
                        } else if (record.toLowerCase().contains("password")) {
                            mListener.writeData(details.get("password"));
                            record = "";
                            state++;
                        }
                    } else {
                        state++;
                        record = "";
                        mListener.writeData("");
                    }
                    break;
                case 9:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("copy ftp: bootflash:");
                        record = "";
                    } else if (record.toLowerCase().contains("filename:")) {
                        mListener.writeData(details.get("sysimg"));
                        record = "";
                    } else if (record.toLowerCase().contains("server:")) {
                        mListener.writeData(details.get("ftpAddr"));
                        record = "";
                    } else if (record.toLowerCase().contains("username:")) {
                        mListener.writeData(details.get("username"));
                        record = "";
                    } else if (record.toLowerCase().contains("password")) {
                        mListener.writeData(details.get("password"));
                        record = "";
                        state++;
                    }
                    break;
                case 10:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("load bootflash:" + details.get("sysimg"));
                        state++;
                        record = "";
                    }
                    break;
                case 11:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("load bootflash:" + details.get("sysimg"));
                        state++;
                        record = "";
                    }
                    break;
            }
        }
    }

}
