package com.example.iftachy.duckcontrol;

import android.util.Log;

/**
 * Created by iftachyakar on 4/11/15.
 */
//simple math functions
public class SimpleMath {
    //constrain value to be in given range min-max
    public static float constrain (float value,float min,float max){
        if (value>max)  return max;
        if (value<min)  return min;
        return value;
    }

    //constrain value to be in given range min-max
    public static int constrain (int value,int min,int max){
        if (value>max)  return max;
        if (value<min)  return min;
        return value;
    }

    //map value from origin range to target range
    public static float map(float x, float in_min, float in_max, float out_min, float out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    //increase value by incValue in a cyclic fashion between min and max (if value is outside given range or min>max returns value
    public static float incCyclic(float value, float min, float max, float incValue){
        float distance = max-min;
        float resedue = incValue-(int)(incValue/distance);
        if (value<min || value>max || min>max)
            return value;
        if (resedue+value<=max)
            return (value+resedue);
        else{
            float leftResedue = resedue - (max-value);
            return (min+leftResedue);
        }
    }

    //decrease value by decValue in a cyclic fashion between min and max (if value is outside given range or min>max returns value
    public static float decCyclic(float value, float min, float max, float decValue) {
        //Log.d("SYNTH", String.valueOf(value));
        value = constrain(value,min,max);
        float ret_val = map(incCyclic(map(value, min, max, max, min), min, max, decValue), min, max, max, min);
        return ret_val;
    }
}
