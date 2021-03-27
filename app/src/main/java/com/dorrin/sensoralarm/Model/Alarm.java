package com.dorrin.sensoralarm.Model;

import org.threeten.bp.LocalTime;

import static com.dorrin.sensoralarm.Model.Alarm.StopType.SHAKE;

public class Alarm implements Stoppable, Startable {
    private LocalTime time = LocalTime.of(0, 0);
    private StopType stopType = SHAKE;

    private static Alarm alarm;

    private Alarm() {

    }

    private Alarm(LocalTime time) {
        this.time = time;
    }

    private Alarm(StopType stopType) {
        this.stopType = stopType;
    }

    private Alarm(LocalTime time, StopType stopType) {
        this.time = time;
        this.stopType = stopType;
    }

    public static Alarm getAlarm() {
        if (alarm == null)
            alarm = new Builder().build();
        return alarm;
    }

    @Override
    public void stop() {
        // TODO: 3/27/21
    }

    @Override
    public void start() {
        // TODO: 3/27/21
    }

    public enum StopType {
        ROTATE,
        SHAKE
    }

    public LocalTime getTime() {
        return time;
    }

    public StopType getStopType() {
        return stopType;
    }

    public static class Builder {
        private LocalTime time;
        private StopType stopType;

        public Builder withTime(LocalTime time) {
            this.time = time;
            return this;
        }

        public Builder withStopType(StopType stopType) {
            this.stopType = stopType;
            return this;
        }

        public Alarm build() {
            Alarm alarm;
            if (time == null)
                if (stopType == null)
                    alarm = new Alarm();
                else
                    alarm = new Alarm(stopType);
            else {
                if (stopType == null)
                    alarm = new Alarm(time);
                else
                    alarm = new Alarm(time, stopType);
            }
            Alarm.alarm = alarm;

            return alarm;
        }

        public LocalTime getTime() {
            return time;
        }

        public StopType getStopType() {
            return stopType;
        }
    }
}

interface Stoppable {
    void stop();
}

interface Startable {
    void start();
}