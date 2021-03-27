package com.dorrin.sensoralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dorrin.sensoralarm.Model.Alarm;
import com.dorrin.sensoralarm.Model.Alarm.Builder;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.threeten.bp.LocalTime;

import java.util.concurrent.ExecutorService;

import static android.app.AlarmManager.RTC_WAKEUP;
import static android.app.PendingIntent.getBroadcast;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.ROTATE;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.SHAKE;
import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;
import static com.dorrin.sensoralarm.R.id.rotateBtn;
import static com.dorrin.sensoralarm.R.id.shakeBtn;
import static com.dorrin.sensoralarm.R.id.time;
import static com.dorrin.sensoralarm.R.id.typeToggleBtn;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.threeten.bp.LocalTime.now;
import static org.threeten.bp.LocalTime.of;
import static org.threeten.bp.format.DateTimeFormatter.ofPattern;

public class MainActivity extends AppCompatActivity {
    private Database database;
    private ExecutorService executor = newFixedThreadPool(5);

    private Builder alarmBuilder = new Builder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new Database(this);

        ((MaterialButtonToggleGroup) findViewById(typeToggleBtn)).addOnButtonCheckedListener(this::typeSelected);

        if (database.alarmExists()) showPrevAlarm(database.getAlarm());
    }

    private void showPrevAlarm(Alarm alarm) {
        TextView timeTxtVw = (TextView) findViewById(time);
        timeTxtVw.setText(alarm.getTime().format(ofPattern("HH:mm")));

        typeSelected(findViewById(typeToggleBtn), alarm.getStopType() == SHAKE ? shakeBtn : rotateBtn, true);
    }

    public void typeSelected(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        group.check(checkedId);

        if (checkedId == shakeBtn)
            alarmBuilder.withStopType(SHAKE);
        else
            alarmBuilder.withStopType(ROTATE);
    }

    public void setTime(View view) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view1, hourOfDay, minute) ->
                        executor.execute(() -> {
                            LocalTime setTime = of(hourOfDay, minute);
                            alarmBuilder.withTime(setTime);
                            runOnUiThread(() -> ((TextView) findViewById(time))
                                    .setText(setTime.format(ofPattern("HH:mm"))));
                        }),
                getAlarm().getTime().getHour(),
                getAlarm().getTime().getMinute(),
                true);

        timePickerDialog.show();
    }

    public void saveChanges(View view) {
        executor.execute(() -> {
            Alarm alarm = alarmBuilder.build();
            database.updateAlarm(alarm);

            setAlarm(alarm);
        });
    }

    private void setAlarm(Alarm alarm) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = getBroadcast(
                getApplicationContext(), 234324243, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(RTC_WAKEUP, currentTimeMillis()
                + (alarm.getTime().toSecondOfDay() - now().toSecondOfDay()) * 1000, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.getReadableDatabase().close();
        database.getWritableDatabase().close();
    }
}