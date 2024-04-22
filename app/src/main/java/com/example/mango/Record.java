package com.example.mango;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.sql.DataTruncation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Record {
    private DataLib dataLib;
    private Context context;
    private android.widget.Button startBtn, pauseBtn, stopBtn;
    private AMap aMap;
    private Polyline polyline;
    private List<LatLng> latLngs;
    private TextView ns, ms;
    private TextView trueLatlngText, preLatlngText, ENUText;
    private TextView yawGyroText, yawMagText;
    private TextView accModText, accFilterText;
    private TextView stepNumText, stepLengthText;

    private SensorManager sensorManager;
    private Sensor accSensor, gyroSensor, magSensor;

    private StringBuilder dataString, filterString, positionString;
    private boolean pauseBtnStatus;
    private double[] accData, gyroData, magData, attAngle;
    private long timestamp, lastTimestamp;
    private int stepNum;
    private double N, E;

    private AttitudeAngle attitudeAngle;
    private Filter filter;
    private StepDetect stepDetect;
    private PosTrans posTrans;
    public AMapLocationClient mLocationClient;

    public Record(AMap aMap,
                  AMapLocationClient mLocationClient,
                  DataLib dataLib,
                  Context context,
                  android.widget.Button startBtn, android.widget.Button pauseBtn, android.widget.Button stopBtn,
                  TextView trueLatlngText,
                  TextView preLatlngText,
                  TextView ENUText,
                  TextView ns, TextView ms,
                  TextView yawGyroText, TextView yawMagText,
                  TextView accModText, TextView accFilterText,
                  TextView extremumText, TextView PVText,
                  TextView peakMeanText, TextView valleyMeanText,
                  TextView peakMSDText, TextView valleyMSDText,
                  TextView peakText, TextView valleyText,
                  TextView deltaTimeText, TextView lastDeltaTimeText,
                  TextView stepNumText, TextView stepLengthText) {
        this.aMap = aMap;
        this.dataLib = dataLib;
        this.context = context;
        this.startBtn = startBtn;
        this.pauseBtn = pauseBtn;
        this.stopBtn = stopBtn;
        this.ns = ns;
        this.ms = ms;
        this.trueLatlngText = trueLatlngText;
        this.preLatlngText = preLatlngText;
        this.ENUText = ENUText;
        this.yawGyroText = yawGyroText;
        this.yawMagText = yawMagText;
        this.accModText = accModText;
        this.accFilterText = accFilterText;
        this.stepNumText = stepNumText;
        this.stepLengthText = stepLengthText;
        this.mLocationClient = mLocationClient;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        dataString = new StringBuilder();
        filterString = new StringBuilder();
        positionString = new StringBuilder();
        pauseBtnStatus = false;
        accData = new double[3];
        gyroData = new double[3];
        magData = new double[3];
        attAngle = new double[3];
        timestamp = 0;
        lastTimestamp = 0;
        stepNum = 0;
        N = 0;
        E = 0;
        latLngs = new ArrayList<>();
        polyline = null;

        attitudeAngle = new AttitudeAngle();
        filter = new Filter();
        stepDetect = new StepDetect(extremumText, PVText,
                peakMeanText, valleyMeanText,
                peakMSDText, valleyMSDText,
                peakText, valleyText,
                deltaTimeText, lastDeltaTimeText);
        posTrans = new PosTrans(dataLib);
    }

    private SensorEventListener accListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            ns.setText("ns: " + String.format("%d", event.timestamp));
            ms.setText("ms: " + String.format("%d", System.currentTimeMillis()));

            Log.d("accSensor", "加速度数据");

            String accString = String.format("%s, %d, %.6f, %.6f, %.6f",
                    "acc", event.timestamp, event.values[0], event.values[1], event.values[2]);
            dataString.append(accString).append("\n");

            accData[0] = event.values[0];
            accData[1] = event.values[1];
            accData[2] = event.values[2];

//            double accModData = Math.sqrt(Math.pow(accData[0], 2.0) + Math.pow(accData[1], 2.0) + Math.pow(accData[2], 2.0)) - dataLib.grav;
            double accZData = accData[2] - dataLib.grav;
            double HMAFilterData = filter.HMAFilter(accZData);
            double kalmanFilterData = filter.KalmanFilter(HMAFilterData);
            double BWLPFilterData = filter.BWLPFilter(kalmanFilterData);
            double accFilterData = BWLPFilterData;

            accModText.setText("加速度计z轴值: " + String.format("%.6f", accData[2]));
            accFilterText.setText("滤波值: " + String.format("%.6f", accFilterData));

            double stepLength = stepDetect.GetStepLength(accFilterData, event.timestamp);

            String accFilterString = String.format("%d, %.6f, %.6f, %.6f, %.6f, %.6f", event.timestamp, accZData, HMAFilterData, kalmanFilterData, BWLPFilterData, stepLength);
            filterString.append(accFilterString).append("\n");

            if (stepLength != 0) {
                mLocationClient.startLocation();

                double[] trueGCJ02 = new double[]{dataLib.latitude, dataLib.longtitude, dataLib.altitude};
                double[] trueWGS84 = posTrans.GCJ022WGS84(trueGCJ02);
                double[] trueENU = posTrans.WGS842ENU(trueWGS84);

                trueLatlngText.setText("GCJ02定位经纬度: " + String.format("%.6f", dataLib.latitude) + ", "
                        + String.format("%.6f", dataLib.longtitude));

//                double correctAngle = dataLib.angle - Math.toRadians(5) + Math.sin(Math.toRadians(trueWGS84[0])) * Math.toRadians(trueWGS84[1] - 117);
                double correctAngle = dataLib.angle - Math.toRadians(5);
//                Log.d("Angle", String.valueOf(Math.sin(Math.toRadians(trueWGS84[0]))) + ", " + String.valueOf(Math.toRadians(trueWGS84[1] - 117)) + ", " + String.valueOf(Math.sin(Math.toRadians(trueWGS84[0])) * Math.toRadians(trueWGS84[1] - 117)));

                yawMagText.setText("改正后航向角: " + String.format("%.6f", Math.toDegrees(correctAngle)));

                E += stepLength * Math.sin(correctAngle);
                N += stepLength * Math.cos(correctAngle);

                double[] ENU = new double[]{E, N, 0};
                double[] WGS84 = posTrans.ENU2WGS84(ENU);
                double[] GCJ02 = posTrans.WGS842GCJ02(WGS84);

                ENUText.setText("ENU: " + String.format("%.6f", ENU[0]) + ", "
                        + String.format("%.6f", ENU[1]) + ", "
                + String.format("%.6f", Math.toDegrees(correctAngle)));

                preLatlngText.setText("GCJ02预测经纬度: " + String.format("%.6f", GCJ02[0]) + ", "
                        + String.format("%.6f", GCJ02[1]));

                draw(GCJ02[0], GCJ02[1]);

                updateMapCenter(new LatLng(GCJ02[0], GCJ02[1]));

                stepNum++;
                stepNumText.setText("步数: " + String.format("%d", stepNum));
                stepLengthText.setText("步长: " + String.format("%.6f", stepLength));

                String posString = String.format("%d, %.6f, %.6f, %.6f, %.6f", event.timestamp, ENU[0], ENU[1], trueENU[0], trueENU[1]);
                positionString.append(posString).append("\n");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            String gyroString = String.format("%s, %d, %.6f, %.6f, %.6f",
                    "gyro", event.timestamp, event.values[0], event.values[1], event.values[2]);
            dataString.append(gyroString).append("\n");

            gyroData[0] = event.values[0];
            gyroData[1] = event.values[1];
            gyroData[2] = event.values[2];

            timestamp = event.timestamp;
            if (lastTimestamp == 0) lastTimestamp = timestamp;
            double deltaT = (double) (timestamp - lastTimestamp) / 1000000000.0;
            lastTimestamp = timestamp;

            if (deltaT != 0) {
                attAngle = attitudeAngle.getAttAngle(accData, gyroData, deltaT);
                dataLib.angle = attAngle[2] + dataLib.initAngle;
            }

            yawGyroText.setText("陀螺仪航向角: " + String.format("%.6f", Math.toDegrees(dataLib.angle)));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener magListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            String magString = String.format("%s, %d, %.6f, %.6f, %.6f",
                    "mag", event.timestamp, event.values[0], event.values[1], event.values[2]);
            dataString.append(magString).append("\n");

            magData[0] = event.values[0];
            magData[1] = event.values[1];
            magData[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void StartRecord() {
        sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(magListener, magSensor, SensorManager.SENSOR_DELAY_GAME);

        StartRecordAnimate();
    }

    public void PauseRecord() {
        if (!pauseBtnStatus) {
            pauseBtnStatus = true;

            sensorManager.unregisterListener(accListener, accSensor);
            sensorManager.unregisterListener(gyroListener, gyroSensor);
            sensorManager.unregisterListener(magListener, magSensor);

            pauseBtn.setBackgroundResource(R.drawable.resume_button);

            showMsg("暂停记录");
        } else {
            pauseBtnStatus = false;

            sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(magListener, magSensor, SensorManager.SENSOR_DELAY_GAME);

            pauseBtn.setBackgroundResource(R.drawable.pause_button);

            showMsg("恢复记录");
        }
    }

    public void StopRecord() {
        sensorManager.unregisterListener(accListener, accSensor);
        sensorManager.unregisterListener(gyroListener, gyroSensor);
        sensorManager.unregisterListener(magListener, magSensor);

        dataLib.sensorDataString = dataString;
        dataLib.filterDataString = filterString;
        dataLib.positionDataString = positionString;

        dataString = new StringBuilder();
        filterString = new StringBuilder();
        positionString = new StringBuilder();

        StopRecordAnimate();
    }

    //点击开始按钮后的动画
    //开始按钮变为不可见
    //暂停和停止按钮变为可见，并从开始按钮的位置，分别向左右移动
    private void StartRecordAnimate() {
        ObjectAnimator moveLeft = ObjectAnimator.ofFloat(pauseBtn, "translationX", 0f, -400f);
        ObjectAnimator moveRight = ObjectAnimator.ofFloat(stopBtn, "translationX", 0f, 400f);

        moveLeft.setDuration(250);
        moveRight.setDuration(250);

        moveLeft.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                startBtn.setVisibility(View.INVISIBLE);
                startBtn.setEnabled(false);
                pauseBtn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                pauseBtn.setEnabled(true);
            }
        });

        moveRight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                stopBtn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                stopBtn.setEnabled(true);
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveLeft, moveRight);
        animatorSet.start();
    }

    //点击停止按钮后的动画
    //暂停按钮和停止按钮分别向右左移动，回到开始按钮的位置上，并变为不可见
    //开始按钮变为可见
    private void StopRecordAnimate() {
        ObjectAnimator moveLeft = ObjectAnimator.ofFloat(stopBtn, "translationX", 400f, 0f);
        ObjectAnimator moveRight = ObjectAnimator.ofFloat(pauseBtn, "translationX", -400f, 0f);

        moveLeft.setDuration(250);
        moveRight.setDuration(250);

        moveLeft.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                stopBtn.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                stopBtn.setVisibility(View.INVISIBLE);
            }
        });

        moveRight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                pauseBtn.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                pauseBtn.setVisibility(View.INVISIBLE);
                pauseBtn.setBackgroundResource(R.drawable.pause_button);
                pauseBtnStatus = false;
                startBtn.setVisibility(View.VISIBLE);
                startBtn.setEnabled(true);
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveLeft, moveRight);
        animatorSet.start();
    }

    private void updateMapCenter(LatLng latLng) {
        // CameraPosition 第一个参数： 目标位置的屏幕中心点经纬度坐标。
        // CameraPosition 第二个参数： 目标可视区域的缩放级别
        // CameraPosition 第三个参数： 目标可视区域的倾斜度，以角度为单位。
        // CameraPosition 第四个参数： 可视区域指向的方向，以角度为单位，从正北向顺时针方向计算，从0度到360度
        CameraPosition cameraPosition = new CameraPosition(latLng, 20, 0, 0);
        //位置变更
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        //动画移动
        aMap.animateCamera(cameraUpdate);
    }

    private void draw(double latitude, double longitude) {
        latLngs.add(new LatLng(latitude, longitude));
        polyline = aMap.addPolyline(new PolylineOptions().
                addAll(latLngs).width(25).color(Color.argb(255, 2, 122, 255)));

        Log.d("Polyline", "添加" + (stepNum + 1) + "点");
    }


    private void showMsg(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
