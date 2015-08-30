package com.adamnickle.nfcalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Adam on 8/27/2015.
 */
public final class Alarm
{
    public static final String EXTRA_ALARM = BuildConfig.APPLICATION_ID + ".extra.alarm";

    private static final Object LOCK = new Object();

    private static final int ALARM_REQUEST_CODE = 1001;
    private static final int SHOW_ALARM_REQUEST_CODE = 1002;

    private static final String PREF_ALL_ALARMS = "pref_all_alarms";

    private static final String PREF_SAVED_TO_ALL = "pref_alarm_saved_to_all";
    private static final String PREF_ALARM_TIME_HOUR = "pref_alarm_time_hour";
    private static final String PREF_ALARM_TIME_MINUTE = "pref_alarm_time_minute";
    private static final String PREF_ALARM_ENABLED = "pref_alarm_enabled";
    private static final String PREF_ALARM_ID = "pref_alarm_id";
    private static final String PREF_ALARM_REPEATS = "pref_alarm_repeats";
    private static final String PREF_ALARM_DAYS = "pref_alarm_days";
    private static final String PREF_ALARM_SOUND = "pref_alarm_sound";
    private static final String PREF_ALARM_VIBRATES = "pref_alarm_vibrates";

    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;

    private static final int[] CALENDAR_TO_ALARM = { 0, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY };

    private boolean mSavedToAll;
    private int mHour;
    private int mMinute;
    private boolean mEnabled = false;
    private boolean mRepeats;
    private int mDays;
    private String mId;
    private Uri mAlarmSound;
    private boolean mVibrates;

