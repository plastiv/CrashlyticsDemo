package com.github.plastiv.crashlyticsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.Crashlytics;

public class CrashlyticsDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crashlytics_demo);

        findViewById(R.id.buttonHandled).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    throw new RuntimeException("Check Crashlytics handled exceptions are working");
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        });

        findViewById(R.id.buttonUnhandled).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                throw new RuntimeException("Check Crashlytics Unhandled exceptions are working");
            }
        });
    }
}
