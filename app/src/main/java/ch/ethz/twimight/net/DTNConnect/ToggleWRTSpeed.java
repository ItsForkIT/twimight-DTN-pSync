package ch.ethz.twimight.net.DTNConnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by sanna on 3/29/17.
 */

public class ToggleWRTSpeed implements Runnable {
    private final Handler handler;
    private final Context context;
    String TAG = "ToggleWRTSpeed";
    float speed,thresholdSpeed = 2;
    private int counterForDC = 0,counterForDataMule = 0;
    public DCService myService;
    private boolean myServiceBound = false;

    public ToggleWRTSpeed(android.os.Handler handler, Context context) {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }

    @Override
    public void run() {
        //checking GPS ON
        /*if (LocationState.with(context).locationServicesEnabled()){
            //checking GPS location listener ON
            if (PrefUtils.getFromPrefs(context, SplashActivity.GPS_LOC_LISTENER,"0").equals("0")){
                MLocation.subscribe(context);
                Toast.makeText(context, "Turing GPS on..", Toast.LENGTH_SHORT).show();
            }else {
                //getting speed
                speed = getSpeedFromLoc();
            }
        }else {
            Toast.makeText(context, " GPS is OFF", Toast.LENGTH_SHORT).show();
        }
            //comparing speed with threshold
        if (speed > thresholdSpeed){
            if (counterForDataMule > 4){
                //stopping disarm connect service
                final Intent dataMuleIntent = new Intent(context, DCService.class);
                context.unbindService(myServiceConnection);
                myServiceBound = false;
                context.stopService(dataMuleIntent);
                //checking and turing hotspot ON
                if (!ApManager.isApOn(context)){
                    ApManager.configApState(context);
                }
                counterForDataMule = 0;
            }else {
                counterForDataMule += 1;
            }

        }else{
            if (counterForDC > 4){
                //Turning disarm conenct service
                final Intent myServiceIntent = new Intent(context, DCService.class);
                context.bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
                context.startService(myServiceIntent);
                counterForDC = 0;
            }else{
                counterForDC += 1;
            }
        }

        handler.postDelayed(this,5000);
    }

    private float getSpeedFromLoc(){
        Location l = MLocation.getLocation(context);
        if (l != null)
            return l.getSpeed();
        return 0;*/
    }

    //DisarmConnect serviceconnection
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            DCService.MyServiceBinder binder = (DCService.MyServiceBinder) service;
            myService = binder.getService();
            myServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            myServiceBound = false;
        }
    };
}
