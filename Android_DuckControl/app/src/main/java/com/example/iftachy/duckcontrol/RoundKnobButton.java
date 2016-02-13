package com.example.iftachy.duckcontrol;

import android.app.Notification;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;import java.lang.Float;import java.lang.Math;import java.lang.Override;
import java.util.HashMap;

/*
File:              RoundKnobButton
Version:           1.0.0
Release Date:      November, 2013
License:           GPL v2
Description:	   A round knob button to control volume and toggle between two states

****************************************************************************
Copyright (C) 2013 Radu Motisan  <radu.motisan@gmail.com>

http://www.pocketmagic.net

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
****************************************************************************/

//round knob button view
public class RoundKnobButton extends RelativeLayout implements OnGestureListener, View.OnDragListener {

    private static final float KNOB_RIGHT_MIN_DEGREES = 210;    //degrees when set all the way to the right
    private static final float KNOB_LEFT_MAX_DEGREES = 150;     //degrees when set all the way to the left
    public static final boolean INITIAL_STATE=false;            //if false - default state is a red dot, otherwise its green (changes when assigning an axis to a knob)
    private Context mContext;                                   //app context
    private GestureDetector 	gestureDetector;
	private float 				mAngleDown , mAngleUp;
	private ImageView			ivRotor;
	private Bitmap 				bmpRotorOn , bmpRotorOff;
	private boolean 			mState = INITIAL_STATE;
	private int					m_nWidth = 0, m_nHeight = 0;
    private boolean verticalDrag =false;
    private float               lastAngle;
    private float               extraYdrag=0;
    private int                 id;                             //knob id
    private float               value=0;                        //knob value from 0-1

    ImageView[] axisInfoImg = new ImageView[MainSynthActivity.SENSOR_AXIS_NUM];
    private static HashMap<Integer,RoundKnobButton> buttonHashMap = new HashMap<Integer,RoundKnobButton>(); //class is aware of its objects

    //on drag event
    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        // Handles each of the expected events
        switch(action) {

            case DragEvent.ACTION_DRAG_STARTED:
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                return true;

            case DragEvent.ACTION_DROP:
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);

                // Gets the text data from the item.
                String dragData = (String) item.getText();
                int sensorNum = Integer.parseInt(dragData);
                if (sensorNum==MainSynthActivity.RESET_KNOB_DRAG_OBJECT_NUM){
                    ((MainSynthActivity)mContext).clearKnobAssignments(this);
                }else{
                    ((MainSynthActivity)mContext).attachSensorToKnob(sensorNum,this);
                }
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                return true;

