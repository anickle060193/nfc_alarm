package com.adamnickle.nfcalarm;

import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Adam on 8/28/2015.
 */
public final class ClockViewHelper
{
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat( "h:mm", Locale.getDefault() );

    private ClockViewHelper() { }

    public static void setTime( View clock, Calendar time )
    {
        if( clock == null )
        {
            return;
        }
        final TextView timeView = (TextView)clock.findViewById( R.id.clockTime );
        final View amView = clock.findViewById( R.id.clockAm );
        final View pmView = clock.findViewById( R.id.clockPm );

        if( time != null )
        {
            timeView.setText( TIME_FORMAT.format( time.getTime() ) );

            if( time.get( Calendar.AM_PM ) == Calendar.AM )
            {
                amView.setVisibility( View.VISIBLE );
                pmView.setVisibility( View.INVISIBLE );
            }
            else
            {
                amView.setVisibility( View.INVISIBLE );
                pmView.setVisibility( View.VISIBLE );
            }
        }
        else
        {
            timeView.setText( "--:--" );
            amView.setVisibility( View.GONE );
            pmView.setVisibility( View.GONE );
        }
    }
}
