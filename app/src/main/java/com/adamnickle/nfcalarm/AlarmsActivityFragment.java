package com.adamnickle.nfcalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AlarmsActivityFragment extends Fragment
{
    private static final int REQUEST_NDEF_DISCOVERED = 1001;
    private static final int REQUEST_PICK_ALARM_SOUND = 1002;

    private View mMainView;
    @Bind( R.id.recyclerView ) RecyclerView mRecyclerView;
    private AlarmArrayAdapter mAdapter;

    private AlarmViewHolder mModifyingAlarmHolder;
    private Alarm mProgrammingAlarm;
    private AlertDialog mProgrammingDialog;

    private PendingIntent mNdefDiscoveredPendingIntent;
    private IntentFilter[] mNdefIntentFilters;
    private String[][] mTechList;
    private NfcAdapter mNfcAdapter;

    public static AlarmsActivityFragment createInstance()
    {
        return new AlarmsActivityFragment();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setRetainInstance( true );
        setHasOptionsMenu( true );

        final Intent nfcIntent = new Intent( getActivity(), AlarmsActivity.class )
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

        mNfcAdapter = NfcAdapter.getDefaultAdapter( getActivity() );
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

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == REQUEST_PICK_ALARM_SOUND )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                if( mModifyingAlarmHolder != null )
                {
                    final Uri uri = data.getParcelableExtra( RingtoneManager.EXTRA_RINGTONE_PICKED_URI );
                    mModifyingAlarmHolder.Alarm.setAlarmSound( uri );
                    mModifyingAlarmHolder.updateUI();
                }
            }
        }
    }

    public void onNewIntent( Intent intent )
    {
        if( mProgrammingAlarm == null || intent == null )
        {
            return;
        }
        final Tag tag = intent.getParcelableExtra( NfcAdapter.EXTRA_TAG );
        if( tag != null )
        {
            if( programTag( tag, mProgrammingAlarm ) )
            {
                mProgrammingAlarm.saveAlarm( getActivity() );
                mProgrammingDialog.dismiss();
                mProgrammingDialog = null;
                mProgrammingAlarm = null;
                NfcAlarm.toast( "The NFC tag has been programmed." );
            }
        }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        if( mMainView == null )
        {
            mMainView = inflater.inflate( R.layout.recycler_view, container, false );
            ButterKnife.bind( this, mMainView );

            mRecyclerView.setLayoutManager( new LinearLayoutManager( getActivity() ) );
            mAdapter = new AlarmArrayAdapter();
            mRecyclerView.setAdapter( mAdapter );

            mAdapter.addAll( Arrays.asList( Alarm.getAlarms( getActivity() ) ) );
        }
        else
        {
            final ViewGroup parent = (ViewGroup)mMainView.getParent();
            if( parent != null )
            {
                parent.removeView( mMainView );
            }
        }
        return mMainView;
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        inflater.inflate( R.menu.menu_alarms, menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.addAlarm:
                final Alarm newAlarm = Alarm.createNewAlarm( getActivity() );
                mAdapter.add( newAlarm );
                newAlarm.saveAlarm( getActivity() );
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private boolean programTag( final Tag tag, final Alarm alarm )
    {
        final Ndef ndef = Ndef.get( tag );
        if( ndef == null )
        {
            new AlertDialog.Builder( getActivity() )
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
            final byte[] id = alarm.getID().getBytes( "UTF-8" );
            final NdefRecord idRecord = NdefRecord.createMime( getString( R.string.mime_type ), id );
            final NdefRecord appRecord = NdefRecord.createApplicationRecord( BuildConfig.APPLICATION_ID );
            final NdefMessage appMessage = new NdefMessage( idRecord, appRecord );
            ndef.writeNdefMessage( appMessage );
            return true;
        }
        catch( IOException ex )
        {
            ex.printStackTrace();

            new AlertDialog.Builder( getActivity() )
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

            new AlertDialog.Builder( getActivity() )
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

    public class AlarmViewHolder extends RecyclerView.ViewHolder
    {
        private static final float EXPAND_TOGGLE_EXPANDED_ROTATION = 180.0f;
        private static final float EXPAND_TOGGLE_UNEXPANDED_ROTATION = 0.0f;

        public Alarm Alarm;

        private boolean mExpanded;

        private View mMainView;
        @Bind( R.id.alarmTime ) View Clock;
        @Bind( R.id.enabled ) Switch Enabled;
        @Bind( R.id.expansion ) View Expansion;
        @Bind( R.id.repeat ) CheckBox Repeat;
        @Bind( R.id.days ) View DaysGroup;
        @Bind( { R.id.sunday, R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday, R.id.saturday } )
        CheckBox[] DayCheckboxes;
        @Bind( R.id.alarmSound ) TextView AlarmSound;
        @Bind( R.id.vibrate ) CheckBox Vibrate;
        @Bind( R.id.expandToggle ) ImageView ExpandToggle;

        public AlarmViewHolder( View itemView )
        {
            super( itemView );

            mExpanded = false;

            mMainView = itemView;
            ButterKnife.bind( this, mMainView );

            for( int i = 0; i < DayCheckboxes.length; i++ )
            {
                DayCheckboxes[ i ].setTag( i );
            }

            if( mExpanded )
            {
                ExpandToggle.setRotation( EXPAND_TOGGLE_EXPANDED_ROTATION );
                Expansion.setVisibility( View.VISIBLE );
            }
            else
            {
                ExpandToggle.setRotation( EXPAND_TOGGLE_UNEXPANDED_ROTATION );
                Expansion.setVisibility( View.GONE );
            }
        }

        public void updateUI()
        {
            DateTimeViewHelper.setTime( Clock, Alarm.getNextAlarmTime() );
            Enabled.setChecked( Alarm.isEnabled() );
            if( Alarm.getRepeats() )
            {
                Repeat.setChecked( true );
                for( int i = 0; i < DayCheckboxes.length; i++ )
                {
                    DayCheckboxes[ i ].setChecked( Alarm.getDay( i ) );
                }
                DaysGroup.setVisibility( View.VISIBLE );
            }
            else
            {
                Repeat.setChecked( false );
                DaysGroup.setVisibility( View.GONE );
            }
            final Ringtone alarmSound = RingtoneManager.getRingtone( getActivity(), Alarm.getAlarmSound() );
            AlarmSound.setText( alarmSound.getTitle( getActivity() ) );
            Vibrate.setChecked( Alarm.getVibrates() );
        }

        @OnClick( R.id.enabled )
        void onEnabledClick()
        {
            if( Enabled.isChecked() )
            {
                Alarm.setEnabled( true );
                Alarm.activateAlarm( getActivity() );
            }
            else
            {
                Alarm.setEnabled( false );
                Alarm.cancelAlarm( getActivity() );
            }
        }

        @OnClick( R.id.vibrate )
        void onVibrateClick()
        {
            Alarm.setVibrates( Vibrate.isChecked() );
            Alarm.saveAlarm( getActivity() );
        }

        @OnClick( R.id.repeat )
        void onRepeatClick()
        {
            Alarm.setRepeat( Repeat.isChecked() );
            updateUI();
            Alarm.saveAlarm( getActivity() );
        }

        @OnClick( { R.id.sunday, R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday, R.id.saturday } )
        void onDayClick( View view )
        {
            final CheckBox dayCheckbox = (CheckBox)view;
            Alarm.setDay( (int)dayCheckbox.getTag(), dayCheckbox.isChecked() );
            updateUI();
            Alarm.saveAlarm( getActivity() );
        }

        @OnClick( R.id.alarmTime )
        void onAlarmTimeClick()
        {
            final Calendar time = Alarm.getNextAlarmTime();
            final int hourOfDay = time.get( Calendar.HOUR_OF_DAY );
            final int minute = time.get( Calendar.MINUTE );
            new TimePickerDialog( getActivity(), new TimePickerDialog.OnTimeSetListener()
            {
                @Override
                public void onTimeSet( TimePicker view, int hourOfDay, int minute )
                {
                    Alarm.setTime( hourOfDay, minute );
                    updateUI();
                    Alarm.saveAlarm( getActivity() );
                }
            }, hourOfDay, minute, false ).show();
        }

        @OnClick( R.id.alarmSoundGroup )
        void onAlarmSoundClick()
        {
            mModifyingAlarmHolder = this;
            final Intent intent = new Intent( RingtoneManager.ACTION_RINGTONE_PICKER )
                    .putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE, "Select sound for alarm:" )
                    .putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true )
                    .putExtra( RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true )
                    .putExtra( RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM )
                    .putExtra( RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Alarm.getAlarmSound() );
            startActivityForResult( intent, REQUEST_PICK_ALARM_SOUND );
        }

        @OnClick( R.id.deleteAlarm )
        void onDeleteAlarmClick()
        {
            mAdapter.remove( Alarm );
            Alarm.deleteAlarm( getActivity() );
        }

        @OnClick( R.id.programTag )
        void onProgramTagClick()
        {
            mProgrammingAlarm = Alarm;
            mProgrammingDialog = new AlertDialog.Builder( getActivity() )
                    .setTitle( "Programming NFC Tag" )
                    .setMessage( "Place tag near NFC sensor for programming." )
                    .setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            dialog.cancel();
                        }
                    } )
                    .setOnDismissListener( new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss( DialogInterface dialog )
                        {
                            mProgrammingAlarm = null;
                        }
                    } ).show();
        }

        @OnClick( R.id.expandToggle )
        void onExpandToggleClick()
        {
            mExpanded = !mExpanded;
            ExpandToggle
                    .animate()
                    .rotation( mExpanded ? EXPAND_TOGGLE_EXPANDED_ROTATION : EXPAND_TOGGLE_UNEXPANDED_ROTATION )
                    .setDuration( 300 )
                    .start();
            Expansion.setVisibility( mExpanded ? View.VISIBLE : View.GONE );
        }
    }

    private class AlarmArrayAdapter extends ArrayRecyclerAdapter<Alarm, AlarmViewHolder>
    {
        @Override
        public AlarmViewHolder onCreateViewHolder( ViewGroup viewGroup, int i )
        {
            final View view = LayoutInflater.from( getActivity() )
                    .inflate( R.layout.alarm_card_layout, viewGroup, false );
            return new AlarmViewHolder( view );
        }

        @Override
        public void onBindViewHolder( AlarmViewHolder alarmViewHolder, int i )
        {
            alarmViewHolder.Alarm = get( i );
            alarmViewHolder.updateUI();
        }
    }
}
