package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.Position;
import lombok.extern.slf4j.Slf4j;

/**
 * 二维卡尔曼滤波器
 * 用于位置数据滤波优化，平滑定位轨迹
 */
@Slf4j
public class KalmanFilter2D {
    private double[] state = new double[2]; // 状态向量 [x, y]
    private double[][] covariance = new double[2][2]; // 误差协方差矩阵
    private double[][] processNoise = new double[2][2]; // 过程噪声协方差
    private double[][] measurementNoise = new double[2][2]; // 测量噪声协方差
    private double[][] stateTransition = {{1, 0}, {0, 1}}; // 状态转移矩阵
    private double[][] observationMatrix = {{1, 0}, {0, 1}}; // 观测矩阵

    private long lastTimestamp;
    private boolean initialized = false;

    // 配置参数
    private double defaultProcessNoise;
    private double defaultMeasurementNoise;

    /**
     * 构造函数
     */
    public KalmanFilter2D(double processNoise, double measurementNoise) {
        this.defaultProcessNoise = processNoise;
        this.defaultMeasurementNoise = measurementNoise;

        // 初始化噪声矩阵
        initializeNoiseMatrices(processNoise, measurementNoise);

        log.info("卡尔曼滤波器初始化: 过程噪声={}, 测量噪声={}", processNoise, measurementNoise);
    }

    /**
     * 初始化滤波器
     */
    public void initialize(double x, double y, long timestamp) {
        state[0] = x;
        state[1] = y;

        // 初始化协方差矩阵
        covariance[0][0] = defaultProcessNoise;
        covariance[0][1] = 0;
        covariance[1][0] = 0;
        covariance[1][1] = defaultProcessNoise;

        lastTimestamp = timestamp;
        initialized = true;

        log.debug("卡尔曼滤波器初始化位置: ({}, {})", x, y);
    }

    /**
     * 滤波处理
     */
    public Position filter(double measuredX, double measuredY, long timestamp, Double qualityFactor) {
        if (!initialized) {
            initialize(measuredX, measuredY, timestamp);
            return new Position(measuredX, measuredY, "kalman", "initial");
        }

        // 根据质量因子调整噪声参数
        if (qualityFactor != null) {
            adjustNoiseForQuality(qualityFactor);
        }

        // 计算时间间隔
        double dt = (timestamp - lastTimestamp) / 1000.0;
        lastTimestamp = timestamp;

        // 更新状态转移矩阵（如果时间间隔较大）
        if (dt > 0.1) {
            updateStateTransitionMatrix(dt);
        }

        // 预测步骤
        predict();

        // 更新步骤
        update(measuredX, measuredY);

        // 重置噪声矩阵为默认值
        initializeNoiseMatrices(defaultProcessNoise, defaultMeasurementNoise);

        return new Position(state[0], state[1], "kalman", "filtered");
    }

