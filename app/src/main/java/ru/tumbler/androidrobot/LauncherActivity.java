package ru.tumbler.androidrobot;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;


@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends ActionBarActivity {
    @Click(R.id.btnCar)
    void launchCarActivity() {
        startActivity(new Intent(this, CarActivity_.class));
    };

    @Click(R.id.btnRemote)
    void launchRemoteControlActivity() {};

    @Click(R.id.btnCalibrate)
    void launchCalibrateActivity() {};
}
