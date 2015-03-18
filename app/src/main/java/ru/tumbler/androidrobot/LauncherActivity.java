package ru.tumbler.androidrobot;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;

import ru.tumbler.androidrobot.remote.RemoteControlActivity_;


@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends ActionBarActivity {
    @Click(R.id.btnCar)
    void launchCarActivity() {
        startActivity(new Intent(this, CarActivity_.class));
    };

    @Click(R.id.btnRemote)
    void launchRemoteControlActivity() {
        startActivity(new Intent(this, RemoteControlActivity_.class));
    };

    @Click(R.id.btnCalibrate)
    void launchCalibrateActivity() {};
}
