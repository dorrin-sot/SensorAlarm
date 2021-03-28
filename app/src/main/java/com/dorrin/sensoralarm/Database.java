package com.dorrin.sensoralarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dorrin.sensoralarm.Model.Alarm;
import com.dorrin.sensoralarm.Model.Alarm.Builder;

import static com.dorrin.sensoralarm.Model.Alarm.StopType.valueOf;
import static org.threeten.bp.LocalTime.parse;
import static org.threeten.bp.format.DateTimeFormatter.ofPattern;

public class Database extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Alarm.db";
    private static final String TABLE_NAME = "Alarm";
    private static final String TIME_KEY = "Time",
            TYPE_KEY = "Type",
            TITLE_KEY = "Title";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                TIME_KEY + " TIME, " +
                TYPE_KEY + " TEXT," +
                TITLE_KEY + " TEXT" +
                ")");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void updateAlarm(Alarm alarm) {
        SQLiteDatabase writableDatabase = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TIME_KEY, alarm.getTime().format(ofPattern("HH:mm")));
        values.put(TYPE_KEY, alarm.getStopType().toString());
        values.put(TITLE_KEY, alarm.getAlarmName());

        if (!alarmExists())
            writableDatabase.insert(TABLE_NAME, null, values);
        else
            writableDatabase.update(TABLE_NAME, values, null, null);
    }

    public boolean alarmExists() {
        SQLiteDatabase readableDatabase = getReadableDatabase();

        Cursor cursor = readableDatabase.query(TABLE_NAME, new String[]{TIME_KEY}, null, null, null, null, null);

        return cursor.getCount() != 0;
    }

    public Alarm getAlarm() {
        SQLiteDatabase readableDatabase = getReadableDatabase();

        Alarm alarm;
        try (Cursor cursor = readableDatabase.query(TABLE_NAME, new String[]{TIME_KEY, TYPE_KEY, TITLE_KEY}, null, null, null, null, null)) {
            cursor.moveToFirst();
            alarm = new Builder()
                    .withAlarmName(cursor.getString(cursor.getColumnIndex(TITLE_KEY)))
                    .withTime(parse(cursor.getString(cursor.getColumnIndex(TIME_KEY)), ofPattern("HH:mm")))
                    .withStopType(valueOf(cursor.getString(cursor.getColumnIndex(TYPE_KEY))))
                    .build();
        }
        return alarm;
    }
}
