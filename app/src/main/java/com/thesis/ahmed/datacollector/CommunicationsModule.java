package com.thesis.ahmed.datacollector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ahmed on 10/3/2016.
 */

public class CommunicationsModule {
    String ServerAddr;
    MainActivity context;
    int port;
    int fileUploadCount;
    int fileQueueCount;
    File queueLocation;
    Queue<String> uploadQueue;
    boolean isUploading = false;
    boolean useData = false;
    CommunicationsModule(String serverAddr, int port, MainActivity context){
        this.ServerAddr = serverAddr;
        this.context = context;
        this.port = port;
        this.uploadQueue = new ConcurrentLinkedQueue<String>();
    }

    public void setQueueLocation(File filepath){
        this.queueLocation = new File(filepath + "./uploadQueue");
    }

    public void enq(String filename){
        uploadQueue.add(filename);
    }

    public boolean isConnectedToNetwork(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean useData = sharedPref.getBoolean("pref_key_useData", false);

        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (!useData &&
                    networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public void sendFiles(){
        while (! uploadQueue.isEmpty()){
            Log.d("UploadQ",uploadQueue.peek());
            Log.d("Network",isConnectedToNetwork() + "");
            if (isConnectedToNetwork()){
                String fname = uploadQueue.poll();
                sendFile(fname);
                Log.d("Sending File", fname);
            }
        }
    }

    public void sendFiles(String[] files){
        fileUploadCount = 0;
        fileQueueCount = files.length;
        for (String file : files){
            Log.d("sendFiles", file);
            sendFile(file);
        }
    }

    public void sendFile(final String filepath){
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                if (! UploadFile(filepath)){
                    uploadQueue.add(filepath);
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Object o){
                fileUploadCount ++;
                Log.d("Uploaded", filepath);
                context.updateStatus("Uploading "+fileUploadCount+"/"+fileQueueCount
                );
            }
        };
        AsyncTask T = task.execute();
        try {
            if ((Boolean) T.get()){
                File f = new File(filepath);
                f.delete();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            uploadQueue.add(filepath);
        } catch (ExecutionException e) {
            e.printStackTrace();
            uploadQueue.add(filepath);
        }
    }
    public boolean UploadFile(String filepath){
        try {
            // Set your file path here
            FileInputStream fstrm = new FileInputStream(filepath);
            String filename = filepath.split("/")[filepath.split("/").length - 1];

            // Set your server page url (and the file title/description)
            HttpFileUpload hfu = new HttpFileUpload("" +
                    "uploadSample", filename,"{\"type\": \"audio\", \"location\": \""+context.mChosenFile+"\"}");

            return hfu.Send_Now(fstrm);

        } catch (FileNotFoundException e) {
            // Error: File not found
        }
        return true;
    }
}
