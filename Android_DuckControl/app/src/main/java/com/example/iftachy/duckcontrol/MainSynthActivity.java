package com.example.iftachy.duckcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
//import com.aronbordin.*;

//Main synth activity
public class MainSynthActivity extends Activity implements View.OnClickListener {

    //processing flags:
    private static final boolean DEBUG_BLUETOOTH = false;//true;
    private static final boolean DISABLE_BLUETOOTH = false;
    private static final boolean TRY_CONNECTING_BT_ON_STARTUP = true;   //useful for debugging
    private static final boolean DEBUG_NEW_SPINNER = false;

    private static final int MINIMUM_GAP_BETWEEN_BT_COMMANDS = 20;  //in milliseconds. this determines the minimum gap between sending values for the same knob (to prevent bluetooth over-load)
    public static final int SYNTH_NUM_OF_KNOBS = 5;                 //number of synth knobs
    public static final int SENSOR_AXIS_NUM = 3;                    //number of gyroscope axis
    public static final int SENSOR_BUTTON_NUM = SENSOR_AXIS_NUM+1;  //number of sensor assignment buttons (all axis+clear axis button)
    public static final int RESET_KNOB_DRAG_OBJECT_NUM = 3;         //number of clear axis button
    private static final int SETTINGS_REQUEST = 0;                  //request ID for settings (for startActivityForResult)
    private static final int SET_AS_INIT_PRESET = 0;                //when saving - save as init
    private static final int SAVE_AS_NEW_PRESET = 1;                //when saving - save as new preset
    private static final int KNOB_WIDTH = 180;                      //size of knob (hard-coded)
    private static final String DE_DUCK_SYNTH_NAME_TAG = "HC-05";//"DeDuckSynth";   //name of bluetooth device in android bluetooth menu
    private static final int ARDUINO_MAX_KNOB_VALUE = 1023;//127;               //maximum value of knob to send to arduino - sent as int
    private static final String BT_KNOB_CHANGE_CMD = "k";                    //command for setting a knob to a certain value
    private static final String BT_CONTROL_STATE_CMD = "d";             //command for assuming and giving control to arduino


    RoundKnobButton[] knob = new RoundKnobButton[SYNTH_NUM_OF_KNOBS];       //knob view object
    Singleton m_Inst = Singleton.getInstance();                             //singleton instance for knob views
    private boolean knobVerticalDrag;                                       //if true then knobs are dragged in a vertical fashion, rather than round
    static FileInOut fileInOut=new FileInOut();                             //file in out object

    Button saveButton,loadButton;                                   //load and save buttons
    private static ProgressBar spinner;                             //loading progress bar for long procedures
    private static ProgressDialog progress;

    //Gyroscope:
    static SensorHandler sensorHandler;                             //sensor handler for gyro
    Button calibrateZbutton;                                        //calibrate Z axis button

    //Bluetooth:
    BluetoothArduino mBlue;                                         //bluetooth handler
    private boolean btDeviceConnected = false;                      //is true if BT device is connected, false otherwise
    private boolean btHasControl = false;                           //is true if android should send arduino control messages, false otherwise.

