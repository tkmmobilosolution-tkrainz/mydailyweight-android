package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by tkrainz on 04/07/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notificationService = new Intent(context, NotiService.class);
        context.startService(notificationService);
    }
}
