package ru.tumbler.androidrobot.remote;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import ru.tumbler.androidrobot.R;
import ru.tumbler.androidrobot.connection.NetworkDiscovery;

@Fullscreen
@WindowFeature({ Window.FEATURE_NO_TITLE})
@EActivity(R.layout.activity_remote_control)
public class RemoteControlActivity extends Activity implements WebSocket.StringCallback,
        DataCallback, CompletedCallback, AsyncHttpClient.WebSocketConnectCallback, NetworkDiscovery.OnFoundListener {

    private static final String LOG_TAG = RemoteControlActivity.class.getName();

    private final Handler mHandler = new Handler();

    @ViewById(R.id.surfaceView)
    DrawView mSurfaceView;

    @ViewById(R.id.textView)
    TextView mConsole;

    @ViewById(R.id.graph)
    GraphView mGraph;

    LineGraphSeries<DataPoint> mAngleSeries;
    LineGraphSeries<DataPoint> mSpeedSeries;
    private Runnable graphUpdateTimer;
    private Runnable pingTimer;
    private double graph2LastXValue;
    private long lastPingSend;
    private double mLatency = 0;
    private int mPrevAngle;
    private int mPrevSpeed;
    private Map<Integer, Byte[]> mPendingCommands = new HashMap<Integer, Byte[]>();
    private Runnable mSyncTask;

    @AfterViews
    void init() {
        mAngleSeries = new LineGraphSeries<>();
        mAngleSeries.setTitle("Angle");
        for(int i=0;i<30;i++) {
            mAngleSeries.appendData(new DataPoint((double) i, Math.random()), true, 30);
        }
        mSpeedSeries = new LineGraphSeries<>();
        mSpeedSeries.setTitle("Speed");
        for(int i=0;i<30;i++) {
            mSpeedSeries.appendData(new DataPoint((double) i, 0), true, 30);
        };
        // mGraph.addSeries(mAngleSeries);
        mGraph.addSeries(mSpeedSeries);
        graph2LastXValue = 30;

        graphUpdateTimer = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 1d;
                mAngleSeries.appendData(new DataPoint(graph2LastXValue, Math.random()), true, 30);
                mSpeedSeries.appendData(new DataPoint(graph2LastXValue, mLatency), true, 30);
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(graphUpdateTimer, 1000);

        pingTimer = new Runnable() {
            @Override
            public void run() {
                lastPingSend = System.currentTimeMillis();
                if (mWebSocket != null)
                    mWebSocket.send("PING");
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.postDelayed(pingTimer, 1000);
    }

    private String mServiceUri;
    private WebSocket mWebSocket;
    private List<String> mBuffer = new ArrayList<String>();
    private NetworkDiscovery mNetworkDiscovery;

    @Touch(R.id.overlay)
    void onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            float screenX = event.getRawX();
            float screenY = event.getRawY();

            float viewX = screenX - location[0];
            float viewY = screenY - location[1];

            float viewL = v.getLeft();
            float viewT = v.getTop();
            float viewR = v.getRight();
            float viewB = v.getBottom();
            float relAngle = (viewX - viewL) / (viewR - viewL) - 0.5f;
            float relSpeed = (viewY - viewB) / (viewT - viewB) - 0.5f;
            int angle = (int)(relAngle * 127);
            int speed = (int)(relSpeed * 127);
            if (angle > 127) {
                angle = 127;
            } else if (angle < -127) {
                angle = -127;
            }
            if (speed > 127) {
                speed = 127;
            } else if (speed < -127) {
                speed = -127;
            }
            mSurfaceView.updateCursor(angle, speed, viewX, viewY);
            if (mPrevAngle!= angle) {
                sendAngleCommand(angle);
            }
            if (mPrevSpeed != speed) {
                sendSpeedCommand(speed);
            }
        }
        if (action == MotionEvent.ACTION_UP) {
            mSurfaceView.updateCursor(0, 0, 0, 0);
            sendSpeedCommand(0);
            sendAngleCommand(0);
        }
    }

    private void sendAngleCommand(int angle) {
        if (mWebSocket == null)
            return;
        Byte[] cmd = new Byte[2];
        cmd[0] = 11;
        cmd[1] = (byte)angle;
        mPrevAngle = angle;
        mPendingCommands.put(11, cmd);
        sync();
    }

    private void sendSpeedCommand(int speed) {
        if (mWebSocket == null)
            return;
        Byte[] cmd = new Byte[2];
        cmd[0] = 12;
        cmd[1] = (byte)speed;
        mPendingCommands.put(12, cmd);
        mPrevSpeed = speed;
        sync();
    }

    synchronized void sync() {
        if (mSyncTask != null) return;
        mSyncTask = new Runnable() {
            @Override
            public void run() {
                doSend();
                mSyncTask = null;
            }
        };
        mHandler.postDelayed(mSyncTask, 100);
    }

    private byte[] toPrimitives(Byte[] oBytes)
    {

        byte[] bytes = new byte[oBytes.length];
        for(int i = 0; i < oBytes.length; i++){
            bytes[i] = oBytes[i];
        }
        return bytes;

    }

    private synchronized void doSend() {
        for(Map.Entry<Integer, Byte[]> entry: mPendingCommands.entrySet()) {
            Byte[] value = entry.getValue();
            mWebSocket.send(toPrimitives(value));
        }
        mPendingCommands.clear();
    }

    @Override
    protected void onStop() {
        stopDiscovery();
        mHandler.removeCallbacks(graphUpdateTimer);
        mHandler.removeCallbacks(pingTimer);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDiscovery();
    }

    private void startDiscovery() {
        log("startDiscovery");
        stopDiscovery();
        discoverServices();
        log("discovery started async");
    }

    @Background
    protected void discoverServices() {
        mNetworkDiscovery = new NetworkDiscovery(this);
        mNetworkDiscovery.findServers(this);
    }

    private void stopDiscovery() {
        if(mNetworkDiscovery!=null)
            mNetworkDiscovery.reset();
    }

    @UiThread
    public void log(String message) {
        Log.d(LOG_TAG, message);
        updateReceivedData(message);
    }

    public void startWebSocket() {
        Log.d(LOG_TAG, "Connecting to " + mServiceUri);
        AsyncHttpClient.getDefaultInstance().websocket(mServiceUri, null, this);
    }

    private void updateReceivedData(String data) {
        if (mBuffer.size() == 3)
            mBuffer.remove(0);
        mBuffer.add(data);

        StringBuilder builder = new StringBuilder();
        for (String s: mBuffer) {
            builder.append(s);
            builder.append('\n');
        }
        mConsole.setText(builder.toString());
    }

    @Override
    public void onStringAvailable(final String s) {
        if ("PONG".equals(s)) {
            mLatency = (System.currentTimeMillis() - lastPingSend) / 2000.0d;
            return;
        }
        log(s);
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        bb.recycle();
    }

    @Override
    public void onCompleted(Exception ex) {
        log("WS closed");
        mLatency = 0;
        mWebSocket = null;
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        log("WD Suddenly closed");
        if (ex != null) {
            ex.printStackTrace();
            if (mWebSocket != null)
            {
                mLatency = 0;
                mWebSocket.close();
                mWebSocket = null;
            }
            return;
        }
        mWebSocket = webSocket;
        webSocket.send("Connect to: " + android.os.Build.MODEL);
        webSocket.setStringCallback(this);
        webSocket.setDataCallback(this);
        webSocket.setClosedCallback(this);
    }

    @Override
    public void onFound(ServiceInfo info) {
        log("RC: found service: " + String.valueOf(info));
        Inet4Address host = info.getInet4Address();
        int port = info.getPort();
        mServiceUri = "http://" + host.getHostAddress() + ":" + String.valueOf(port) + "/ws";
        startWebSocket();
    }
}
