package com.thesis.ahmed.datacollector;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Ahmed on 10/3/2016.
 */

public class CommunicationsModule {
    String ServerAddr;
    MainActivity context;
    int port;
    int fileUploadCount;
    int fileQueueCount;
    String queueLocation;
    boolean isUploading = false;
    boolean useData = false;
    CommunicationsModule(String serverAddr, int port, MainActivity context){
        this.ServerAddr = serverAddr;
        this.context = context;
        this.port = port;
    }

    public void setQueueLocation(String filepath){
        this.queueLocation = filepath;
    }

    public boolean isConnectedToNetwork(){
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }


    public void sendFiles(String[] files){
        fileUploadCount = 0;
        fileQueueCount = files.length;
        for (String file : files){
            sendFile(file);
        }
    }

    public void sendFile(final String filepath){
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                UploadFile(filepath);
                return null;
            }

            @Override
            protected void onPostExecute(Object o){
                fileUploadCount ++;
                Log.d("Uploaded", filepath);
                context.updateStatus("Uploding "+fileUploadCount+"/"+fileQueueCount
                );
            }
        };
        task.execute();

    }
    public void UploadFile(String filepath){
        try {
            // Set your file path here
            FileInputStream fstrm = new FileInputStream(filepath);
            String filename = filepath.split("/")[filepath.split("/").length - 1];

            // Set your server page url (and the file title/description)
            HttpFileUpload hfu = new HttpFileUpload("http://10.27.9.25:8000/sherlockserver/uploadSample", filename,"{\"type\": \"audio\", \"location\": \""+context.mChosenFile+"\"}");

            hfu.Send_Now(fstrm);

        } catch (FileNotFoundException e) {
            // Error: File not found
        }
    }
}