    private Alarm()
    {
        this.mSavedToAll = false;
        this.mHour = 0;
        this.mMinute = 0;
        this.mEnabled = false;
        this.mRepeats = false;
        this.mDays = 0;
        this.mId = UUID.randomUUID().toString();
        this.mAlarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
        this.mVibrates = true;
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
        if( getRepeats() )
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

    public void setVibrates( boolean vibrates )
    {
        mVibrates = vibrates;
    }

    public boolean getVibrates()
    {
        return mVibrates;
    }

    private static String getPreferenceFileName( String id )
    {
        return BuildConfig.APPLICATION_ID + "." + id;
    }

    private static SharedPreferences getSharedPreferences( Context context, String id )
    {
        return context.getSharedPreferences( Alarm.getPreferenceFileName( id ), Context.MODE_PRIVATE );
    }

    private static SharedPreferences getAllAlarmPreferences( Context context )
    {
        return Alarm.getSharedPreferences( context, "ALL_ALARMS" );
    }

    public static Alarm[] getAlarms( Context context )
    {
        final Set<String> alarmIdsSet = Alarm.getAlarmIds( context );
        final String[] alarmIds = alarmIdsSet.toArray( new String[ alarmIdsSet.size() ] );
        final Alarm[] alarms = new Alarm[ alarmIds.length ];
        for( int i = 0; i < alarms.length; i++ )
        {
            alarms[ i ] = Alarm.getAlarm( context, alarmIds[ i ] );
        }
        return alarms;
    }

    public static Set<String> getAlarmIds( Context context )
    {
        final SharedPreferences prefs = Alarm.getAllAlarmPreferences( context );
        final Set<String> emptySet = Collections.emptySet();
        return prefs.getStringSet( PREF_ALL_ALARMS, emptySet );
    }

    public static Alarm getAlarm( Context context, @NonNull String id )
    {
        final SharedPreferences prefs = Alarm.getSharedPreferences( context, id );

        final Alarm alarm = new Alarm();
        alarm.mSavedToAll = prefs.getBoolean( PREF_SAVED_TO_ALL, false );
        alarm.mHour = prefs.getInt( PREF_ALARM_TIME_HOUR, 0 );
        alarm.mMinute = prefs.getInt( PREF_ALARM_TIME_MINUTE, 0 );
        alarm.mEnabled = prefs.getBoolean( PREF_ALARM_ENABLED, false );
        alarm.mRepeats = prefs.getBoolean( PREF_ALARM_REPEATS, false );
        alarm.mDays = prefs.getInt( PREF_ALARM_DAYS, 0 );
        alarm.mId = prefs.getString( PREF_ALARM_ID, id );
        final String uriString = prefs.getString( PREF_ALARM_SOUND, null );
        if( uriString != null )
        {
            alarm.mAlarmSound = Uri.parse( uriString );
        }
        alarm.mVibrates = prefs.getBoolean( PREF_ALARM_VIBRATES, true );
        return alarm;
    }

    public static Alarm createNewAlarm( Context context )
    {
        return new Alarm();
    }

    public void deleteAlarm( Context context )
    {
        synchronized( LOCK )
        {
            final Set<String> alarmIdsSet = Alarm.getAlarmIds( context );
            alarmIdsSet.remove( getID() );
            Alarm.getAllAlarmPreferences( context )
                    .edit()
                    .putStringSet( PREF_ALL_ALARMS, alarmIdsSet )
                    .commit();

            Alarm.getSharedPreferences( context, getID() )
                    .edit().clear().commit();

            final File dir = new File( context.getFilesDir().getParent() + "/shared_prefs/" );
            final String filename = Alarm.getPreferenceFileName( getID() ) + ".xml";
            final boolean deleted = new File( dir, filename ).delete();
        }
    }

    public void saveAlarm( Context context )
    {
        synchronized( LOCK )
        {
            if( !mSavedToAll )
            {
                final Set<String> alarmIdsSet = Alarm.getAlarmIds( context );
                alarmIdsSet.add( getID() );
                mSavedToAll = Alarm.getAllAlarmPreferences( context )
                        .edit()
                        .putStringSet( PREF_ALL_ALARMS, alarmIdsSet )
                        .commit();
            }

            Alarm.getSharedPreferences( context, getID() )
                    .edit()
                    .putBoolean( PREF_SAVED_TO_ALL, mSavedToAll )
                    .putInt( PREF_ALARM_TIME_HOUR, mHour )
                    .putInt( PREF_ALARM_TIME_MINUTE, mMinute )
                    .putBoolean( PREF_ALARM_ENABLED, mEnabled )
                    .putBoolean( PREF_ALARM_REPEATS, mRepeats )
                    .putInt( PREF_ALARM_DAYS, mDays )
                    .putString( PREF_ALARM_ID, mId )
                    .putString( PREF_ALARM_SOUND, mAlarmSound.toString() )
                    .putBoolean( PREF_ALARM_VIBRATES, mVibrates )
                    .commit();
        }
    }

    private PendingIntent createAlarmPendingIntent( Context context )
    {
        final Intent intent = new Intent( context, AlarmReceiver.class );
        intent.setAction( AlarmReceiver.ACTION_ALARM );
        intent.putExtra( Alarm.EXTRA_ALARM, getID() );
        return PendingIntent.getBroadcast( context, Alarm.ALARM_REQUEST_CODE, intent, 0 );
    }

    public PendingIntent createShowPendingIntent( Context context )
    {
        final Intent intent = new Intent( context, AlarmsActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        intent.putExtra( Alarm.EXTRA_ALARM, getID() );
        return PendingIntent.getActivity( context, SHOW_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    public void activateAlarm( Context context )
    {
        synchronized( LOCK )
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
    }

    public void dismissAlarm( Context context )
    {
        synchronized( LOCK )
        {
            this.cancelAlarm( context );

            if( this.getRepeats() )
            {
                this.activateAlarm( context );
            }
        }
    }

    public void cancelAlarm( Context context )
    {
        synchronized( LOCK )
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

    @Override
    public boolean equals( Object o )
    {
        return this == o
            || ( o instanceof Alarm
              && mId.equals( ( (Alarm)o ).mId ) );

    }

    @Override
    public int hashCode()
    {
        return mId.hashCode();
    }
}
