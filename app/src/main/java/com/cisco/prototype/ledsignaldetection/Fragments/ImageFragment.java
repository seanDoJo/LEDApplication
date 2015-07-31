package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This fragment is for displaying detected images
 */
public class ImageFragment extends Fragment {
    public String log;
    private BluetoothInterface mListener;
    public int state = 31;
    public boolean kickstart;
    public boolean success;
    public boolean readOutput;
    private boolean firstLoader;
    private boolean firstSys;
    private ArrayList<String> kst;
    private ArrayList<String> syst;
    private ArrayList<String> files;
    public String sysImage = "";
    public String kickImage = "";
    private ArrayAdapter<String> kickAdapter;
    private ArrayAdapter<String> sysAdapter;
    private ArrayAdapter<String> fileAdapter;

    private Pattern recordP = Pattern.compile("(?s).*bootflash:(.*)[lL]oader>[^>]*");
    private Pattern weirdItem = Pattern.compile("^[^\\s]*$");
    private Pattern itemExtract = Pattern.compile("^*.\\s{1}([^\\s]*)$");
    private Pattern ks = Pattern.compile("^.*-kickstart.*\\.bin$");
    private Pattern system = Pattern.compile("^.*\\.bin$");
    private Pattern empty = Pattern.compile("");

    private View view;
    private TextView infoText;
    private Button submit;
    private TextView terminal;
    private Spinner kickSpin;
    private Spinner sysSpin;
    private Spinner fileSpin;
    private RelativeLayout imageOptions;

    public ImageFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        state = 0;
        super.onCreate(savedInstanceState);
        log = "";
        mListener.onImageFragment();
        success = false;
        firstLoader = true;
        firstSys = true;
        kst = new ArrayList<String>();
        syst = new ArrayList<String>();
        files = new ArrayList<String>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_image, container, false);
        infoText = (TextView) view.findViewById(R.id.image_text);
        terminal = (TextView) view.findViewById(R.id.image_terminal);
        submit = (Button) view.findViewById(R.id.submit_image);
        imageOptions = (RelativeLayout) view.findViewById(R.id.image_options);

        kickSpin = (Spinner) view.findViewById(R.id.kickImages);
        kickAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, kst);
        kickAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kickSpin.setAdapter(kickAdapter);

        sysSpin = (Spinner) view.findViewById(R.id.sysImages);
        sysAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, syst);
        sysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sysSpin.setAdapter(sysAdapter);

        fileSpin = (Spinner) view.findViewById(R.id.file_spinner);
        fileAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, files);
        fileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileSpin.setAdapter(fileAdapter);

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

    public void success(String message){
        imageOptions.setVisibility(View.GONE);
        infoText.setText(message);
        submit.setText("OK");
        submit.setEnabled(true);
    }

    public void setText(String message){infoText.setText(message);}

    public void setReadOutput(boolean bool){
        readOutput = bool;
    }

    public void read(String data){
        log += data;
        terminal.setText(log);
        Log.i("log", log);
        if(readOutput) {
            switch (state) {
                case 0:
                    if (log.contains("loader>")) {
                        if (firstLoader) {
                            firstLoader = false;
                            log = log.replace("loader>", "");
                        } else {
                            readOutput = false;
                            onFileListObtained();
                        }
                    } else if(log.contains("(boot)#")){
                        kickstart = false;
                        if (firstSys) {
                            firstSys = false;
                            log = log.replace("(boot)#", "");
                        } else {
                            readOutput = false;
                            onFileListObtained();
                        }
                    } else if(log.toLowerCase().contains("more")){
                        mListener.writeData("");
                    }
                    break;
                case 2:
                    Log.i("case", "Case 2 entered");
                    if (log.contains("loader>")) {
                        //fail
                        state = 7;
                        readOutput = false;
                        kickSpin.setEnabled(true);
                        submit.setEnabled(true);
                        mListener.imageStateMachine(state);
                    } else if (log.contains("(boot)#")) {
                        //success!
                        state = 9;
                        sysSpin.setEnabled(true);
                        submit.setEnabled(true);
                        kickstart = false;
                        readOutput = false;
                        mListener.imageStateMachine(state);
                    }else if(data.toLowerCase().contains("more")){
                        mListener.writeData("");
                    }
                    break;
                case 4:
                    if(log.contains("Could not load") || log.contains("opensource.org")){
                        //fail
                        state = 8;
                        readOutput = false;
                        sysSpin.setEnabled(true);
                        submit.setEnabled(true);
                        mListener.imageStateMachine(state);
                    } else if(log.toLowerCase().contains("login") || log.toLowerCase().contains("user access")){
                        //success!
                        state = 6;
                        readOutput = false;
                        mListener.imageStateMachine(state);
                    }else if(log.toLowerCase().contains("more")){
                        mListener.writeData("");
                    }
                    break;
            }
        }

    }

    public void onFileListObtained(){
        Log.i("image", "onFileListObtained entered");

        //load kickstart stuff
        Matcher recordM = recordP.matcher(log);
        if(recordM.find()){
            log = recordM.group(1);
            Log.e("LEDApp", log);
        }
        ArrayList<String[]> imagePairs = new ArrayList<>();
        log = log.replaceAll("\\[\\d+m--More-- \\[\\d+m", "");
        log = log.replaceAll("\\[ .* \\[m", "");
        log = log.replaceAll("\\[\\d+m?", "");
        String[] directoryContents = log.split("\n");
        Log.e("LEDApp", log);
        for (String piece : directoryContents) {
            if(!empty.matcher(piece.trim()).matches()) files.add(piece);
            String item = "";
            if(weirdItem.matcher(piece.trim()).matches()){
                item = piece;
            }
            else{
                Matcher itemFinder = itemExtract.matcher(piece.trim());
                item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                Log.e("LEDApp", piece + ", " + item);
            }
            if (ks.matcher(item.trim()).matches()) {
                Log.e("LEDApp", "found kickstart: " + item);
                kst.add(item.trim());
                kickAdapter.notifyDataSetChanged();
            } else if (system.matcher(item.trim()).matches()) {
                syst.add(item.trim());
                sysAdapter.notifyDataSetChanged();
                Log.e("LEDApp", "found sys: " + item);
            }
        }
        Log.i("array", "ks: " + kst.size());
        Log.i("array", "sys: " + syst.size());
        imageOptions.setEnabled(true);
        if (kst.size() == 1 && syst.size() == 1) {
            this.sysImage = syst.get(0).trim();
            this.kickImage = kst.get(0).trim();
            mListener.imageStateMachine(1);
        } else if(kst.size() == 0 || syst.size() == 0){
            Log.e("LEDMatch", "image pair size is 0");
            kst.clear();
            syst.clear();
            for(int i = 0; i < files.size(); i ++){
                kst.add(files.get(i));
                syst.add(files.get(i));
            }
            kickAdapter.notifyDataSetChanged();
            sysAdapter.notifyDataSetChanged();
            mListener.imageStateMachine(0);
        }  else {
            mListener.imageStateMachine(0);
        }
    }

}