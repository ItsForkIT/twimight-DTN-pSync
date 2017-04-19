package ch.ethz.twimight.net.DTNConnect;

/**
 * Created by cse on 12/8/16.
 */


import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.FileHandler;

public class Logger {

    public static FileHandler logger = null;
    private static String filename = "DisarmConnect_log", logFileUpdated;
    public static File logFile;
    static boolean isExternalStorageAvailable = false;
    static boolean isExternalStorageWriteable = false;
    public static int flag1 = 1;
    public static String logFileName;
    static String state = Environment.getExternalStorageState();
    public static String phoneID = "" , date;
    public static DateFormat df;


   /* public Logger(String phoneVal) {
        this.phoneID = phoneVal;
    }*/
    public Logger() {

    }

    public static void addRecordToLog(String message) {

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            isExternalStorageAvailable = isExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            isExternalStorageAvailable = true;
            isExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            isExternalStorageAvailable = isExternalStorageWriteable = false;
        }

        // Log File Updated with every new entry for onLocationChanged()
        File dir = Environment.getExternalStoragePublicDirectory("DMS/");
        Log.v("Logger Initiated","");
        if (Environment.MEDIA_MOUNTED.equals(state)) {

            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            String date = df.format(Calendar.getInstance().getTime());
            logFile = new File(dir, filename + "_" + phoneID + ".txt");
            if (!logFile.exists()) {
                try {
                    Log.d("File created ", "File created ");
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                FileWriter buf = new FileWriter(logFile, true);
                df = new SimpleDateFormat("yyyyMMddHHmmss");
                date = df.format(Calendar.getInstance().getTime());
                buf.write(date + ',' + message + "\r\n");

                buf.flush();
                buf.close();
            }
            catch (Exception e)
            {}
            /*try {
                Log.v("log file check :", logFile.toString());
                FileWriter buf = new FileWriter(logFile, true);
                df = new SimpleDateFormat("yyyyMMddHHmmss");
                date = df.format(Calendar.getInstance().getTime());
                buf.write(message+','+date +"\r\n");

                buf.flush();
                buf.close();


                logFileUpdated = logFile.toString().substring(0, logFile.toString().length()-19);

                Log.v("Phone ID: ", phoneID);
                Log.v("Log File:",logFile.toString());
                File to = new File(logFileUpdated.toString() + "_" + date + ".txt");
                Log.v("Log File Updated:",logFileUpdated.toString()+ "_" + date + ".txt");

                boolean success = logFile.renameTo(to);

                Log.v("Success:",String.valueOf(success));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
        }
    }
}

