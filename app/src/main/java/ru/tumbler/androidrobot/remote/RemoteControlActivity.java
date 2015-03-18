package ru.tumbler.androidrobot.remote;

import android.app.Activity;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import ru.tumbler.androidrobot.R;

@Fullscreen
@WindowFeature({ Window.FEATURE_NO_TITLE})
@EActivity(R.layout.activity_remote_control)
public class RemoteControlActivity extends Activity implements WebSocket.StringCallback,
        DataCallback, CompletedCallback, AsyncHttpClient.WebSocketConnectCallback {

    @ViewById(R.id.surfaceView)
    DrawView mSurfaceView;

    @ViewById(R.id.textView)
    TextView mConsole;

    private String mServiceUri;
    private WebSocket mWebSocket;
    private List<String> mBuffer = new ArrayList<String>();

    @Touch(R.id.surfaceView)
    void onTouch(View v, MotionEvent event) {}

    private NsdManager.DiscoveryListener mDiscoveryListener;

    @SystemService
    protected NsdManager mNsdManager;

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDiscovery();
    }

    public NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                log("RC: Resolve failed " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                InetAddress host = serviceInfo.getHost();
                int port = serviceInfo.getPort();
                log(String.format("RC: Resolve Succeeded: %s at %s:%d",
                        serviceInfo.getServiceName(),
                        host.getHostAddress(),
                        port));
                mServiceUri = "http://" + host.getHostAddress() + ":" + String.valueOf(port) + "/ws";
                startWebSocket();
            }
        };
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                log("RC: Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                log("RC: Service discovery success");
                if (!service.getServiceType().equals("_http._tcp.")) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    log("RC: Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals("RobotWebSocket")) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    mNsdManager.resolveService(service, initializeResolveListener());
                } else if (service.getServiceName().contains("RobotWebSocket")){
                    mNsdManager.resolveService(service, initializeResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                log("RC: service lost");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                log("RC: Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                log("RC: Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                log("RC: Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void startDiscovery() {
        if (mDiscoveryListener != null)
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (IllegalArgumentException ignored) {}
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @UiThread
    public void log(String message) {
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
}
