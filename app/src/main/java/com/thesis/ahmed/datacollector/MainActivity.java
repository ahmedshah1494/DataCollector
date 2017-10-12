package com.thesis.ahmed.datacollector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback{
    int AUDIO_SAMPLING_RATE = 30000;
    int AUDIO_AMPLITUDE_SAMPLING_RATE = 1000;
    int AUDIO_AMPLITUDE_SAMPLE_LENGTH = 500;
    int AUDIO_SAMPLE_LENGTH = 3000;
    int IMAGE_SAMPLING_RATE = 60000;
    int AUDIO_SAMPLING_DELAY = 60000;
    int IMAGE_SAMPLING_DELAY = 60000;
    int SENSOR_SAMPLING_RATE = 250;

    AudioRecorder recorder;
    boolean is_recording = false;
    Handler handler = new Handler(Looper.getMainLooper());
    int[] sensors = {Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LIGHT};
    HashMap<Sensor, TextView> sensorMap = new HashMap<Sensor, TextView>();
    SensorManager mSensorManager;
    ActivityMonitor mActivityMonitor;
    TextView positionLabel;
    Position position;
    boolean pictureDone = false;
    boolean recordingDone = false;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    ThreadPoolExecutor mThreadPool;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    LinkedBlockingQueue<Runnable> Q;
    String[] mFileList;
    String mChosenFile = "null";
    TextView statusTV;
    TextView roomTV;
    CommunicationsModule comms;
    double amplitude;
    private CameraView mCameraView;
    public CameraModule mCameraModule;
    long appStartTime;
    private static final String DATA_COLLECTOR_FOLDER = "DataCollector";
    private String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_LOGS,
    };
    int nRecs = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appStartTime = System.currentTimeMillis();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mCameraView = (CameraView) findViewById(R.id.camera);
        Q = new LinkedBlockingQueue<Runnable>();
        mThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES,
                                                NUMBER_OF_CORES,
                                                10,
                                                TimeUnit.SECONDS,
                                                Q);
        recorder = new AudioRecorder("DataCollector/");
        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/"+DATA_COLLECTOR_FOLDER );
        File logDirectory = new File( appDirectory + "/log" );
        File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );
        File commsDir = new File(appDirectory + "/comms");
        // create app folder
        if ( !appDirectory.exists() ) {
            appDirectory.mkdir();
        }

        // create log folder
        if ( !logDirectory.exists() ) {
            logDirectory.mkdir();
        }
        if ( !commsDir.exists() ) {
            commsDir.mkdir();
        }
        comms = new CommunicationsModule("workhorse.lti.cs.cmu.edu", 9999, this);
        comms.setQueueLocation(commsDir);

        ActivityCompat.requestPermissions(this,
                permissions,
                permissions.length);


        // clear the previous logcat and then write the new one to the file
        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
            process = Runtime.getRuntime().exec("logcat -f " + logFile);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        mActivityMonitor = new ActivityMonitor(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mCameraModule = new CameraModule(this);
        mCameraModule.openCamera();
        mCameraModule.setFolder(this.DATA_COLLECTOR_FOLDER+"/"+mChosenFile);

        for (int i = 0; i < this.sensors.length; i++) {
            Sensor sensor = mSensorManager.getDefaultSensor(this.sensors[i]);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

            TextView textView_head = new TextView(this);
//            textView_head.setText(sensor.getName());
            textView_head.setTextSize(10);

            TextView textView_val = new TextView(this);
            textView_val.setTextSize(8);
            textView_val.setPadding(10, 0, 0, 0);
            textView_val.setMaxLines(10);
            textView_val.setSingleLine(false);

            LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
//            ll.addView(textView_head);
//            ll.addView(textView_val);

            this.sensorMap.put(sensor, textView_val);
        }
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
//        positionLabel = new TextView(this);
//        ll.addView(positionLabel);
        positionLabel = (TextView) findViewById(R.id.position);
        roomTV = (TextView) findViewById(R.id.room);
        Button recordB = new Button(this);
        recordB.setText("Record Audio");
        ll.addView(recordB);
        Button shootB = new Button(this);
        shootB.setText("Take Picture");
        ll.addView(shootB);
        Button sendB = new Button(this);
        sendB.setText("Send Files");
        ll.addView(sendB);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton settingsFab = (FloatingActionButton) findViewById(R.id.settingsFAB);
//        statusTV = new TextView(this);
//        statusTV.setMaxLines(1);
//        statusTV.setMinLines(1);
//        statusTV.setText("Status:");
        statusTV = (TextView) findViewById(R.id.status);
//        ll.addView(statusTV);
        recordB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                EditText dur = (EditText) findViewById(R.id.duration);
                recordAudio(System.currentTimeMillis()+"", 3000);
            }
        });
        shootB.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                mCameraModule.takePicture(false);
            }
        });
//        shootB.setOnClickListener(mOnClickListener);
        fab.setOnClickListener(new FolderClickListener(this));
        settingsFab.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                changeSettings(v);
            }
        });


