/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/
package ch.ethz.twimight.activities;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ch.ethz.twimight.R;
import ch.ethz.twimight.fragments.FavoritesFragment;
import ch.ethz.twimight.fragments.MentionsFragment;
import ch.ethz.twimight.fragments.TimelineFragment;
import ch.ethz.twimight.fragments.adapters.FragmentListPagerAdapter;
import ch.ethz.twimight.listeners.TabListener;
import ch.ethz.twimight.net.DTNConnect.DCService;
import ch.ethz.twimight.net.SyncService.SyncService;
import ch.ethz.twimight.net.opportunistic.ScanningService;
import ch.ethz.twimight.net.twitter.FileObserverTwitter;
import ch.ethz.twimight.net.twitter.NotificationService;
import ch.ethz.twimight.net.twitter.Tweets;
import ch.ethz.twimight.net.twitter.TwitterUsers;
import ch.ethz.twimight.util.Constants;
import ch.ethz.twimight.util.InternalStorageHelper;
import ch.ethz.twimight.util.Preferences;
import ch.ethz.twimight.util.SDCardHelper;

import static ch.ethz.twimight.net.twitter.Tweets.PHOTO_PATH;

/**
 * The main Twimight view showing the Timeline, favorites and mentions
 * 
 * @author thossmann
 * 
 */
public class HomeScreenActivity extends TwimightBaseActivity {

	private static final String TAG = HomeScreenActivity.class.getName();
	private FileObserverTwitter fileObserverTwitter;
	private static String sdcard = Environment.getExternalStorageDirectory().toString();
	public static String workingDirectory = sdcard +"/DMS/Working/";
	public static boolean running = false;
	// handler
//	static Handler handler;

	// LOGS
//	LocationHelper locHelper;
	long timestamp;
	ConnectivityManager cm;
//	StatisticsDBHelper locDBHelper;
//	CheckLocation checkLocation;
	public static final String ON_PAUSE_TIMESTAMP = "onPauseTimestamp";

	ActionBar actionBar;
	public static final String EXTRA_KEY_INITIAL_TAB = "EXTRA_KEY_INITIAL_TAB";
	public static final String EXTRA_INITIAL_TAB_TIMELINE = "EXTRA_INITIAL_TAB_TIMELINE";
	public static final String EXTRA_INITIAL_TAB_FAVORITES = "EXTRA_INITIAL_TAB_FAVORITES";
	public static final String EXTRA_INITIAL_TAB_MENTIONS = "EXTRA_INITIAL_TAB_MENTIONS";
	public static final String PHONE_NO = "phone_no." ;
	public static String User_Phone_no = null;

	ViewPager mViewPager;
	FragmentListPagerAdapter mPagerAdapter;

	private String[] mFragmentTitles;

	DCService myService;
	SyncService syncService;
	private static boolean myServiceBound = false;
	private static boolean syncServiceBound = false;



	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		setContentView(R.layout.main);
		User_Phone_no = Preferences.getString(this, R.string.pref_key_phone_number,"NA");
		Log.v("Phone_no",User_Phone_no);
		// reduces overdraw of whole screen by 1
		getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.transparent));

//		// statistics
//		locDBHelper = new StatisticsDBHelper(getApplicationContext());
//		locDBHelper.open();

		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		timestamp = System.currentTimeMillis();

//		locHelper = LocationHelper.getInstance(this);
//		locHelper.registerLocationListener();
//
//		handler = new Handler();
//		checkLocation = new CheckLocation();
//		handler.postDelayed(checkLocation, 1 * 60 * 1000L);

		getActionBar().setSubtitle("@" + LoginActivity.getTwitterScreenname(this));
		mFragmentTitles = new String[] { getString(R.string.timeline), getString(R.string.favorites),
				getString(R.string.mentions) };

		mPagerAdapter = new FragmentListPagerAdapter(getFragmentManager());
		mPagerAdapter.addFragment(new TimelineFragment());
		mPagerAdapter.addFragment(new FavoritesFragment());
		mPagerAdapter.addFragment(new MentionsFragment());

		mViewPager = (ViewPager) findViewById(R.id.viewpager);

		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the
				// corresponding tab.
				setFragment(position);
			}
		});

		// action bar
		actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Tab timelineTab = actionBar.newTab().setIcon(R.drawable.ic_timeline)
				.setTabListener(new TabListener(mViewPager));
		actionBar.addTab(timelineTab);

		Tab favoritesTab = actionBar.newTab().setIcon(R.drawable.ic_favorites)
				.setTabListener(new TabListener(mViewPager));
		actionBar.addTab(favoritesTab);

		Tab mentionsTab = actionBar.newTab().setIcon(R.drawable.ic_mentions)
				.setTabListener(new TabListener(mViewPager));
		actionBar.addTab(mentionsTab);
		setSelectedTab(getIntent());

		//fileoberver initaization
		fileObserverTwitter = new FileObserverTwitter(workingDirectory,this);
		fileObserverTwitter.startWatching();

	}

	private void setSelectedTab(Intent intent) {
		int initialPosition = 0;
		if (intent.hasExtra(EXTRA_KEY_INITIAL_TAB)) {
			String initialTab = intent.getStringExtra(EXTRA_KEY_INITIAL_TAB);

			if (EXTRA_INITIAL_TAB_TIMELINE.equals(initialTab)) {
				initialPosition = 0;
			} else if (EXTRA_INITIAL_TAB_FAVORITES.equals(initialTab)) {
				initialPosition = 1;
			} else if (EXTRA_INITIAL_TAB_MENTIONS.equals(initialTab)) {
				initialPosition = 2;
			}
			intent.removeExtra(EXTRA_KEY_INITIAL_TAB);
		}
		mViewPager.setCurrentItem(initialPosition);
		setFragment(initialPosition);
	}

	private void setFragment(int position) {
		getActionBar().setTitle(mFragmentTitles[position]);
		getActionBar().setSelectedNavigationItem(position);
	}

