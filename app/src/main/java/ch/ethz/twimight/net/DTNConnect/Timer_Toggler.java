package ch.ethz.twimight.net.DTNConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hridoy on 19/8/16.
 */
public class Timer_Toggler implements Runnable{
    private BufferedReader br;
    private FileReader fr = null;
    private List<String> IpAddr;
    private android.os.Handler handler;
    private Context context;
    public Timer_Toggler(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run() {
        DCService.macCount = 0;
        DCService.wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiInfo wifiInfo = DCService.wifi.getConnectionInfo();
        DCService.checkWifiState = wifiInfo.getSSID();
        Log.v(DCService.TAG1, "Ticking Random Function");
        Logger.addRecordToLog("Ticking");
        Log.v(DCService.TAG1, DCService.checkWifiState);
        DCService.count++;
        List<ScanResult> allScanResults = DCService.wifi.getScanResults();

        Logger.addRecordToLog("Battery Level," + DCService.level);

        if (DCService.checkWifiState.equals("<unknown ssid>")) {
            Log.v(DCService.TAG1, "Hotspot Mode Detected");
            Logger.addRecordToLog("Hotspot Mode Detected");
            boolean isReachable = false;
            try {

                fr = new FileReader("/proc/net/arp");
                br = new BufferedReader(fr);
                String line;
                IpAddr = new ArrayList<String>();
                DCService.c = false;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");

                    if (splitted != null) {
                        if (splitted[3].matches("..:..:..:..:..:..")) {
                            Log.v("Timer_toggle","Pinging: " + splitted[0]  );
                            Process p1 = Runtime.getRuntime().exec("ping -c 1 -t 1 " + splitted[0]);

                            int returnVal = p1.waitFor();
                            isReachable = (returnVal == 0);

                        }
                        if (isReachable) {
                            DCService.c = true;
                            Log.v(DCService.TAG1, "C IS TRUE !!! ");

                        }

                        // Basic sanity check
                        String mac = splitted[3];
                        System.out.println("Mac : Outside If " + mac);

                        if (mac.matches("..:..:..:..:..:..")) {
                            DCService.macCount++;

                            IpAddr.add(splitted[0]);

                            Log.v(DCService.TAG1, "IP Address  " + splitted[0] + "   MAC_ADDRESS  " + mac);
                            Logger.addRecordToLog("Connected Client, IP :" + splitted[0] + ",mac:" + mac);
                        }
                    }
                }
                if (DCService.c) {
                    Log.v(DCService.TAG1, "Connected!!! ");
                } else {
                    Log.v(DCService.TAG1, "Not Connected!!! ");

                }
            } catch (Exception e) {
                Log.v(DCService.TAG1, "exception", e);
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                        br.close();
                        IpAddr.clear();
                    } catch (IOException e) {
                        // This is unrecoverable. Just report it and move on
                        e.printStackTrace();
                    }
                }
            }

            if (!DCService.c) {
                Toggler.toggle(context);
            }


        } //if Completed check

        else if(DCService.checkWifiState.contains("DisarmHotspotDB")) {
            Log.v(DCService.TAG1, "DisarmHotspotDB Not Toggling");
        //    MainActivity.textConnect.setText("DB Connected");

        }
        else if (DCService.checkWifiState.contains("DH-")) {
            /////////////////////////
           // this.handler.post(searchingDisarmDB);
            String connectedText = DCService.checkWifiState + " connected";
//            MainActivity.textConnect.setText(connectedText);
            Log.v(DCService.TAG1, "DisarmHotspot Not Toggling");
            Log.v(DCService.TAG1,"Trying to find better DH");
            //Logger.addRecordToLog("Connected to DH");
            // Trying to search for better DH
            findBetterDHAvailable(allScanResults);
        }

        else
        {
            Toggler.toggle(context);
        }
        boolean apOn = ApManager.isApOn(context);
        if(apOn){

            this.handler.postDelayed(this,Toggler.addIncreasehp);
        }else{
            this.handler.postDelayed(this,Toggler.addIncreasewifi);
        }
    }

    public void findBetterDHAvailable(List<ScanResult> allScanResults)
    {
        // Store all DH available in allDHAvailable
        Map allDHAvailable = new HashMap<String, Integer>();

        // Put all found DH to allDHAvailable Map
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().contains("DH-")) {
                allDHAvailable.put(scanResult.SSID,scanResult.level);
            }
        }

        Log.v("AllDH Available:", Arrays.asList(allDHAvailable).toString());
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
                    Log.v("Best Found SSID:", pair.getKey());     // Print the key with max value
                    Logger.addRecordToLog("Best Found SSID"+ ',' + pair.getKey());
                    bestFoundSSID = pair.getKey().toString();
                }
            }
        }
        catch (Exception e)
        {}
        // Connect to the best found network
        String pass = "password123";
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + bestFoundSSID + "\""; //IMPORTANT! This should be in Quotes!!
        wc.preSharedKey = "\""+ pass +"\"";
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        int res = DCService.wifi.addNetwork(wc);
        boolean b = DCService.wifi.enableNetwork(res, true);
        Log.v(DCService.TAG2, "Connected");

    }
}
