package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class MainActivity extends Activity implements MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener, CapabilityClient.OnCapabilityChangedListener {
    private static final String
            VOICE_TRANSCRIPTION_CAPABILITY_NAME = "voice_transcription";
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
    TextView goal;
    LinearLayout ll;
    int flag = 0;
    Button stop;
    Button start;

    Button test;
    private String transcriptionNodeId = null;
    int age = 20;
    int max = 220 - age;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//
        //연동 테스트 코드
        test = (Button) findViewById(R.id.test);

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setupVoiceTranscription();
                            } catch (Exception e) {
                                Log.d("테스트 이것은 세팅 에러", e.toString());
                            }
                        }
                    }).start();
                    Log.d("테스트", "이건 성공");
                    Log.d("테스트", transcriptionNodeId.toString());
                    requestTranscription(hh.getText().toString().getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    Log.d("테스트 이것은 스레드 에러", e.toString());
                }


            }
        });


        //여기서 부터 진짜 코드
        goal = (TextView) findViewById(R.id.gaol);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        Button reset = (Button) findViewById(R.id.reset);

        beat = (TextView) findViewById(R.id.beat);
        hh = (TextView) findViewById(R.id.mtext);
        hh_2 = (TextView) findViewById(R.id.mtext_2);
        ll = (LinearLayout) findViewById(R.id.ll);
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
                if (istt) {
                    timeThread = new Thread(new timeThread_3());
                    timeThread.start();
                    Log.d("123", "스레드 3 작동");
                    istt = false;
                } else {
                    timeThread = new Thread(new timeThread_2());
                    timeThread.start();
                    Log.d("123", "스레드 2 작동");

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
                hh.setVisibility(View.VISIBLE);
                hh_2.setVisibility(View.GONE);
                flag = 0;
            }
        });



        //db 생성
        DBHelper helper;

        helper = new DBHelper(MainActivity.this,"beat.db",null,1);
        db = helper.getWritableDatabase();
        helper.onCreate(db);

        try{
            //db 읽어오기
            String sql = "select * from mytable where id='1'";
            Cursor cursor = db.rawQuery(sql,null);
            cursor.moveToNext();
            age = cursor.getInt(1);
            max = 220-age;
            goal.setText(String.valueOf(max));
        }catch (Exception e){
            Log.d("테스트", e.toString());
        }

    }

    @SuppressLint("HandlerLeak")
    Handler handler_3 = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int aa = 18000 - msg.arg1;
            int mSec = aa % 100;
            int sec = (aa / 100) % 60;
            int min = (aa / 100) / 60;
            int hour = (aa / 100) / 360;
            //1000이 1초 1000*60 은 1분 1000*60*10은 10분 1000*60*60은 한시간

            @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d:%02d:%02d", hour, min, sec, mSec);
            hh.setText(result);
            if (aa == 0) {
                hh_2.setVisibility(View.VISIBLE);
                hh.setVisibility(View.GONE);
                start.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d("테스트", "메시지 체인지" + messageEvent.toString());
        Log.d("테스트", new String(messageEvent.getData(), StandardCharsets.UTF_8));
        age = Integer.parseInt(new String(messageEvent.getData(), StandardCharsets.UTF_8));
        
        //DB 업데이트
        String sql = "update mytable set beat ="+age+" where id='1'";
        db.execSQL(sql);
        max = 220 - age;
        goal.setText(String.valueOf(max));
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d("테스트", "데이터 체인지" + dataEventBuffer.toString());
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Log.d("테스트", "케파블리티 체인지" + capabilityInfo.toString());

    }


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
                    if (i > 18000) {
                        hh.setText("");
                        hh.setText("00:03:00:00");
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
            int aa = 12000 - msg.arg1;
            int mSec = aa % 100;
            int sec = (aa / 100) % 60;
            int min = (aa / 100) / 60;
            int hour = (aa / 100) / 360;
            //1000이 1초 1000*60 은 1분 1000*60*10은 10분 1000*60*60은 한시간

            @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d:%02d:%02d", hour, min, sec, mSec);
            hh_2.setText(result);


            if (aa == 0) {
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
                    if (i > 12000) {
                        Log.d("123", "성공");
                        hh_2.setText("");
                        hh_2.setText("00:02:00:00");
                        return;
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hh_2.setText("");
                                hh_2.setText("00:02:00:00");
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
            if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values.length > 0) {
                float mHeartRateFloat = sensorEvent.values[0];
                int mHeartRate = Math.round(mHeartRateFloat);
                beat.setText(Integer.toString(mHeartRate));
                if (mHeartRate >= max && flag == 1) {
                    ll.setBackgroundResource(R.drawable.reen);
                } else if (mHeartRate >= 0 && flag == 1) {
                    ll.setBackgroundResource(R.drawable.red);
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

    //통신용 코드

    //메시지 전달 세팅
    private void setupVoiceTranscription() throws ExecutionException, InterruptedException {
        CapabilityInfo capabilityInfo = Tasks.await(
                Wearable.getCapabilityClient(this).getCapability(
                        VOICE_TRANSCRIPTION_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE));
        // capabilityInfo has the reachable nodes with the transcription capability

        updateTranscriptionCapability(capabilityInfo);
    }


    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        transcriptionNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily.
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    public static final String VOICE_TRANSCRIPTION_MESSAGE_PATH = "/message-item-received";

    //메시지 전송
    private void requestTranscription(byte[] voiceData) {
        if (transcriptionNodeId != null) {
            Task<Integer> sendTask =
                    Wearable.getMessageClient(getApplicationContext()).sendMessage(
                            transcriptionNodeId, VOICE_TRANSCRIPTION_MESSAGE_PATH, voiceData);
            // You can add success and/or failure listeners,

            // Or you can call Tasks.await() and catch ExecutionException
            Log.d("테스트", "requestTranscription:" + voiceData);

        } else {
            Log.d("테스트____", "오류");
            // Unable to retrieve node with transcription capability
        }
    }



}