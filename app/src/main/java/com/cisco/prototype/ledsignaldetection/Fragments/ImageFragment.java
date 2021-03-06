package com.cisco.prototype.ledsignaldetection.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cisco.prototype.ledsignaldetection.BluetoothInterface;
import com.cisco.prototype.ledsignaldetection.R;

import org.w3c.dom.Text;

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
    public ArrayList<String> kst;
    public ArrayList<String> syst;
    public ArrayList<String> files;
    public String sysImage = "";
    public String kickImage = "";
    public String kickstartImageName = "";
    private String ksver = "";
    private boolean findKs = false;
    private String logForKs = "";
    private ArrayAdapter<String> kickAdapter;
    private ArrayAdapter<String> sysAdapter;
    private ArrayAdapter<String> fileAdapter;
    private boolean create;

    private Pattern recordP = Pattern.compile("(?s).*bootflash:(.*)[lL]oader>[^>]*");
    private Pattern weirdItem = Pattern.compile("^[^\\s]*$");
    private Pattern itemExtract = Pattern.compile("^*.\\s{1}([^\\s]*)$");
    private Pattern ks = Pattern.compile("^.*-kickstart.*\\.bin$");
    private Pattern system = Pattern.compile("^.*\\.bin$");
    private Pattern empty = Pattern.compile("");
    private Pattern version = Pattern.compile("^[^\\.]*\\.(\\d+.*)\\.bin$");

    private View view;
    private TextView infoText;
    private Button submit;
    private TextView terminal;
    private Spinner kickSpin;
    private Spinner sysSpin;
    private Spinner fileSpin;
    private RelativeLayout imageOptions;
    private ScrollView scroll;
    private TextView additional;

    public ImageFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        create = true;
        state = 0;
        super.onCreate(savedInstanceState);
        log = "";
        success = false;
        firstLoader = true;
        firstSys = true;
        kst = new ArrayList<String>();
        syst = new ArrayList<String>();
        files = new ArrayList<String>();
        mListener.onImageFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_image, container, false);
        infoText = (TextView) view.findViewById(R.id.image_text);
        terminal = (TextView) view.findViewById(R.id.image_terminal);
        submit = (Button) view.findViewById(R.id.submit_image);
        imageOptions = (RelativeLayout) view.findViewById(R.id.image_options);
        scroll = (ScrollView) view.findViewById(R.id.image_output);
        additional = (TextView)view.findViewById(R.id.additional);

        scroll.fullScroll(View.FOCUS_DOWN);
        terminal.setMovementMethod(new ScrollingMovementMethod());

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
    }

    public void setText(String message){infoText.setText(message);}

    public void setReadOutput(boolean bool){
        readOutput = bool;
    }

    public void read(String data){
        if(findKs){
            logForKs += data;
        }else{
            log += data;
        }
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
                            firstSys = true;
                            logForKs = "";
                            mListener.writeData("show version");
                            findKs = true;
                            state = 37;
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
                        setKsVersion();
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
                case 37:
                    Log.i("logks", logForKs);
                    if (firstSys && logForKs.contains("show version")) {
                        firstSys = false;
                        logForKs = logForKs.replace("(boot)#", "");
                    } else if(logForKs.toLowerCase().contains("more")) {
                        logForKs = logForKs.replace("more", "");
                        logForKs = logForKs.replace("More", "");
                        mListener.writeData("");
                    } else if (logForKs.contains("(boot)#") && logForKs.contains("show version")) {
                        readOutput = false;
                        findKs = false;
                        getKickstartVersion();
                    }
                    break;
                default:break;
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
            String item = "";
            if(weirdItem.matcher(piece.trim()).matches()){
                item = piece;
            }
            else{
                Matcher itemFinder = itemExtract.matcher(piece.trim());
                item = itemFinder.find() ? itemFinder.group(1) : piece.trim();
                Log.i("piece, item", piece + ", " + item);
            }
            if (ks.matcher(item.trim()).matches()) {
                Log.e("LEDApp", "found kickstart: " + item);
                kst.add(item.trim());
                kickAdapter.notifyDataSetChanged();
            } else if (system.matcher(item.trim()).matches()) {
                if(ksver != ""){
                    String blah = "";
                    Matcher kickMatch = version.matcher(item);
                    if(kickMatch.find()){
                        blah = kickMatch.group(1);
                    }
                    if(blah.equals(ksver)) syst.add(item.trim());
                } else syst.add(item.trim());
                sysAdapter.notifyDataSetChanged();
                Log.e("LEDApp", "found sys: " + item);
            }
            if(!empty.matcher(item.trim()).matches() && !(item.contains("(boot)") || item.contains("dir"))) files.add(item);
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

    public void getKickstartVersion(){
        Log.i("ksversion", logForKs);
        String[] lines = logForKs.split("\n");

        for(int i = 0; i < lines.length; i++){
            Log.i("lines", lines[i]);
            if (lines[i].contains("kickstart image")) {
                kickstartImageName = lines[i];
                break;
            }
        }

        if(kickstartImageName!= null ){
            String item = "";
            if(weirdItem.matcher(kickstartImageName.trim()).matches()){
                item = kickstartImageName;
            }
            else{
                Matcher itemFinder = itemExtract.matcher(kickstartImageName.trim());
                item = itemFinder.find() ? itemFinder.group(1) : kickstartImageName.trim();
                Log.i("piece, item", kickstartImageName + ", " + item);
            }

            Matcher itemFinder = itemExtract.matcher(item.trim());
            while(itemFinder.find()){
                itemFinder = itemExtract.matcher(item.trim());
                item = itemFinder.group(1);
            }
            item = item.trim();

            String[] stuff = item.split("/");

            item = stuff[stuff.length - 1];

            kickImage = item;

            Matcher kickMatch = version.matcher(item);
            if(kickMatch.find()){
                ksver = kickMatch.group(1);
            }
        }

        logForKs = "";
        onFileListObtained();
    }

    public void setKsVersion(){
        Matcher kickMatch = version.matcher(kickImage);
        if(kickMatch.find()){
            ksver = kickMatch.group(1);
        }

        ArrayList<String> temp = new ArrayList<String>();
        for(int k = 0; k < syst.size(); k++){
            temp.add(syst.get(k));
            Log.i("ksver", "temp: " + temp.get(k));
        }
        syst.clear();
        sysAdapter.notifyDataSetChanged();

        for(int i = 0; i < syst.size(); i++){
            Log.i("ksver", "syst cleared: " + syst.get(i));
        }

        for(int i = 0; i < temp.size(); i++){
            String blah = "";
            kickMatch = version.matcher(temp.get(i));
            if(kickMatch.find()){
                blah = kickMatch.group(1);
            }
            if(blah.equals(ksver)){
                syst.add(temp.get(i));
                Log.i("ksver", "syst equal: " + syst.get(syst.size() - 1));
            }
        }
        sysAdapter.notifyDataSetChanged();

        for(int i = 0; i < syst.size(); i++){
            Log.i("ksver", "syst repopulated: " + syst.get(i));
        }
    }

    public void setAdditional(String string){additional.setText(string);}

    public void onResume(){
        super.onResume();
        if(!create){
            mListener.setDataForImage();
        } else create = false;
    }
}