//	private class CheckLocation implements Runnable {
//
//		@Override
//		public void run() {
//			if (locHelper != null && locHelper.getCount() > 0 && locDBHelper != null
//					&& cm.getActiveNetworkInfo() != null) {
//				Log.i(TAG, "writing log");
//				locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(),
//						StatisticsDBHelper.APP_STARTED, null, timestamp);
//				locHelper.unRegisterLocationListener();
//			}
//		}
//	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		setSelectedTab(intent);
	}

	/**
	 * On resume
	 */
	@Override
	public void onResume() {
		super.onResume();
		running = true;
		markTimelineSeen();
		markMentionsSeen();
//		Long pauseTimestamp = getOnPauseTimestamp(this);
//		if (pauseTimestamp != 0 && (System.currentTimeMillis() - pauseTimestamp) > 10 * 60 * 1000L) {
//			handler = new Handler();
//			handler.post(new CheckLocation());
//		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		setOnPauseTimestamp(System.currentTimeMillis(), this);
		markTimelineSeen();
		markMentionsSeen();
	}

	private void markTimelineSeen() {
		Intent timelineSeenIntent = new Intent(this, NotificationService.class);
		timelineSeenIntent
				.putExtra(NotificationService.EXTRA_KEY_ACTION, NotificationService.ACTION_MARK_TIMELINE_SEEN);
		startService(timelineSeenIntent);
	}

	private void markMentionsSeen() {
		Intent mentionsSeenIntent = new Intent(this, NotificationService.class);
		mentionsSeenIntent
				.putExtra(NotificationService.EXTRA_KEY_ACTION, NotificationService.ACTION_MARK_MENTIONS_SEEN);
		startService(mentionsSeenIntent);
	}

	/**
	 * 
	 * @param id
	 * @param context
	 */
	private static void setOnPauseTimestamp(long timestamp, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putLong(ON_PAUSE_TIMESTAMP, timestamp);
		prefEditor.commit();
	}

	/**
	 * Gets the Twitter ID from shared preferences
	 * 
	 * @param context
	 * @return
	 */
	public static Long getOnPauseTimestamp(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(ON_PAUSE_TIMESTAMP, 0);
	}

	@Override
	protected void onStop() {
		running = false;
//		locHelper.unRegisterLocationListener();
		super.onStop();

	}

	/**
	 * Called at the end of the Activity lifecycle
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		running = false;

		mPagerAdapter = null;
		mViewPager = null;

		actionBar = null;

		fileObserverTwitter.stopWatching();

		Log.i(TAG, "destroying main activity");
//		if ((System.currentTimeMillis() - timestamp <= 1 * 60 * 1000L) && locHelper != null && locDBHelper != null
//				&& cm.getActiveNetworkInfo() != null) {
//
//			if (locHelper.getCount() > 0 && cm.getActiveNetworkInfo() != null) {
////				handler.removeCallbacks(checkLocation);
//				locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(),
//						StatisticsDBHelper.APP_STARTED, null, timestamp);
//			} else {
//			}
//		}
//
//		if ((locHelper != null && locHelper.getCount() > 0) && locDBHelper != null && cm.getActiveNetworkInfo() != null) {
//			locDBHelper.insertRow(locHelper.getLocation(), cm.getActiveNetworkInfo().getTypeName(),
//					StatisticsDBHelper.APP_CLOSED, null, System.currentTimeMillis());
//		} else {
//		}

		TwimightBaseActivity.unbindDrawables(findViewById(R.id.rootRelativeLayout));

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefDisasterMode",
				Constants.DISASTER_DEFAULT_ON) == true)
			Toast.makeText(this, getString(R.string.disastermode_running), Toast.LENGTH_LONG).show();

	}
}
