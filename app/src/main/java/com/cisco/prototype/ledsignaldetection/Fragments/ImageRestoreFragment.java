package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageRestoreFragment extends Fragment {
    private BluetoothInterface mListener;
    private EditText ipView = null;
    private EditText maskView = null;
    private EditText gwView = null;
    private String record = "";
    private TextView log = null;
    private int state = 1;
    private int beginState = 1;
    private boolean recoveryStarted = false;
    private String gw = "";
    private String ip = "";
    private String ftp = "";
    private String ksimg =  "";
    private String sysimg = "";
    private String username = "";
    private String password = "";
    private String mask = "";
    private Pattern ipPattern = Pattern.compile("(?s).*show ip(.*)loader>.*");
    private Pattern gwPattern = Pattern.compile(("(?s).*show gw(.*)loader>.*"));

    public ImageRestoreFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_restore, container, false);
        log = (TextView)view.findViewById(R.id.recoveryLog);
        ipView = (EditText)view.findViewById(R.id.ipaddr);
        maskView = (EditText)view.findViewById(R.id.maskaddr);
        gwView = (EditText)view.findViewById(R.id.gwaddr);
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
    public void onResume(){
        super.onResume();
        mListener.writeData("");
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void collectInfo(String[] information){
        String[] bits = information;
        LinearLayout startView = (LinearLayout)getActivity().findViewById(R.id.recoveryStart);
        LinearLayout logView = (LinearLayout)getActivity().findViewById(R.id.recoveryLogShell);
        startView.setVisibility(SurfaceView.GONE);
        logView.setVisibility(SurfaceView.VISIBLE);
        if(!bits[0].trim().equals(""))ip = bits[0];
        if(!bits[1].trim().equals(""))gw = bits[1];
        if(!bits[2].trim().equals(""))ftp = bits[2];
        ksimg = bits[3];
        sysimg = bits[4];
        username = bits[5];
        password = bits[6];
        recoveryStarted = true;
        mListener.writeData("");
    }

    private boolean ping(String addr){
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c " + addr);
            int mExitValue = mIpAddrProcess.waitFor();
            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
        return false;
    }

    public void step(String received){
        record += received;
        log.setText(log.getText() + received);
        if(recoveryStarted) {
            switch (state) {
                case 1:
                    if (record.toLowerCase().contains("loader>") && !ip.trim().equals("")) {
                        mListener.writeData("set ip " + ip.trim() + " " + mask.trim());
                        state++;
                        record = "";
                    } else if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("");
                        state = 4;
                        record = "";
                    } else if(ip.trim().equals("") && record.toLowerCase().contains("loader>")){
                        Log.e("LEDApp", record);
                        mListener.writeData("");
                        state++;
                        record = "";
                    }
                    break;
                case 2:
                    if (record.toLowerCase().contains("loader>") && !gw.trim().equals("")) {
                        mListener.writeData("set gw " + gw.trim());
                        state++;
                        record = "";
                    }else if(gw.trim().equals("") && record.toLowerCase().contains("loader>")){
                        mListener.writeData("");
                        state++;
                        record = "";
                    }
                    break;
                case 3:
                    if (record.toLowerCase().contains("loader>")) {
                        mListener.writeData("boot tftp://" + ftp.trim() + "/" + ksimg.trim());
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
                    if (record.toLowerCase().contains("(config")) {
                        mListener.writeData("int mgmt 0");
                        state++;
                        record = "";
                    }
                    break;
                case 6:
                    if (record.toLowerCase().contains("(config")) {
                        mListener.writeData("no shut");
                        state++;
                        record = "";
                    }
                    break;
                case 7:
                    if (record.toLowerCase().contains("(config")) {
                        mListener.writeData("exit");
                        //recoveryStarted = false;
                        record = "";
                    }else if(record.toLowerCase().contains("boot)#")){
                        mListener.writeData("");
                        state++;
                        record = "";
                    }
                    break;
                case 8:
                    if (record.toLowerCase().contains("(config")) {
                        mListener.writeData("exit");
                        state++;
                        record = "";
                    }else if (!ksimg.trim().equals("")) {
                        if(record.toLowerCase().contains("can't connect") || record.toLowerCase().contains("not connected")){
                            log.setText("CAN'T CONNECT TO NETWORK");
                            record = "";
                        }
                        if (record.toLowerCase().contains("boot)#")) {
                            mListener.writeData("copy ftp: bootflash:");
                            record = "";
                        } else if (record.toLowerCase().contains("filename:")) {
                            mListener.writeData(ksimg.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("server:")) {
                            mListener.writeData(ftp.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("username:")) {
                            mListener.writeData(username.trim());
                            record = "";
                        } else if (record.toLowerCase().contains("password")) {
                            mListener.writeData(password.trim());
                            record = "";
                            state++;
                        }
                    } else if(record.toLowerCase().contains("boot)#")) {
                        state++;
                        recoveryStarted = false;
                        record = "";
                        mListener.writeData("");
                    }
                    break;
                case 9:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("copy ftp: bootflash:");
                        record = "";
                    } else if (record.toLowerCase().contains("filename:")) {
                        mListener.writeData(sysimg.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("server:")) {
                        mListener.writeData(ftp.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("username:")) {
                        mListener.writeData(username.trim());
                        record = "";
                    } else if (record.toLowerCase().contains("password")) {
                        mListener.writeData(password.trim());
                        record = "";
                        state++;
                    }
                    break;
                case 10:
                    if (record.toLowerCase().contains("boot)#")) {
                        mListener.writeData("load bootflash:" + sysimg.trim());
                        state++;
                        record = "";
                    }
                    break;
                case 11:
                    if (record.toLowerCase().contains("boot)#")) {
                        state++;
                        record = "";
                    }
                    break;
            }
        } else {
            if(record.toLowerCase().contains("boot)#")){
                beginState = 4;

                TextView ipV = (TextView)getActivity().findViewById(R.id.ipaddrT);
                ipV.setVisibility(SurfaceView.GONE);
                TextView gwV = (TextView)getActivity().findViewById(R.id.gwaddrT);
                gwV.setVisibility(SurfaceView.GONE);
                TextView maskV = (TextView)getActivity().findViewById(R.id.maskaddrT);
                maskV.setVisibility(SurfaceView.GONE);

                EditText ipt = (EditText)getActivity().findViewById(R.id.ipaddr);
                ipt.setVisibility(SurfaceView.GONE);
                EditText maskt = (EditText)getActivity().findViewById(R.id.maskaddr);
                maskt.setVisibility(SurfaceView.GONE);
                EditText gwt = (EditText)getActivity().findViewById(R.id.gwaddr);
                gwt.setVisibility(SurfaceView.GONE);

                LinearLayout linearLayout = (LinearLayout)getActivity().findViewById(R.id.recoveryStart);
                linearLayout.setVisibility(SurfaceView.VISIBLE);
            }
            else {
                switch (beginState) {
                    case 1:
                        if (record.toLowerCase().contains("loader>")) {
                            Log.e("LEDApp", "show ip sent");
                            mListener.writeData("show ip");
                            record = "";
                            beginState++;
                        }
                        break;
                    case 2:
                        if (ipPattern.matcher(record).matches()) {
                            String line = "";
                            Matcher extractor = ipPattern.matcher(record);
                            if (extractor.find()) {
                                line = extractor.group(1);
                            }
                            String[] lines = line.split("\n");
                            for (String lin : lines) {
                                if (lin.toLowerCase().contains("ip addr")) {
                                    ip = lin.split(":")[1].trim();
                                    ipView.setText(ip);
                                } else if (lin.toLowerCase().contains("addr mask")) {
                                    mask = lin.split(":")[1].trim();
                                    maskView.setText(mask);
                                }
                            }
                            beginState++;
                            record = "";
                            mListener.writeData("show gw");
                        }
                        break;
                    case 3:
                        if (gwPattern.matcher(record).matches()) {
                            String line = "";
                            Matcher extractor = gwPattern.matcher(record);
                            if (extractor.find()) {
                                line = extractor.group(1);
                            }
                            String[] lines = line.split("\n");
                            for (String lin : lines) {
                                if (lin.toLowerCase().contains("default gateway")) {
                                    gw = lin.split(":")[1].trim();
                                    gwView.setText(gw);
                                }
                            }
                            beginState++;
                            record = "";
                            LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.recoveryStart);
                            linearLayout.setVisibility(SurfaceView.VISIBLE);
                        }
                        break;
                    case 4:
                        if (record.toLowerCase().contains("boot)#")) {
                            if (ping(ip)) {
                                beginState++;
                                recoveryStarted = true;
                                record = "";
                                mListener.writeData("");
                            } else {
                                log.setText("ATTEMPT TO REACH DEVICE FAILED!!!");
                            }
                        } else if (record.toLowerCase().contains("(config")) {
                            mListener.writeData("exit");
                            record = "";
                        }
                        break;
                }
            }
        }
    }

}
