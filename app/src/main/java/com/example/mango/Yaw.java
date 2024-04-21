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
import java.util.StringTokenizer;

public class Yaw {
    private DataLib dataLib;
    private ProgressDialog progressDialog;
    private TextView yawText;
    private Context context;
    private double[] accData, magData;
    private LinkedList<Double> yawQueue;
    private SensorManager sensorManager;
    private Sensor accSensor, magSensor;

    public Yaw(DataLib dataLib, Context context, TextView yawText) {
        this.dataLib = dataLib;
        this.context = context;
        this.yawText = yawText;

        accData = new double[3];
        magData = new double[3];
        yawQueue = new LinkedList<>();

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private SensorEventListener accListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accData[0] = event.values[0];
            accData[1] = event.values[1];
            accData[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener magListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            magData[0] = event.values[0];
            magData[1] = event.values[1];
            magData[2] = event.values[2];

            float[] accDataF = new float[3];
            float[] magDataF = new float[3];
            for (int i = 0; i < 3; i++) {
                accDataF[i] = (float) accData[i];
                magDataF[i] = (float) magData[i];
            }

            float[] R = new float[9];
            float[] values = new float[3];
            SensorManager.getRotationMatrix(R, null, accDataF, magDataF);
            sensorManager.getOrientation(R, values);

            yawQueue.addLast((double) values[0]);

            updateProgress(yawQueue.size());

            Log.d("yaw", "航向角数据" + yawQueue.size());
            if (yawQueue.size() == 100) {
                for (double value : yawQueue) dataLib.initAngle += value;
                dataLib.initAngle /= yawQueue.size();

                yawQueue = new LinkedList<>();

                yawText.setText("初始航向角: " + String.format("%.6f", Math.toDegrees(dataLib.initAngle)));

                showMsg("获取初始航向角成功");

                sensorManager.unregisterListener(accListener, accSensor);
                sensorManager.unregisterListener(magListener, magSensor);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void getYaw() {
        showProgress(context);

        sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(magListener, magSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void showProgress(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("获取初始航向中，请尽量远离电子设备");
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
