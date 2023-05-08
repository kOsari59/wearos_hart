package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.example.myapplication.databinding.ActivityMainBinding;


public class MainActivity extends Activity {
    private Chronometer chronometer;
    private boolean running;
    private long pauseOffset;
    private ActivityMainBinding binding;
    private Thread timeThread = null;
    private Boolean isRunning = true;
    private Boolean istt = true;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private HeartListener mheart = new HeartListener();
    TextView hh;
    TextView hh_2;
    TextView beat;
    ConstraintLayout ll;
    int flag = 0;
    Button stop;
    Button start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        Button reset = (Button) findViewById(R.id.reset);
        beat = (TextView) findViewById(R.id.beat);
        hh = (TextView) findViewById(R.id.mtext);
        hh_2 = (TextView) findViewById(R.id.mtext_2);
        ll=(ConstraintLayout) findViewById(R.id.ll);
        timeThread = new Thread(new timeThread_3()); //강제종료를 막기 위해 한번 초기화

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 1);
        } else {
            Log.d("11", "ALREADY GRANTED");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(mheart, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            Log.d("123123", "TYPE_HEART_RATE supports");
        } else {
            Log.d("123123", "no TYPE_HEART_RATE supports");

        }
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                stop.setVisibility(View.VISIBLE);
                flag = 1;
                if (isRunning) {
                    stop.setText("정지");
                } else {
                    stop.setText("시작");
                }
                if(istt){
                    timeThread = new Thread(new timeThread_3());
                    timeThread.start();
                    istt = false;
                }else{
                    timeThread = new Thread(new timeThread_2());
                    timeThread.start();
                    istt = true;
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning = !isRunning;
                if (isRunning) {
                    stop.setText("정지");
                } else {
                    stop.setText("시작");
                }
            }
        });


        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                timeThread.interrupt();
                isRunning = true;
                istt = true;
                hh.setText("00:03:00:00");
                //ll.setBackgroundResource(R.drawable.dark);
                start.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
                flag = 0;
            }
        });

    }

    @SuppressLint("HandlerLeak")
    Handler handler_3 = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int aa = 18000-msg.arg1;
            int mSec = aa% 100;
            int sec = (aa / 100) % 60;
            int min = (aa / 100) / 60;
            int hour = (aa / 100) / 360;
            //1000이 1초 1000*60 은 1분 1000*60*10은 10분 1000*60*60은 한시간

            @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d:%02d:%02d", hour, min, sec, mSec);
            hh.setText(result);
            if(aa==0){
                hh_2.setVisibility(View.VISIBLE);
                hh.setVisibility(View.GONE);
                start.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
            }
        }
    };


    public class timeThread_3 implements Runnable {
        boolean flags = true;
        @Override
        public void run() {
            int i = 0;

            while (true) {
                while (isRunning) { //일시정지를 누르면 멈춤
                    Message msg = new Message();
                    msg.arg1 = i++;
                    handler_3.sendMessage(msg);
                    if(i>18000){
                        return;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Log.d("123", "run: 123");
                        e.printStackTrace();
                        Log.d("123", "run: 123");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hh.setText("");
                                hh.setText("00:03:00:00");
                            }
                        });
                        return;
                        // 인터럽트 받을 경우 return
                    }
                }
            }
        }
    }

    Handler handler_2 = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int aa = 12000-msg.arg1;
            int mSec = aa% 100;
            int sec = (aa / 100) % 60;
            int min = (aa / 100) / 60;
            int hour = (aa / 100) / 360;
            //1000이 1초 1000*60 은 1분 1000*60*10은 10분 1000*60*60은 한시간

            @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d:%02d:%02d", hour, min, sec, mSec);
            hh.setText(result);


            if(aa==0){
                hh_2.setVisibility(View.GONE);
                hh.setVisibility(View.VISIBLE);
                start.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
            }


        }
    };


    public class timeThread_2 implements Runnable {
        boolean flags = true;
        @Override
        public void run() {
            int i = 0;

            while (true) {
                while (isRunning) { //일시정지를 누르면 멈춤
                    Message msg = new Message();
                    msg.arg1 = i++;
                    handler_2.sendMessage(msg);
                    if(i>=12000){
                        Log.d("123", "성공");
                        return;
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hh.setText("");
                                hh.setText("00:02:00:00");
                            }
                        });
                        return; // 인터럽트 받을 경우 return
                    }
                }
            }
        }
    }
    public class HeartListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.sensor.getType()==Sensor.TYPE_HEART_RATE && sensorEvent.values.length>0 ) {
                float mHeartRateFloat = sensorEvent.values[0];
                int mHeartRate = Math.round(mHeartRateFloat);
                beat.setText(Integer.toString(mHeartRate));
                if(mHeartRate >= 195 && flag ==1){
                    //ll.setBackgroundResource(R.drawable.green);
                }else if(mHeartRate>0 && flag ==1){
                    //.setBackgroundResource(R.drawable.red);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mheart);
    }
}