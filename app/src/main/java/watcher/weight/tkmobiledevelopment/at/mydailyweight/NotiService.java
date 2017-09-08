package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

/**
 * Created by tkrainz on 04/07/2017.
 */

public class NotiService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent[] mainIntent = new Intent[1];
        mainIntent[0] = new Intent(this, StartupActivity.class);

        Notification notification = new Notification.Builder(this)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivities(this, 13, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            .setContentText("Do not forget to enter your weight")
            .setContentTitle("Enter your daily weight")
            .setSmallIcon(R.drawable.ic_launcher)
            .setTicker("Do not forget to enter your daily weight today")
            .setPriority(Notification.PRIORITY_HIGH)
            .build();

        NotificationManager nManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(12, notification);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Screen Notifications");
        wakeLock.acquire();
        wakeLock.release();
    }
}