    /**
     * 预测步骤
     */
    private void predict() {
        // 状态预测: state = stateTransition * state
        double[] newState = new double[2];
        newState[0] = stateTransition[0][0] * state[0] + stateTransition[0][1] * state[1];
        newState[1] = stateTransition[1][0] * state[0] + stateTransition[1][1] * state[1];
        state = newState;

        // 协方差预测: covariance = stateTransition * covariance * stateTransition^T + processNoise
        double[][] newCovariance = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                newCovariance[i][j] = 0;
                for (int k = 0; k < 2; k++) {
                    for (int l = 0; l < 2; l++) {
                        newCovariance[i][j] += stateTransition[i][k] * covariance[k][l] * stateTransition[j][l];
                    }
                }
                newCovariance[i][j] += processNoise[i][j];
            }
        }
        covariance = newCovariance;
    }

    /**
     * 更新步骤
     */
    private void update(double measuredX, double measuredY) {
        // 计算创新向量: innovation = measurement - observationMatrix * state
        double[] innovation = new double[2];
        innovation[0] = measuredX - (observationMatrix[0][0] * state[0] + observationMatrix[0][1] * state[1]);
        innovation[1] = measuredY - (observationMatrix[1][0] * state[0] + observationMatrix[1][1] * state[1]);

        // 计算创新协方差: S = observationMatrix * covariance * observationMatrix^T + measurementNoise
        double[][] innovationCovariance = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                innovationCovariance[i][j] = measurementNoise[i][j];
                for (int k = 0; k < 2; k++) {
                    for (int l = 0; l < 2; l++) {
                        innovationCovariance[i][j] += observationMatrix[i][k] * covariance[k][l] * observationMatrix[j][l];
                    }
                }
            }
        }

        // 计算卡尔曼增益: K = covariance * observationMatrix^T * S^-1
        double detS = innovationCovariance[0][0] * innovationCovariance[1][1] - innovationCovariance[0][1] * innovationCovariance[1][0];
        if (Math.abs(detS) < 1e-10) {
            log.warn("卡尔曼滤波: 创新协方差矩阵奇异，跳过更新");
            return;
        }

        double[][] S_inverse = {
                {innovationCovariance[1][1] / detS, -innovationCovariance[0][1] / detS},
                {-innovationCovariance[1][0] / detS, innovationCovariance[0][0] / detS}
        };

        double[][] kalmanGain = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                kalmanGain[i][j] = 0;
                for (int k = 0; k < 2; k++) {
                    kalmanGain[i][j] += covariance[i][k] * observationMatrix[j][k];
                }
                kalmanGain[i][j] *= S_inverse[j][j];
            }
        }

        // 更新状态估计: state = state + K * innovation
        state[0] += kalmanGain[0][0] * innovation[0] + kalmanGain[0][1] * innovation[1];
        state[1] += kalmanGain[1][0] * innovation[0] + kalmanGain[1][1] * innovation[1];

        // 更新协方差估计: covariance = (I - K * observationMatrix) * covariance
        double[][] I = {{1, 0}, {0, 1}};
        double[][] KH = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                KH[i][j] = 0;
                for (int k = 0; k < 2; k++) {
                    KH[i][j] += kalmanGain[i][k] * observationMatrix[k][j];
                }
            }
        }

        double[][] I_KH = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                I_KH[i][j] = I[i][j] - KH[i][j];
            }
        }

        double[][] newCovariance = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                newCovariance[i][j] = 0;
                for (int k = 0; k < 2; k++) {
                    newCovariance[i][j] += I_KH[i][k] * covariance[k][j];
                }
            }
        }
        covariance = newCovariance;
    }

    /**
     * 根据质量因子调整噪声参数
     */
    private void adjustNoiseForQuality(double qualityFactor) {
        // 质量因子越低（信号越差），测量噪声越大
        double adjustedMeasurementNoise = defaultMeasurementNoise * (2.0 - qualityFactor);
        initializeNoiseMatrices(defaultProcessNoise, adjustedMeasurementNoise);

        log.debug("卡尔曼滤波质量调整: 质量因子={}, 测量噪声={}", qualityFactor, adjustedMeasurementNoise);
    }

    /**
     * 更新状态转移矩阵（考虑速度）
     */
    private void updateStateTransitionMatrix(double dt) {
        // 简单模型：假设匀速运动
        // 在实际应用中，这里可以根据速度信息更新状态转移矩阵
        stateTransition[0][0] = 1;
        stateTransition[0][1] = 0;
        stateTransition[1][0] = 0;
        stateTransition[1][1] = 1;
    }

    /**
     * 初始化噪声矩阵 - 修正版本
     */
    private void initializeNoiseMatrices(double processNoiseValue, double measurementNoiseValue) {
        // 使用this关键字明确指定成员变量，避免参数名冲突
        this.processNoise[0][0] = processNoiseValue;
        this.processNoise[0][1] = 0;
        this.processNoise[1][0] = 0;
        this.processNoise[1][1] = processNoiseValue;

        this.measurementNoise[0][0] = measurementNoiseValue;
        this.measurementNoise[0][1] = 0;
        this.measurementNoise[1][0] = 0;
        this.measurementNoise[1][1] = measurementNoiseValue;
    }

    /**
     * 获取当前状态
     */
    public Position getCurrentState() {
        return new Position(state[0], state[1], "kalman", "current");
    }

    /**
     * 重置滤波器
     */
    public void reset() {
        initialized = false;
        log.info("卡尔曼滤波器已重置");
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}