            // An unknown action type was received.
            default:
                Log.e("DragDrop Example","Unknown action type received by OnDragListener.");
                break;
        }

        return false;
    }

    //return knob id
    public Integer getID() {
        return id;
    }

    //return knob value as float 0-1
    public float getValue() {
        return value;
    }

    //return knob object by its ID (from class itself)
    public static RoundKnobButton getButtonByID(int knobNum) {
        return buttonHashMap.get(knobNum);
    }

    //button listener interface
    interface RoundKnobButtonListener {
		public void onStateChange(boolean newstate) ;
		public void onRotate(int percentage);
	}

    //button listener
	private RoundKnobButtonListener m_listener;

    //set button listener method
	public void setListener(RoundKnobButtonListener l) {
		m_listener = l;
	}

    //set button state (affects image)
	public void setState(boolean state) {
		mState = state;
		ivRotor.setImageBitmap(state?bmpRotorOn:bmpRotorOff);
	}

    //set vertical drag state. if true, movements are analyzed in a vertival fashion rather than a round one.
    public void setVerticalDrag(boolean newState){
        verticalDrag =newState;
    }

    //constructor
	public RoundKnobButton(Context context, int back, int rotoron, int rotoroff, final int w, final int h,int id) {
		super(context);
        mContext = context;
        this.id=id;
        buttonHashMap.put(id,this);
		// we won't wait for our size to be calculated, we'll just store out fixed size
		m_nWidth = w; 
		m_nHeight = h;
		// create stator
		ImageView ivBack = new ImageView(context);
		ivBack.setImageResource(back);
		RelativeLayout.LayoutParams lp_ivBack = new RelativeLayout.LayoutParams(
				w,h);
		lp_ivBack.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(ivBack, lp_ivBack);
		// load rotor images
		Bitmap srcon = BitmapFactory.decodeResource(context.getResources(), rotoron);
		Bitmap srcoff = BitmapFactory.decodeResource(context.getResources(), rotoroff);
	    float scaleWidth = ((float) w) / srcon.getWidth();
	    float scaleHeight = ((float) h) / srcon.getHeight();
	    Matrix matrix = new Matrix();
	    matrix.postScale(scaleWidth, scaleHeight);
		    
		bmpRotorOn = Bitmap.createBitmap(
				srcon, 0, 0, 
				srcon.getWidth(),srcon.getHeight() , matrix , true);
		bmpRotorOff = Bitmap.createBitmap(
				srcoff, 0, 0, 
				srcoff.getWidth(),srcoff.getHeight() , matrix , true);
		// create rotor
		ivRotor = new ImageView(context);
		ivRotor.setImageBitmap(bmpRotorOn);
		RelativeLayout.LayoutParams lp_ivKnob = new RelativeLayout.LayoutParams(w,h);//LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp_ivKnob.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(ivRotor, lp_ivKnob);
		// set initial state
		setState(mState);
        //set initial percentage:
        setRotorPercentage(100);
		// enable gesture detector
		gestureDetector = new GestureDetector(getContext(), this);
        setOnDragListener(this);

        //add Axis info images. these will be visible only when an axis is assigned to a knob
        for (int i=0;i<MainSynthActivity.SENSOR_AXIS_NUM;i++){
            axisInfoImg[i] = new ImageView(context);

        }
        axisInfoImg[0].setImageResource(R.drawable.xaxis);
        axisInfoImg[1].setImageResource(R.drawable.yaxis);
        axisInfoImg[2].setImageResource(R.drawable.zaxis);
        RelativeLayout.LayoutParams lp_axis = new RelativeLayout.LayoutParams(w/6,h/6);
        lp_axis.addRule(RelativeLayout.ALIGN_LEFT);
        for (int i=0;i<MainSynthActivity.SENSOR_AXIS_NUM;i++){
            addView(axisInfoImg[i],lp_axis);
            axisInfoImg[i].setVisibility(INVISIBLE);
        }
    }

    public void addSensorInfoImg(int axisNum){
        axisInfoImg[axisNum].setVisibility(VISIBLE);
        setState(!RoundKnobButton.INITIAL_STATE);
    }

    public void removeSensorInfoImg(){
        for (int i=0;i<MainSynthActivity.SENSOR_AXIS_NUM;i++){
            axisInfoImg[i].setVisibility(INVISIBLE);
        }
        setState(RoundKnobButton.INITIAL_STATE);
    }
	/**
	 * math..
	 * @param x
	 * @param y
	 * @return
	 */
	private float cartesianToPolar(float x, float y) {
		return (float) -Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
	}

	
	@Override public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) return true;
		else return super.onTouchEvent(event);
	}
	
	public boolean onDown(MotionEvent event) {
		float x = event.getX() / ((float) getWidth());
		float y = event.getY() / ((float) getHeight());
		mAngleDown = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
		return true;
	}
	
	public boolean onSingleTapUp(MotionEvent e) {
        //Degenerated
        return true;
	}

    //sets image rotation to degrees
	public void setRotorPosAngle(float deg) {

		if (deg >= KNOB_RIGHT_MIN_DEGREES || deg <= KNOB_LEFT_MAX_DEGREES) {
            lastAngle = deg;
			if (deg > 180) deg = deg - 360;
			Matrix matrix=new Matrix();
			ivRotor.setScaleType(ScaleType.MATRIX);   
			//matrix.postRotate((float) deg, 210/2,210/2);//getWidth()/2, getHeight()/2);   //original
            float width = getWidth();
            float height = getHeight();
            //matrix.postRotate((float) deg, width/2, height/2);
            matrix.postRotate((float) deg, m_nWidth/2,m_nHeight/2);

			ivRotor.setImageMatrix(matrix);
		}
	}

    //sets knob to precentage 0-100 given(and changes image accordingly)
	public void setRotorPercentage(int percentage) {
		int posDegree = percentage * 3 - 150;
		if (posDegree < 0) posDegree = 360 + posDegree;
		setRotorPosAngle(posDegree);
        value=percentage/100;
        ((MainSynthActivity)mContext).sendBTvalue(id,value);
	}

    ////sets knob to precentage from 0-1 given (and changes image accordingly)
    public void setRotorPercentage(float percentage) {
        percentage*=100;
        float posDegree = percentage * 3 - 150;
        if (posDegree < 0) posDegree = 360 + posDegree;
        setRotorPosAngle(posDegree);
        value=percentage/100;
        ((MainSynthActivity)mContext).sendBTvalue(id,value);
    }
	

    //when moving finger on knob:
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		float x = e2.getX() / ((float) getWidth());
		float y = e2.getY() / ((float) getHeight());

        float rotDegrees;
		if (verticalDrag){
            rotDegrees = (lastAngle+distanceY)%360;
            //degenerated some other options:

            /*
            //Log.d("SYNTH",String.valueOf(rotDegrees));

            //Log.d("SYNTH", String.valueOf(extraYdrag));
            if ((extraYdrag>0) && (extraYdrag<distanceY)){
                extraYdrag=0;
                distanceY-=extraYdrag;
            }
            rotDegrees = (lastAngle+distanceY);
            if ((rotDegrees> KNOB_LEFT_MAX_DEGREES)&&(rotDegrees< (KNOB_RIGHT_MIN_DEGREES+KNOB_LEFT_MAX_DEGREES)/2) || (extraYdrag>0)){
                if (extraYdrag>0){
                    extraYdrag+=distanceY;
                }else{
                    extraYdrag+=(rotDegrees- KNOB_LEFT_MAX_DEGREES);
                }

                rotDegrees= KNOB_LEFT_MAX_DEGREES;
            }
            */
        }else{
            rotDegrees = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
        }
		
		if (! Float.isNaN(rotDegrees)) {
			// instead of getting 0-> 180, -180 0 , we go for 0 -> 360
			float posDegrees = rotDegrees;
			if (rotDegrees < 0) posDegrees = 360 + rotDegrees;
            //posDegrees = SimpleMath.constrain(rotDegrees,0,360);
			// deny full rotation, start start and stop point, and get a linear scale
			if (posDegrees > KNOB_RIGHT_MIN_DEGREES || posDegrees < KNOB_LEFT_MAX_DEGREES) {
				// rotate our imageview
				setRotorPosAngle(posDegrees);
				// get a linear scale
				float scaleDegrees = rotDegrees + 150; // given the current parameters, we go from 0 to 300
				// get position percent
                value = (float) (scaleDegrees / 3)/100;
                ((MainSynthActivity)mContext).sendBTvalue(id,value);
                //setRotorPercentage(percentFromOne);
				int percent = (int) (scaleDegrees / 3);
				if (m_listener != null) m_listener.onRotate(percent);
				return true; //consumed
			} else
				return false;
		} else
			return false; // not consumed
	}


    public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) { return false; }

	public void onLongPress(MotionEvent e) {

    }
}
