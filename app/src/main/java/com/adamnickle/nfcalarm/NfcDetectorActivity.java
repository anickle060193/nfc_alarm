package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

import java.io.IOException;

public class NfcDetectorActivity extends Activity
{
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        if( AlarmService.isAnnoying() )
        {
            if( matchesAlarmId() )
            {
                AlarmService.stopAlarm( this );
            }
        }
        else
        {
            final Intent intent = new Intent( this, AlarmActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_NO_ANIMATION );
            startActivity( intent );
        }
        finish();
    }

    private boolean matchesAlarmId()
    {
        final Alarm alarm = Alarm.getAlarm( this );

        final Tag tag = getIntent().getParcelableExtra( NfcAdapter.EXTRA_TAG );
        if( tag == null )
        {
            return false;
        }

        final Ndef ndef = Ndef.get( tag );
        if( ndef == null )
        {
            return false;
        }

        try
        {
            ndef.connect();
            final NdefMessage message = ndef.getNdefMessage();
            if( message == null )
            {
                return false;
            }

            for( NdefRecord record : message.getRecords() )
            {
                final String mimeType = record.toMimeType();
                if( mimeType != null && mimeType.equals( getString( R.string.mime_type ) ) )
                {
                    final String id = new String( record.getPayload(), "UTF-8" );
                    if( alarm.getID().equals( id ) )
                    {
                        return true;
                    }
                }
            }
        }
        catch( IOException | FormatException e )
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                ndef.close();
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
        }
        return false;
    }
}
