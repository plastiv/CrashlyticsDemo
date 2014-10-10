package com.github.plastiv.crashlyticsdemo;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

public class CrashlyticsDemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.start(this);
        }
    }
}
