package com.dorrin.sensoralarm;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dorrin.sensoralarm.Model.Alarm;
import com.dorrin.sensoralarm.Model.Alarm.Builder;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.threeten.bp.LocalTime;

import java.util.concurrent.ExecutorService;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.SET_ALARM;
import static android.Manifest.permission.VIBRATE;
import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.createChooser;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.net.Uri.parse;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_DAYS;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;
import static android.provider.AlarmClock.EXTRA_RINGTONE;
import static android.view.animation.Animation.RELATIVE_TO_SELF;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.dorrin.sensoralarm.FileUtils.getPath;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.ROTATE;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.SHAKE;
import static com.dorrin.sensoralarm.Model.Alarm.deleteAlarm;
import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;
import static com.dorrin.sensoralarm.R.id.browseBtn;
import static com.dorrin.sensoralarm.R.id.ringtonePath;
import static com.dorrin.sensoralarm.R.id.rotateBtn;
import static com.dorrin.sensoralarm.R.id.shakeBtn;
import static com.dorrin.sensoralarm.R.id.time;
import static com.dorrin.sensoralarm.R.id.titleEdit;
import static com.dorrin.sensoralarm.R.id.typeToggleBtn;
import static java.lang.String.valueOf;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
import static org.threeten.bp.Duration.ofSeconds;
import static org.threeten.bp.LocalTime.of;
import static org.threeten.bp.format.DateTimeFormatter.ofPattern;

public class MainActivity extends AppCompatActivity {
    private Database database;
    private ExecutorService executor = newFixedThreadPool(5);

    private Builder alarmBuilder = new Builder();

    @RequiresApi(api = M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new Database(this);

        ((MaterialButtonToggleGroup) findViewById(typeToggleBtn)).addOnButtonCheckedListener(this::typeSelected);
        findViewById(browseBtn).setOnClickListener(v -> openFileChooser());

        if (database.alarmExists()) showPrevAlarm(database.getAlarm());
    }

    private void showPrevAlarm(Alarm alarm) {
        TextView timeTxtVw = findViewById(time);
        timeTxtVw.setText(alarm.getTime().format(ofPattern("HH:mm")));

        typeSelected(findViewById(typeToggleBtn), alarm.getStopType() == SHAKE ? shakeBtn : rotateBtn, true);

        ((EditText) findViewById(titleEdit)).setText(alarm.getAlarmName());

        ((EditText) findViewById(ringtonePath)).setText(valueOf(alarm.getRingtonePath()));
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

    @RequiresApi(api = N)
    public void saveChanges(View view) {
        if (checkSelfPermission(SET_ALARM) != PERMISSION_GRANTED) {
            requestPermissions(new String[]{SET_ALARM}, RequestCodes.SET_ALARM.ordinal());
        }
        executor.execute(() -> {
            Alarm alarm = alarmBuilder
                    .withAlarmName(valueOf(((EditText) findViewById(titleEdit)).getText()))
                    .withRingtonePath(parse(valueOf(((EditText) findViewById(ringtonePath)).getText())))
                    .build();
            database.updateAlarm(alarm);

            int permission = ActivityCompat.checkSelfPermission(this, SET_ALARM);

            if (permission != PERMISSION_GRANTED) {
                requestPermissions(new String[]{SET_ALARM}, RequestCodes.SET_ALARM.ordinal());
                return;
            }

            setAlarm(alarm);
        });
    }

    @RequiresApi(api = N)
    private void setAlarm(Alarm alarm) {
        Intent intent = new Intent(this, AlarmService.class);
        intent.putExtra(EXTRA_HOUR, alarm.getTime().getHour());
        intent.putExtra(EXTRA_MINUTES, alarm.getTime().getMinute());
        intent.putExtra(EXTRA_DAYS, range(1, 8).toArray());
        intent.putExtra(EXTRA_MESSAGE, alarm.getStopType().getMessage());
        intent.putExtra(EXTRA_RINGTONE, alarm.getRingtonePath());
        intent.putExtra(VIBRATE, true);
        intent.setAction(ACTION_SET_ALARM);

        startService(intent);
    }

    @RequiresApi(api = M)
    private void openFileChooser() {
        int permission = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);

        if (permission != PERMISSION_GRANTED) {
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, RequestCodes.READ_EXTERNAL_STORAGE.ordinal());
            return;
        }

        doBrowseFile();
    }

    private void doBrowseFile() {
        Intent chooseFileIntent = new Intent(ACTION_GET_CONTENT);
        chooseFileIntent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        chooseFileIntent.addCategory(CATEGORY_OPENABLE);

        chooseFileIntent = createChooser(chooseFileIntent, "Choose a alarm ringtone");
        startActivityForResult(chooseFileIntent, ResultCodes.READ_EXTERNAL_STORAGE.ordinal());
    }

    // When you have the request results
    @RequiresApi(api = N)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RequestCodes.READ_EXTERNAL_STORAGE.ordinal())
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                makeText(this, "Permission granted!", LENGTH_SHORT).show();
                doBrowseFile();
            } else
                makeText(this, "Permission denied!", LENGTH_SHORT).show();

        else if (requestCode == RequestCodes.SET_ALARM.ordinal())
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                makeText(this, "Permission granted!", LENGTH_SHORT).show();
                setAlarm(getAlarm());
            } else
                makeText(this, "Permission denied!", LENGTH_SHORT).show();
    }


    @RequiresApi(api = N)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ResultCodes.READ_EXTERNAL_STORAGE.ordinal())
            if (resultCode == RESULT_OK) if (data != null) {
                Uri fileUri = data.getData();

                try {
                    String filePath = getPath(this, fileUri);
                    ((TextView) findViewById(ringtonePath)).setText(filePath);
                } catch (Exception e) {
                    makeText(this, "Error: " + e, LENGTH_SHORT).show();
                }
            } else if (requestCode == RequestCodes.SET_ALARM.ordinal())
                if (resultCode == RESULT_OK)
                    if (data != null)
                        try {
                            setAlarm(getAlarm());
                        } catch (Exception e) {
                            makeText(this, "Error: " + e, LENGTH_SHORT).show();
                        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.getReadableDatabase().close();
        database.getWritableDatabase().close();
    }

    public void reset(View view) {
        // animate refreshBtn
        runOnUiThread(() -> {
            RotateAnimation rotateAnimation = new RotateAnimation(0, 360, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setRepeatCount(2);
            rotateAnimation.setDuration(ofSeconds(1).toMillis());
            view.startAnimation(rotateAnimation);
        });

        // remove from database and view
        executor.execute(() -> {
            database.deleteAlarm();
            deleteAlarm();
            alarmBuilder = new Builder();

            runOnUiThread(() -> {
                ((EditText) findViewById(ringtonePath)).setText("");
                ((TextView) findViewById(time)).setText(of(0, 0).format(ofPattern("HH:mm")));
                ((EditText) findViewById(titleEdit)).setText("");
            });
        });

    }
}

enum RequestCodes {
    SET_ALARM,
    READ_EXTERNAL_STORAGE
}

enum ResultCodes {
    SET_ALARM,
    READ_EXTERNAL_STORAGE
}