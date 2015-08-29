package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.IOException;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ProgramAlarmActivityFragment extends Fragment
{
    private static final int REQUEST_NDEF_DISCOVERED = 1001;

    private View mMainView;
    @Bind( R.id.currentTime ) View mCurrentClock;
    @Bind( R.id.alarmTime ) View mAlarmClock;
    @Bind( R.id.tagProgrammingStatus ) TextView mTagProgrammingStatus;
    @Bind( R.id.done ) Button mDone;

    private Alarm mNewAlarm;

    private boolean mTagProgrammed;

    private AlertDialog mProgrammingAlertDialog;

    private PendingIntent mNdefDiscoveredPendingIntent;
    private IntentFilter[] mNdefIntentFilters;
    private String[][] mTechList;
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getActivity().setResult( Activity.RESULT_CANCELED );

        mNewAlarm = Alarm.getAlarm( getContext() );
        mTagProgrammed = false;

        final Intent nfcIntent = new Intent( getActivity(), ProgramAlarmActivity.class )
                .addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
        mNdefDiscoveredPendingIntent = PendingIntent.getActivity( getActivity(), REQUEST_NDEF_DISCOVERED, nfcIntent, 0 );
        final IntentFilter filter = new IntentFilter();
        filter.addAction( NfcAdapter.ACTION_NDEF_DISCOVERED );
        filter.addAction( NfcAdapter.ACTION_TECH_DISCOVERED );
        try
        {
            filter.addDataType( "*/*" );
        }
        catch( IntentFilter.MalformedMimeTypeException e )
        {
            e.printStackTrace();
        }
        mNdefIntentFilters = new IntentFilter[]{ filter };
        mTechList = new String[][]{ { Ndef.class.getName() } };

        mNfcAdapter = NfcAdapter.getDefaultAdapter( getContext() );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        mMainView = inflater.inflate( R.layout.fragment_program_alarm, container, false );
        ButterKnife.bind( this, mMainView );
        mMainView.post( new Runnable()
        {
            @Override
            public void run()
            {
                updateCurrentTime();
                mMainView.postDelayed( this, 100 );
            }
        } );
        updateAlarmTime();
        return mMainView;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mNfcAdapter.enableForegroundDispatch( getActivity(), mNdefDiscoveredPendingIntent, mNdefIntentFilters, mTechList );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch( getActivity() );
    }

    public void onNewIntent( Intent intent )
    {
        if( intent != null )
        {
            final Tag tag = intent.getParcelableExtra( NfcAdapter.EXTRA_TAG );
            if( tag != null )
            {
                mTagProgrammed = programTag( tag );
                if( mTagProgrammed )
                {
                    mTagProgrammingStatus.setText( "NFC tag has been programmed." );
                }
                mDone.setEnabled( mTagProgrammed );
            }
        }
    }

    private void updateCurrentTime()
    {
        ClockViewHelper.setTime( mCurrentClock, Calendar.getInstance() );
    }

    private void updateAlarmTime()
    {
        ClockViewHelper.setTime( mAlarmClock, mNewAlarm.getTime() );
    }

    @OnClick( R.id.alarmTime )
    void onAlarmTimeClick()
    {
        final Calendar time = mNewAlarm.getTime();
        final int hourOfDay = time.get( Calendar.HOUR_OF_DAY );
        final int minute = time.get( Calendar.MINUTE );
        new TimePickerDialog( getContext(), new TimePickerDialog.OnTimeSetListener()
        {
            @Override
            public void onTimeSet( TimePicker view, int hourOfDay, int minute )
            {
                mNewAlarm.setTime( hourOfDay, minute );
                updateAlarmTime();
            }
        }, hourOfDay, minute, false ).show();
    }

    @OnClick( R.id.cancel )
    void onCancelClick()
    {
        getActivity().finish();
    }

    @OnClick( R.id.done )
    void onDoneClick()
    {
        mNewAlarm.activateAlarm( getContext() );
        getActivity().setResult( Activity.RESULT_OK );
        getActivity().finish();
    }

    private boolean programTag( final Tag tag )
    {
        final Ndef ndef = Ndef.get( tag );
        if( ndef == null )
        {
            new AlertDialog.Builder( getContext() )
                    .setTitle( "Incompatible NFC Tag" )
                    .setMessage( "The NFC tag does not support NDEF format. Try again with a different tag." )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            dialog.dismiss();
                        }
                    } ).show();
            return false;
        }
        try
        {
            ndef.connect();
            final byte[] id = mNewAlarm.getID().getBytes( "UTF-8" );
            final NdefRecord idRecord = NdefRecord.createMime( getString( R.string.mime_type ), id );
            final NdefRecord appRecord = NdefRecord.createApplicationRecord( BuildConfig.APPLICATION_ID );
            final NdefMessage appMessage = new NdefMessage( idRecord, appRecord );
            ndef.writeNdefMessage( appMessage );
            return true;
        }
        catch( IOException ex )
        {
            ex.printStackTrace();

            new AlertDialog.Builder( getContext() )
                    .setTitle( "NFC Tag Connection Error" )
                    .setMessage( "Could not connect to NFC tag. Try again." )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            dialog.dismiss();
                        }
                    } ).show();
        }
        catch( FormatException ex )
        {
            ex.printStackTrace();

            new AlertDialog.Builder( getContext() )
                    .setTitle( "NFC Tag Programming Error" )
                    .setMessage( "An error occurred while programming the NFC tag. Try again." )
                    .setPositiveButton( "OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            dialog.dismiss();
                        }
                    } ).show();
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
