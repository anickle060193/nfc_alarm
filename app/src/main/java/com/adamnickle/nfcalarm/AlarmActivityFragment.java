package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AlarmActivityFragment extends Fragment
{
    private static final int REQUEST_CREATE_ALARM = 1001;

    private View mMainView;
    @Bind( R.id.alarmClock ) View mAlarmClock;
    @Bind( R.id.setAlarm) Button mSetAlarm;
    @Bind( R.id.createAlarm) Button mCreateAlarm;
    @Bind( R.id.clearAlarm ) Button mClearAlarm;

    private Alarm mAlarm;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setRetainInstance( true );

        mAlarm = Alarm.getAlarm( getContext() );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        mMainView = inflater.inflate( R.layout.fragment_alarm, container, false );
        ButterKnife.bind( this, mMainView );

        updateTime();

        return mMainView;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        updateActions();
        new Runnable()
        {
            @Override
            public void run()
            {
                updateActions();

                if( AlarmActivityFragment.this.isResumed() )
                {
                    mMainView.postDelayed( this, 100 );
                }
            }
        }.run();
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == REQUEST_CREATE_ALARM )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                mAlarm = Alarm.getAlarm( getContext() );
                onAlarmSet();
                updateTime();
            }
        }
    }

    @OnClick( R.id.setAlarm )
    void onSetAlarmClick()
    {
        mAlarm.activateAlarm( getContext() );
        onAlarmSet();
    }

    @OnClick( R.id.createAlarm )
    void onCreateAlarmClick()
    {
        startActivityForResult( new Intent( getContext(), ProgramAlarmActivity.class ), REQUEST_CREATE_ALARM );
    }

    @OnClick( R.id.clearAlarm )
    void onClearAlarmClick()
    {
        mAlarm.cancelAlarm( getContext() );
    }

    private void onAlarmSet()
    {
        final long now = System.currentTimeMillis();
        final long alarm = mAlarm.getTime().getTimeInMillis();
        final String alarmString = DateUtils.getRelativeTimeSpanString( alarm, now, 0 ).toString();
        Toast.makeText( getContext(), "Alarm set for " + alarmString, Toast.LENGTH_SHORT ).show();
    }

    private void updateTime()
    {
        ClockViewHelper.setTime( mAlarmClock, mAlarm.getTime() );
    }

    private void updateActions()
    {
        if( AlarmService.isAnnoying() )
        {
            mSetAlarm.setVisibility( View.GONE );
            mCreateAlarm.setVisibility( View.GONE );
            mClearAlarm.setVisibility( View.GONE );
        }
    }
}
