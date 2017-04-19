package ch.ethz.twimight.net.DTNConnect;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;


/**
 * Created by sanna on 3/30/17.
 */

public class DataMuleService extends Service {
    private final IBinder myServiceBinder = new DataMuleService.MyServiceBinder();
    private Handler handler;
    private ToggleWRTSpeed toggleWRTSpeed;

    public class MyServiceBinder extends Binder {
        public DataMuleService getService() {
            // Return this instance of SyncService so activity can call public methods
            return DataMuleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        toggleWRTSpeed = new ToggleWRTSpeed(handler,getApplicationContext());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
