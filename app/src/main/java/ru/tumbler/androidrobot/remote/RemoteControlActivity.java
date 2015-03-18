package ru.tumbler.androidrobot.remote;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.WindowFeature;

import ru.tumbler.androidrobot.R;

@WindowFeature({ Window.FEATURE_NO_TITLE})
@Fullscreen
@EActivity(R.layout.activity_remote_control)
public class RemoteControlActivity extends Activity {

    @ViewById(R.id.surfaceView)
    DrawView mSurfaceView;

    @Touch(R.id.surfaceView)
    void onTouch(View v, MotionEvent event) {}
}
