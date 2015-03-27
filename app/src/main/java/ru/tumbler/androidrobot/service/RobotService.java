package ru.tumbler.androidrobot.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.tumbler.androidrobot.connection.NetworkDiscovery;

@EService
public class RobotService extends Service implements IRobot, SerialInputOutputManager.Listener {

    private static final String LOG_TAG = RobotService.class.getName();
    private static UsbSerialPort sPort = null;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private String mBuffer;

    public static final int WS_PORT = 8080;
    public static final String SERVICE_NAME = "RobotWebSocket";
    public static final String SERVICE_TYPE = "_http._tcp.robot.";
    private final RobotWebSocketServer ws;
    private NetworkDiscovery mNetworkDiscovery;

    public void tryConnectUsb() {
        log("Connecting to USB Device");
        connect();
    }

    public void tryDisconnectUsb() {
        log("Disconnecting USB device");
        disconnect();
    }

    private void onLineReceived(String string) {
        log("USB: " + string);
        ws.send("USB: " + string);
    }

    @Override
    public void onNewData(byte[] bytes) {
        String data = new String(bytes);
        Log.d(LOG_TAG, "USB << " + data);
        int start = 0;
        int i = data.indexOf('\n');
        int len = data.length();
        while (i >= 0 && start < len) {
            String prevLine = data.substring(start, i);
            if (mBuffer.isEmpty())
                onLineReceived(prevLine);
            else {
                onLineReceived(mBuffer + prevLine);
                mBuffer = "";
            }
            start = i + 1;
            i = data.indexOf('\n', start);
        }
        if (start < data.length())
            mBuffer = data.substring(start);
        else
            mBuffer = "";

        try {
            sPort.purgeHwBuffers(true, true);
        } catch (IOException e) {
            Log.d(LOG_TAG, "Read flush failed");
        }
    }

    @Override
    public void onRunError(Exception e) {
        log("Runner stopped: " + Log.getStackTraceString(e));
    }

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
            log("listening " + mNetworkDiscovery.getServerInfo());
    }

    public void log(String message) {
        Log.d(LOG_TAG, message);
        if (mLogListener!=null)
            mLogListener.log("Service: " + message);
    }

    @Override
    public void setSpeed(int speed) {
        if (sPort == null)
            return;
        usbWrite(String.format("e %d\n", speed));
    }

    private void usbWrite(String command) {
        try {
            sPort.write(command.getBytes(), 1000);
            sPort.purgeHwBuffers(true, true);
            log("USB >> " + command);
        } catch (IOException e) {
            e.printStackTrace();
            log("USB error: " + e.getMessage());
        }


    }

    @Override
    public void setAngle(int angle) {
        if (sPort == null)
            return;
        usbWrite(String.format("r %d\n", angle));
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
        disconnect();
        stopIoManager();
        super.onDestroy();
    }

    private void disconnect() {
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sPort = null;
        }
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
        log("WS server started");
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

    @Background
    void registerService(int port) {
        log("init service registration");
        mNetworkDiscovery = new NetworkDiscovery(this);
        log("start service registration");
        mNetworkDiscovery.startServer(port);
        log("listening " + mNetworkDiscovery.getServerInfo());
    }


    private void stopIoManager() {
        if (mSerialIoManager != null) {
            log("Stopping io manager ...");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            log("Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, this);
            mExecutor.submit(mSerialIoManager);
        }
    }


    private void connect() {
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (sPort == null) {
            log("Connecting");
            final List<UsbSerialDriver> drivers =
                    UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
            if (drivers.size() > 0) {
                UsbSerialDriver driver = drivers.get(0);
                List<UsbSerialPort> ports = driver.getPorts();
                log(String.format("+ %s: %s port%s",
                        driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                if (ports.size() > 0)
                    sPort = ports.get(0);
            }
        }
        if (sPort == null) {
            log("No serial device.");
        } else {
            log("Has serial device.");


            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                log("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                log("Error setting up device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            log("Serial device connected.");
        }
        onDeviceStateChange();
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

}
