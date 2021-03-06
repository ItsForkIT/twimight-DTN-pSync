package ch.ethz.twimight.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;

import ch.ethz.twimight.R;
import ch.ethz.twimight.activities.HomeScreenActivity;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.net.DTNConnect.DCService;
import ch.ethz.twimight.net.SyncService.SyncService;
import ch.ethz.twimight.net.opportunistic.ScanningAlarm;
import ch.ethz.twimight.net.twitter.NotificationService;
import ch.ethz.twimight.net.twitter.TwitterAlarm;
import ch.ethz.twimight.util.Constants;
import ch.ethz.twimight.util.Preferences;

/**
 * User settings fragment
 * 
 * @author Steven Meliopoulos
 * 
 */
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private static final String TAG = SettingsFragment.class.getName();

	BluetoothAdapter mBluetoothAdapter;
	static final int REQUEST_DISCOVERABLE = 2;

	DCService myService;
	SyncService syncService;
	private boolean myServiceBound = false;
	private boolean syncServiceBound = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.user_settings);
	}

	@Override
	public void onResume() {
		super.onResume();
		initializePreferenceStates();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Initializes preference properties that need to be set dynamically.
	 */
	private void initializePreferenceStates() {
		updateBackgroundUpdatePreference();
		updateNotificationSoundPreference();
	}

	/**
	 * Sets the summary of the background update interval preference to display
	 * the selected value.
	 */
	private void updateBackgroundUpdatePreference() {
		ListPreference backgroundUpdatePreference = (ListPreference) findPreference(getString(R.string.pref_key_update_interval));
		String selectedValue = backgroundUpdatePreference.getValue();
		String[] values = getResources().getStringArray(R.array.backgroundUpdateIntervalValues);
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null && values[i].equals(selectedValue)) {
				String[] names = getResources().getStringArray(R.array.backgroundUpdateIntervalNames);
				backgroundUpdatePreference.setSummary(names[i]);
				break;
			}
		}
	}

	/**
	 * Sets the summary of the notification ringtone preference to display the
	 * name of the selected ringtone.
	 */
	private void updateNotificationSoundPreference() {
		RingtonePreference ringtonePreference = (RingtonePreference) getPreferenceScreen().findPreference(
				getString(R.string.pref_key_notification_ringtone));
		String uri = Preferences.getString(getActivity(), R.string.pref_key_notification_ringtone, null);
		String summary;
		if (!TextUtils.isEmpty(uri)) {
			Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(uri));
			summary = ringtone.getTitle(getActivity());
		} else {
			summary = getString(R.string.ringtone_title_none);
		}
		ringtonePreference.setSummary(summary);
	}

	/**
	 * Enables Bluetooth when Disaster Mode is enabled.
	 */
	private void enableDisasterMode() {
		/*mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isEnabled())
			ScanningAlarm.setBluetoothInitialState(getActivity().getBaseContext(), true);
		else
			ScanningAlarm.setBluetoothInitialState(getActivity().getBaseContext(), false);
		// for statistics
		Preferences.update(getActivity(), R.string.pref_key_disastermode_has_been_used, true);
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
			startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
		} else {
			new ScanningAlarm(getActivity().getApplicationContext(), true);
			getActivity().finish();
		}*/
		final Intent myServiceIntent = new Intent(getActivity().getApplicationContext(), DCService.class);
		getActivity().getApplicationContext().bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().getApplicationContext().startService(myServiceIntent);

		final Intent syncServiceIntent = new Intent(getActivity().getApplicationContext(), SyncService.class);
		getActivity().getApplicationContext().bindService(syncServiceIntent, syncServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().getApplicationContext().startService(syncServiceIntent);
	}
	//Psync
	private ServiceConnection syncServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			SyncService.SyncServiceBinder binder = (SyncService.SyncServiceBinder) service;
			syncService = binder.getService();
			syncServiceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			syncServiceBound = false;
		}
	};

	//DTNConnect
	private ServiceConnection myServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			DCService.MyServiceBinder binder = (DCService.MyServiceBinder) service;
			myService = binder.getService();
			myServiceBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			myServiceBound = false;
		}
	};

	public void disableDisasterMode(Context context) {
		/*if (getBluetoothInitialState(context) == false) {
			if (BluetoothAdapter.getDefaultAdapter().isEnabled())
				BluetoothAdapter.getDefaultAdapter().disable();
		}
		ScanningAlarm.stopScanning(context);
		Intent in = new Intent(context, ScanningService.class);
		context.stopService(in);*/
		Log.v("service ","closed");
		final Intent myServiceIntent = new Intent(context, DCService.class);
		if (myServiceBound) {
			context.unbindService(myServiceConnection);
		}
		myServiceBound = false;
		context.stopService(myServiceIntent);

		final Intent syncServiceIntent = new Intent(getActivity().getApplicationContext(), SyncService.class);
		if (syncServiceBound) {
			getActivity().getApplicationContext().unbindService(syncServiceConnection);
		}
		syncServiceBound = false;
		getActivity().getApplicationContext().stopService(syncServiceIntent);
	}
	/**
	 * Takes necessary actions when settings are changed (starting services,
	 * updating preference summary etc.).
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		final HomeScreenActivity homeScreenActivity = new HomeScreenActivity();
		if (key.equals(getString(R.string.pref_key_disaster_mode))) {
			if (preferences.getBoolean(getString(R.string.pref_key_disaster_mode), Constants.DISASTER_DEFAULT_ON) == true) {
				if (LoginActivity.getTwitterId(getActivity().getBaseContext()) != null
						&& LoginActivity.getTwitterScreenname(getActivity().getBaseContext()) != null) {

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							enableDisasterMode();
							getActivity().finish();
						}
					});
					builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// User cancelled the dialog
						}
					});
					AlertDialog dialog = builder.create();
					dialog.setMessage("Turn ON Disaster Mode");
					dialog.setCancelable(false);
					dialog.show();
				}
			} else {
				disableDisasterMode(getActivity().getBaseContext());
				getActivity().finish();
			}
		} else if (key.equals(getString(R.string.pref_key_tds_communication))) {
			// toggle TDS communication
			if (preferences.getBoolean(getString(R.string.pref_key_tds_communication), Constants.TDS_DEFAULT_ON) == true) {
				// new TDSAlarm(getApplicationContext(),
				// Constants.TDS_UPDATE_INTERVAL);
			} else {
				// stopService(new Intent(getApplicationContext(),
				// TDSService.class));
				// TDSAlarm.stopTDSCommuniction(getApplicationContext());
			}
		} else if (key.equals(getString(R.string.pref_key_update_interval))) {
			updateBackgroundUpdatePreference();
			TwitterAlarm.initialize(getActivity());
		} else if (key.equals(getString(R.string.pref_key_notification_ringtone))) {
			updateNotificationSoundPreference();
		} else if (key.equals(getString(R.string.pref_key_notify_tweets))) {
			Intent timelineSeenIntent = new Intent(getActivity(), NotificationService.class);
			timelineSeenIntent.putExtra(NotificationService.EXTRA_KEY_ACTION,
					NotificationService.ACTION_MARK_TIMELINE_SEEN);
			getActivity().startService(timelineSeenIntent);
		} else if (key.equals(getString(R.string.pref_key_notify_mentions))) {
			Intent mentionsSeenIntent = new Intent(getActivity(), NotificationService.class);
			mentionsSeenIntent.putExtra(NotificationService.EXTRA_KEY_ACTION,
					NotificationService.ACTION_MARK_MENTIONS_SEEN);
			getActivity().startService(mentionsSeenIntent);
		} else if (key.equals(getString(R.string.pref_key_notify_direct_messages))) {
			Intent directMessagesSeenIntent = new Intent(getActivity(), NotificationService.class);
			directMessagesSeenIntent.putExtra(NotificationService.EXTRA_KEY_ACTION,
					NotificationService.ACTION_MARK_DIRECT_MESSAGES_SEEN);
			getActivity().startService(directMessagesSeenIntent);
		}
	}



	private static boolean getBluetoothInitialState(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean("wasBlueEnabled", true);
	}

	public static boolean isDisModeActive(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefDisasterMode", false);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_DISCOVERABLE:
			if (resultCode == BluetoothAdapter.STATE_CONNECTING) {
				new ScanningAlarm(getActivity().getApplicationContext(), true);
				getActivity().finish();
			} else if (resultCode == BluetoothAdapter.STATE_DISCONNECTED) {
				Preferences.update(getActivity(), R.string.pref_key_disaster_mode, Constants.DISASTER_DEFAULT_ON);
				CheckBoxPreference disPref = (CheckBoxPreference) findPreference(getString(R.string.pref_key_disaster_mode));
				disPref.setChecked(false);
			}
		}
	}
}
