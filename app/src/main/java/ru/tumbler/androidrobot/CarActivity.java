package ru.tumbler.androidrobot;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;


@EActivity(R.layout.activity_car)
public class CarActivity extends ActionBarActivity {

    @ViewById(R.id.consoleText)
    TextView mConsole;

    void log(String message) {
        mConsole.append(message);
        if (!message.endsWith("\n"))
            mConsole.append("\n");
    }
}
