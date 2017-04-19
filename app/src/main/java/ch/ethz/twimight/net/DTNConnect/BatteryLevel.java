package ch.ethz.twimight.net.DTNConnect;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * Created by hridoy on 19/8/16.
 */
public class BatteryLevel extends BroadcastReceiver {
    @Override
    public void onReceive(Context arg0, Intent intent) {
        DCService.level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

    }
}