//        For benchmarking mic power consumption
//        ==================================================================
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                int len = 6;
//                if (nRecs < 30/len){
//                    recordAudio(System.currentTimeMillis()+"", len);
//                    nRecs += 1;
//                    handler.postDelayed(this, 500 + len*1000);
//                }
//            }
//        });
//        ===================================================================
        handler.post(new Runnable() {
            @Override
            public void run() {
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        position = mActivityMonitor.getPosition();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                positionLabel.setText(position.toString());
                            }
                        });
                        doAction();
                    }
                });
                handler.postDelayed(this, SENSOR_SAMPLING_RATE);
            }
        });
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!is_recording){
                    is_recording = true;
                    recorder.saveRecording = false;
                    recorder.startRecording();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            is_recording=false;
                            recorder.stopRecording(true);
                            recorder.saveRecording = true;
                            amplitude = recorder.maxAmp;
                            Log.d("amp", amplitude+"");
                        }
                    }, AUDIO_AMPLITUDE_SAMPLE_LENGTH);
                }
                handler.postDelayed(this, AUDIO_AMPLITUDE_SAMPLING_RATE);
            }
        });

        sendB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            sendFilesCurrDir();
            }
        });
    }

    public void changeSettings(View view){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    private void sendFilesCurrDir(){
        if (mChosenFile != null){
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath, DATA_COLLECTOR_FOLDER+"/"+mChosenFile);
            String[] mFileList = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return !sel.isDirectory();
                }
            });
            String[] files = new String[mFileList.length];
            for(int i = 0; i < mFileList.length; i++){
                files[i] = (file.getAbsolutePath() + "/" + mFileList[i]);
            }
            comms.sendFiles(files);
        }
    }
    private void recordAudio(final String filename, final int duration){
        if (is_recording){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordAudio(filename,duration);
                }
            }, 10);
            return;
        }
        if (!is_recording){
            recorder.startRecording();
            statusTV.setText("Status: Recording for "+duration+"s ...");
            is_recording = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String fname = recorder.stopRecording(false);
                    statusTV.setText("Status: Finished Recording");
                    Log.d("max amp", recorder.maxAmp + "");
                    is_recording = false;
                    comms.enq(fname);
                }
            }, duration);
        }
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void doAction(){

        if (mCameraModule.lastCaptureHistogram != null &&
                mCameraModule.lastCaptureHistogram.percentageLessThanVal(50) > 90){
            Log.d("Last Picture", "Dark");
            pictureDone = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pictureDone = false;
                }
            }, IMAGE_SAMPLING_DELAY);
        }

        if (!pictureDone && !position.flat && ! position.face_down && !position.moving && !position.pocket){
            pictureDone = true;
            mThreadPool.execute(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    mCameraModule.takePicture(false);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pictureDone = false;
                        }
                    }, IMAGE_SAMPLING_RATE);
                }
            });
        }
        float[] lightReadings = mActivityMonitor.getLatestSensorReading(Sensor.TYPE_LIGHT);

        if (position.pocket ||
                lightReadings != null &&
                lightReadings[0] < 2 &&
                !position.moving){
            recordingDone = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordingDone = false;
                }
            }, AUDIO_SAMPLING_DELAY);

        }

        if (!is_recording && !recordingDone && amplitude > 10000.0){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recordAudio(mChosenFile+"_"+System.currentTimeMillis()+"", AUDIO_SAMPLE_LENGTH);
                    recordingDone = true;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            recordingDone = false;
                        }
                    }, AUDIO_SAMPLING_RATE);
                }
            });
        }

        comms.sendFiles();
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String s = "";
                float[] readings = event.values.clone();
                mActivityMonitor.addSensorReading(event.sensor.getType(), readings);

                for (int i = 0; i < event.values.length; i++) {
                    s += event.values[i] + "(" + mActivityMonitor.getReadingVariance(event.sensor.getType(), i) + "," + mActivityMonitor.getReadingMean(event.sensor.getType(),i) + ")" + "\n";
                }
                final String S = s + "";
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView t = sensorMap.get(event.sensor);
                        t.setText(S);
                    }
                });

            }
        });

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class FolderClickListener implements View.OnClickListener{
        File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File selectedFolder;
        Activity a;
        public FolderClickListener(Activity a){
            this.a = a;
        }
        @Override
        public void onClick(View v) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory();
                }
            };
            mFileList = file.list(filter);
            AlertDialog.Builder builder = new AlertDialog.Builder(a);
            builder.setTitle("Select Folder");
            builder.setItems(mFileList, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mChosenFile = mFileList[which];
                        roomTV.setText("Room: "+ mChosenFile);
                        updateFolderData();
                        selectedFolder = new File(file.getPath(),mChosenFile);
                        String[] filesToBeQed = selectedFolder.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String s) {
                                String ext = s.substring(s.length() - 3);
                                if (ext == "wav" ||
                                        ext == "jpg"){
                                    return true;
                                }
                                return false;
                            }
                        });
                        comms.enq(filesToBeQed);
                    }
                });
            builder.setNeutralButton("Create New Folder", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(a);
                    builder.setTitle("Type Folder Name");
                    final EditText folderName = new EditText(a);
                    builder.setView(folderName);
                    builder.setNeutralButton("Create Folder", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            File newFolder = new File(file.getPath(),folderName.getText().toString());
                            Log.d("in create folder", newFolder.toString());
                            Log.d("in create folder",((Boolean)newFolder.mkdirs()).toString());
                            mChosenFile = folderName.getText().toString();
                            roomTV.setText("Room: " + mChosenFile);
                            updateFolderData();
                        }
                    });
                    AlertDialog newFolderDialog = builder.create();
                    newFolderDialog.show();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void updateFolderData(){
        this.recorder.setFolder(DATA_COLLECTOR_FOLDER+"/"+mChosenFile);
        this.mCameraModule.setFolder(DATA_COLLECTOR_FOLDER+"/"+mChosenFile);
    }

    public void updateStatus(final String s){
        handler.post(new Runnable() {
            @Override
            public void run() {
                statusTV.setText("Status: "+s);
            }
        });
    }

}
