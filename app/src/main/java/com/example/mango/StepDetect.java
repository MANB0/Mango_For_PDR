package com.example.mango;

import android.util.Log;
import android.widget.TextView;

import com.amap.api.maps.BaseMapView;

import org.w3c.dom.Text;

import java.util.LinkedList;

import kotlinx.coroutines.DebugKt;

public class StepDetect {
    private LinkedList<Double> dataQueue, maxQueue, minQueue, peakQueue, valleyQueue;
    protected double alpha = 0.2;
    private long timestamp, lastTimestamp, beforeLastTimestamp;
    private double stepLength;
    private boolean trueStep;
    protected double a = 0.371, b = 0.227, c = 1, H = 1.72;
    private double backupDeltaT;


    private TextView extremumText, PVText;
    private TextView peakMeanText, valleyMeanText;
    private TextView peakMSDText, valleyMSDText;
    ;
    private TextView peakText, valleyText;
    private TextView deltaTimeText, lastDeltaTimeText;

    public StepDetect(TextView extremumText, TextView PVText,
                      TextView peakMeanText, TextView valleyMeanText,
                      TextView peakMSDText, TextView valleyMSDText,
                      TextView peakText, TextView valleyText,
                      TextView deltaTimeText, TextView lastDeltaTimeText) {
        dataQueue = new LinkedList<>();
        maxQueue = new LinkedList<>();
        minQueue = new LinkedList<>();
        peakQueue = new LinkedList<>();
        valleyQueue = new LinkedList<>();

        this.extremumText = extremumText;
        this.PVText = PVText;
        this.peakMeanText = peakMeanText;
        this.valleyMeanText = valleyMeanText;
        this.peakMSDText = peakMSDText;
        this.valleyMSDText = valleyMSDText;
        this.peakText = peakText;
        this.valleyText = valleyText;
        this.deltaTimeText = deltaTimeText;
        this.lastDeltaTimeText = lastDeltaTimeText;

        timestamp = 0;
        lastTimestamp = 0;
        backupDeltaT = 0.8;
        beforeLastTimestamp = 0;
        trueStep = true;
        stepLength = 0;
    }

    public double GetStepLength(double data, long time) {
        timestamp = time;
        if (lastTimestamp == 0) lastTimestamp = timestamp;
        if (beforeLastTimestamp == 0) beforeLastTimestamp = timestamp;

        stepLength = 0;

        if (dataQueue.size() == 3) dataQueue.removeFirst();
        dataQueue.addLast(data);

        if (dataQueue.size() == 3) getExtremum();

        return stepLength;
    }

    private void getExtremum() {
        double data0 = dataQueue.get(0);
        double data1 = dataQueue.get(1);
        double data2 = dataQueue.get(2);

        if (data1 > data0 && data1 > data2) {
            extremumText.setText("极值类型: 极大值");

            if (maxQueue.size() == 2) maxQueue.removeFirst();
            maxQueue.addLast(data1);

            if (maxQueue.size() == 2) RemoveOutliers(true);
        } else if (data1 < data0 && data1 < data2) {
            extremumText.setText("极值类型: 极小值");
            if (minQueue.size() == 2) minQueue.removeFirst();
            minQueue.addLast(data1);

            if (minQueue.size() == 2) RemoveOutliers(false);
        }
    }

    private void RemoveOutliers(boolean type) {
        if (type) {
            double max0 = maxQueue.get(0);
            double max1 = maxQueue.get(1);

            if (max0 + max1 > 0.1) {
                PVText.setText("峰谷值类型: 峰值");

                if (peakQueue.size() == 50) peakQueue.removeFirst();
                peakQueue.addLast(max1);
            }
        } else {
            double min0 = minQueue.get(0);
            double min1 = minQueue.get(1);

            if (min0 + min1 < -0.1) {
                PVText.setText("峰谷值类型: 谷值");

                if (valleyQueue.size() == 50) valleyQueue.removeFirst();
                valleyQueue.addLast(min1);
            }
        }

        if (!peakQueue.isEmpty() && !valleyQueue.isEmpty()) DynamicThreshold();
    }

    private void DynamicThreshold() {
        double peakMean = 0, valleyMean = 0;
        double peakMSD = 0, valleyMSD = 0;

        for (double value : peakQueue) peakMean += value;
        peakMean /= peakQueue.size();
        for (double value : peakQueue) peakMSD += Math.pow(value - peakMean, 2.0);
        peakMSD = Math.sqrt(peakMSD / (peakQueue.size() - 1));

        for (double value : valleyQueue) valleyMean += value;
        valleyMean /= valleyQueue.size();
        for (double value : valleyQueue) valleyMSD += Math.pow(value - valleyMean, 2.0);
        valleyMSD = Math.sqrt(valleyMSD / (valleyQueue.size() - 1));

        peakMeanText.setText("PeakMean: " + String.format("%.6f", peakMean));
        valleyMeanText.setText("ValleyMean: " + String.format("%.6f", valleyMean));
        peakMSDText.setText("PeakMSD: " + String.format("%.6f", peakMSD));
        valleyMSDText.setText("ValleyMSD: " + String.format("%.6f", valleyMSD));

        boolean peakDetected = false, valleyDetected = false;
        boolean runStatus = false;

        if (peakMSD - valleyMSD > alpha) {
            runStatus = true;
            if (peakQueue.getLast() >= peakMSD * peakMSD) {
                peakText.setText("Peak: " + String.format("%.6f", peakQueue.getLast()));
                peakDetected = true;
            }
            if (valleyQueue.getLast() <= -valleyMSD * valleyMSD) {
                valleyDetected = true;
            }
        } else {
            if (peakQueue.getLast() >= peakMSD) {
                peakText.setText("Peak: " + String.format("%.6f", peakQueue.getLast()));
                peakDetected = true;
            }
            if (valleyQueue.getLast() <= -valleyMSD) {
                valleyText.setText("Valley: " + String.format("%.6f", valleyQueue.getLast()));
                valleyDetected = true;
            }
        }
        if (peakDetected && valleyDetected) {
            if (trueStep) CalcStepLength(runStatus);
            trueStep = !trueStep;
        }
    }

    private void CalcStepLength(boolean runStatus) {
        double deltaT = (double) (timestamp - lastTimestamp) / 1000000000.0;
        double lastDeltaT = (double) (lastTimestamp - beforeLastTimestamp) / 1000000000.0;
        Log.d("StepDeltaT", String.valueOf(deltaT));
        Log.d("LastStepDeltaT", String.valueOf(lastDeltaT));

        if (deltaT <= 1) backupDeltaT = deltaT;
        Log.d("BackupDeltaT", String.valueOf(backupDeltaT));

        if (deltaT > 1) deltaT = backupDeltaT;
        if (lastDeltaT > 1) lastDeltaT = backupDeltaT;
        Log.d("JudgeStepDeltaT", String.valueOf(deltaT));
        Log.d("JudgeLastStepDeltaT", String.valueOf(lastDeltaT));

        deltaTimeText.setText("本步间隔时间: " + String.format("%.6f", deltaT));
        lastDeltaTimeText.setText("上步间隔时间: " + String.format("%.6f", lastDeltaT));

        double SF = 1 / (0.8 * deltaT + 0.2 * lastDeltaT);
        stepLength = (0.7 + a * (H - 1.6) + b * (SF - 1.79) * H / 1.6) * c;

        beforeLastTimestamp = lastTimestamp;
        lastTimestamp = timestamp;
    }
}
