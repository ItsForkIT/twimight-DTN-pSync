package ch.ethz.twimight.net.twitter;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.text.Html;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import ch.ethz.twimight.activities.HomeScreenActivity;
import ch.ethz.twimight.activities.LoginActivity;
import ch.ethz.twimight.util.InternalStorageHelper;
import ch.ethz.twimight.util.SDCardHelper;

/**
 * Created by sanna on 4/25/17.
 */

public class FileObserverTwitter extends FileObserver{
    private static String TAG = "FileObserverTwitter";
    private HomeScreenActivity homeScreenActivity;
    Context context;
    // photo
    private String photoPath;
    private static final String PHOTO_PATH = "twimight_photos";

    // SDcard helper
    private SDCardHelper sdCardHelper;

    static final int mask = (FileObserver.CREATE |
            FileObserver.DELETE |
            FileObserver.DELETE_SELF |
            FileObserver.MODIFY |
            FileObserver.MOVED_FROM |
            FileObserver.MOVED_TO |
            FileObserver.MOVE_SELF);

    public FileObserverTwitter(String path , Context context) {
        super(path);
        this.context= context;
    }

    @Override
    public void onEvent(int event,String path) {
        switch(event) {
            case FileObserver.CREATE:
                Log.d(TAG, "CREATE:" + HomeScreenActivity.workingDirectory + path);
                break;
            case FileObserver.DELETE:
                Log.d(TAG, "DELETE:" + HomeScreenActivity.workingDirectory + path);
                break;
            case FileObserver.DELETE_SELF:
                Log.d(TAG, "DELETE_SELF:" + HomeScreenActivity.workingDirectory + path);
                break;
            case FileObserver.MODIFY:
                Log.d(TAG, "MODIFY:" + HomeScreenActivity.workingDirectory + path);
                try {
                    parseFile(HomeScreenActivity.workingDirectory+path);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case FileObserver.MOVED_FROM:
                Log.d(TAG, "MOVED_FROM:" + HomeScreenActivity.workingDirectory + path);
                break;
            case FileObserver.MOVED_TO:
                Log.d(TAG, "MOVED_TO:" + HomeScreenActivity.workingDirectory + path);
                break;
            case FileObserver.MOVE_SELF:
                Log.d(TAG, "MOVE_SELF:" + HomeScreenActivity.workingDirectory + path);
                break;
            default:
                // just ignore
                break;
        }
    }

    public void parseFile(String path) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        String[] tweetFile = path.split("_");
        StringBuilder text = new StringBuilder();
        Log.v("FileObserver","1");
        Log.v("phone",HomeScreenActivity.User_Phone_no);
        if(tweetFile[2].equals("tweet") && !tweetFile[3].equals(HomeScreenActivity.User_Phone_no)){
            try {
                Log.v("FileObserver","2");
                BufferedReader br = new BufferedReader(new FileReader(path));
                if (!br.ready()) {
                    throw new IOException();
                }
                Log.v("FileObserver","3");
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    Log.v(TAG, "text : "+text+" : end");
                }
                br.close();
                String[] words = text.toString().split(" ");
                for (int i=0; i<words.length;i++){
                    String[] name_value = words[i].split("=");
                    jsonObj.accumulate(name_value[0],name_value[1]);
                }
                processTweet(jsonObj);
                Log.v("json",jsonObj.toString());
            }catch (IOException e){
                Log.v(TAG,e.toString());
            }
        }
    }
    private void processTweet(JSONObject o) {
        try {
            Log.i(TAG, "processTweet");
            ContentValues cvTweet = getTweetCV(o);
            cvTweet.put(Tweets.COL_BUFFER, Tweets.BUFFER_DISASTER);

            // we don't enter our own tweets into the DB.
            if (!cvTweet.getAsLong(Tweets.COL_USER_TID).toString()
                    .equals(LoginActivity.getTwitterId(context))) {

                ContentValues cvUser = getUserCV(o);

                // insert the tweet
                Uri insertUri = Uri.parse("content://" + Tweets.TWEET_AUTHORITY + "/" + Tweets.TWEETS + "/"
                        + Tweets.TWEETS_TABLE_TIMELINE + "/" + Tweets.TWEETS_SOURCE_DISASTER);
                context.getContentResolver().insert(insertUri, cvTweet);

                // insert the user
                Uri insertUserUri = Uri.parse("content://" + TwitterUsers.TWITTERUSERS_AUTHORITY + "/"
                        + TwitterUsers.TWITTERUSERS);
                context.getContentResolver().insert(insertUserUri, cvUser);
            }

        } catch (JSONException e1) {
            Log.e(TAG, "Exception while receiving disaster tweet ", e1);
        }

    }
    /**
     * Creates content values for a Tweet from a JSON object TODO: Move this to
     * where it belongs
     *
     * @param o
     * @return
     * @throws JSONException
     */
    protected ContentValues getTweetCV(JSONObject o) throws JSONException {

        ContentValues cv = new ContentValues();

        if (o.has(Tweets.COL_CERTIFICATE))
            cv.put(Tweets.COL_CERTIFICATE, o.getString(Tweets.COL_CERTIFICATE));

        if (o.has(Tweets.COL_SIGNATURE))
            cv.put(Tweets.COL_SIGNATURE, o.getString(Tweets.COL_SIGNATURE));

        if (o.has(Tweets.COL_CREATED_AT))
            cv.put(Tweets.COL_CREATED_AT, o.getLong(Tweets.COL_CREATED_AT));

        if (o.has(Tweets.COL_TEXT)) {
            cv.put(Tweets.COL_TEXT, o.getString(Tweets.COL_TEXT));
            cv.put(Tweets.COL_TEXT_PLAIN, Html.fromHtml(o.getString(Tweets.COL_TEXT)).toString());
        }

        if (o.has(Tweets.COL_USER_TID)) {
            cv.put(Tweets.COL_USER_TID, o.getLong(Tweets.COL_USER_TID));
        }

        if (o.has(Tweets.COL_TID)) {
            cv.put(Tweets.COL_TID, o.getLong(Tweets.COL_TID));
        }

        if (o.has(Tweets.COL_REPLY_TO_TWEET_TID))
            cv.put(Tweets.COL_REPLY_TO_TWEET_TID, o.getLong(Tweets.COL_REPLY_TO_TWEET_TID));

        if (o.has(Tweets.COL_LAT))
            cv.put(Tweets.COL_LAT, o.getDouble(Tweets.COL_LAT));

        if (o.has(Tweets.COL_LNG))
            cv.put(Tweets.COL_LNG, o.getDouble(Tweets.COL_LNG));

        if (o.has(Tweets.COL_SOURCE))
            cv.put(Tweets.COL_SOURCE, o.getString(Tweets.COL_SOURCE));

        if (o.has(Tweets.COL_MEDIA_URIS)) {
            String userID = cv.getAsString(Tweets.COL_USER_TID);
            photoPath = PHOTO_PATH + "/" + userID;
            String photoFileName = o.getString(Tweets.COL_MEDIA_URIS);
            File targetFile = sdCardHelper.getFileFromSDCard(photoPath, photoFileName);

            cv.put(Tweets.COL_MEDIA_URIS, Uri.fromFile(targetFile).toString());

        }

        if (o.has(Tweets.COL_HTML_PAGES))
            cv.put(Tweets.COL_HTML_PAGES, o.getString(Tweets.COL_HTML_PAGES));

        if (o.has(TwitterUsers.COL_SCREEN_NAME)) {
            cv.put(Tweets.COL_SCREEN_NAME, o.getString(TwitterUsers.COL_SCREEN_NAME));
        }

        return cv;
    }
    /**
     * Creates content values for a User from a JSON object TODO: Move this to
     * where it belongs
     *
     * @param o
     * @return
     * @throws JSONException
     */
    protected ContentValues getUserCV(JSONObject o) throws JSONException {

        // create the content values for the user
        ContentValues cv = new ContentValues();
        String screenName = null;

        if (o.has(TwitterUsers.COL_SCREEN_NAME)) {
            screenName = o.getString(TwitterUsers.COL_SCREEN_NAME);
            cv.put(TwitterUsers.COL_SCREEN_NAME, o.getString(TwitterUsers.COL_SCREEN_NAME));
        }

        if (o.has(TwitterUsers.JSON_FIELD_PROFILE_IMAGE) && screenName != null) {
            InternalStorageHelper helper = new InternalStorageHelper(context.getApplicationContext());
            byte[] image = Base64.decode(o.getString(TwitterUsers.JSON_FIELD_PROFILE_IMAGE), Base64.DEFAULT);
            helper.writeImage(image, screenName);
            String profileImageUri = Uri.fromFile(new File(context.getFilesDir(), screenName)).toString();
            Log.d(TAG, "storing profile image at: " + profileImageUri);
            cv.put(TwitterUsers.COL_PROFILE_IMAGE_URI, profileImageUri);
        }

        if (o.has(Tweets.COL_USER_TID)) {
            cv.put(TwitterUsers.COL_TWITTER_USER_ID, o.getLong(Tweets.COL_USER_TID));

        }
        cv.put(TwitterUsers.COL_IS_DISASTER_PEER, 1);

        return cv;
    }
}
