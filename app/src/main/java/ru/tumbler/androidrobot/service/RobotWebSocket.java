package ru.tumbler.androidrobot.service;

import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

public class RobotWebSocket implements AsyncHttpServer.WebSocketRequestCallback, CompletedCallback, WebSocket.StringCallback, DataCallback {
    private static final String LOG_TAG = RobotWebSocket.class.getName();
    private WebSocket mWebSocket = null;
    private IRobot mRobot = null;

    public RobotWebSocket(IRobot robot) {
        this.mRobot = robot;
    }

    @Override
    public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
        if (mWebSocket != null)
            mWebSocket.close();

        mWebSocket = webSocket;
        mWebSocket.setClosedCallback(this);
        mWebSocket.setStringCallback(this);
        mWebSocket.setDataCallback(this);

    }

    public void send(String string) {
        if (mWebSocket != null)
            mWebSocket.send(string);
    }

    public void closeWS() {
        if (mWebSocket!=null) {
            mWebSocket.send("server stopped");
            mWebSocket.close();
        }
    }

    @Override
    public void onCompleted(Exception ex) {
        try {
            if (ex != null) {
                Log.e("WebSocket", "Error:" + String.valueOf(ex));
                Log.e("WebSocket",  Log.getStackTraceString(ex));
            }
        } finally {
            mWebSocket = null;
        }
    }

    @Override
    public void onStringAvailable(String s) {
        if ("PING".equals(s)) {
            mWebSocket.send("PONG");
            return;
        } else {
            Log.d(LOG_TAG, s);
        }
        String result = mRobot.send(s);
        if (result != null)
            mWebSocket.send(result);
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        byte cmd = bb.get();
        switch (cmd){
            case 11:
                int angle = (int) bb.get();
                saveAngle(angle);
                mWebSocket.send("Angle " + String.valueOf(angle));
                mRobot.log("Angle " + String.valueOf(angle));
                break;
            case 12:
                int speed = (int) bb.get();
                saveSpeed(speed);
                mWebSocket.send("Speed " + String.valueOf(speed));
                mRobot.log("Speed " + String.valueOf(speed));
                break;
            default:
                mWebSocket.send("Unknown");
        }
        bb.recycle();
    }

    private void saveSpeed(int speed) {
        mRobot.setSpeed(speed);

    }

    private void saveAngle(int angle) {
        mRobot.setAngle(angle);
    }
}