package com.dorrin.sensoralarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes.Builder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import static android.app.Notification.VISIBILITY_PUBLIC;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.graphics.BitmapFactory.decodeResource;
import static android.media.AudioAttributes.USAGE_ALARM;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static androidx.core.app.NotificationManagerCompat.from;
import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;
import static com.dorrin.sensoralarm.R.drawable.alarm_clock;
import static java.lang.System.out;
import static java.time.Duration.ofMinutes;

public class AlarmService extends BroadcastReceiver {
    private static final String CHANNEL_ID = "com.dorrin.sersoralarm";
    private static final String CHANNEL_NAME = "Alarm";
    private static final int NOTIFICATION_ID = 1;

    @RequiresApi(api = O)
    @Override
    public void onReceive(Context context, Intent intent) {
        sendNotification(context);
        out.println("Alarm. . . . .");
    }

    @RequiresApi(api = O)
    private void sendNotification(Context context) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(alarm_clock)
                .setLargeIcon(decodeResource(context.getResources(), alarm_clock))
                .setContentTitle(getAlarm().getAlarmName())
                .setContentText(getAlarm().getStopType().getMessage())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setTimeoutAfter(ofMinutes(10).toMillis())
                .setPriority(PRIORITY_MAX);
        Notification notification = builder.build();

        from(context).notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel(Context context) {
        if (SDK_INT >= O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            channel.setSound(getAlarm().getRingtonePath(), new Builder().setFlags(USAGE_ALARM).build());
            channel.setBypassDnd(true);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
