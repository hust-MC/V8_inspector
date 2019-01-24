package com.example.zhanfang.test;

import android.app.Application;
import android.content.Context;

public class MainApplication extends Application {

    private static MainApplication instance = null;

    public MainApplication() {
        super();
        instance = this;
    }

    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static MainApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

}
