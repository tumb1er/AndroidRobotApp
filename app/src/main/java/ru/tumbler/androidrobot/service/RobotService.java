package ru.tumbler.androidrobot.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.androidannotations.annotations.EService;

@EService
public class RobotService extends Service implements IRobot {

    private static final String LOG_TAG = RobotService.class.getName();

    public interface LogListener {
        void log(String message);
    }

    private LogListener mLogListener;

    private final IBinder mBinder = new RobotBinder();

    public RobotService() {}

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

}
