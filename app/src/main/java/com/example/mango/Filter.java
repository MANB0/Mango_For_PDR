package com.example.mango;

import java.util.LinkedList;

public class Filter {
    public LinkedList<Double> HMAFilterQueue1, HMAFilterQueue2, HMAFilterQueue3;

    //赫尔移动平均滤波所使用的窗口的大小
    protected int windowSize = 10;

    //卡尔曼滤波使用的参数
    protected double Q = 0.01; // 过程噪声方差
    protected double R = 0.1; // 测量噪声方差
    private double X; // 状态估计值
    private double P; // 状态估计误差的协方差

    //巴特沃斯低通滤波使用的参数
    private double filterData;
    protected double alpha = 0.33;

    public Filter() {
        HMAFilterQueue1 = new LinkedList<>();
        HMAFilterQueue2 = new LinkedList<>();
        HMAFilterQueue3 = new LinkedList<>();

        X = 0;
        P = 1;

        filterData = 0;
    }

    public double getFilterData(double data) {
        double HMA = HMAFilter(data);
        double Kalman = KalmanFilter(HMA);
        double BWLP = BWLPFilter(Kalman);
        return BWLP;
    }

    //赫尔移动平均滤波
    public double HMAFilter(double data) {
        //设置三个窗口大小
        int W1 = (int) ((double) windowSize / 2.0);
        int W2 = windowSize;
        int W3 = (int) Math.sqrt(windowSize);

        //第一个队列进行滤波
        if (HMAFilterQueue1.size() == W1) HMAFilterQueue1.removeFirst();
        HMAFilterQueue1.addLast(data);

        double sum1 = 0.0;
        int num1 = 1;
        for (double value : HMAFilterQueue1) {
            sum1 += num1 * value;
            num1++;
        }
        sum1 /= (double) (HMAFilterQueue1.size() * (HMAFilterQueue1.size() + 1)) / 2;

        //第二个队列滤波
        if (HMAFilterQueue2.size() == W2) HMAFilterQueue2.removeFirst();
        HMAFilterQueue2.addLast(data);

        double sum2 = 0.0;
        int num2 = 1;
        for (double value : HMAFilterQueue2) {
            sum2 += num2 * value;
            num2++;
        }
        sum2 /= (double) (HMAFilterQueue2.size() * (HMAFilterQueue2.size() + 1)) / 2;

        //第三个队列进行滤波
        if (HMAFilterQueue3.size() == W3) HMAFilterQueue3.removeFirst();
        HMAFilterQueue3.addLast(2 * sum1 - sum2);

        double sum3 = 0.0;
        int num3 = 1;
        for (double value : HMAFilterQueue3) {
            sum3 += num3 * value;
            num3++;
        }
        sum3 /= (double) (HMAFilterQueue3.size() * (HMAFilterQueue3.size() + 1)) / 2;

        return sum3;
    }

    //卡尔曼滤波
    public double KalmanFilter(double data) {
        double Xp = X;
        double Pp = P + Q;
        double K = Pp / (Pp + R);

        X = Xp + K * (data - Xp);
        P = (1 - K) * Pp;

        return X;
    }

    //巴特沃斯低通滤波
    public double BWLPFilter(double data) {
        filterData = alpha * data + (1 - alpha) * filterData;

        return filterData;
    }
}
