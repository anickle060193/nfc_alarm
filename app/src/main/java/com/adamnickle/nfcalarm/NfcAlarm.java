package com.adamnickle.nfcalarm;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by Adam on 8/30/2015.
 */
public class NfcAlarm extends Application
{
    private static Context sContext;

    @Override
    public void onCreate()
    {
        super.onCreate();

        sContext = this;
    }

    static void toast( final String message )
    {
        Toast.makeText( sContext, message, Toast.LENGTH_SHORT ).show();
    }
}
