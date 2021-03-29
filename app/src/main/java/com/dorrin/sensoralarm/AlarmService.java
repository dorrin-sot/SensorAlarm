package com.dorrin.sensoralarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.Build.VERSION_CODES.O;
import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;
import static java.lang.String.valueOf;
import static java.lang.System.out;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofMinutes;
import static java.util.Calendar.getInstance;
import static org.threeten.bp.LocalDateTime.now;

public class AlarmService extends Service implements OnPreparedListener {
    MediaPlayer mediaPlayer;

    @RequiresApi(api = O)
    @Override
    public void onCreate() {
        super.onCreate();

        Calendar calendar = getInstance();
        calendar.set(
                now().getYear(),
                now().getMonthValue()-1,
                now().getDayOfMonth(),
                getAlarm().getTime().getHour(),
                getAlarm().getTime().getMinute(),
                0);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                playAlarmSound();
            }
        }, new Date(calendar.getTime().getTime()), ofDays(1).toMillis());
    }

    private void playAlarmSound() {
        mediaPlayer = new MediaPlayer();
        try {
            out.println(getAlarm());
//            if (getAlarm().getRingtonePath() == null)
//                mediaPlayer.setDataSource(this, DEFAULT_ALARM_ALERT_URI);
//            else
            mediaPlayer.setDataSource(valueOf(getAlarm().getRingtonePath()));
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @RequiresApi(api = O)
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();
            }
        };
        timer.schedule(timerTask, ofMinutes(10).toMillis());
    }
}
