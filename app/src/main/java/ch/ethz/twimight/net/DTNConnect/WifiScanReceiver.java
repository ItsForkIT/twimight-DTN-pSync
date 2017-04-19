package ch.ethz.twimight.net.DTNConnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hridoy on 19/8/16.
 */
public class WifiScanReceiver extends BroadcastReceiver {
    public static String wifis[]={"None"};
    public void onReceive(Context c, Intent intent) {

        DCService.wifiScanList = DCService.wifi.getScanResults();
        wifis = new String[DCService.wifiScanList.size()];

        for(int i = 0; i < DCService.wifiScanList.size(); i++){
            wifis[i] = String.valueOf(DCService.wifiScanList.get(i));
            Log.v("Available Networks : ", DCService.wifiScanList.get(i).SSID);
        }

    }
}
