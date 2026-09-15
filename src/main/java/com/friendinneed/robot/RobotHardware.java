package com.friendinneed.robot;

public interface RobotHardware {
    void moveHead(double pan, double tilt);

    void setLed(String color, String pattern);

    double getDistance();

    double getBatteryPercent();

    double getTemperature();

    byte[] captureImage();

    void playAudio(byte[] mp3Data);

    boolean isTouchActive(String zone);
}
