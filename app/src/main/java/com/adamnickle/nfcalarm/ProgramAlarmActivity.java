package com.adamnickle.nfcalarm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ProgramAlarmActivity extends AppCompatActivity
{
    private ProgramAlarmActivityFragment mFragment;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_program_alarm );

        mFragment = (ProgramAlarmActivityFragment)getSupportFragmentManager()
                .findFragmentById( R.id.programAlarmFragment );
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        super.onNewIntent( intent );

        if( mFragment != null )
        {
            mFragment.onNewIntent( intent );
        }
    }
}
