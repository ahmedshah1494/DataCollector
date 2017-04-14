package com.thesis.ahmed.datacollector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by Ahmed on 9/29/2016.
 */

public class ActivityMonitor{
    public static final int POSITION_FACE_DOWN = 0;
    public static final int POSITION_FLAT = 1;
    public static final int POSITION_HAND = 2;
    public static final int POSITION_EAR = 3;
    public static final int POSITION_STATIONARY = 4;
    public static final int POSITION_MOVING = 5;
    public static final int POSITION_POCKET = 7;
    int[] sensors = {Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LIGHT};
    private HashMap<Integer, ArrayList<float[]>> sensorData;
    private SensorManager mSensorManager;
    private final Semaphore m = new Semaphore(1);
    private boolean phoneMovedUp = false;
    private boolean phoneMovedDown = false;
    private boolean phoneAtEar = false;
    private boolean phoneInPocket = false;
    MainActivity activity;
    ActivityMonitor(MainActivity activity){
        this.activity = activity;
        sensorData = new HashMap<Integer, ArrayList<float[]>>();
        for (int i = 0; i < this.sensors.length; i++) {
            sensorData.put(sensors[i], new ArrayList<float[]>());
        }
    }

    public synchronized float[] getLatestSensorReading(int sensor){
        if (sensorData.get(sensor).size() > 0){
            return sensorData.get(sensor).get(Math.min(sensorData.get(sensor).size() - 1, 100));
        }
        return null;
    }
    private double mean(ArrayList<Float> e){
        double sum = 0.0;
        for (double d : e){
            sum += d;
        }
        return sum/e.size();
    }

    private double variance(ArrayList<Float> e){
        double sum = 0.0;
        double mean = this.mean(e);
        for (double d : e){
//            Log.d("Variance", (mean - d) + "");
            sum += (mean - d) * (mean - d);
        }
        return sum/e.size();
    }

