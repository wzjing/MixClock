package com.infinitytech.classicalmix;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class Pedometer extends Service implements SensorEventListener {

    private final String TAG = "Pedometer";

    private static int mFrequency = 1;
    private OnDataChangeListener mListener;
    private SensorManager sensorManager;
    private int stepCount = 0;
    private boolean running;

    public Pedometer() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service Create");
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor pedometer = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, pedometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        runProgram();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        runProgram();
        return new MBinder();
    }

    private void runProgram(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                running = true;
//                while (running) {
//                    try {
//                        Thread.sleep(2000);
//                        Log.i(TAG, "Running:"+System.currentTimeMillis());
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
    }

    @Override
    public void onDestroy() {
        running = false;
        sensorManager.unregisterListener(this);
        Log.i(TAG, "Service Destroyed");
        super.onDestroy();
    }

    public class MBinder extends Binder {
        void setFrequency(int frequency) {
            mFrequency = frequency;
        }
        Pedometer getService(){
            return Pedometer.this;
        }
    }

    public void setListener(OnDataChangeListener mListener) {
        this.mListener = mListener;
    }

    interface OnDataChangeListener {
        void onDataChange(float step);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_COUNTER:
                if (mListener != null) {
                    mListener.onDataChange(event.values[0]);
                }
                for (int i=0;i<event.values.length;i++)
                    Log.i(TAG, String.format("Step Values:%d = %f", i, event.values[i]));
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                for (int i=0;i<event.values.length;i++)
                    Log.i(TAG, String.format("Detector Values:%d = %f", i, event.values[i]));
                break;
            default:
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
