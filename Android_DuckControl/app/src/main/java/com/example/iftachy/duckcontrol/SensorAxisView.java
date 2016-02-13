package com.example.iftachy.duckcontrol;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by iftachyakar on 4/12/15.
 */
//A view class for the sensor assignment task. these views are draggable. when dragged onto a knob - a gyro axis is to be mapped
//to that knob
public class SensorAxisView extends ImageView{
    private int axisNumber;
    //private String tag;

    public SensorAxisView(Context context) {
        super(context);
    }

    public SensorAxisView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SensorAxisView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //set Axis Number
    public void setAxisNumber(int n){
        axisNumber=n;
    }
    //get Axis number
    public int getAxisNumber(){
        return axisNumber;
    }

    //init sensor control view
    public void init(String tag){
        this.setTag(tag);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //add axis id for the dragged object
                ClipData data = ClipData.newPlainText("AXIS", String.valueOf(getAxisNumber()));
                // Instantiates the drag shadow builder.
                View.DragShadowBuilder myShadow = new DragShadowBuilder(SensorAxisView.this);

                // Starts the drag

                v.startDrag(data,  // the data to be dragged
                        myShadow,  // the drag shadow builder
                        null,      // no need to use local data
                        0          // flags (not currently used, set to 0)
                );
                return true;
            }
        });
    }
}