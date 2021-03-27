package com.dorrin.sensoralarm;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import static com.dorrin.sensoralarm.MessageWhat.SET_TIMER;
import static com.dorrin.sensoralarm.R.id.*;

public class MainActivity extends AppCompatActivity {
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void typeSelected (View v) {
        MaterialButton otherBtn, thisBtn = (MaterialButton) v;
        if (v.getId() == shakeBtn)
            otherBtn = findViewById(rotateBtn);
        else
            otherBtn = findViewById(shakeBtn);

        otherBtn.setSelected(false);
        thisBtn.setSelected(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler(getMainLooper(), msg -> {
//            switch (msg.what) {
//                case SET_TIMER.ordinal():
//                    break;
//            }
            return true;
        });
    }
}

enum MessageWhat {
    SET_TIMER,
    START_TIMER,
    STOP_TIMER
}