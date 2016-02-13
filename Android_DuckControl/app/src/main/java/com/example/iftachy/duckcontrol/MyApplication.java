package com.example.iftachy.duckcontrol;

import android.app.Application;
import android.content.Context;

/**
 * Created by iftachy on 20/04/2015.
 */
//needed for getting appliation context in static methods
public class MyApplication extends Application {

    private static Context context;

    public void onCreate(){
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}