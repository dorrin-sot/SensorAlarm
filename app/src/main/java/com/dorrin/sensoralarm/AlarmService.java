package com.dorrin.sensoralarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;

import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;

public class AlarmService extends Service implements OnPreparedListener {
    MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(String.valueOf(getAlarm().getRingtonePath()));
            System.out.println(getAlarm().getRingtonePath());
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int startCommand = super.onStartCommand(intent, flags, startId);

        return startCommand;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
