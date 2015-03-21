package ru.tumbler.androidrobot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import ru.tumbler.androidrobot.service.RobotService;
import ru.tumbler.androidrobot.service.RobotService_;


@EActivity(R.layout.activity_car)
public class CarActivity extends ActionBarActivity implements RobotService.LogListener {

    private static final String LOG_TAG = CarActivity.class.getName();
    @ViewById(R.id.consoleText)
    TextView mConsole;
    private boolean mIsBound;

    @UiThread
    public void log(String message) {
        if (mConsole == null)
            return;
        mConsole.append(message);
        if (!message.endsWith("\n"))
            mConsole.append("\n");
    }

    private RobotService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            log("Car: Local service connected");
            mBoundService = ((RobotService.RobotBinder)service).getService();
            mBoundService.setLogListener(CarActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            log("Car: Local service disconnected");
            mBoundService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, RobotService_.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
//        doUnbindService();
//        stopService(new Intent(this, RobotService_.class));
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("Car: Starting service");
        startService(new Intent(this, RobotService_.class));
        log("Car: doBindService");
        doBindService();
    }
}
