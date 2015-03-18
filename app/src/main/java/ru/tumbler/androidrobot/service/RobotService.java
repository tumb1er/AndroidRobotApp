package ru.tumbler.androidrobot.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.androidannotations.annotations.EService;
import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

@EService
public class RobotService extends Service implements IRobot {

    private static final String LOG_TAG = RobotService.class.getName();
    public static final int WS_PORT = 8080;
    public static final String SERVICE_NAME = "RobotWebSocket";
    public static final String SERVICE_TYPE = "_http._tcp.";
    private final RobotWebSocketServer ws;

    public interface LogListener {
        void log(String message);
    }

    private LogListener mLogListener;

    private final IBinder mBinder = new RobotBinder();

    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdManager mNsdManager;


    public RobotService() {
        ws = new RobotWebSocketServer(this);
    }

    public class RobotBinder extends Binder {

        public RobotService getService() {
            return RobotService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public String send(String command) {
        return null;
    }

    public void setLogListener(LogListener listener) {
        mLogListener = listener;
    }

    void log(String message) {
        if (mLogListener!=null)
            mLogListener.log(message);
    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate();
        startServer();
        initializeRegistrationListener();
        registerService(WS_PORT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
        mNsdManager.unregisterService(mRegistrationListener);
    }

    private void stopServer() {
        if (ws == null)
            return;
        ws.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void startServer() {
        if (ws == null)
            return;
        ws.stop();
        ws.listen(WS_PORT);
        log("Car: WS server started");
    }

    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                InetAddress host = serviceInfo.getHost();
                int port = serviceInfo.getPort();
                log(String.format("Service registered: %s at %s:%d",
                        serviceInfo.getServiceName(),
                        (host != null) ? host.getHostAddress() : getIPAddress(true),
                        (port > 0)? port : WS_PORT));
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                log("onRegistrationFailed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                log("onServiceUnregistered");
                stopServer();
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                log("onUnregistrationFailed");
            }
        };
    }

    public void registerService(int port) {
        log("start service registration");
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

}
