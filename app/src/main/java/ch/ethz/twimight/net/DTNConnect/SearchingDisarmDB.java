package ch.ethz.twimight.net.DTNConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;
import java.util.StringTokenizer;

import static ch.ethz.twimight.net.DTNConnect.DCService.dbAPName;
import static ch.ethz.twimight.net.DTNConnect.DCService.wifiInfo;

/**
 * Created by hridoy on 21/8/16.
 */
public class SearchingDisarmDB implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private int timerDBSearch = 3000;
    public int minDBLevel = 2;
    private String ssid;
    public String connectedSSID = DCService.wifi.getConnectionInfo().getSSID().toString().replace("\"","");
    public String lastConnectedSSID = connectedSSID;


    public SearchingDisarmDB(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run()
    {

        connectedSSID = DCService.wifi.getConnectionInfo().getSSID().toString().replace("\"","");
        lastConnectedSSID.replace("\"","");
        if(lastConnectedSSID.startsWith("DH-") && !(lastConnectedSSID.equals(connectedSSID)))
        {
            Log.v("Disconnected DH:",lastConnectedSSID);
            Logger.addRecordToLog("Disconnected : " + lastConnectedSSID);
            lastConnectedSSID = "";
        }
        else if(lastConnectedSSID.contains("DB") && !(lastConnectedSSID.equals(connectedSSID))) {
            Log.v("Disconnected DB:",lastConnectedSSID);
            Logger.addRecordToLog("Disconnected : " + lastConnectedSSID);
            lastConnectedSSID = "";
        }
        else if(connectedSSID.startsWith("DH-") || connectedSSID.contains("DB"))
        {
            Log.v("Connected SSID:", connectedSSID + "LastConnectedSSID:" + lastConnectedSSID);
            lastConnectedSSID = connectedSSID;
        }
        else
        {
            Log.v("Connected SSID:", connectedSSID);
            lastConnectedSSID = "";
            connectedSSID = "";
        }
        Log.v(DCService.TAG4,"searching DB");
        List<ScanResult> allScanResults = DCService.wifi.getScanResults();
        if (allScanResults.toString().contains(DCService.dbAPName)) {
            // compare signal level
            int level = findDBSignalLevel(allScanResults);
            if (level < minDBLevel)
            {
                if(connectedSSID.contains("DB")) {
                    if (DCService.wifi.disconnect()) {
                        Logger.addRecordToLog("DB Disconnected as Level = " + level);
                        Log.v(DCService.TAG1, "DB Disconnected as Level = " + level);
                    }
                }
            }
            else if(!connectedSSID.contains("DB"))
            {
                Log.v(DCService.TAG4, "Connecting DisarmDB");

                DCService.wifi.disconnect();


                //  handler.removeCallbacksAndMessages(null); !-- Dont use it all other handler will be closed
                for (int i = 0 ; i < allScanResults.size(); i++)
                {
                    if(allScanResults.get(i).SSID.contains("DB")) {
                        ssid = allScanResults.get(i).SSID;
                        break;
                    }
                }
                WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
                // wc.preSharedKey = "\""+ DCService.dbPass +"\"";
                //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);




                int res = DCService.wifi.addNetwork(wc);
                boolean b = DCService.wifi.enableNetwork(res, true);
                Log.v(DCService.TAG4, "Connected to DB");
            }

        }
        else {
            Log.v(DCService.TAG4,"DisarmHotspotDB not found");
        }
        handler.postDelayed(this, timerDBSearch);
    }
    public int findDBSignalLevel(List<ScanResult> allScanResults)
    {
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().contains(DCService.dbAPName)) {
                int level =  WifiManager.calculateSignalLevel(scanResult.level, 5);
                Logger.addRecordToLog(scanResult.SSID.toString() + " level (out of 5)," + level);
                return level;
            }
        }
        return 0;
    }

}
