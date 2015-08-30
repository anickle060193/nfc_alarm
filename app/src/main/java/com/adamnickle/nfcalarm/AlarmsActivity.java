package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class AlarmsActivity extends Activity
{
    private AlarmsActivityFragment mFragment;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_single_fragment );

        mFragment = AlarmsActivityFragment.createInstance();
        getFragmentManager()
                .beginTransaction()
                .add( R.id.content, mFragment )
                .commit();
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        mFragment.onNewIntent( intent );
    }
}
