package ru.tumbler.androidrobot.remote;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.ServiceInfo;

import ru.tumbler.androidrobot.R;
import ru.tumbler.androidrobot.connection.NetworkDiscovery;

@Fullscreen
@WindowFeature({ Window.FEATURE_NO_TITLE})
@EActivity(R.layout.activity_remote_control)
public class RemoteControlActivity extends Activity implements WebSocket.StringCallback,
        DataCallback, CompletedCallback, AsyncHttpClient.WebSocketConnectCallback, NetworkDiscovery.OnFoundListener {

    private static final String LOG_TAG = RemoteControlActivity.class.getName();
    @ViewById(R.id.surfaceView)
    DrawView mSurfaceView;

    @ViewById(R.id.textView)
    TextView mConsole;

    private String mServiceUri;
    private WebSocket mWebSocket;
    private List<String> mBuffer = new ArrayList<String>();
    private NetworkDiscovery mNetworkDiscovery;

    @Touch(R.id.surfaceView)
    void onTouch(View v, MotionEvent event) {}

    @Override
    protected void onStop() {
        stopDiscovery();
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
        log(s);
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        bb.recycle();
    }

    @Override
    public void onCompleted(Exception ex) {
        log("WS closed");
        mWebSocket = null;
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        if (ex != null) {
            ex.printStackTrace();
            if (mWebSocket != null) {
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
