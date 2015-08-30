package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AlarmActivity extends Activity
{
    private static final int REQUEST_CREATE_ALARM = 1001;

    @Bind( R.id.alarmClock ) View mAlarmClock;
    @Bind( R.id.setAlarm) Button mSetAlarm;
    @Bind( R.id.createAlarm) Button mCreateAlarm;
    @Bind( R.id.clearAlarm ) Button mClearAlarm;

    private Alarm mAlarm;

    private boolean mIsVisible;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_alarm );
        ButterKnife.bind( this );

        mAlarm = Alarm.getAlarm( this );

        updateAlarmTime();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        updateActions();

        mIsVisible = true;

        final Handler handler = new Handler();
        handler.post( new Runnable()
        {
            @Override
            public void run()
            {
                updateActions();

                if( mIsVisible )
                {
                    handler.postDelayed( this, 100 );
                }
            }
        } );
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mIsVisible = false;
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == REQUEST_CREATE_ALARM )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                mAlarm = Alarm.getAlarm( this );
                onAlarmSet();
                updateAlarmTime();
            }
        }
    }

    @OnClick( R.id.setAlarm )
    void onSetAlarmClick()
    {
        if( !AlarmService.isAnnoying() )
        {
            mAlarm.activateAlarm( this );
            onAlarmSet();
        }
    }

    @OnClick( R.id.createAlarm )
    void onCreateAlarmClick()
    {
        if( !AlarmService.isAnnoying() )
        {
            startActivityForResult( new Intent( this, ProgramAlarmActivity.class ), REQUEST_CREATE_ALARM );
        }
    }

    @OnClick( R.id.clearAlarm )
    void onClearAlarmClick()
    {
        if( !AlarmService.isAnnoying() )
        {
            mAlarm.cancelAlarm( this );
        }
    }

    private void onAlarmSet()
    {
        Toast.makeText( this, mAlarm.getAlarmActivationString(), Toast.LENGTH_SHORT ).show();
    }

    private void updateAlarmTime()
    {
        ClockViewHelper.setTime( mAlarmClock, mAlarm.getNextAlarmTime() );
    }

    private void updateActions()
    {
        if( AlarmService.isAnnoying() )
        {
            mSetAlarm.setVisibility( View.GONE );
            mCreateAlarm.setVisibility( View.GONE );
            mClearAlarm.setVisibility( View.GONE );
        }
        else
        {
            mSetAlarm.setVisibility( View.VISIBLE );
            mCreateAlarm.setVisibility( View.VISIBLE );
            mClearAlarm.setVisibility( View.VISIBLE );
        }
    }
}
