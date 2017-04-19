package ch.ethz.twimight.net.DTNConnect;

/**
 * Created by sanna on 2/12/15.
 */

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;

import ch.ethz.twimight.activities.HomeScreenActivity;

public class ApManager {

    private static final String TAG = "AP Creation";
    //check whether wifi hotspot on or off
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Throwable ignored) {}
        return false;
    }


    // toggle wifi hotspot on or off
    public static boolean configApState(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it off
            if(isApOn(context)) {
                wifimanager.setWifiEnabled(false);
            }

            //Change Name of the Created Hotspot
            try {
                Method getConfigMethod = wifimanager.getClass().getMethod("getWifiApConfiguration");

                WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifimanager);
                Log.v("ApManager:", "Wifi Ap Config:" + Arrays.deepToString(wifiConfig.getClass().getFields()));

                //wifiConfig.getClass().getField("apChannel").setInt(wifiConfig, 6);
                Log.v("ApManager", "Best Available Channel:" + DCService.bestAvailableChannel);
                Logger.addRecordToLog("Ap Created Channel," + DCService.bestAvailableChannel);

                // Channel Allocation

                if (Build.VERSION.SDK_INT > 22) {
                    // Created hotspot in the best available channel
                    wifiConfig.getClass().getField("apChannel").setInt(wifiConfig, DCService.bestAvailableChannel);
                } else {
                    wifiConfig.getClass().getField("channel").setInt(wifiConfig, DCService.bestAvailableChannel);
                }

                wifiConfig.allowedAuthAlgorithms.clear();
                wifiConfig.allowedGroupCiphers.clear();
                wifiConfig.allowedKeyManagement.clear();
                wifiConfig.allowedPairwiseCiphers.clear();
                wifiConfig.allowedProtocols.clear();

                // Create AP with SSID Name DH - <SOURCE> with preSharedKey
                wifiConfig.SSID = "DH-" + HomeScreenActivity.User_Phone_no;


                wifiConfig.preSharedKey = "password123";

                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                Method setWifiApMethod = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                boolean apstatus = (Boolean) setWifiApMethod.invoke(wifimanager, wifiConfig, true);
                //Log.v("WifiConfig: " , wifiConfig.toString());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Enable the AP state
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wificonfiguration, !isApOn(context));
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}