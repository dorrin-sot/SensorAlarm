package com.dorrin.sensoralarm.Model;

import android.net.Uri;

import org.threeten.bp.LocalTime;

import static android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;
import static com.dorrin.sensoralarm.MainActivity.getDatabase;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.SHAKE;
import static org.threeten.bp.LocalTime.of;

public class Alarm {
    private LocalTime time = of(0, 0);
    private StopType stopType = SHAKE;
    private String alarmName;
    private Uri ringtonePath;

    private static Alarm alarm;

    private Alarm(String alarmName, Uri ringtonePath) {

        this.alarmName = alarmName;
        this.ringtonePath = ringtonePath;
    }

    private Alarm(LocalTime time, String alarmName, Uri ringtonePath) {
        this.time = time;
        this.alarmName = alarmName;
        this.ringtonePath = ringtonePath;
    }

    private Alarm(StopType stopType, String alarmName, Uri ringtonePath) {
        this.stopType = stopType;
        this.alarmName = alarmName;
        this.ringtonePath = ringtonePath;
    }

    private Alarm(LocalTime time, StopType stopType, String alarmName, Uri ringtonePath) {
        this.time = time;
        this.stopType = stopType;
        this.alarmName = alarmName;
        this.ringtonePath = ringtonePath;
    }

    public static Alarm getAlarm() {
        if (alarm == null)
            if (getDatabase().alarmExists())
                alarm = getDatabase().getAlarm();
            else
                alarm = new Builder().build();
        return alarm;
    }

    public static void deleteAlarm() {
        alarm = null;
    }

    public enum StopType {
        ROTATE,
        SHAKE;

        public String getMessage() {
            switch (this) {
                case ROTATE:
                    return "Spin your phone at a rate of 5(change later) to stop the timer";
                case SHAKE:
                    return "Shake your phone at a rate of 5(change later) to stop the timer";

                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    public LocalTime getTime() {
        return time;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public Uri getRingtonePath() {
        return ringtonePath;
    }

    public StopType getStopType() {
        return stopType;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "time=" + time +
                ", stopType=" + stopType +
                ", alarmName='" + alarmName + '\'' +
                ", ringtonePath=" + ringtonePath +
                '}';
    }

    public static class Builder {
        private LocalTime time;
        private StopType stopType;
        private String alarmName;
        private Uri ringtonePath = DEFAULT_ALARM_ALERT_URI;

        public Builder withRingtonePath(Uri ringtonePath) {
            this.ringtonePath = ringtonePath;
            return this;
        }

        public Builder withTime(LocalTime time) {
            this.time = time;
            return this;
        }

        public Builder withStopType(StopType stopType) {
            this.stopType = stopType;
            return this;
        }

        public Builder withAlarmName(String alarmName) {
            this.alarmName = alarmName;
            return this;
        }

        /**
         * @throws NullPointerException if alarmName hasn't been set
         */
        public Alarm build() {
            Alarm alarm;
            if (time == null)
                if (stopType == null)
                    alarm = new Alarm(alarmName, ringtonePath);
                else
                    alarm = new Alarm(stopType, alarmName, ringtonePath);
            else {
                if (stopType == null)
                    alarm = new Alarm(time, alarmName, ringtonePath);
                else
                    alarm = new Alarm(time, stopType, alarmName, ringtonePath);
            }
            Alarm.alarm = alarm;

            return alarm;
        }
    }
}