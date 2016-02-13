package com.example.iftachy.duckcontrol;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by iftachyakar on 4/12/15.
 */
//this class takes care of all things gyro:
//analyzing gyro data and setting the proper knobs to the needed values
//also handles everything regarding "subscriptions" meaning which sensor axis is assigned to which knob
public class SensorHandler implements SensorEventListener{
    private static final boolean CONCURRENTMODIFICATIONEXCEPTION_WORKAROUND = true;
    //Subscription system:
    List<List<RoundKnobButton>> sensorSubscribers = new ArrayList<List<RoundKnobButton>>(MainSynthActivity.SENSOR_AXIS_NUM);    //a list of all axis and for every axis - which knob is subscribed to it
    //axis IDs:
    public static int X_AXIS = 0;
    public static int Y_AXIS = 1;
    public static int Z_AXIS = 2;
    private boolean listenersRegistered=false;                      //this is true if at least one knob is controlled by some axis, and false otherwise.


    //sensor calculations:
    private static final float COMPLEMENTARY_FILTER_COEFF= 0.98f;
    private SensorManager mSensorManager = null;
    // angular speeds from gyro
    private float[] gyro = new float[3];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // magnetic field vector
    private float[] magnet = new float[3];
    // accelerometer vector
    private float[] accel = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    private float[] orientationNormalized = new float[3];           //final axis values normalized to 0-1 float
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    private float zAxisBaseValue=0;

    //constructor
    public SensorHandler(Context mContext) {
        for (int i=0;i<MainSynthActivity.SENSOR_AXIS_NUM;i++){
            sensorSubscribers.add(0,new ArrayList<RoundKnobButton>());
        }

        //sensor init:
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        calibrateZaxis();
    }

    //when at least one axis is controlling some knob - all listeners will be connected with this method
    private void registerListeners(){
        listenersRegistered=true;
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    //if no axis is controlling a knob, no need for the listeners
    private void unregisterListeners(){
        listenersRegistered=false;
        mSensorManager.unregisterListener(this);
    }

    //assign an axis to a specific knob
    public void subscribe(int axis,RoundKnobButton knob){
        //remove other knob assignments:
        unsubscribeKnob(knob);
        //subscribe knob to axis:
        if (!sensorSubscribers.get(axis).contains(knob)){
            sensorSubscribers.get(axis).add(knob);
            knob.addSensorInfoImg(axis);
            if (!listenersRegistered)
                registerListeners();
        }
    }

    //detach an axis from controlling a specific knob
    public void unsubscribe(int axis,RoundKnobButton knob){
        //remove that knob from axis
        sensorSubscribers.get(axis).remove(knob);
        //unregisted listeners if no knob connected at all
        if (isSubscriptionsEmpty())
            unregisterListeners();
        //change knob state if knob not connected to any axis
        if (!isKnobConnectedToSensor(knob))
            //knob.setState(RoundKnobButton.INITIAL_STATE);
            knob.removeSensorInfoImg();
    }

    //detach all axis controlling a specific knob (knob will be free henceforth)
    public void unsubscribeKnob(RoundKnobButton knob){
        for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
            //change knob states for all knobs connected to sensors:
            if (CONCURRENTMODIFICATIONEXCEPTION_WORKAROUND){
                for (int j=0;j<sensorSubscribers.get(i).size();j++){
                    if (sensorSubscribers.get(i).get(j)==knob){
                        unsubscribe(i,knob);
                        j--;
                    }
                }
            }else{
                for (RoundKnobButton subscriber : sensorSubscribers.get(i)){
                    if (subscriber==knob){
                        unsubscribe(i,knob);
                        break;
                    }
                }
            }
        }
    }

