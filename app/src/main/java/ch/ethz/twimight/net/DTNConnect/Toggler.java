package ch.ethz.twimight.net.DTNConnect;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hridoy on 19/8/16.
 */
public class Toggler extends Activity{
    // Randomly value less than 0.50 will make HotspotActive else WifiActive
    private static double toggleBetweenHotspotWifi = 0.5;

    public static  int addIncreasewifi = 5000,wifiIncrease= 5000,hpIncrease=5000,addIncreasehp = 5000;

    // max increase of Wifi and HP Value
    private static int maxWifiIncrease = 20000;
    private static int maxHPIncrease = 20000;
    public static ArrayList<Integer> allFrequency = new ArrayList<>();

    // Set hotspot creation minimum battery level
    private static double minimumBatteryLevel = 10;

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    public static int findChannelWeight()
    {
        // Get frequency of all the channel available
        for ( int i = 0 ; i < DCService.wifiScanList.size();i++) {

            allFrequency.add(convertFrequencyToChannel(DCService.wifiScanList.get(i).frequency));
            Log.v("Channel  + AP: ", String.valueOf(convertFrequencyToChannel(DCService.wifiScanList.get(i).frequency)) + "->" + String.valueOf(DCService.wifiScanList.get(i).SSID));
        }
        Log.v("All frequency:",allFrequency.toString());

        double channelArray[] = new double[13];

        // Fill all the elements with 0
        Arrays.fill(channelArray,0);

        double factor = 0.20;

        for (int band:allFrequency)
        {
            for (int i=band-5 ;i<= band+6 ;i++)
            {
                if (i<1 || i>13)
                    continue;
                channelArray[i-1] = (channelArray[i-1] + (6-Math.abs(i-band))*factor);
            }
        }
        Log.v("Channel Array:",Arrays.toString(channelArray));
        Logger.addRecordToLog("Channel Array," + Arrays.toString(channelArray));

        // Minimum value of channel array
        int bestFoundChannel;

        // Find minimum value from the channel array
        double small = channelArray[0];
        int index = 0;
        for (int i = 0; i < channelArray.length; i++) {
            if (channelArray[i] < small) {
                small = channelArray[i];
                index = i;
            }
        }

        // Find in channel array the elements with minimum channel value
        ArrayList<Integer> bestFoundAvailableChannels = new ArrayList();
        for(int items = 0;items < channelArray.length;items++)
        {
            if(channelArray[items] == small)
            {
                bestFoundAvailableChannels.add(items);
            }
        }
        if(channelArray[6] < channelArray[0] && channelArray[6] <channelArray[11] )
            return  6;
        if(channelArray[11] < channelArray[0] && channelArray[11] < channelArray[6])
            return 11;

        return 1;

    }
    public static void toggle(Context c){
        Log.v(DCService.TAG3, "Toggling randomly!!!");

        // Start wifi for the first time and then randomly toggle
        if (DCService.startwififirst == 1){
            DCService.wifiState= 1.00;
            DCService.startwififirst = 0;
        }
        else
        {
            DCService.wifiState = Math.random()*1.0;
            Log.v(DCService.TAG3, String.valueOf(DCService.wifiState));
        }
        Log.v("Battery Level:", String.valueOf(DCService.level));
        Log.v("Present State:", DCService.presentState);

        if(DCService.wifiState <= toggleBetweenHotspotWifi && DCService.level > minimumBatteryLevel ) {
            // Present State
            DCService.presentState = "hotspot";

            // Set ImageView to Hotspot
            //MainActivity.img_wifi_state.setImageResource(R.drawable.hotspot);

            // Set text to textConnect TextView
            String apHotspotName = "DH" + DCService.phoneVal;
//            MainActivity.textConnect.setText(apHotspotName);

            // Find channel weight of all Wifis
            DCService.bestAvailableChannel = findChannelWeight();

            // Hotspot Mode Activated
            Log.v(DCService.TAG1,"hptoggling for " +String.valueOf(addIncreasehp));
            Logger.addRecordToLog("HA : " + addIncreasehp + " secs," + "Random :" + String.format("%.2f", DCService.wifiState));

            // Adding hotspot increase time counter by factor of hpIncrease
            addIncreasehp += hpIncrease;

            // Disabling Wifi and Enabling Hotspot
            DCService.wifi.setWifiEnabled(false);
            DCService.isHotspotOn = ApManager.isApOn(c);

            if (!DCService.isHotspotOn) {
                ApManager.configApState(c);
            }
            Log.v(DCService.TAG3, "Hotspot Active");

        }
        else {
            DCService.presentState = "wifi";

            // Set ImageView to Wifi
            //MainActivity.img_wifi_state.setImageResource(R.drawable.wifi);

            // Set text to textConnect TextView
            //MainActivity.textConnect.setText("");

            // Wifi Mode Activated
            Log.v(DCService.TAG3,"wifitogging for "+ String.valueOf(addIncreasewifi));

            Logger.addRecordToLog("WA : " + addIncreasewifi + " secs," + "Random :" + String.format("%.2f", DCService.wifiState));

            // Adding wifi increase time counter by factor of wifiIncrease
            addIncreasewifi += wifiIncrease;

            // Disabling hotspot and enable Wifi
            DCService.isHotspotOn = ApManager.isApOn(c);

            if(DCService.isHotspotOn)
            {
                ApManager.configApState(c);
            }
            DCService.wifi.setWifiEnabled(true);
            Log.v(DCService.TAG3, "Wifi Active");

            DCService.wifi.startScan();

        }

        if(addIncreasewifi == maxWifiIncrease){
            addIncreasewifi = 10000;
        }
        else if(addIncreasehp == maxHPIncrease){
            addIncreasehp = 10000;
        }

    }
}
