package com.infinitytech.classicalmix;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements ServiceConnection, Pedometer.OnDataChangeListener {

    public final String TAG = "Pedometer";

    private TextView databaseTv;
    private TextView sensorTv;
    private Pedometer.MBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseTv = (TextView) findViewById(R.id.sql_data_tv);
        sensorTv = (TextView) findViewById(R.id.sensor_data_tv);
        Intent intent = new Intent(this, Pedometer.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i("STEP", "Service Connected");
        binder = (Pedometer.MBinder) service;
        binder.getService().setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("STEP", "Service DisConnected");
    }

    @Override
    public void onDataChange(float step) {
        Bundle data = new Bundle();
        data.putFloat("Step", step);
        Message msg = new Message();
        msg.setData(data);
        handler.sendMessage(msg);
    }

    private Handler handler = new UIHandler(this);

    static class UIHandler extends Handler{
        private WeakReference<MainActivity> mActivity;

        UIHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        };

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mActivity.get().databaseTv.setText(String.format(
                    mActivity.get().getResources().getString(R.string.databaseText),
                    msg.getData().getFloat("StepDatabase", 0f)));
            mActivity.get().sensorTv.setText(String.format(
                    mActivity.get().getResources().getString(R.string.databaseText),
                    msg.getData().getFloat("StepSensor", 0f)));

        }
    }
}
