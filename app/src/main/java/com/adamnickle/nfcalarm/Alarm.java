package com.adamnickle.nfcalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Adam on 8/27/2015.
 */
public final class Alarm
{
    private static final int ALARM_REQUEST_CODE = 1001;
    private static final int SHOW_ALARM_REQUEST_CODE = 1002;

    private static final String PREF_HAS_ALARM = "pref_has_alarm";
    private static final String PREF_ALARM_TIME = "pref_alarm_time";
    private static final String PREF_ALARM_ENABLED = "pref_alarm_enabled";
    private static final String PREF_ALARM_ID = "pref_alarm_id";

    private Calendar Time;
    private boolean Enabled = false;
    private String mId;

    private Alarm()
    {
        this.Time = Calendar.getInstance();
        this.Enabled = false;
        mId = UUID.randomUUID().toString();
    }

    public void setTime( int hourOfDay, int minute )
    {
        Time.set( Calendar.HOUR_OF_DAY, hourOfDay );
        Time.set( Calendar.MINUTE, minute );
        this.updateToFuture();
    }

    public Calendar getTime()
    {
        return (Calendar)Time.clone();
    }

    public boolean isEnabled()
    {
        return Enabled;
    }

    public void setEnabled( boolean enabled )
    {
        Enabled = enabled;
    }

    public String getID()
    {
        return mId;
    }

    private static SharedPreferences getSharedPreferences( Context context )
    {
        return context.getSharedPreferences( BuildConfig.APPLICATION_ID + ".alarm_pref", Context.MODE_PRIVATE );
    }

    public static Alarm getInstance()
    {
        return new Alarm();
    }

    public static Alarm getAlarm( Context context )
    {
        final SharedPreferences prefs = Alarm.getSharedPreferences( context );

        if( prefs.getBoolean( PREF_HAS_ALARM, false ) )
        {
            final Alarm alarm = new Alarm();
            alarm.Time.setTimeInMillis( prefs.getLong( PREF_ALARM_TIME, System.currentTimeMillis() ) );
            alarm.Enabled = prefs.getBoolean( PREF_ALARM_ENABLED, false );
            alarm.mId = prefs.getString( PREF_ALARM_ID, alarm.mId );
            return alarm;
        }
        else
        {
            return Alarm.getInstance();
        }
    }

    private void updateToFuture()
    {
        final Calendar now = Calendar.getInstance();
        Time.set( now.get( Calendar.YEAR ), now.get( Calendar.MONTH ), now.get( Calendar.DATE ) );
        if( Time.before( now ) )
        {
            Time.add( Calendar.DATE, 1 );
        }
    }

    private void saveAlarm( Context context )
    {
        Alarm.getSharedPreferences( context )
                .edit()
                .putBoolean( PREF_HAS_ALARM, true )
                .putLong( PREF_ALARM_TIME, Time.getTimeInMillis() )
                .putBoolean( PREF_ALARM_ENABLED, Enabled )
                .putString( PREF_ALARM_ID, mId )
                .commit();
    }

    private PendingIntent createAlarmPendingIntent( Context context )
    {
        final Intent intent = new Intent( context, AlarmReceiver.class );
        intent.setAction( AlarmReceiver.ACTION_ALARM );
        return PendingIntent.getBroadcast( context, Alarm.ALARM_REQUEST_CODE, intent, 0 );
    }

    public PendingIntent createShowPendingIntent( Context context )
    {
        final Intent intent = new Intent( context, AlarmActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        return PendingIntent.getActivity( context, SHOW_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    public void activateAlarm( Context context )
    {
        if( !Enabled )
        {
            Enabled = true;
        }
        this.updateToFuture();
        this.saveAlarm( context );

        final PendingIntent alarmPendingIntent = createAlarmPendingIntent( context );
        final PendingIntent showPendingIntent = createShowPendingIntent( context );

        final AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo( Time.getTimeInMillis(), showPendingIntent );

        final AlarmManager manager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        manager.setAlarmClock( info, alarmPendingIntent );
    }

    public void cancelAlarm( Context context )
    {
        if( Enabled )
        {
            Enabled = false;
        }
        this.saveAlarm( context );

        final PendingIntent pendingIntent = createAlarmPendingIntent( context );

        final AlarmManager manager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        manager.cancel( pendingIntent );

        AlarmService.stopAlarm( context );
    }
}
