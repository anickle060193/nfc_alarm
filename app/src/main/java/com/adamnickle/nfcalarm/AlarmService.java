package com.adamnickle.nfcalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class AlarmService extends Service
{
    public interface OnAnnoyingListener
    {
        void onAnnoyingStart();
        void onAnnoyingStop();
    }

    private static final int ALARM_NOTIFICATION_ID = 1001;
    private static final long[] VIBRATE_PATTERN = new long[]{ 100, 200, 100, 200, 100, 50, 100, 50, 100, 200, 100, 200, 100, 50, 100, 50, 100, 50 };

    private static boolean mIsAnnoying;
    private static List<OnAnnoyingListener> mListeners = new ArrayList<>();

    private Alarm mAlarm;
    private Ringtone mRingtone;
    private Vibrator mVibrator;

    @Override
    public IBinder onBind( Intent intent )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        mAlarm = Alarm.getAlarm( this );
        startAnnoying();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        stopAnnoying();
    }

    public static boolean isAnnoying()
    {
        return mIsAnnoying;
    }

    public static void addOnAnnoyingListener( OnAnnoyingListener listener )
    {
        mListeners.add( listener );
    }

    public static void removeOnAnnoyingListener( OnAnnoyingListener listener )
    {
        mListeners.remove( listener );
    }

    private void startAnnoying()
    {
        final Notification notification = new NotificationCompat.Builder( getApplicationContext() )
                .setSmallIcon( R.drawable.ic_notification )
                .setContentTitle( "NFC Alarm" )
                .setContentText( "Scan programmed NFC tag to stop alarm." )
                .setOngoing( true )
                .setContentIntent( mAlarm.createShowPendingIntent( this ) )
                .build();
        final NotificationManager manager = (NotificationManager)getSystemService( Context.NOTIFICATION_SERVICE );
        manager.notify( ALARM_NOTIFICATION_ID, notification );

        mRingtone = RingtoneManager.getRingtone( this, mAlarm.getAlarmSound() );
        if( mRingtone == null )
        {
            final Uri uri = RingtoneManager.getActualDefaultRingtoneUri( this, RingtoneManager.TYPE_ALARM );
            mRingtone = RingtoneManager.getRingtone( this, uri );
        }
        final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage( AudioAttributes.USAGE_ALARM )
                .build();
        mRingtone.setAudioAttributes( audioAttributes );
        mRingtone.play();

        mVibrator = (Vibrator)getSystemService( Context.VIBRATOR_SERVICE );
        mVibrator.vibrate( VIBRATE_PATTERN, 0, audioAttributes );

        mIsAnnoying = true;
        for( OnAnnoyingListener listener : mListeners )
        {
            listener.onAnnoyingStart();
        }
    }

    private void stopAnnoying()
    {
        final NotificationManager manager = (NotificationManager)getSystemService( Context.NOTIFICATION_SERVICE );
        manager.cancel( ALARM_NOTIFICATION_ID );

        if( mRingtone != null && mRingtone.isPlaying() )
        {
            mRingtone.stop();
        }

        if( mVibrator != null )
        {
            mVibrator.cancel();
        }

        mIsAnnoying = false;
        for( OnAnnoyingListener listener : mListeners )
        {
            listener.onAnnoyingStop();
        }
    }

    private static Intent createAlarmIntent( Context context )
    {
        return new Intent( context, AlarmService.class );
    }

    public static void startAlarm( Context context )
    {
        if( !AlarmService.isAnnoying() )
        {
            context.startService( AlarmService.createAlarmIntent( context ) );
        }
    }

    public static void stopAlarm( Context context )
    {
        context.stopService( AlarmService.createAlarmIntent( context ) );
    }
}
