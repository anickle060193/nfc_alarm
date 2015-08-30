package com.adamnickle.nfcalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver
{
    static final String ACTION_ALARM = BuildConfig.APPLICATION_ID + ".action_alarm";

    @Override
    public void onReceive( Context context, Intent intent )
    {
        final String action = intent.getAction();
        if( Intent.ACTION_BOOT_COMPLETED.equals( action ) )
        {
            final Alarm[] alarms = Alarm.getAlarms( context );
            for( Alarm alarm : alarms )
            {
                if( alarm.isEnabled() )
                {
                    alarm.activateAlarm( context );
                }
            }
        }
        else if( ACTION_ALARM.equals( action ) )
        {
            AlarmService.startAlarm( context );
        }
    }
}
