package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.IOException;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProgramAlarmActivity extends Activity
{
    private static final int REQUEST_NDEF_DISCOVERED = 1001;
    private static final int REQUEST_PICK_ALARM_SOUND = 1002;

    @Bind( R.id.alarmTime ) View mAlarmClock;
    @Bind( R.id.tagProgrammingStatus ) TextView mTagProgrammingStatus;
    @Bind( R.id.repeat ) CheckBox mRepeat;
    @Bind( R.id.days ) View mDays;
    @Bind( { R.id.sunday, R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday, R.id.saturday } )
    CheckBox[] mDayCheckboxes;
    @Bind( R.id.done ) Button mDone;
    @Bind( R.id.alarmSound) Button mAlarmSound;

    private Alarm mNewAlarm;

    private boolean mTagProgrammed;

    private PendingIntent mNdefDiscoveredPendingIntent;
    private IntentFilter[] mNdefIntentFilters;
    private String[][] mTechList;
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setResult( Activity.RESULT_CANCELED );
        setContentView( R.layout.activity_program_alarm );
        ButterKnife.bind( this );

        mNewAlarm = Alarm.getAlarm( this );
        mTagProgrammed = false;

        for( int i = 0; i < mDayCheckboxes.length; i++ )
        {
            mDayCheckboxes[ i ].setTag( i );
        }

        updateAlarmTime();
        updateDaysContainer();
        updateAlarmSound();

        final Intent nfcIntent = new Intent( this, ProgramAlarmActivity.class )
                .addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
        mNdefDiscoveredPendingIntent = PendingIntent.getActivity( this, REQUEST_NDEF_DISCOVERED, nfcIntent, 0 );
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

        mNfcAdapter = NfcAdapter.getDefaultAdapter( this );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mNfcAdapter.enableForegroundDispatch( this, mNdefDiscoveredPendingIntent, mNdefIntentFilters, mTechList );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch( this );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == REQUEST_PICK_ALARM_SOUND )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                final Uri uri = data.getParcelableExtra( RingtoneManager.EXTRA_RINGTONE_PICKED_URI );
                mNewAlarm.setAlarmSound( uri );
                updateAlarmSound();
            }
        }
    }

    @Override
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
                else
                {
                    mTagProgrammingStatus.setText( "Scanning for NFC tag..." );
                }
                mDone.setEnabled( mTagProgrammed );
            }
        }
    }

    private void updateAlarmSound()
    {
        final Ringtone alarmSound = RingtoneManager.getRingtone( this, mNewAlarm.getAlarmSound() );
        mAlarmSound.setText( alarmSound.getTitle( this ) );
    }

    private void updateAlarmTime()
    {
        DateTimeViewHelper.setTime( mAlarmClock, mNewAlarm.getNextAlarmTime() );
    }

    private void updateDaysContainer()
    {
        if( mNewAlarm.getRepeats() )
        {
            mRepeat.setChecked( true );
            for( int i = 0; i < mDayCheckboxes.length; i++ )
            {
                mDayCheckboxes[ i ].setChecked( mNewAlarm.getDay( i ) );
            }
            mDays.setVisibility( View.VISIBLE );
        }
        else
        {
            mRepeat.setChecked( false );
            mDays.setVisibility( View.GONE );
        }
    }

    @OnClick( R.id.alarmTime )
    void onAlarmTimeClick()
    {
        final Calendar time = mNewAlarm.getNextAlarmTime();
        final int hourOfDay = time.get( Calendar.HOUR_OF_DAY );
        final int minute = time.get( Calendar.MINUTE );
        new TimePickerDialog( this, new TimePickerDialog.OnTimeSetListener()
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
        this.finish();
    }

    @OnClick( R.id.done )
    void onDoneClick()
    {
        mNewAlarm.activateAlarm( this );
        this.setResult( Activity.RESULT_OK );
        this.finish();
    }

    @OnClick( R.id.repeat )
    void onRepeatCheckedChanged( View view )
    {
        final boolean checked = ( (CheckBox)view ).isChecked();
        mNewAlarm.setRepeat( checked );
        updateDaysContainer();
    }

    @OnClick( { R.id.sunday, R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday, R.id.saturday } )
    void onDayCheckedChanged( View view )
    {
        final CheckBox dayCheckbox = (CheckBox)view;
        final boolean checked = dayCheckbox.isChecked();
        mNewAlarm.setDay( (int)dayCheckbox.getTag(), checked );
        updateDaysContainer();
    }

    @OnClick( R.id.alarmSound)
    void onAlarmSoundClick()
    {
        final Intent intent = new Intent( RingtoneManager.ACTION_RINGTONE_PICKER )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE, "Select sound for alarm:" )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM )
                .putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Parcelable)null );
        startActivityForResult( intent, REQUEST_PICK_ALARM_SOUND );
    }

    private boolean programTag( final Tag tag )
    {
        final Ndef ndef = Ndef.get( tag );
        if( ndef == null )
        {
            new AlertDialog.Builder( this )
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

            new AlertDialog.Builder( this )
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

            new AlertDialog.Builder( this )
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
