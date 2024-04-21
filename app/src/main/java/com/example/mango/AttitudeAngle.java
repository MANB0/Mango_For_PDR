package com.example.mango;

public class AttitudeAngle {
    private double q0, q1, q2, q3;
    private double exInt = 0, eyInt = 0, ezInt = 0;
    private double Kp, Ki;

    public AttitudeAngle() {
        // 初始化四元素为 1 0 0 0
        q0 = 1;
        q1 = 0;
        q2 = 0;
        q3 = 0;

        Ki = 0.005;
        Kp = 1.5;
    }

    public double[] getAttAngle(double[] accData, double[] gyrData, double deltaT) {
        //数据准备
        double ax = accData[0];
        double ay = accData[1];
        double az = accData[2];
        double gx = gyrData[0];
        double gy = gyrData[1];
        double gz = gyrData[2];

        //重力加速度归一化（机体坐标系）
        double norm_acc = invSqrt(ax * ax + ay * ay + az * az);
        ax *= norm_acc;
        ay *= norm_acc;
        az *= norm_acc;

        //提取四元素的等效余弦矩阵中的重力分量（机体坐标系）
        double vx = 2 * (q1 * q3 - q0 * q2);
        double vy = 2 * (q0 * q1 + q2 * q3);
        double vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3;

        //向量叉乘得出姿态误差
        double ex = ay * vz - az * vy;
        double ey = az * vx - ax * vz;
        double ez = ax * vy - ay * vx;

        //对误差进行积分
        exInt += ex * deltaT;
        eyInt += ey * deltaT;
        ezInt += ez * deltaT;

        //互补滤波，将姿态误差补偿到角速度上，修正角速度积分漂移
        gx += Kp * ex + exInt * Ki;
        gy += Kp * ey + eyInt * Ki;
        gz += Kp * ez + ezInt * Ki;

        //一阶龙格库塔法更新四元数
        q0 += (-q1 * gx - q2 * gy - q3 * gz) * deltaT / 2;
        q1 += (q0 * gx + q2 * gz - q3 * gy) * deltaT / 2;
        q2 += (q0 * gy - q1 * gz + q3 * gx) * deltaT / 2;
        q3 += (q0 * gz + q1 * gy - q2 * gx) * deltaT / 2;

        //四元数归一化
        double norm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= norm;
        q1 *= norm;
        q2 *= norm;
        q3 *= norm;

        //四元数转欧拉角
        double pitch = Math.atan2(2 * q2 * q3 + 2 * q0 * q1, 1 - 2 * (q1 * q1 + q2 * q2));
        double roll = Math.asin(2 * (q0 * q2 - q1 * q3));
        double yaw = -Math.atan2(2 * q0 * q3 + 2 * q1 * q2, 1 - 2 * (q2 * q2 + q3 * q3));

        return new double[]{pitch, roll, yaw};
    }

    private double invSqrt(double x) {
        return 1.0 / Math.sqrt(x);
    }
}