    public synchronized void addSensorReading(int sensorType, float[] readings){
        sensorData.get(sensorType).add(readings);
        if (sensorData.get(sensorType).size() > 100){
            sensorData.get(sensorType).remove(0);
        }
        if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION){
            int len = sensorData.get(sensorType).size();
            List<float[]> last = sensorData.get(sensorType).subList(Math.max(0,len - 10), len - 1);
            boolean monoDec = true;
            boolean monoInc = true;
            int axis = 2;
            if (sensorData.get(Sensor.TYPE_GRAVITY).size() > 0){
                float[] gravity = sensorData.get(Sensor.TYPE_GRAVITY).get(sensorData.get(Sensor.TYPE_GRAVITY).size() - 1);
                float max = -10.0f;
                for(int i = 0; i<gravity.length; i++){
                    if (gravity[i] > max){
                        max = Math.abs(gravity[i]);
                        axis = i;
                    }
                }
            }
//            Log.d("axis", axis+"");
            for (int i = 0; (i < last.size() - 1) && (monoDec || monoInc) ; i++){
                if (last.get(i)[axis] < last.get(i+1)[axis]){
                    monoDec = false;
                }
                if (last.get(i)[axis] > last.get(i+1)[axis]){
                    monoInc = false;
                }
            }
            if (monoDec){
                phoneMovedDown = true;
                phoneMovedUp = false;
                Log.d("Postion", "phone moved down on axis " + axis);
            }
            if (monoInc){
                phoneMovedUp = true;
                phoneMovedDown = false;
                Log.d("Postion", "phone moved up on axis " + axis);
            }
        }
    }

    public synchronized double getReadingVariance(int sensorType, int axis){
        ArrayList<float[]> readings = sensorData.get(sensorType);
        if (readings.size() == 0) return -1;
        double[] vars = new double[3];
        ArrayList<Float> vals = new ArrayList<Float>();
        for (float[] l : readings){
//            String val = "" + l[axis]+"000000";
//            vals.add(Float.parseFloat(val.substring(0,6)));
            vals.add(l[axis]);
        }
        return variance(vals);
    }

    public synchronized double getReadingMean(int sensorType, int axis){
        ArrayList<float[]> readings = sensorData.get(sensorType);
        if (readings.size() == 0) return -1;
        double[] vars = new double[3];
        ArrayList<Float> vals = new ArrayList<Float>();
        for (float[] l : readings){
//            String val = "" + l[axis]+"000000";
//            vals.add(Float.parseFloat(val.substring(0,6)));
            vals.add(l[axis]);
        }

        return mean(vals);
    }

    public Position getPosition(){
        float[] readings;
        Position map = new Position();

        if (getReadingMean(Sensor.TYPE_GRAVITY,2) < -7) {
            map.face_down = true;
        }
        if ((readings = getLatestSensorReading(Sensor.TYPE_LINEAR_ACCELERATION)) != null &&
                (Math.abs(readings[0]) > 0.3 ||
                        Math.abs(readings[0]) > 0.3||
                        Math.abs(readings[0]) > 0.3)){
            map.moving = true;

        }

//            Check if phone is flat face up
        if (getReadingMean(Sensor.TYPE_GRAVITY,2) > 9){
            map.flat = true;
        }
//        If light <= 5 and gravityX <= 0 and 0 < gravityY < 9 then on ear
        if (getLatestSensorReading(Sensor.TYPE_LIGHT) != null &&
                getLatestSensorReading(Sensor.TYPE_LIGHT)[0] <= 5 &&
                getLatestSensorReading(Sensor.TYPE_GRAVITY)[0] <= 0 &&
                getLatestSensorReading((Sensor.TYPE_GRAVITY))[1] > 0 &&
                getLatestSensorReading((Sensor.TYPE_GRAVITY))[1] < 9 ){
            map.ear = true;
        }
//        If light <= 2 and gravityY < -5 then in pocket
        if (getLatestSensorReading(Sensor.TYPE_LIGHT) != null &&
                getLatestSensorReading(Sensor.TYPE_LIGHT)[0] <= 2 &&
                (getLatestSensorReading(Sensor.TYPE_GRAVITY)[0] <= -5)){
            map.pocket = true;
        }
////        If light == 0, phone not flat and moving then phone in pocket
//        if (phoneAtEar && phoneMovedDown){
//            map.ear = false;
//            phoneAtEar = false;
//        }
//        if (phoneAtEar){
//            map.ear = true;
//        }
//        if (phoneInPocket &&
//                sensorData.get(Sensor.TYPE_LIGHT).get(sensorData.get(Sensor.TYPE_LIGHT).size() - 1)[0] > 1){
//            map.pocket = false;
//            phoneInPocket = false;
//        }
//        if (phoneInPocket){
//            map.pocket = true;
//        }
//        else if (map.moving &&
//                (!map.flat &&
//                        (sensorData.get(Sensor.TYPE_LIGHT).size() > 0 &&
//                                getLatestSensorReading(Sensor.TYPE_LIGHT)[0] <= 2))){
//            double gravityMean = getReadingMean(Sensor.TYPE_GRAVITY,2);
//            Log.d("Phone Moved Up", phoneMovedUp + "");
//            Log.d("gravity mean", gravityMean +"");
//            if (phoneMovedUp &&
//                    gravityMean < 1.0 &&
//                    gravityMean > -3.0){
//                map.ear = true;
//                phoneAtEar = true;
//            }
//        }

//            Check if bright enough to take picture
            if (sensorData.get(Sensor.TYPE_LIGHT).size() > 0 &&
                    getLatestSensorReading(Sensor.TYPE_LIGHT)[0] >= 2){
//                check if phone is moving enough to cause significant blur
                double gReading;
                if ((gReading = getReadingMean(Sensor.TYPE_GRAVITY,2)) < 9 &&
                        gReading > 3.5 &&
                        ((readings = getLatestSensorReading(Sensor.TYPE_LINEAR_ACCELERATION)) != null &&
                        (Math.abs(readings[0]) > 0.05 ||
                                Math.abs(readings[1]) > 0.05||
                                Math.abs(readings[2]) > 0.05))){
                    map.hand = true;
                }
            }

        return map;
    }
}