    private long[] lastMsgSentTime = new long[SYNTH_NUM_OF_KNOBS];  //time in millis of last command sent for a certain knob. used to prevent bluetooth over-load
    private int[] knobLastValue = new int[SYNTH_NUM_OF_KNOBS];      //last sent value for each knob. used to prevent the same value to be sent over and over again
    private boolean spinnerOn = false;                              //true if spinner is on, false otherwise
    //private Timer myTimer;
    private boolean[] knobValueWaiting = new boolean[SYNTH_NUM_OF_KNOBS];
    private float[] knobNextValue = new float[SYNTH_NUM_OF_KNOBS];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_main_synth_activity);
        initSpinner();

        setListeners();
        handlePreferences();

        initKnobs();
        sensorHandler = new SensorHandler(this);
        initSensorViews();
        loadInitPreset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (TRY_CONNECTING_BT_ON_STARTUP && !btDeviceConnected){
            //showSpinner();
            initBluetooth();
            //hideSpinner();
        }
    }

    //shows spinner
    public void showSpinner(){
        if (DEBUG_NEW_SPINNER){

            progress.setMessage("Trying to connect to Bluetooth device");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
            spinnerOn = true;
            final int totalProgressTime = 100;
            final Thread t = new Thread(){

                @Override
                public void run(){

                    int jumpTime = 0;
                    while(spinnerOn){
                        try {
                            sleep(200);
                            jumpTime += 5;
                            progress.setProgress(jumpTime);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
//                    progress.hide();

                }
            };
            t.start();
        }else{
            spinner.setVisibility(View.VISIBLE);
        }
    }

    //hides spinner
    public void hideSpinner(){
        if (DEBUG_NEW_SPINNER){
            spinnerOn = false;
        }else{
            spinner.setVisibility(View.GONE);
        }
    }

    //init and connect to bluetooth module and display a proper message if succesful or not
    private boolean initBluetooth() {
        showSpinner();
        if (!DISABLE_BLUETOOTH){
            mBlue = BluetoothArduino.getInstance(DE_DUCK_SYNTH_NAME_TAG);
            btDeviceConnected = mBlue.Connect();
            if (!btDeviceConnected){
                Toast toast = Toast.makeText(getApplicationContext(), "Unable to connect to Bluetooth device", Toast.LENGTH_LONG);
                toast.show();
            }else{
                bluetoothControlSetState(true);
                Toast toast = Toast.makeText(getApplicationContext(), "Device connected via bluetooth", Toast.LENGTH_LONG);
                toast.show();
            }
        }
        for (int i=0;i<SYNTH_NUM_OF_KNOBS;i++){
            lastMsgSentTime[i]=System.currentTimeMillis();
            knobValueWaiting[i]=false;
        }
        hideSpinner();
        return btDeviceConnected;
    }

    //change control state of bluetooth device. if set to true - following knob changes will be sent to arduino device. otherwise they will be ignored
    private void bluetoothControlSetState(boolean newState){
        if (!DISABLE_BLUETOOTH){
            if (btDeviceConnected){
                mBlue.SendMessage(BT_CONTROL_STATE_CMD +" "+String.valueOf(newState?1:0));
                btHasControl = newState;
            }
        }
    }

    //send a value to arduino - set knob_id's value to value.
    public void sendBTvalue(int knob_id,float value){
        if ((!DISABLE_BLUETOOTH)){
            int valueToSend = Math.round(SimpleMath.map(value, 0, 1, 0, (float)ARDUINO_MAX_KNOB_VALUE));
            if (System.currentTimeMillis()-lastMsgSentTime[knob_id]>MINIMUM_GAP_BETWEEN_BT_COMMANDS){
                if (valueToSend!=knobLastValue[knob_id]){   //prevent un-needed sends of same-value
                    if (btDeviceConnected && btHasControl) {
                        if (DEBUG_BLUETOOTH) {
                            if (knob_id == 0) {
                                mBlue.SendMessage("0");
                                Log.d("SYNTH","bluetooth send: "+String.valueOf(0));
                            }
                            if (knob_id == 1) {
                                mBlue.SendMessage("1");
                                Log.d("SYNTH","bluetooth send: "+String.valueOf(1));
                            }
                        } else {
                                String msg = BT_KNOB_CHANGE_CMD+" " + String.valueOf(knob_id) + "=" + String.valueOf(valueToSend);
                                mBlue.SendMessage(msg);
                                //Log.d("SYNTH","bluetooth send: \""+msg+"\"");
                        }
                    }
                    lastMsgSentTime[knob_id] = System.currentTimeMillis();
                    knobLastValue[knob_id]=valueToSend;
                }

            }else{
                //schedule value for future send:
                BTmsgSendLater(knob_id,value);
            }
        }

    }

    private void BTmsgSendLater(final int knob_id,float value){
        if (knobValueWaiting[knob_id]){
            knobNextValue[knob_id]=value;
        }else{
            knobValueWaiting[knob_id]=true;
            /*
            Timer myTimer = new Timer();
            myTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    TimerMethod();
                }

            }, 0, MINIMUM_GAP_BETWEEN_BT_COMMANDS);
            */
            new RefreshTask(knob_id).execute();
        }
    }

    class RefreshTask extends AsyncTask {

        private int mKnobId;

        public RefreshTask(int knob_id) {
            mKnobId = knob_id;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            String text = String.valueOf(System.currentTimeMillis());
            //myTextView.setText(text);

        }

        @Override
        protected Object doInBackground(Object... params) {
            //while(someCondition) {
                try {
                    //sleep for 1s in background...
                    Thread.sleep(MINIMUM_GAP_BETWEEN_BT_COMMANDS);
                    //and update textview in ui thread
                    //publishProgress();
                    sendBTvalue(mKnobId,knobNextValue[mKnobId]);
                    knobValueWaiting[mKnobId]=false;
                } catch (InterruptedException e) {
                    e.printStackTrace();

                };
                return null;
            //}
        }
    }

    //loads the initial synth preset
    private void loadInitPreset() {
        if (fileInOut.isFileExist(FileInOut.INIT_PRESET_FILENAME)){
            loadPreset(FileInOut.INIT_PRESET_FILENAME);
        }else{
            //create init preset
            for (int i=0;i<SYNTH_NUM_OF_KNOBS;i++){
                knob[i].setRotorPercentage(50);
            }
            fileInOut.savePreset(FileInOut.INIT_PRESET_FILENAME,FileInOut.VALUES_AND_SENSORS,knobValues(),sensorHandler.getSensorSubscribers());
        }
    }

    //init spinning progress bar
    private void initSpinner() {
        if (DEBUG_NEW_SPINNER){
            progress = new ProgressDialog(this);
        }else{
            spinner = (ProgressBar)findViewById(R.id.progressBar1);
            spinner.setVisibility(View.GONE);
        }
    }

    //set button listeners
    private void setListeners() {
        saveButton=(Button)findViewById(R.id.save_preset_button);
        saveButton.setOnClickListener(this);
        loadButton=(Button)findViewById(R.id.load_preset_button);
        loadButton.setOnClickListener(this);
    }


    //options menu opens:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_synth_activity2, menu);
        //set bluetooth connect action to be disabled if already connect and change text to proper text
        //if not connected make action available.
        for (int i=0;i<menu.size();i++){
            if (menu.getItem(i).getItemId()==R.id.action_connect_to_bluetooth){
                MenuItem bluetoothConnectItem = menu.getItem(i);
                if (btDeviceConnected){
                    bluetoothConnectItem.setEnabled(false);
                    bluetoothConnectItem.setTitle(getResources().getString(R.string.connect_bt_title)+" (connected)");
                }else{
                    bluetoothConnectItem.setEnabled(true);
                    bluetoothConnectItem.setTitle(getResources().getString(R.string.connect_bt_title));
                }
                break;
            }
        }

        return true;
    }

    //options menu item selected:
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_settings:
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST);
                return true;
            case R.id.action_set_init_preset:
                openSavePresetDialog(SET_AS_INIT_PRESET);
                return true;
            case R.id.action_connect_to_bluetooth:
                initBluetooth();
                return true;
            case R.id.action_about:
                Intent intentAbout = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intentAbout);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //when returning from activity for result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SETTINGS_REQUEST:
                //update things that changed from settings change without calling onCreate
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                //update UI to match vertical-drag parameter:
                boolean newKnobVerticalDrag = sharedPref.getBoolean("pref_vertical_key",false);
                if (knobVerticalDrag!=newKnobVerticalDrag){
                    knobVerticalDrag=newKnobVerticalDrag;
                    for (int i=0;i<SYNTH_NUM_OF_KNOBS;i++){
                        knob[i].setVerticalDrag(knobVerticalDrag);
                    }
                }
        }
    }

    //init draggable axis assignment views and clear knob view
    private void initSensorViews() {
        LinearLayout panel = (LinearLayout)findViewById(R.id.axis_bar);

        //build sensor axis views and store as an array
        SensorAxisView[] sensorAxisView = new SensorAxisView[SENSOR_BUTTON_NUM];
        for (int i=0;i<SENSOR_BUTTON_NUM;i++){
            sensorAxisView[i] = new SensorAxisView(this);
            sensorAxisView[i].setAxisNumber(i);
        }
        //set images:
        sensorAxisView[0].setBackgroundResource(R.drawable.xaxis);
        sensorAxisView[1].setBackgroundResource(R.drawable.yaxis);
        sensorAxisView[2].setBackgroundResource(R.drawable.zaxis);
        sensorAxisView[3].setBackgroundResource(R.drawable.delete);  //draggable that resets an assignment to a knob
        //insert to layout
        for (int i=0;i<SENSOR_BUTTON_NUM;i++){

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            panel.addView(sensorAxisView[i],lp);

            sensorAxisView[i].getLayoutParams().width=(int) getResources().getDimension(R.dimen.axis_image_width);
            sensorAxisView[i].getLayoutParams().height=(int) getResources().getDimension(R.dimen.axis_image_height);
            sensorAxisView[i].init(String.valueOf(i));
            //axisLinearLayout.addView(sensorAxisView[i]);
        }
        calibrateZbutton = new Button(this);
        calibrateZbutton.setText("Calibrate Z");
        panel.addView(calibrateZbutton);
        calibrateZbutton.setOnClickListener(this);
    }

    //use sharedPreferences and apply them to current world
    private void handlePreferences(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        knobVerticalDrag = sharedPref.getBoolean("pref_vertical_key",false);
    }

    //attach a sensor axis to a certain knob
    public void attachSensorToKnob(int axisNum,RoundKnobButton knob){
        sensorHandler.subscribe(axisNum, knob);
    }

    //detach all sensor axis assignments from a knob
    public void clearKnobAssignments(RoundKnobButton knob){
        sensorHandler.unsubscribeKnob(knob);
    }

    //init knob views
    private void initKnobs(){
        //rotary knob code:
        //super.onCreate(savedInstanceState);

        // Scaling mechanism, as explained on:
        // http://www.pocketmagic.net/2013/04/how-to-scale-an-android-ui-on-multiple-screens/
        m_Inst.InitGUIFrame(this);

        //RelativeLayout panel = new RelativeLayout(this);
        GridLayout panel = (GridLayout)findViewById(R.id.main_knob_grid_layout);
//        int knob_width=((LinearLayout)panel.getParent()).getWidth()/3;
        int knob_width=KNOB_WIDTH;
        //setContentView(panel);
        for (int i=0;i<SYNTH_NUM_OF_KNOBS;i++){
            if (i==4){  //add a blank space for symmetry
                //i--;
                TextView tvTemp=new TextView(this,null);
                panel.addView(tvTemp);
            }
            RelativeLayout.LayoutParams lp;// = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            knob[i] = new RoundKnobButton(this, R.drawable.stator, R.drawable.rotoron, R.drawable.rotoroff,
                    m_Inst.Scale(knob_width), m_Inst.Scale(knob_width),i);
            lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            panel.addView(knob[i], lp);

            knob[i].setVerticalDrag(knobVerticalDrag);

            knob[i].setRotorPercentage(50);
            knob[i].setListener(new RoundKnobButton.RoundKnobButtonListener() {
                public void onStateChange(boolean newstate) {
                    Toast.makeText(MainSynthActivity.this, "New state:" + newstate, Toast.LENGTH_SHORT).show();
                }

                public void onRotate(final int percentage) {
                    //Toast.makeText(MainSynthActivity.this, i+"'s percentage:" + percentage, Toast.LENGTH_SHORT).show();
                /*
                tv2.post(new Runnable() {
                    public void run() {
                        tv2.setText("\n" + percentage + "%\n");
                    }
                });
                */
                    //handle percentage change:
                }
            });
        }
    }

    //open save preset diaglog box when trying to save a new preset or init preset
    private void openSavePresetDialog(int type){
       AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Add the buttons
       builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
               // User cancelled the dialog
           }
       });
       switch (type){
           case SET_AS_INIT_PRESET:
               builder.setTitle(R.string.save_as_init_preset_title);
               builder.setItems(R.array.save_array, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       fileInOut.savePreset(FileInOut.INIT_PRESET_FILENAME, which, knobValues(), sensorHandler.getSensorSubscribers());
                   }
               });
               break;
           case SAVE_AS_NEW_PRESET:
               builder.setTitle(R.string.save_as_new_preset_title);
               builder.setItems(R.array.save_array, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       nameFileDialog(which);
                   }
               });
               break;
       }
       // Create the AlertDialog
       AlertDialog dialog = builder.create();
       dialog.show();
    }

    //open file name dialog - for naming a preset
    private void nameFileDialog(final int saveType){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Preset name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText("De Duck Preset");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();
                fileInOut.savePreset(m_Text,saveType,knobValues(),sensorHandler.getSensorSubscribers());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    //open the load file dialog
    private void openLoadPresetDialog(){
        LoadFileDialog loadDialog = new LoadFileDialog(this,sensorHandler,fileInOut);
        loadDialog.show();
        //fileInOut.loadPreset("test",sensorHandler,spinner);
    }

    //get knob values as a float array
    private float[] knobValues() {
        float[] values = new float[SYNTH_NUM_OF_KNOBS];
        //gather values:
        for (int i=0;i<SYNTH_NUM_OF_KNOBS;i++){
            values[i]=knob[i].getValue();
        }
        return values;
    }


    //on button clicks
    @Override
    public void onClick(View v) {
        int id=v.getId();

        //check which button is clicked:
        if (id==saveButton.getId()){
            openSavePresetDialog(SAVE_AS_NEW_PRESET);
        }else if (id==loadButton.getId()){
            openLoadPresetDialog();
        }else if (id==calibrateZbutton.getId()){
            sensorHandler.calibrateZaxis();
        }
    }

    //load a preset from SDcard or local space
    public static void loadPreset(String name) {
        int result = fileInOut.loadPreset(name,sensorHandler);
        if (result==FileInOut.CANNOT_LOAD_PRESET){
            Toast toast = Toast.makeText(MyApplication.getAppContext(), "Unable to load preset", Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
