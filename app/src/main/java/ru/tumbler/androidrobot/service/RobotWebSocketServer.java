package ru.tumbler.androidrobot.service;

import com.koushikdutta.async.http.server.AsyncHttpServer;


public class RobotWebSocketServer extends AsyncHttpServer {


    private RobotWebSocket callback = null;
    private IRobot mRobot = null;

    public RobotWebSocketServer(IRobot robot) {
        mRobot = robot;
        init();
    }

    private void init() {
        callback = new RobotWebSocket(mRobot);
        websocket("/ws", callback);
    }

    @Override
    public void stop() {
        callback.closeWS();
        super.stop();
    }

    public void send(String string) {
        callback.send(string);
    }
}