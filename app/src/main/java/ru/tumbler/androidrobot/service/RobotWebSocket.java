package ru.tumbler.androidrobot.service;

import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

public class RobotWebSocket implements AsyncHttpServer.WebSocketRequestCallback {
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
        //Use this to clean up any references to your websocket
        mWebSocket.setClosedCallback(new CompletedCallback() {
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
        });

        mWebSocket.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                if ("Hello Server".equals(s)) {
                    webSocket.send("Welcome Client!");
                    return;
                }
                String result = mRobot.send(s);
                if (result != null)
                    webSocket.send(result);
            }
        });

        mWebSocket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                bb.recycle();
            }
        });

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
}