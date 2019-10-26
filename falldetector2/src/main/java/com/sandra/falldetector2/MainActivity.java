package com.sandra.falldetector2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextView;
    private static final int ACCELEROMETER_SAMPLING_PERIOD = 20000;
    private static final double CSV_THRESHOLD = 23;
    private static final double CAV_THRESHOLD = 18;
    private static final double CCA_THRESHOLD = 65.5;
    private List<Map<AccelerometerAxis, Double>> accelerometerValues = new ArrayList<>();
    private List<Map<AccelerometerAxis, Double>> accelerometerValues2 = new ArrayList<>();
    private List<Map<AccelerometerAxis, Double>> accelerometerValues3 = new ArrayList<>();
    private List<Map<AccelerometerAxis, Double>> accelerometerValues04seg = new ArrayList<>();
    private SensorManager sensorManager;
    CountDownTimer countDownTimer;

    private TextView mTextView2;
    private TextView mTextView3;
    private ImageButton imageButton;
    private static final String
            FALL_CAPABILITY_NAME = "fall_notification";
    private String transcriptionNodeId = null;
    boolean isObserving =true;
    boolean countOneQuarterset = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.textView);
        mTextView2 = findViewById(R.id.textView2);
        mTextView3 = findViewById(R.id.textView3);
        imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSensor();
                mTextView.setVisibility(View.VISIBLE);
                mTextView.setText("Monitorando...");
                mTextView2.setVisibility(View.GONE);
                mTextView3.setVisibility(View.GONE);
                imageButton.setVisibility(View.GONE);
                isObserving = true;
                countDownTimer.cancel();
                countDownTimer = null;
            }
        });
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        initSensor();

        Wearable.getCapabilityClient(this)
                .getCapability(FALL_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
                    @Override
                    public void onComplete(@NonNull Task<CapabilityInfo> task) {
                        if (task.getResult() != null)
                            updateTranscriptionCapability(task.getResult());
                    }
                });

        setAmbientEnabled();
    }

    public void initSensor() {

        if( sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {

            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    ACCELEROMETER_SAMPLING_PERIOD
            );
        }

    }

    public void stopReadings(){
        sensorManager.unregisterListener(this);
        Log.d("teste", "stopReadings: ");
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor,
                                        int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Axis of the rotation sample, not normalized yet.
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        if (this.isFallDetected(x, y, z)) {
            //setupFallLayout();
        }

    }

    public void setupFallLayout() {
        Log.d("acc", "onSensorChanged: Fall Happen"  );
        isObserving = false;
        startCronometer();
        mTextView.setText("Queda detectada!");
        mTextView.setVisibility(View.GONE);
        mTextView2.setVisibility(View.VISIBLE);
        mTextView3.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
        //stopReadings();
    }
    public void startCronometer(){
        countDownTimer = new CountDownTimer(5*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTextView3.setText("" + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                requestTranscription("teste".getBytes());
            }

        };

        countDownTimer.start();
    }

    private boolean isFallDetected(double x,
                                   double y,
                                   double z) {

        double acceleration = this.calculateSumVector(x, y, z);
        this.addAccelerometerValuesToList(x, y, z, acceleration);

        StringBuilder msg = new StringBuilder("x: ").append(x)
                .append(" y: ").append(y)
                .append(" z: ").append(z)
                .append(" acc: ").append(acceleration);
       Log.d("FDS-Acc-Values", msg.toString());

        if (acceleration > CSV_THRESHOLD) {
            //Log.d("teste", "isFallDetected: Entrou aqui CSV >");
            double angleVariation = this.calculateAngleVariation();
            if (angleVariation > CAV_THRESHOLD) {
                //Log.d("teste", "isFallDetected: Entrou aqui CAV >");
                double changeInAngle = this.calculateChangeInAngle();
                if (changeInAngle > CCA_THRESHOLD) {
                    countOneQuarterset = true;
                    msg.append(System.currentTimeMillis());
                    Log.d("teste", "isFallDetected: Entrou aqui CCA >");



                    return false;
                }
            }
        }
        return false;
    }

    public Double getDesvPadrao(){
        Double media = getMedia();
        int tam = accelerometerValues2.size();
        Double desvPadrao = 0D;
        for (Map<AccelerometerAxis, Double> map:accelerometerValues2){
            Double vlr = map.get(AccelerometerAxis.ACCELERATION);
            Double aux = vlr - media;
            desvPadrao += aux * aux;
        }
        return Math.sqrt(desvPadrao / (tam));
    }

    public Double getMedia(){

        Double soma = 0.0;
        for (Map<AccelerometerAxis, Double> map:accelerometerValues2){
            soma += map.get(AccelerometerAxis.ACCELERATION);
        }

        return soma/accelerometerValues2.size();
    }

    private void addAccelerometerValuesToList(double x,
                                              double y,
                                              double z,
                                              double acceleration) {
        if(this.accelerometerValues.size() >= 4) {
            this.accelerometerValues.remove(0);
        }
        if(this.accelerometerValues2.size() >= 40) {
            this.accelerometerValues2.remove(0);
        }
        if(this.accelerometerValues3.size() >= 150) {
            this.accelerometerValues3.remove(0);
        }
        Map<AccelerometerAxis, Double> map = new HashMap<>();
        map.put(AccelerometerAxis.X, x);
        map.put(AccelerometerAxis.Y, y);
        map.put(AccelerometerAxis.Z, z);
        map.put(AccelerometerAxis.ACCELERATION, acceleration);
        this.accelerometerValues2.add(map);
        this.accelerometerValues.add(map);
        this.accelerometerValues3.add(map);
        if (countOneQuarterset){
            if (this.accelerometerValues04seg.size() >= 20){
                countOneQuarterset = false;
                stopReadings();
                Log.d("teste", "run: Desvio Padrao " + getDesvPadrao());
                if(getDesvPadrao() > 1.5 * 9.8){
                    Log.d("teste", "run: desvio padrao > ");
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    setupFallLayout();
                    this.accelerometerValues04seg = new ArrayList<>();
//
                }else {
                    initSensor();
                }

            }
            else
                this.accelerometerValues04seg.add(map);
        }
    }

    private double calculateSumVector(double x,
                                      double y,
                                      double z) {
        return Math.abs(x) + Math.abs(y) + Math.abs(z);
    }

    private double calculateSVM(){
        Double svm = 0D;
        for (int i = 50;i<accelerometerValues3.size() - 1;i++){

            Double vlr = accelerometerValues3.get(i).get(AccelerometerAxis.ACCELERATION);
            svm += vlr;
        }
        Log.d("teste", "calculateSVM: Valor svm:  " + svm);
        return svm;
    }

    private double calculateAngleVariation() {
        int size = this.accelerometerValues.size();
        if (size < 2){
            return -1;
        }

        Map<AccelerometerAxis, Double> minusTwo = this.accelerometerValues.get(size - 2);
        Map<AccelerometerAxis, Double> minusOne = this.accelerometerValues.get(size - 1);

        double anX = minusTwo.get(AccelerometerAxis.X) * minusOne.get(AccelerometerAxis.X);
        double anY = minusTwo.get(AccelerometerAxis.Y) * minusOne.get(AccelerometerAxis.Y);
        double anZ = minusTwo.get(AccelerometerAxis.Z) * minusOne.get(AccelerometerAxis.Z);
        double an = anX + anY + anZ;

        double anX0 = Math.pow(minusTwo.get(AccelerometerAxis.X), 2);
        double anY0 = Math.pow(minusTwo.get(AccelerometerAxis.Y), 2);
        double anZ0 = Math.pow(minusTwo.get(AccelerometerAxis.Z), 2);
        double an0 = Math.sqrt(anX0 + anY0 + anZ0);

        double anX1 = Math.pow(minusOne.get(AccelerometerAxis.X), 2);
        double anY1 = Math.pow(minusOne.get(AccelerometerAxis.Y), 2);
        double anZ1 = Math.pow(minusOne.get(AccelerometerAxis.Z), 2);
        double an1 = Math.sqrt(anX1 + anY1 + anZ1);

        double a = an / (an0 * an1);

        return Math.acos(a) * (180 / Math.PI);
    }

    private double calculateChangeInAngle() {
        int size = this.accelerometerValues.size();
        if (size < 4){
            return -1;
        }
        Map<AccelerometerAxis, Double> first = this.accelerometerValues.get(0);
        Map<AccelerometerAxis, Double> third = this.accelerometerValues.get(3);

        double aX = first.get(AccelerometerAxis.X) * third.get(AccelerometerAxis.X);
        double aY = first.get(AccelerometerAxis.Y) * third.get(AccelerometerAxis.Y);
        double aZ = first.get(AccelerometerAxis.Z) * third.get(AccelerometerAxis.Z);

        double a0 = aX + aY + aZ;

        aX = Math.pow(aX, 2);
        aY = Math.pow(aY, 2);
        aZ = Math.pow(aZ, 2);
        double a1 = (Math.sqrt(aX) + Math.sqrt(aY) + Math.sqrt(aZ));

        return Math.acos(a0 / a1) * (180 / Math.PI);
    }

    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        transcriptionNodeId = pickBestNodeId(connectedNodes);
        //requestTranscription("fall".getBytes());
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            Log.d("node", "pickBestNodeId: " + node.getDisplayName());
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void requestTranscription(byte[] info) {
        if (transcriptionNodeId != null) {
            Task<Integer> sendTask =
                    Wearable.getMessageClient(this).sendMessage(
                            transcriptionNodeId, FALL_CAPABILITY_NAME, info);
            // You can add success and/or failure listeners,
            // Or you can call Tasks.await() and catch ExecutionException
            sendTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    Log.d("watch", "onSuccess: Deu certo" );
                }
            });
            sendTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("watch", "onSuccess: Deu ruim" );
                }
            });
        } else {
            // Unable to retrieve node with transcription capability
        }
    }

}
