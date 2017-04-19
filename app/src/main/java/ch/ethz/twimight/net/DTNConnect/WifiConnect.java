package ch.ethz.twimight.net.DTNConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ch.ethz.twimight.net.DTNConnect.DCService.checkWifiState;
import static ch.ethz.twimight.net.DTNConnect.DCService.wifiInfo;

/**
 * Created by hridoy on 21/8/16.
 */
public class WifiConnect implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private FileReader fr= null;
    private BufferedReader br = null;
    public int minDBLevel = 2;
    public int isDBConnected = 0;
    public WifiConnect(android.os.Handler handler, Context context) {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }

    @Override
    public void run() {
        DCService.wifi.startScan();
        List<ScanResult> allScanResults = DCService.wifi.getScanResults();

        Log.v(DCService.TAG2,"Running Autoconnector");
        wifiInfo = DCService.wifi.getConnectionInfo();
        String ssidName = wifiInfo.getSSID();
        Log.v(DCService.TAG2, ssidName);
        if(ssidName.contains(DCService.dbAPName)) {
            Log.v(DCService.TAG2,"Already Connected DB ");
            Logger.addRecordToLog("Already DB Connected");

        }
        else if(ssidName.contains("DH-")) {
            Log.v(DCService.TAG2,"Already Connected");
            Logger.addRecordToLog("DH Connected:" + ssidName);
            try {

                fr = new FileReader("/proc/net/arp");
                br = new BufferedReader(fr);
                String line;
                DCService.IpAddr = new ArrayList<String>();
                DCService.c = false;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");
                    Log.v("Splitted:" , Arrays.deepToString(splitted));
                }
            }
            catch(Exception e)
            {}
        }

        else
        {

            if (allScanResults.toString().contains("DH-")) {

                // Store all DH available in allDHAvailable
                Map allDHAvailable = new HashMap<String, Integer>();

                // Put all found DH to allDHAvailable Map
                for (ScanResult scanResult : allScanResults) {
                    if(scanResult.SSID.toString().contains("DH-")) {
                        allDHAvailable.put(scanResult.SSID,scanResult.level);
                    }
                }

                Logger.addRecordToLog("All DH available:" + Arrays.asList(allDHAvailable).toString());

                // Find key with the maximum value from allDHAvailable
                String bestFoundSSID="";
                int maxValueInMap = 0;
                try {
                    maxValueInMap = (int) Collections.max(allDHAvailable.values());  // This will return max value in the Hashmap
                    Iterator it = allDHAvailable.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Integer> pair = (Map.Entry) it.next();
                        if (pair.getValue() == maxValueInMap) {
                            Logger.addRecordToLog("Best Found SSID"+ ',' + pair.getKey());
                            bestFoundSSID = pair.getKey().toString();
                        }
                    }
                }
                catch (Exception e)
                {}
                DCService.wifi.disconnect();
                // Connect to the best found network
                String pass = "password123";
                WifiConfiguration wc = new WifiConfiguration();

                wc.SSID = "\"" + bestFoundSSID + "\""; //IMPORTANT! This should be in Quotes!!
                wc.preSharedKey = "\""+ pass +"\"";
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                int res = DCService.wifi.addNetwork(wc);
                boolean b = DCService.wifi.enableNetwork(res, true);
                //  Log.v("WifiConnect:","Res:" + res + ",b:" + b);
                Log.v(DCService.TAG2, "Connected");
                // Log.v("Parameters:" ,wc.SSID + "," + wc.BSSID + "," + wc.allowedAuthAlgorithms + "," + wc.allowedProtocols + "," + wc.allowedKeyManagement + "," + wc.allowedGroupCiphers + "," + wc.allowedPairwiseCiphers + "," + wc.FQDN + "," + wc.status);
                Logger.addRecordToLog("DH Connected Successfully," + bestFoundSSID);
            }
            else{
                Log.v(DCService.TAG2,"Disarm Not Available");

                Logger.addRecordToLog("no DH/DB network available");

            }

        }
        handler.postDelayed(this,3000);
    }
    public int findDBSignalLevel(List<ScanResult> allScanResults)
    {
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().contains(DCService.dbAPName)) {
                Log.v("SSID:",scanResult.SSID.toString());
                int level =  WifiManager.calculateSignalLevel(scanResult.level, 5);
                return level;
            }
        }
        return 0;
    }
}
