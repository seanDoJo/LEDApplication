package com.cisco.prototype.ledsignaldetection;

import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.tftp.TFTP;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

/**
 * Created by seandonohoe on 7/28/15.
 */
public class TFTPUtil extends Thread {
    private FTPClient tClient;
    private boolean running = true;
    public TFTPUtil(){
        this.tClient = new FTPClient();
        this.tClient.setDataTimeout(20000);
    }

    public void run(){
        while(running){}
    }

    public void transfer(String fileName, String remoteFile, String ip, String username, String password, CountDownLatch latch){
        FileInputStream input = null;
        Pattern ipPat = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");
        try{
            input = new FileInputStream(fileName);
        }catch(FileNotFoundException e){e.printStackTrace();}
        try{
            if(ipPat.matcher(ip).matches()){
                tClient.connect(InetAddress.getByName(ip));
            }
            else{
                tClient.connect(ip);
            }
            if(!tClient.login(username, password)){
                Log.i("LEDApp", "login unsuccessful");
            }
            if (FTPReply.isPositiveCompletion(tClient.getReplyCode())){
                tClient.setFileType(FTP.ASCII_FILE_TYPE);
                tClient.enterLocalPassiveMode();
                tClient.storeFile(remoteFile, input);
            }
            tClient.disconnect();
        }catch(UnknownHostException e){e.printStackTrace();}catch(IOException e){e.printStackTrace();}
        try{
            if(input != null)input.close();
        }catch(IOException e){e.printStackTrace();}
        if(latch != null)latch.countDown();
    }

    public void close(){
        running = false;
    }
}
