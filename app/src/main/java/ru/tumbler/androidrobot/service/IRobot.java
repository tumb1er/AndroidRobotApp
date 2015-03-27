package ru.tumbler.androidrobot.service;

/**
 * Created by tumbler on 28.01.15.
 */
public interface IRobot {
    String send(String command);

    void log(String msg);

    void setSpeed(int speed);

    void setAngle(int angle);
}
