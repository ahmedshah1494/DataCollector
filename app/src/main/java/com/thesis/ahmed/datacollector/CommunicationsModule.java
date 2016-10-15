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
    boolean isUploading = false;
    CommunicationsModule(String serverAddr, int port, MainActivity context){
        this.ServerAddr = serverAddr;
        this.context = context;
        this.port = port;
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

    public String getStatusString(){
        if (isUploading){
            return "Uploding "+fileUploadCount+"/"+fileQueueCount;
        }
        else{
            return "Idle";
        }
    }
    private Socket connect(){
        try {
            if (isConnectedToNetwork()) {
                Socket sock = new Socket(ServerAddr,port);
                return sock;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] checkFilesOnServer(String[] files){
//      TODO:   need to implement
        String s = "##CheckFiles\n\n";
        for (String file: files){
            s += file+"\n";
        }
        s += "//**//";
        Socket servSoc = connect();
        try {
            DataOutputStream out = new DataOutputStream(servSoc.getOutputStream());
            out.writeUTF(s);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
                _sendFile_(filepath);
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
    private void _sendFile_(String filepath){
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        File file = new File(filepath);
        long size = file.length();
        String[] split = filepath.split("/");
        String filename = split[split.length - 1];
        String folder = split[split.length - 2];
        filename = filename.replace("\n","");
//        String filename = split[split.length - 1];
        String fileData = "";
        byte[] buf = new byte[1000];
        int nRead = 0;
        int total = 0;
        try {
            fis = new FileInputStream(filepath);
            bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            Socket sock = connect();
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeUTF(folder.trim());
            dos.writeUTF("\n");
            dos.writeUTF(filename.trim());
            dos.writeUTF("\n");
            dos.writeUTF(size+"");
            dos.writeUTF("\r\n\r\n");

            while ((nRead = dis.read(buf)) != -1){
                dos.write(buf);
                fileData += String.valueOf(Arrays.copyOfRange(buf, 0, nRead));
                total += nRead;
                buf = new byte[1000];
            }
            dos.close();
            dis.close();
            if (sock.isClosed()){
                sock = connect();
            }
            dis = new DataInputStream(sock.getInputStream());
            dis.read(buf);
            sock.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
