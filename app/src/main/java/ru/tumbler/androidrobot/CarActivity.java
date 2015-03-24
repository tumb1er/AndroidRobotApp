package ru.tumbler.androidrobot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import ru.tumbler.androidrobot.service.RobotService;
import ru.tumbler.androidrobot.service.RobotService_;


@OptionsMenu(R.menu.menu_car)
@EActivity(R.layout.activity_car)
public class CarActivity extends ActionBarActivity implements RobotService.LogListener {

    private static final String LOG_TAG = CarActivity.class.getName();
    @ViewById(R.id.consoleText)
    TextView mConsole;
    private boolean mIsBound;
    private List<String> mBuffer = new ArrayList<String>();

    @UiThread
    public void log(String message) {
       updateReceivedData(message);
    }

    @AfterViews
    void init() {
        mConsole.setMovementMethod(new ScrollingMovementMethod());
    }

    private RobotService mBoundService;

    private void updateReceivedData(String data) {
        if (mBuffer.size() == 50)
            mBuffer.remove(0);
        mBuffer.add(data);

        StringBuilder builder = new StringBuilder();
        for (String s: mBuffer) {
            builder.append(s);
            builder.append('\n');
        }
        mConsole.setText(builder.toString());
    }


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
            log("Car: Unbinding service");
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
        // stopService();

        super.onDestroy();
    }

    @OptionsItem(R.id.action_stop)
    protected void stopService() {
                doUnbindService();
        log("Car: stopping service");
        stopService(new Intent(this, RobotService_.class));
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
