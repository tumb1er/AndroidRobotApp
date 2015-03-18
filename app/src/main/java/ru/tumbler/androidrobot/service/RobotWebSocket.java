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
        if ("Hello Server".equals(s)) {
            mWebSocket.send("Welcome Client!");
            return;
        }
        String result = mRobot.send(s);
        if (result != null)
            mWebSocket.send(result);
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        bb.recycle();
    }
}