    //detach all sensor controls for all knobs
    public void unsubscribeAll(){
        for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
            //change knob states for all knobs connected to sensors:
            if (CONCURRENTMODIFICATIONEXCEPTION_WORKAROUND){
                for (int j=0;j<sensorSubscribers.get(i).size();j++){
                    unsubscribe(i,sensorSubscribers.get(i).get(j));
                    j--;
                }
            }else{
                for (RoundKnobButton subscriber : sensorSubscribers.get(i)){
                    unsubscribe(i,subscriber);
                    break;
                }
            }
            //clear all subscriptions
            sensorSubscribers.get(i).clear();
        }
        unregisterListeners();
    }

    //send sensor values to subscibed knobs (change their values to the given ones
    private void updateValuesToSubscribers(float[] orientationNormalized) {
        for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
            for (RoundKnobButton subscriber : sensorSubscribers.get(i)){
                subscriber.setRotorPercentage(orientationNormalized[i]);
            }
        }
    }

    //returns true if no knob is connected to given axis, false otherwise
    private boolean isSubscriptionEmpty(int axis){
        if (sensorSubscribers.get(axis).size()>0)
            return false;
        return true;
    }

    //returns true if any sensor is controlling this knob, false otherwise
    private boolean isKnobConnectedToSensor(RoundKnobButton knob){
        boolean knobConnected=false;
        for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
            if (sensorSubscribers.get(i).contains(knob))
                return true;
        }
        return false;
    }

    //returns true if there exists no axis which controls a knob, false otherwise
    public boolean isSubscriptionsEmpty(){
        for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
            if (!isSubscriptionEmpty(i))
                return false;
        }
        return true;
    }

    public void calibrateZaxis(){
        zAxisBaseValue = fusedOrientation[0];
    }
    //return subsctiber list
    public List<List<RoundKnobButton>> getSensorSubscribers() {
        return sensorSubscribers;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float pi = (float) Math.PI;
        float fixedZaxis;

        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }


        float oneMinusCoeff = 1.0f - COMPLEMENTARY_FILTER_COEFF;

        //This is a fix for the original code. when passing from +pi to -pi or vice verse, the complementary filter would go though all the number between
        //the two value rather than just jumping between them
        //now the complementary filter works no matter what values it is given.
        for (int i=0;i<MainSynthActivity.SENSOR_AXIS_NUM;i++){
            if (gyroOrientation[i]>2.5 && accMagOrientation[i]<-2.5){   //current value is close to +pi and next value is close to -pi
                float tempAccMagOrientation = accMagOrientation[i] + 2*pi;
                fusedOrientation[i] = SimpleMath.incCyclic(COMPLEMENTARY_FILTER_COEFF * gyroOrientation[i],(float)-pi,(float)pi,oneMinusCoeff * tempAccMagOrientation);
            }else if (accMagOrientation[i]>2.5 && gyroOrientation[i]<-2.5){   //crossing from -pi to +pi
                float tempAccMagOrientation = accMagOrientation[i] - 2*pi;
                fusedOrientation[i] = SimpleMath.decCyclic(COMPLEMENTARY_FILTER_COEFF * gyroOrientation[i],(float)-pi,(float)pi,-(oneMinusCoeff * tempAccMagOrientation));

            }else{  //regular situation, no crossing from - to + or vice verse
                fusedOrientation[i] =
                        COMPLEMENTARY_FILTER_COEFF * gyroOrientation[i]
                                + oneMinusCoeff * accMagOrientation[i];
            }

        }

//        fusedOrientation[1] =
//                COMPLEMENTARY_FILTER_COEFF * gyroOrientation[1]
//                        + oneMinusCoeff * accMagOrientation[1];
//
//        fusedOrientation[2] =
//                COMPLEMENTARY_FILTER_COEFF * gyroOrientation[2]
//                        + oneMinusCoeff * accMagOrientation[2];

        //normalize values:
        orientationNormalized[0] = SimpleMath.constrain(SimpleMath.map(fusedOrientation[2],(float)-0.5*pi,(float)0.5*pi,0f,1f),0f,1f); //x axis
        orientationNormalized[1] = SimpleMath.constrain(SimpleMath.map(fusedOrientation[1],(float)-0.5*pi,(float)0.5*pi,0f,1f),0f,1f); //y axis
        //change z to fit calibration value:
        if (zAxisBaseValue>0){
            fixedZaxis = SimpleMath.decCyclic(fusedOrientation[0],-pi,pi,zAxisBaseValue);
        }else{
            fixedZaxis = SimpleMath.incCyclic(fusedOrientation[0],-pi,pi,-zAxisBaseValue);
        }
        orientationNormalized[2] = SimpleMath.constrain(SimpleMath.map(fixedZaxis,(float)-0.5*pi,(float)0.5*pi,0f,1f),0f,1f); //z axis
        /*
        debugText[0].setText(String.valueOf(orientationNormalized[0]));
        debugText[1].setText(String.valueOf(orientationNormalized[1]));
        debugText[2].setText(String.valueOf(orientationNormalized[2]));
        */
        this.updateValuesToSubscribers(orientationNormalized);
        // overwrite gyro matrix and orientation with fused orientation
        // to comensate gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //*****************************************************
    //Gyro calculation functions:
    //*****************************************************
    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }


}
