package ru.tumbler.androidrobot.service;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import ru.tumbler.androidrobot.connection.NetworkDiscovery;

@EService
public class RobotService extends Service implements IRobot {

    private static final String LOG_TAG = RobotService.class.getName();
    public static final int WS_PORT = 8080;
    public static final String SERVICE_NAME = "RobotWebSocket";
    public static final String SERVICE_TYPE = "_http._tcp.robot.";
    private final RobotWebSocketServer ws;
    private NetworkDiscovery mNetworkDiscovery;

    public interface LogListener {
        void log(String message);
    }

    private LogListener mLogListener;

    private final IBinder mBinder = new RobotBinder();

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

        Log.d(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public String send(String command) {
        return null;
    }

    public void setLogListener(LogListener listener) {
        mLogListener = listener;
        if (mNetworkDiscovery!= null)
            log("Service: listening " + mNetworkDiscovery.getServerInfo());
    }

    public void log(String message) {
        Log.d(LOG_TAG, message);
        if (mLogListener!=null)
            mLogListener.log("Service: " + message);
    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate();
        startServer();
        registerService(WS_PORT);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        stopServer();
        if (mNetworkDiscovery != null) {

            mNetworkDiscovery.reset();
        }
        super.onDestroy();
    }

    private void stopServer() {
        if (ws == null)
            return;
        Log.i(LOG_TAG, "stopping WS");
        ws.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "onStartCommand");
        log("Received start id " + startId + ": " + intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void startServer() {
        stopServer();
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

    private void logServiceInfo(NsdServiceInfo serviceInfo) {
        InetAddress host = serviceInfo.getHost();
        int port = serviceInfo.getPort();
        log(String.format("Service registered: %s at %s:%d",
                serviceInfo.getServiceName(),
                (host != null) ? host.getHostAddress() : getIPAddress(true),
                (port > 0)? port : WS_PORT));
    }


    @Background
    void registerService(int port) {
        log("Service: init service registration");
        mNetworkDiscovery = new NetworkDiscovery(this);
        log("Service: start service registration");
        mNetworkDiscovery.startServer(port);
        log("Service: listening " + mNetworkDiscovery.getServerInfo());
    }

}
