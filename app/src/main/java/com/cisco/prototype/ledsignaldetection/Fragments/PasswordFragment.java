package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PasswordFragment extends Fragment {
    private BluetoothInterface mListener;
    private Boolean recoveryStarted = false;
    private Boolean adminConfigured = false;
    private HashMap<String, String> responses;
    private TextView textView, output;
    private ProgressBar pBar;
    private int state = 0;
    private String record = "";
    private String secretPw = "";
    private String consolePw = "";
    private String enablePw = "";
    private String adminpass = "";
    private boolean reloadConfirmed = false;
    private int os = 0;
    private boolean toAutoBootConf = false;
    private boolean outputEnabled = false;
    private ArrayList<String> kst;
    private ArrayList<String> syst;
    private ArrayList<String> dirContents;
    private ArrayAdapter<String> kickAdapter;
    private ArrayAdapter<String> sysAdapter;
    private String sysImage = "";
    private String kickImage = "";

    private Pattern yesNo = Pattern.compile("(?s).*\\[yes/?no\\].*");
    private Pattern gt = Pattern.compile("(?s).*>[^>]*");
    private Pattern id = Pattern.compile("(?s)[^#]+#{1}[^#]*");
    private Pattern brackets = Pattern.compile("(?s).*\\[.*\\].*");

    private Pattern booted = Pattern.compile("(?s).*[^()#]+#[^#]*");
    private Pattern boot = Pattern.compile("(?s).*[sS]witch\\(boot\\)#[^#]*");
    private Pattern bootconfig = Pattern.compile("(?s).*[sS]witch\\(boot-config\\)#[^#]*");
    private Pattern altbootconfig = Pattern.compile("(?s).*[sS]witch\\(boot\\)\\(config\\)#[^#]*");
    private Pattern loader = Pattern.compile("(?s).*[lL]{1}oader>[^>]*");

    private Pattern kickstart = Pattern.compile("^.*-kickstart.*\\.bin$");
    private Pattern system = Pattern.compile("^.*\\.bin$");
    private Pattern version = Pattern.compile("^[^\\.]*\\.(\\d+.*)\\.bin$");

    private Pattern recordP = Pattern.compile("(?s).*bootflash:(.*)[lL]oader>.*");
    private Pattern recordS = Pattern.compile("(?s).*dir(.*)[uU]sage.*");
    private Pattern weirdItem = Pattern.compile("^[^\\s]*$");
    private Pattern itemExtract = Pattern.compile("^*.\\s{1}([^\\s]*)$");

    public PasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //responses = new HashMap<String, String>();
        //constructHashMap();
        kst = new ArrayList<>();
        syst = new ArrayList<>();
        dirContents = new ArrayList<>();
    }

    public void setImages(String kick, String sys){
        this.sysImage = sys;
        this.kickImage = kick;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_password, container, false);
        textView = (TextView) view.findViewById(R.id.password_prompt);
        output = (TextView)view.findViewById(R.id.password_output);
        pBar = (ProgressBar)view.findViewById(R.id.progressBar);
        Spinner spin = (Spinner)view.findViewById(R.id.selectpw);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log.i("LEDApp", parent.getItemAtPosition(pos).toString());
                mListener.switchPasswordContext(parent.getItemAtPosition(pos).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner s = (Spinner) view.findViewById(R.id.selectKickstart);
        kickAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, kst);
        kickAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(kickAdapter);
        s = (Spinner) view.findViewById(R.id.selectSys);
        sysAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, syst);
        sysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(sysAdapter);

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
        mListener.destroyFile();
        mListener = null;
    }

    public boolean setMessage(String message){
        textView.setText(message);
        return true;
    }

    public void updateProgress(int prog){
        if(pBar.getProgress() >= 98)pBar.setProgress(100);
        else pBar.setProgress(pBar.getProgress() + prog);
    }

    public void setProgress(int prog){
        pBar.setProgress(prog);
    }

    public void startRecovery(String data){
        String[] fields = data.split(",");
        if(fields[3].trim() == "IOS"){
            os = 0;
            secretPw = fields[0].trim();
            enablePw = fields[1].trim();
            consolePw = fields[2].trim();
            setMessage("Waiting for switch to enter password recovery mode\n\nUnplug the switch, then press and hold the mode button while plugging back in");
        }
        else{
            os = 1;
            adminpass = fields[0].trim();
            Log.i("FIELDS", fields[4]);
            if(fields[4] == "T"){
                state = 1;
                setMessage("Waiting for switch to enter password recovery mode\n\nPlease power cycle the switch!");
            }else{
                mListener.writeData("");
            }
        }
        recoveryStarted = true;
    }

    public void setAutoBootConf(){
        toAutoBootConf = true;
    }

    public void abandonHope(){
        Spinner s = (Spinner)getActivity().findViewById(R.id.selectKickstart);
        kst.clear();
        syst.clear();
        for(String con : dirContents){
            if(s.getVisibility() == SurfaceView.VISIBLE)kst.add(con);
            syst.add(con);
        }
        kickAdapter.notifyDataSetChanged();
        sysAdapter.notifyDataSetChanged();
        Button butt = (Button)getActivity().findViewById(R.id.changeFullList);
        butt.setEnabled(false);
        butt.setVisibility(SurfaceView.GONE);
        if(s.getVisibility() == SurfaceView.VISIBLE)selectBoot(false);
        else selectSys();
    }

    public void selectBoot(boolean pairs){
        TextView ksv = (TextView)getActivity().findViewById(R.id.selectKickstartpwh);
        ksv.setText("Kickstart Image:");
        ksv = (TextView)getActivity().findViewById(R.id.selectSyspwh);
        ksv.setVisibility(SurfaceView.VISIBLE);
        Spinner st = (Spinner)getActivity().findViewById(R.id.selectSys);
        st.setVisibility(SurfaceView.VISIBLE);
        RelativeLayout imageSelection = (RelativeLayout)getActivity().findViewById(R.id.passwordImages);
        imageSelection.setVisibility(SurfaceView.VISIBLE);
        RelativeLayout old = (RelativeLayout)getActivity().findViewById(R.id.password_text);
        old.setVisibility(SurfaceView.GONE);
        if(pairs){
            TextView out = (TextView)getActivity().findViewById(R.id.selectKickstartpwh);
            out.setText("Select an Image Pair:");
            out = (TextView)getActivity().findViewById(R.id.selectSyspwh);
            out.setVisibility(SurfaceView.GONE);
            Spinner s = (Spinner)getActivity().findViewById(R.id.selectSys);
            s.setVisibility(SurfaceView.GONE);
        }
    }
    public void selectSys(){
        RelativeLayout imageSelection = (RelativeLayout)getActivity().findViewById(R.id.passwordImages);
        imageSelection.setVisibility(SurfaceView.VISIBLE);
        Button button = (Button)getActivity().findViewById(R.id.imageSubmitpw);
        button.setEnabled(false);
        button.setVisibility(SurfaceView.GONE);
        button = (Button)getActivity().findViewById(R.id.sysSubmitpw);
        button.setEnabled(true);
        button.setVisibility(SurfaceView.VISIBLE);
        TextView out = (TextView)getActivity().findViewById(R.id.selectSyspwh);
        out.setText("Select a System Image:");
        out = (TextView)getActivity().findViewById(R.id.selectKickstartpwh);
        out.setVisibility(SurfaceView.GONE);
        Spinner s = (Spinner)getActivity().findViewById(R.id.selectKickstart);
        s.setVisibility(SurfaceView.GONE);
    }

    public void toggleOuput(){
        if(!outputEnabled) {
            Button button = (Button)getActivity().findViewById(R.id.togOutput);
            button.setText("Hide Terminal Output");
            ScrollView scrollview = (ScrollView) getActivity().findViewById(R.id.passwordScroll);
            scrollview.setEnabled(true);
            scrollview.setVisibility(SurfaceView.VISIBLE);
            scrollview.fullScroll(View.FOCUS_DOWN);
        }
        else{
            Button button = (Button)getActivity().findViewById(R.id.togOutput);
            button.setText("Show Terminal Output");
            ScrollView scrollview = (ScrollView) getActivity().findViewById(R.id.passwordScroll);
            scrollview.setEnabled(false);
            scrollview.setVisibility(SurfaceView.GONE);
        }
        outputEnabled = !outputEnabled;
    }

    public void read(String data){
        output.setText(output.getText() + data);
        if(outputEnabled) {
            ScrollView sView = (ScrollView) getActivity().findViewById(R.id.passwordScroll);
            sView.fullScroll(View.FOCUS_DOWN);
        }
        record += data;
        //mListener.onPasswordFragment(record);
        if(recoveryStarted && os == 0) {
            if (yesNo.matcher(record.toLowerCase()).matches()) {
                mListener.writeData("n");
                record = "";
            } else if (record.contains("MORE") || record.contains("RETURN") || brackets.matcher(record.toLowerCase()).matches()) {
                mListener.writeData("");
                record = "";
            }
            else {
                switch (state) {
                    case 0:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("flash_init");
                            mListener.onPasswordFragment("Recovery has started!");
                            state++;
                            record = "";
                        }
                        break;
                    case 1:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("rename flash:config.text flash:config.old");
                            mListener.onPasswordFragment("renaming config.text to config.old");
                            state++;
                            record = "";
                        }
                        break;
                    case 2:
                        if (record.toLowerCase().contains("switch:")) {
                            mListener.writeData("boot");
                            mListener.onPasswordFragment("rebooting the switch");
                            state++;
                            record = "";
                        }
                        break;
                    case 3:
                        if (gt.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("en");
                            mListener.onPasswordFragment("enabling the switch");
                            state++;
                            record = "";

                        }
                        break;
                    case 4:
                        if (id.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("rename flash:config.old flash:config.text");
                            mListener.onPasswordFragment("renaming config.old back to config.text");
                            state++;
                            record = "";
                        }
                        else if(record.toLowerCase().contains("connection") || gt.matcher(record.toLowerCase()).find()){
                            mListener.writeData("en");
                            record = "";
                        }
                        else if(record.toLowerCase().contains("password")){
                            //wat
                            Log.e("LEDApp", "password detected in password recovery");
                        }
                        break;
                    case 5:
                        if (id.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("copy flash:config.text system:running-config");
                            mListener.onPasswordFragment("copying config.text to the system running-config");
                            state++;
                            record = "";
                        }
                        break;
                    case 6:
                        if (id.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("conf t");
                            mListener.onPasswordFragment("entering configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 7:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable secret " + secretPw);
                            mListener.onPasswordFragment("setting secret password");
                            state++;
                            record = "";
                        }
                        break;
                    case 8:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("enable password " + enablePw);
                            mListener.onPasswordFragment("setting enable password");
                            state++;
                            record = "";
                        }
                        break;
                    case 9:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("line con 0");
                            mListener.onPasswordFragment("");
                            state++;
                            record = "";
                        }
                        break;
                    case 10:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("password " + consolePw);
                            mListener.onPasswordFragment("setting console password");
                            state++;
                            record = "";
                        }
                        break;
                    case 11:
                        if (record.toLowerCase().contains("(config-line)")) {
                            mListener.writeData("!!!ctrlz");
                            mListener.onPasswordFragment("exiting configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 12:
                        if (record.toLowerCase().contains("config-line)") || record.toLowerCase().contains("(config)")){
                            mListener.writeData("end");
                            record = "";
                        }
                        else if (id.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("write memory");
                            mListener.onPasswordFragment("saving configuration");
                            state++;
                            record = "";
                        }
                        break;
                    case 13:
                        if (id.matcher(record.toLowerCase()).find()) {
                            mListener.onPasswordFragment("deleting config.old");
                            mListener.writeData("del flash:config.old");
                            state++;
                            record = "";
                        }
                        break;
                    case 14:
                        if (id.matcher(record.toLowerCase()).find()) {
                            mListener.onPasswordFragment("Password Recovery Complete!");
                            state++;
                            record = "";
                        }
                        break;
                    case 15:
                        if (id.matcher(record.toLowerCase()).find() && toAutoBootConf) {
                            mListener.onPasswordFragment("Enabling Automatic Booting...");
                            mListener.writeData("conf t");
                            state++;
                            record = "";
                        }
                        break;
                    case 16:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("no boot manual");
                            state++;
                            record = "";
                        }
                        break;
                    case 17:
                        if (record.toLowerCase().contains("(config)")) {
                            mListener.writeData("!!!ctrlz");
                            state++;
                            record = "";
                        }
                        break;
                    case 18:
                        if (record.toLowerCase().contains("config-line)") || record.toLowerCase().contains("(config)")){
                            mListener.writeData("end");
                            record = "";
                        }
                        else if (id.matcher(record.toLowerCase()).find()) {
                            mListener.writeData("write memory");
                            state++;
                            record = "";
                        }
                        break;
                    case 19:
                        if(id.matcher(record.toLowerCase()).find()){
                            mListener.onPasswordFragment("Auto-Booting Configured!");
                            record = "";
                        }

                }
            }
        } else if(recoveryStarted && os == 1){
            if(record.toLowerCase().contains("bios") && state == 1 && !reloadConfirmed){
                reloadConfirmed = true;
            }
            if(record.toLowerCase().contains("y/n")){
                mListener.writeData("y");
                record = "";
            }
            else if(record.toLowerCase().contains("-more-") && !loader.matcher(record).matches() && !boot.matcher(record).matches()){
                record = record.replaceAll("\\[.*\\[\\d*m?", "");
                record = record.replaceAll("\\[d+m?", "");
                mListener.writeData("!!!space");
                if(state != 2 && state != 3 && state != 1)record = "";
            }
            else {
                switch (state) {
                    case 0:
                        if(boot.matcher(record).matches()) {
                            setProgress(30);
                            Log.e("LEDApp", "booter matched");
                            state = 4;
                            mListener.writeData("");
                            record = "";
                        }else if(loader.matcher(record).matches()){
                            setProgress(30);
                            mListener.writeData("");
                            state = 2;
                            record = "";
                        }else if(booted.matcher(record).matches()) {
                            mListener.writeData("reload");
                            setProgress(5);
                            state++;
                            reloadConfirmed = false;
                            record = "";
                            setMessage("reloading switch");
                        } else if(record.toLowerCase().contains("login:") || record.toLowerCase().contains("password:") || record.toLowerCase().contains("username:")){
                            setMessage("Waiting for switch to power cycle!");
                            setProgress(5);
                            state++;
                            reloadConfirmed = false;
                            record = "";
                        }
                    case 1:
                        if(loader.matcher(record).matches()){
                            setProgress(30);
                            mListener.writeData("");
                            state++;
                            record = "";
                        }else if(boot.matcher(record).matches()){
                            setProgress(30);
                            Log.e("LEDApp", "booter matched");
                            state = 4;
                            mListener.writeData("");
                            record = "";
                        }
                        break;
                    case 2:
                        if (loader.matcher(record).matches() || boot.matcher(record).matches()) {
                            //load kickstart stuff
                            mListener.writeData("dir");
                            setMessage("Discovering bootable images...");
                            state++;
                            record = "";
                        }
                        break;
                    case 3:
                        if (recordP.matcher(record).matches()) {
                            setProgress(40);
                            //load kickstart stuff
                            record = record.replaceAll("\\[.*\\[\\d*m?", "");
                            record = record.replaceAll("\\[\\d+m?", "");
                            Matcher recordM = recordP.matcher(record);
                            if(recordM.find()){
                                record = recordM.group(1);
                                Log.e("LEDApp", record);
                            }
                            ArrayList<String[]> imagePairs = new ArrayList<>();
                            record = record.replaceAll("\\[\\d+m?--More-- \\[\\d+m?", "");
                            String[] directoryContents = record.split("\n");

                            for (String piece : directoryContents) {
                                String item = "";
                                if(weirdItem.matcher(piece.trim()).matches()){
                                    item = piece;
                                }
                                else{
                                    Matcher itemFinder = itemExtract.matcher(piece.trim());
                                    item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                                }
                                if(item.trim().replace("\n", "").length() > 0 && !item.trim().contains("loader")){
                                    dirContents.add(piece.trim());
                                }
                                Log.e("LEDApp", item);
                                if (kickstart.matcher(item.trim()).matches()) {
                                    Log.e("LEDApp", "found kickstart");
                                    kst.add(item.trim());
                                } else if (system.matcher(item.trim()).matches()) {
                                    syst.add(item.trim());
                                }
                            }

                            if (kst.size() == 1 && syst.size() == 1) {
                                this.sysImage = syst.get(0).trim();
                                this.kickImage = kst.get(0).trim();
                                kst.clear();
                                syst.clear();
                                kst.add(this.kickImage);
                                syst.add(this.sysImage);
                                kickAdapter.notifyDataSetChanged();
                                sysAdapter.notifyDataSetChanged();
                                selectBoot(false);
                            } else if (kst.size() > 1 || syst.size() > 1) {
                                //multiple kickstart and/or system images
                                for (String kickstart : kst) {
                                    Matcher kickMatch = version.matcher(kickstart);
                                    String ksver = "";
                                    if(kickMatch.find()){
                                        ksver = kickMatch.group(1);
                                    }
                                    for (String sys : syst) {
                                        Matcher sysMatch = version.matcher(sys);
                                        String sysver = "";
                                        if(sysMatch.find()){
                                            sysver = sysMatch.group(1);
                                        }
                                        boolean equal = true;
                                        Log.e("LEDWHY", ksver + " : " + sysver);
                                        String[] sysverBreak = sysver.split("\\.");
                                        String[] ksverBreak = ksver.split("\\.");
                                        if(sysverBreak.length == ksverBreak.length && sysverBreak.length > 0){
                                            for(int x = 0; x < sysverBreak.length; x++){
                                                if(!sysverBreak[x].trim().equals(ksverBreak[x].trim())){
                                                    equal = false;
                                                    break;
                                                } else {
                                                    Log.e("LEDWHAT", sysverBreak[x] + " is apparently equal to " + ksverBreak[x]);
                                                }
                                            }
                                        } else {
                                            equal = false;
                                        }
                                        if (equal) {
                                            String[] couple = new String[2];
                                            couple[0] = kickstart;
                                            couple[1] = sys;
                                            imagePairs.add(couple);
                                            Log.e("LEDMatch", "Added image pair!");
                                            Log.e("LEDMatch", kickstart + " : " + sys);
                                        }
                                    }
                                }
                                if(imagePairs.size() == 1){
                                    this.sysImage = imagePairs.get(0)[1];
                                    this.kickImage = imagePairs.get(0)[0];
                                    kst.clear();
                                    syst.clear();
                                    kst.add(imagePairs.get(0)[0]);
                                    syst.add(imagePairs.get(0)[1]);
                                    kickAdapter.notifyDataSetChanged();
                                    sysAdapter.notifyDataSetChanged();
                                    selectBoot(false);
                                }else if (imagePairs.size() == 0) {
                                    Log.e("LEDMatch", "image pair size is 0");
                                    kst.clear();
                                    syst.clear();
                                    for(String item : directoryContents){
                                        kst.add(item.trim());
                                        syst.add(item.trim());
                                    }
                                    kickAdapter.notifyDataSetChanged();
                                    sysAdapter.notifyDataSetChanged();
                                    Button butt = (Button)getActivity().findViewById(R.id.changeFullList);
                                    butt.setEnabled(false);
                                    butt.setVisibility(SurfaceView.GONE);
                                    selectBoot(false);
                                } else {
                                    //have user choose pair from collected images
                                    kst.clear();
                                    for(String[] pair : imagePairs){
                                        kst.add(pair[0] + ":" + pair[1]);
                                    }
                                    kickAdapter.notifyDataSetChanged();
                                    selectBoot(true);
                                }
                            } else {
                                //have user choose pair from directory
                                if(kst.size() == 0 || syst.size() == 0) {
                                    kst.clear();
                                    syst.clear();
                                    for (String item : directoryContents) {
                                        kst.add(item.trim());
                                        syst.add(item.trim());
                                    }
                                    kickAdapter.notifyDataSetChanged();
                                    sysAdapter.notifyDataSetChanged();
                                    Button butt = (Button) getActivity().findViewById(R.id.changeFullList);
                                    butt.setEnabled(false);
                                    butt.setVisibility(SurfaceView.GONE);
                                }
                                selectBoot(false);
                            }
                            state++;
                            record = "";
                        } else if(recordS.matcher(record).matches() && boot.matcher(record).matches()){
                            setProgress(40);
                            Log.i("LEDApp", "Hit boot matcher");
                            record = record.replaceAll("\\[.*\\[\\d*m?", "");
                            record = record.replaceAll("\\[\\d+m?", "");
                            Matcher recordM = recordS.matcher(record);
                            if(recordM.find()){
                                record = recordM.group(1);
                                Log.e("LEDApp", record);
                            }
                            ArrayList<String[]> imagePairs = new ArrayList<>();
                            record = record.replaceAll("\\[\\d+m?--More-- \\[\\d+m?", "");
                            String[] directoryContents = record.split("\n");

                            for (String piece : directoryContents) {
                                String item = "";
                                if(weirdItem.matcher(piece.trim()).matches()){
                                    item = piece;
                                }
                                else{
                                    Matcher itemFinder = itemExtract.matcher(piece.trim());
                                    item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                                }
                                if(item.trim().replace("\n", "").length() > 0 && !item.trim().contains("loader")){
                                    dirContents.add(piece.trim());
                                }
                                Log.e("LEDApp", item);
                                if (kickstart.matcher(item.trim()).matches()) {
                                    Log.e("LEDApp", "found kickstart");
                                    kst.add(item.trim());
                                } else if (system.matcher(item.trim()).matches()) {
                                    syst.add(item.trim());
                                }
                            }
                            if(syst.size() == 1){
                                this.sysImage = syst.get(0).trim();
                                sysAdapter.notifyDataSetChanged();
                                selectSys();
                            } else {
                                if(syst.size() == 0) {
                                    syst.clear();
                                    for (String piece : directoryContents) {
                                        String item = piece;
                                        if (!weirdItem.matcher(piece.trim()).matches()) {
                                            Matcher itemFinder = itemExtract.matcher(piece.trim());
                                            item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                                        }
                                        Log.i("LEDApp", item);
                                        syst.add(item.trim());
                                    }

                                    Button butt = (Button) getActivity().findViewById(R.id.changeFullList);
                                    butt.setEnabled(false);
                                    butt.setVisibility(SurfaceView.GONE);
                                }
                                sysAdapter.notifyDataSetChanged();
                                selectSys();
                            }
                            state = 8;
                            record = "";
                        } else if (loader.matcher(record).matches() && record.toLowerCase().contains("error")) {
                            //load kickstart stuff
                            mListener.writeData("dir");
                            record = "";
                        }
                        break;
                    case 4:
                        if (boot.matcher(record).matches()) {
                            setProgress(50);
                            mListener.writeData("configure terminal");
                            state++;
                            record = "";
                        }else if(loader.matcher(record).matches()){
                            setProgress(40);
                            mListener.writeData("");
                            state = 2;
                            record = "";
                        }
                        break;
                    case 5:
                        if (bootconfig.matcher(record).matches() || altbootconfig.matcher(record).matches()) {
                            mListener.writeData("admin-password " + adminpass);
                            state++;
                            record = "";
                        } else if(record.toLowerCase().contains("invalid")){
                            mListener.writeData("config terminal");
                            record = "";
                        }
                        break;
                    case 6:
                        if(record.toLowerCase().contains("long command")){
                            mListener.writeData("admin " + adminpass);
                            record = "";
                        }else if (bootconfig.matcher(record).matches() || altbootconfig.matcher(record).matches()) {
                            setMessage("set new admin password");
                            setProgress(60);
                            mListener.writeData("exit");
                            state++;
                            record = "";
                            adminConfigured = true;
                        }
                        break;
                    case 7:
                        if (boot.matcher(record).matches()) {
                            if(sysImage != ""){
                                setMessage("loading system image " + this.sysImage);
                                mListener.writeData("load " + this.sysImage);
                                state++;
                                record = "";
                            }
                            else{
                                mListener.writeData("");
                                record = "";
                                state = 2;
                            }
                        }
                        break;
                    case 8:
                        if(record.toLowerCase().contains("login:")){
                            setMessage("attempting to log in...");
                            setProgress(90);
                            mListener.writeData("admin");
                            state++;
                            record = "";
                        }else{
                            setMessage("loading system image " + this.sysImage);
                        }
                        break;
                    case 9:
                        if(record.toLowerCase().contains("password:")){
                            setProgress(95);
                            mListener.writeData(adminpass);
                            state++;
                            record = "";
                        }
                        break;
                    case 10:
                        if(record.toLowerCase().contains("login incorrect")){
                            setProgress(100);
                            setMessage("Password Recovery Failed!");
                            state++;
                            record = "";
                        } else if(record.toLowerCase().contains("cisco nexus operating system")) {
                            setProgress(100);
                            setMessage("Password Recovery Complete!");
                            state++;
                            record = "";
                        }
                }
            }
            if(state == 1 && reloadConfirmed){
                mListener.writeData("!!!break");
                mListener.writeData("!!!ctrl]");
            }

        }
    }

}
