package com.example.mango;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

public class Gravity {
    private DataLib dataLib;
    private ProgressDialog progressDialog;
    private TextView gravText;
    private Context context;
    private double[] gravData;
    private LinkedList<Double> gravQueue;
    private SensorManager sensorManager;
    private Sensor gravSensor;

    public Gravity(DataLib dataLib, Context context, TextView gravText){
        this.dataLib = dataLib;
        this.context = context;
        this.gravText = gravText;

        gravData = new double[3];
        gravQueue = new LinkedList<>();

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gravSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private SensorEventListener gravListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            gravData[0] = event.values[0];
            gravData[1] = event.values[1];
            gravData[2] = event.values[2];

//            double gravModData = Math.sqrt(Math.pow(gravData[0], 2.0) + Math.pow(gravData[1], 2.0) + Math.pow(gravData[2], 2.0));

            gravQueue.addLast(gravData[2]);

            updateProgress(gravQueue.size());

            Log.d("accSensor", "重力数据" + gravQueue.size());
            if (gravQueue.size() == 100) {
                for (double value : gravQueue) dataLib.grav += value;
                dataLib.grav /= gravQueue.size();

                gravQueue = new LinkedList<>();

                gravText.setText("平均重力值: " + String.format("%.6f", dataLib.grav));

                showMsg("获取平均重力值成功");

                sensorManager.unregisterListener(gravListener, gravSensor);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void getGravity() {
        showProgress(context);

        sensorManager.registerListener(gravListener, gravSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void showProgress(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("获取平均重力中，请保持手机水平稳定");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false); // 禁止取消
        progressDialog.show();
    }

    public void updateProgress(int progress) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
            if (progress >= 100) {
                progressDialog.dismiss();
            }
        }
    }
    private void showMsg(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
