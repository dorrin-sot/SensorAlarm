package com.dorrin.sensoralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Toast;

import java.io.IOException;

import static android.media.AudioAttributes.Builder;
import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;
import static android.media.AudioAttributes.USAGE_MEDIA;
import static android.widget.Toast.LENGTH_LONG;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String s = "Alarm.....";
        Toast.makeText(context, s, LENGTH_LONG).show();
        System.out.println("s = " + s);
    }
}
