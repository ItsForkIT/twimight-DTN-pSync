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

package ch.ethz.twimight.net.twitter;

import java.io.FileNotFoundException;
import java.util.Locale;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import ch.ethz.twimight.data.DBOpenHelper;

/**
 * The content provider for Twitter users
 * 
 * @author thossmann
 * 
 */
public class TwitterUsersContentProvider extends ContentProvider {

	private static final String TAG = TwitterUsersContentProvider.class.getSimpleName();

	private SQLiteDatabase database;
	private DBOpenHelper dbHelper;

	private static final UriMatcher twitterusersUriMatcher;

	private static final int USERS = 1;
	private static final int USERS_ID = 2;

	private static final int USERS_FRIENDS = 4;
	private static final int USERS_FOLLOWERS = 5;
	private static final int USERS_DISASTER = 6;
	private static final int USERS_SEARCH = 7;

	// Here we define all the URIs this provider knows
	static {
		twitterusersUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS, USERS);

		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS + "/#", USERS_ID);

		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS + "/"
				+ TwitterUsers.TWITTERUSERS_FRIENDS, USERS_FRIENDS);
		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS + "/"
				+ TwitterUsers.TWITTERUSERS_FOLLOWERS, USERS_FOLLOWERS);
		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS + "/"
				+ TwitterUsers.TWITTERUSERS_DISASTER, USERS_DISASTER);
		twitterusersUriMatcher.addURI(TwitterUsers.TWITTERUSERS_AUTHORITY, TwitterUsers.TWITTERUSERS + "/"
				+ TwitterUsers.TWITTERUSERS_SEARCH, USERS_SEARCH);

	}

	/**
	 * onCreate we initialize and open the DB.
	 */
	@Override
	public boolean onCreate() {
		dbHelper = DBOpenHelper.getInstance(getContext().getApplicationContext());
		database = dbHelper.getWritableDatabase();

		return true;
	}

	/**
	 * Provides read only access to files that have been downloaded and stored
	 * in the provider cache. Specifically, in this provider, clients can access
	 * the files of downloaded images.
	 */
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		Log.d(TAG, "openFile(" + uri.toString() + ", " + mode + ")");
		// only support read only files
		if ("r".equals(mode.toLowerCase(Locale.getDefault()))) {
			return openFileHelper(uri, mode);
		} else {
			return null;
		}
	}

	/**
	 * Returns the MIME types (defined in TwitterUsers) of a URI
	 */
	@Override
	public String getType(Uri uri) {
		switch (twitterusersUriMatcher.match(uri)) {
		case USERS:
			return TwitterUsers.TWITTERUSERS_CONTENT_TYPE;
		case USERS_ID:
			return TwitterUsers.TWITTERUSER_CONTENT_TYPE;
		case USERS_FRIENDS:
			return TwitterUsers.TWITTERUSERS_CONTENT_TYPE;
		case USERS_FOLLOWERS:
			return TwitterUsers.TWITTERUSERS_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortOrder) {

		Intent i;

		Cursor c = null;
		switch (twitterusersUriMatcher.match(uri)) {
		case USERS:
			Log.d(TAG, "Query USERS");
			c = database.query(DBOpenHelper.TABLE_USERS, projection, where, whereArgs, null, null, sortOrder);
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.CONTENT_URI);
			break;

		case USERS_ID:
			// Log.d(TAG, "Query USERS_ID " + uri.getLastPathSegment());
			c = database.query(DBOpenHelper.TABLE_USERS, projection, TwitterUsers.COL_ROW_ID + "=?",
					new String[] { uri.getLastPathSegment() }, null, null, sortOrder);
			// c.setNotificationUri(getContext().getContentResolver(),uri);
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.CONTENT_URI);
			break;

		case USERS_FOLLOWERS:
			Log.d(TAG, "Query USERS_FOLLOWERS");
			c = database.query(DBOpenHelper.TABLE_USERS, projection, TwitterUsers.COL_IS_FOLLOWER + ">0 AND "
					+ TwitterUsers.COL_SCREEN_NAME + " IS NOT NULL", whereArgs, null, null, sortOrder);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.USERS_FOLLOWERS_URI);

			// start synch service with a synch followers request
			i = new Intent(getContext(), TwitterSyncService.class);
			i.putExtra(TwitterSyncService.EXTRA_KEY_ACTION, TwitterSyncService.EXTRA_ACTION_SYNC_FOLLOWERS);
			getContext().startService(i);

			break;
		case USERS_FRIENDS:
			Log.i(TAG, "Query USERS_FRIENDS");
			c = database.query(DBOpenHelper.TABLE_USERS, projection, TwitterUsers.COL_IS_FRIEND + ">0 AND "
					+ TwitterUsers.COL_SCREEN_NAME + " IS NOT NULL", whereArgs, null, null, sortOrder);
			Log.i(TAG, "cursor count: " + c.getCount());
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.USERS_FRIENDS_URI);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			// start synch service with a synch friends request
			i = new Intent(getContext(), TwitterSyncService.class);
			i.putExtra(TwitterSyncService.EXTRA_KEY_ACTION, TwitterSyncService.EXTRA_ACTION_SYNC_FRIENDS);
			getContext().startService(i);

			break;
		case USERS_DISASTER:
			Log.d(TAG, "Query USERS_DISASTER");
			c = database.query(DBOpenHelper.TABLE_USERS, projection, TwitterUsers.COL_IS_DISASTER_PEER + ">0 AND "
					+ TwitterUsers.COL_SCREEN_NAME + " IS NOT NULL", whereArgs, null, null, sortOrder);
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.USERS_DISASTER_URI);
			break;

		case USERS_SEARCH:
			Log.d(TAG, "Query USERS_SEARCH");
			c = database.query(DBOpenHelper.TABLE_USERS, projection, TwitterUsers.COL_SCREEN_NAME + " IS NOT NULL"
					+ " AND " + TwitterUsers.COL_SCREEN_NAME + " LIKE '%" + where + "%' OR " + TwitterUsers.COL_NAME
					+ " LIKE '%" + where + "%' ", whereArgs, null, null, sortOrder);
			c.setNotificationUri(getContext().getContentResolver(), TwitterUsers.USERS_SEARCH_URI);

			// start synch service with a synch followers request
			i = new Intent(getContext(), TwitterSyncService.class);
			i.putExtra(TwitterSyncService.EXTRA_KEY_ACTION, TwitterSyncService.EXTRA_ACTION_SEARCH_USER);
			i.putExtra(TwitterSyncService.EXTRA_KEY_USER_SEARCH_QUERY, where);
			getContext().startService(i);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		return c;
	}

	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		if (twitterusersUriMatcher.match(uri) != USERS) {
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		if (checkValues(values)) {
			Uri insertUri = insertOrUpdate(values);
			getContext().getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);
			return insertUri;
		} else {
			throw new IllegalArgumentException("Illegal user: " + values);
		}
	}

	private Cursor isUserAlreadyStored(ContentValues values) {
		// if we already have the user, we update with the new info
		String[] projection = { TwitterUsers.COL_ROW_ID, TwitterUsers.COL_PROFILE_IMAGE_URI };
		Cursor c = database
				.query(DBOpenHelper.TABLE_USERS,
						projection,
						TwitterUsers.COL_SCREEN_NAME + " = '" + values.getAsString(TwitterUsers.COL_SCREEN_NAME)
								+ "' OR " + TwitterUsers.COL_TWITTER_USER_ID + "="
								+ values.getAsString(TwitterUsers.COL_TWITTER_USER_ID), null, null, null, null);
		if (c.getCount() == 1) {
			c.moveToFirst();
			return c;
		} else {
			return null;
		}
	}

	private Uri insertOrUpdate(ContentValues values) {

		Cursor c = isUserAlreadyStored(values);

		if (c != null) {
			// user is already in DB
			Uri updateUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/"
					+ TwitterUsers.TWITTERUSERS + "/" + Integer.toString(c.getInt(c.getColumnIndex("_id"))));
			update(updateUri, values, null, null);
			c.close();
			return updateUri;

		} else {
			// user is not yet in DB
			try {
				Log.d(TAG, "inserting " + values.toString());
				long rowId = database.insert(DBOpenHelper.TABLE_USERS, null, values);
				if (rowId >= 0) {
					Uri insertUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/"
							+ TwitterUsers.TWITTERUSERS + "/" + rowId);
					purge(DBOpenHelper.TABLE_USERS, values);
					return insertUri;
				} else {
					return null;
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Inserts a bunch of users into the DB
	 */
	@Override
	public synchronized int bulkInsert(Uri uri, ContentValues[] values) {

		int numInserted = 0;
		database.beginTransaction();
		try {
			for (ContentValues value : values) {
				if (value != null) {
					if (insertOrUpdate(value) != null) {
						numInserted++;
					}
				}
			}
			database.setTransactionSuccessful();

		} finally {
			database.endTransaction();
		}
		getContext().getContentResolver().notifyChange(TwitterUsers.CONTENT_URI, null);
		return numInserted;
	}

	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		if (twitterusersUriMatcher.match(uri) != USERS_ID) {
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		if (checkValues(values)) {
			values.put("_id", Long.parseLong(uri.getLastPathSegment()));
			return updateUser(uri, values);

		} else {
			throw new IllegalArgumentException("Illegal user: " + values);
		}
	}

	private int updateUser(Uri uri, ContentValues values) {

		int nrRows = database.update(DBOpenHelper.TABLE_USERS, values, "_id=" + values.getAsLong("_id"), null);
		if (nrRows > 0) {
			return nrRows;
		} else {
			return -1;
		}
	}

	@Override
	public synchronized int delete(Uri uri, String arg1, String[] arg2) {
		return 0;
	}

	private void purge(String table, ContentValues values) {

	}

	private boolean checkValues(ContentValues values) {
		return true;
	}

}
