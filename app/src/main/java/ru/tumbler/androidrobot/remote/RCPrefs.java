package ru.tumbler.androidrobot.remote;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by tumbler on 30.03.15.
 */
@SharedPref
public interface RCPrefs {

    // The field name will have default value "John"
    String carIpAddress();
}

