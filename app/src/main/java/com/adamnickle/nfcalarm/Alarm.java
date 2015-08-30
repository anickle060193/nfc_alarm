package com.adamnickle.nfcalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.text.format.DateUtils;

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
    private static final String PREF_ALARM_TIME_HOUR = "pref_alarm_time_hour";
    private static final String PREF_ALARM_TIME_MINUTE = "pref_alarm_time_minute";
    private static final String PREF_ALARM_ENABLED = "pref_alarm_enabled";
    private static final String PREF_ALARM_ID = "pref_alarm_id";
    private static final String PREF_ALARM_REPEATS = "pref_alarm_repeats";
    private static final String PREF_ALARM_DAYS = "pref_alarm_days";
    private static final String PREF_ALARM_SOUND = "pref_alarm_sound";

    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;

    private static final int[] CALENDAR_TO_ALARM = { 0, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY };

    private int mHour;
    private int mMinute;
    private boolean mEnabled = false;
    private boolean mRepeats;
    private int mDays;
    private String mId;
    private Uri mAlarmSound;

    private Alarm()
    {
        this.mHour = 0;
        this.mMinute = 0;
        this.mEnabled = false;
        this.mDays = 0;
        this.mId = UUID.randomUUID().toString();
        this.mAlarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
    }

    public void setTime( int hourOfDay, int minute )
    {
        this.mHour = hourOfDay;
        this.mMinute = minute;
    }

    public Calendar getNextAlarmTime()
    {
        final Calendar now = Calendar.getInstance();
        final Calendar alarmTime = (Calendar)now.clone();
        alarmTime.set( Calendar.HOUR_OF_DAY, mHour );
        alarmTime.set( Calendar.MINUTE, mMinute );
        alarmTime.set( Calendar.SECOND, 0 );
        if( alarmTime.before( now ) )
        {
            alarmTime.add( Calendar.DATE, 1 );
        }
        if( mRepeats )
        {
            while( !getDay( CALENDAR_TO_ALARM[ alarmTime.get( Calendar.DAY_OF_WEEK ) ] ) )
            {
                alarmTime.add( Calendar.DATE, 1 );
            }
        }
        return alarmTime;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled( boolean enabled )
    {
        mEnabled = enabled;
    }

    public String getID()
    {
        return mId;
    }

    public void setRepeat( boolean repeat )
    {
        mRepeats = repeat;
        setDay( SUNDAY, mRepeats );
        setDay( MONDAY, mRepeats );
        setDay( TUESDAY, mRepeats );
        setDay( WEDNESDAY, mRepeats );
        setDay( THURSDAY, mRepeats );
        setDay( FRIDAY, mRepeats );
        setDay( SATURDAY, mRepeats );
    }

    public boolean getRepeats()
    {
        return mRepeats && mDays != 0;
    }

    public void setDay( int day, boolean enabled )
    {
        if( enabled )
        {
            mDays |= ( 1 << day );
        }
        else
        {
            mDays &= ~( 1 << day );
        }
    }

    public boolean getDay( int day )
    {
        return ( mDays & ( 1 << day ) ) != 0;
    }

    public String getAlarmActivationString()
    {
        final long now = System.currentTimeMillis();
        final long alarm = getNextAlarmTime().getTimeInMillis();
        final String alarmString = DateUtils.getRelativeTimeSpanString( alarm, now, 0 ).toString();
        return "Alarm to sound " + alarmString;
    }

    public void setAlarmSound( Uri uri )
    {
        mAlarmSound = uri;
    }

    public Uri getAlarmSound()
    {
        return mAlarmSound;
    }

    private static SharedPreferences getSharedPreferences( Context context )
    {
        return context.getSharedPreferences( BuildConfig.APPLICATION_ID + ".alarm_pref", Context.MODE_PRIVATE );
    }

    public static Alarm getAlarm( Context context )
    {
        final SharedPreferences prefs = Alarm.getSharedPreferences( context );

        if( prefs.getBoolean( PREF_HAS_ALARM, false ) )
        {
            final Alarm alarm = new Alarm();
            alarm.mHour = prefs.getInt( PREF_ALARM_TIME_HOUR, 0 );
            alarm.mMinute = prefs.getInt( PREF_ALARM_TIME_MINUTE, 0 );
            alarm.mEnabled = prefs.getBoolean( PREF_ALARM_ENABLED, false );
            alarm.mRepeats = prefs.getBoolean( PREF_ALARM_REPEATS, false );
            alarm.mDays = prefs.getInt( PREF_ALARM_DAYS, 0 );
            alarm.mId = prefs.getString( PREF_ALARM_ID, alarm.mId );
            final String uriString = prefs.getString( PREF_ALARM_SOUND, null );
            if( uriString != null )
            {
                alarm.mAlarmSound = Uri.parse( uriString );
            }
            return alarm;
        }
        else
        {
            return new Alarm();
        }
    }

    private void saveAlarm( Context context )
    {
        Alarm.getSharedPreferences( context )
                .edit()
                .putBoolean( PREF_HAS_ALARM, true )
                .putInt( PREF_ALARM_TIME_HOUR, mHour )
                .putInt( PREF_ALARM_TIME_MINUTE, mMinute )
                .putBoolean( PREF_ALARM_ENABLED, mEnabled )
                .putBoolean( PREF_ALARM_REPEATS, mRepeats )
                .putInt( PREF_ALARM_DAYS, mDays )
                .putString( PREF_ALARM_ID, mId )
                .putString( PREF_ALARM_SOUND, mAlarmSound.toString() )
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
        if( !mEnabled )
        {
            mEnabled = true;
        }
        this.saveAlarm( context );

        final PendingIntent alarmPendingIntent = createAlarmPendingIntent( context );
        final PendingIntent showPendingIntent = createShowPendingIntent( context );

        final AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo( getNextAlarmTime().getTimeInMillis(), showPendingIntent );

        final AlarmManager manager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        manager.setAlarmClock( info, alarmPendingIntent );
    }

    public void dismissAlarm( Context context )
    {
        this.cancelAlarm( context );

        if( this.getRepeats() )
        {
            this.activateAlarm( context );
        }
    }

    public void cancelAlarm( Context context )
    {
        if( mEnabled )
        {
            mEnabled = false;
        }
        this.saveAlarm( context );

        final PendingIntent pendingIntent = createAlarmPendingIntent( context );

        final AlarmManager manager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        manager.cancel( pendingIntent );

        AlarmService.stopAlarm( context );
    }
}
