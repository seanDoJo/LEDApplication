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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import org.w3c.dom.Text;

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
    private int os = 0;
    private boolean toAutoBootConf = false;
    private boolean outputEnabled = false;
    private ArrayList<String> kst;
    private ArrayList<String> syst;
    private ArrayAdapter<String> kickAdapter;
    private ArrayAdapter<String> sysAdapter;
    private String sysImage = "";
    private String kickImage = "";

    private Pattern yesNo = Pattern.compile("(?s).*\\[yes/?no\\].*");
    private Pattern gt = Pattern.compile("(?s).*>[^>]*");
    private Pattern id = Pattern.compile("(?s)[^#]+#{1}[^#]*");
    private Pattern brackets = Pattern.compile("(?s).*\\[.*\\].*");

    private Pattern boot = Pattern.compile("(?s).*[sS]witch\\(boot\\)#[^#]*");
    private Pattern bootconfig = Pattern.compile("(?s).*[sS]witch\\(boot-config\\)#[^#]*");
    private Pattern loader = Pattern.compile("(?s)[^>]*[lL]{1}oader>[^>]*");

    private Pattern kickstart = Pattern.compile("^.*-kickstart.*\\.bin$");
    private Pattern system = Pattern.compile("^.*\\.bin$");
    private Pattern version = Pattern.compile("^[^\\.]*\\.(\\d+.*)\\.bin$");

    private Pattern recordP = Pattern.compile("(?s).*bootflash:(.*)[lL]oader>[^>]*");
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

    public void updateProgress(){
        if(pBar.getProgress() >= 98)pBar.setProgress(100);
        else pBar.setProgress(pBar.getProgress() + 7);
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

    public void selectBoot(boolean pairs){
        RelativeLayout imageSelection = (RelativeLayout)getActivity().findViewById(R.id.passwordImages);
        imageSelection.setVisibility(SurfaceView.VISIBLE);
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
            if(record.toLowerCase().contains("yes/no")){
                mListener.writeData("y");
                record = "";
            }
            else {
                switch (state) {
                    case 0:
                        mListener.writeData("reload");
                        state++;
                        record = "";
                    case 1:
                        if (record.toLowerCase().contains("booting kickstart image")) {
                            mListener.writeData("!!!break");
                            state++;
                            record = "";
                        }else if (record.toLowerCase().contains("post completed")) {

                            mListener.writeData("!!!ctrl]");
                            state = 4;
                            record = "";
                        }else if(loader.matcher(record).matches()){
                            mListener.writeData("");
                            state++;
                            record = "";
                        }else if(boot.matcher(record).matches()){
                            state = 4;
                            mListener.writeData("");
                            record = "";
                        }
                        break;
                    case 2:
                        if (loader.matcher(record).matches() || boot.matcher(record).matches()) {
                            //load kickstart stuff
                            mListener.writeData("dir bootflash:");
                            state++;
                            record = "";
                        }
                        break;
                    case 3:
                        if (recordP.matcher(record).matches()) {
                            //load kickstart stuff
                            Matcher recordM = recordP.matcher(record);
                            if(recordM.find()){
                                record = recordM.group(1);
                                Log.e("LEDApp", record);
                            }
                            ArrayList<String[]> imagePairs = new ArrayList<>();
                            record = record.replaceAll("\\[\\d+m--More-- \\[\\d+m", "");
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
                                mListener.writeData("boot " + kst.get(0).trim());
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
                                                if(Integer.parseInt(sysverBreak[x]) != Integer.parseInt(ksverBreak[x])){
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
                                    mListener.writeData("boot " + imagePairs.get(0)[0]);
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
                                kst.clear();
                                syst.clear();
                                for(String item : directoryContents){
                                    kst.add(item.trim());
                                    syst.add(item.trim());
                                }
                                kickAdapter.notifyDataSetChanged();
                                sysAdapter.notifyDataSetChanged();
                                selectBoot(false);
                            }
                            state++;
                            record = "";
                        } else if(boot.matcher(record).matches()){
                            Log.i("LEDApp", "Hit boot matcher");
                            record = record.replaceAll("\\[\\d+m--More-- \\[\\d+m", "");
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
                                Log.e("LEDApp", item);
                                if (system.matcher(item.trim()).matches()) {
                                    syst.add(item.trim());
                                }
                            }
                            if(syst.size() == 1){
                                this.sysImage = syst.get(0).trim();
                                mListener.writeData("boot " + syst.get(0).trim());
                            } else {
                                syst.clear();
                                for (String piece : directoryContents) {
                                    String item = piece;
                                    if(!weirdItem.matcher(piece.trim()).matches()){
                                        Matcher itemFinder = itemExtract.matcher(piece.trim());
                                        item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                                    }
                                    Log.i("LEDApp", item);
                                    syst.add(item.trim());
                                }
                                sysAdapter.notifyDataSetChanged();
                                selectSys();
                            }
                            state = 8;
                            record = "";
                        }
                        break;
                    case 4:
                        if (boot.matcher(record).matches()) {
                            mListener.writeData("configure terminal");
                            state++;
                            record = "";
                        }else if(loader.matcher(record).matches()){
                            mListener.writeData("");
                            state = 2;
                            record = "";
                        }
                        break;
                    case 5:
                        if (bootconfig.matcher(record).matches()) {
                            mListener.writeData("admin password " + adminpass);
                            state++;
                            record = "";
                        }
                        break;
                    case 6:
                        if (bootconfig.matcher(record).matches()) {
                            mListener.writeData("exit");
                            state++;
                            record = "";
                            adminConfigured = true;
                        }
                        break;
                    case 7:
                        if (boot.matcher(record).matches()) {
                            if(sysImage != ""){
                                state++;
                            }
                            else{
                                state = 2;
                            }
                        }
                        break;
                }
            }

        }
        if(record.length() >= 1000){
            record = "";
        }
    }

}
