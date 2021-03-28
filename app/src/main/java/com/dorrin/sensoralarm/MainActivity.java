package com.dorrin.sensoralarm;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import static android.Manifest.permission.VIBRATE;
import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.CATEGORY_OPENABLE;
import static android.content.Intent.createChooser;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_DAYS;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;
import static android.provider.AlarmClock.EXTRA_RINGTONE;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.dorrin.sensoralarm.FileUtils.getPath;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.ROTATE;
import static com.dorrin.sensoralarm.Model.Alarm.StopType.SHAKE;
import static com.dorrin.sensoralarm.Model.Alarm.getAlarm;
import static com.dorrin.sensoralarm.R.id.browseBtn;
import static com.dorrin.sensoralarm.R.id.ringtonePath;
import static com.dorrin.sensoralarm.R.id.rotateBtn;
import static com.dorrin.sensoralarm.R.id.shakeBtn;
import static com.dorrin.sensoralarm.R.id.time;
import static com.dorrin.sensoralarm.R.id.titleEdit;
import static com.dorrin.sensoralarm.R.id.typeToggleBtn;
import static java.lang.String.valueOf;
import static java.net.URI.create;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
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
        executor.execute(() -> {
            Alarm alarm = alarmBuilder
                    .withAlarmName(valueOf(((EditText) findViewById(titleEdit)).getText()))
                    .withRingtonePath(create(valueOf(((EditText) findViewById(ringtonePath)).getText())))
                    .build();
            database.updateAlarm(alarm);

            setAlarm(alarm);
        });
    }

    @RequiresApi(api = N)
    private void setAlarm(Alarm alarm) {
        Intent intent = new Intent(ACTION_SET_ALARM);
        intent.putExtra(EXTRA_HOUR, alarm.getTime().getHour());
        intent.putExtra(EXTRA_MINUTES, alarm.getTime().getMinute());
        intent.putExtra(EXTRA_DAYS, range(1, 7).toArray());
        intent.putExtra(EXTRA_MESSAGE, alarm.getStopType().getMessage());
        intent.putExtra(EXTRA_RINGTONE, alarm.getRingtonePath());
        intent.putExtra(VIBRATE, true);
        // TODO: 3/28/21
    }

    final int MY_REQUEST_CODE_PERMISSION = 1000, MY_RESULT_CODE_FILECHOOSER = 2000;

    @RequiresApi(api = M)
    private void openFileChooser() {
        // With Android Level >= 23, you have to ask the user
        // for permission to access External Storage.

        // Check if we have Call permission
        int permission = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);

        if (permission != PERMISSION_GRANTED) {
            // If don't have permission so prompt the user.
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, MY_REQUEST_CODE_PERMISSION);
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
        startActivityForResult(chooseFileIntent, MY_RESULT_CODE_FILECHOOSER);
    }

    // When you have the request results
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        switch (requestCode) {
            case MY_REQUEST_CODE_PERMISSION: {
                // Note: If request is cancelled, the result arrays are empty.
                // Permissions granted (CALL_PHONE).
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    makeText(this, "Permission granted!", LENGTH_SHORT).show();
                    doBrowseFile();
                }
                // Cancelled or denied.
                else makeText(this, "Permission denied!", LENGTH_SHORT).show();
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_RESULT_CODE_FILECHOOSER:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Uri fileUri = data.getData();

                        try {
                            String filePath = getPath(this, fileUri);
                            ((TextView) findViewById(ringtonePath)).setText(filePath);
                        } catch (Exception e) {
                            makeText(this, "Error: " + e, LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.getReadableDatabase().close();
        database.getWritableDatabase().close();
    }
}