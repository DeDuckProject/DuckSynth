package com.example.iftachy.duckcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.inputmethodservice.ExtractEditText;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by iftachyakar on 4/15/15.
 */
public class FileInOut {

    //save file types:
    public static final int VALUES_AND_SENSORS=0;
    public static final int VALUES_ONLY=1;
    public static final int SENSORS_ONLY=2;
    private static final int DEFAULT_PRESET_TYPE = VALUES_AND_SENSORS;

    //XML titles and attribute names:
    private static final String ROOT_TITLE = "deDuckSynthPreset";
    private static final String PRESET_TYPE_TITLE = "presettype";
    private static final String VALUES_TITLE = "values";
    private static final String KNOB_ATTRIBUTE_PREFIX = "knob";
    private static final String SENSORS_TITLE = "sensors";
    private static final String SENSOR_ATTRIBUTE_PREFIX = "sensor";
    private static final String CONNECTED_ATT_VALUE = KNOB_ATTRIBUTE_PREFIX;
    private static final String PRESET_ATTRIBUTE = "type";

    //file and dir names:
    private static final String DEFAULT_SAVE_DIR = "DeDuckSynth presets";
    private static final String FILE_EXTENSION = "duk";
    public static final String INIT_PRESET_FILENAME = "initPreset";

    //file load results:
    public static final int CANNOT_LOAD_PRESET = 0;
    private static final int LOAD_OK = 1;

    private static XmlPullParserFactory xmlFactoryObject;       //XML parser factory obj

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    //get default save/load dir
    public static File getDefaultDir(){
        //should make sure sd-card write is available...
        if (!isExternalStorageWritable())
            return null;
        File sdCard = Environment.getExternalStorageDirectory();
        String sdcardPath = sdCard.getAbsolutePath();
        File dir = new File (sdcardPath + "/"+DEFAULT_SAVE_DIR);
        return dir;
    }

    //get file list from dir and filter it by given type (0 - no filter)
    public static ArrayList<File> getFileList(int type){
        ArrayList<File> list=new ArrayList<File>();
        ArrayList<File> filteredList=new ArrayList<File>();
        File dir = FileInOut.getDefaultDir();
        File[] filelist = dir.listFiles();
        for (int i=0;i<filelist.length;i++){
            if (filelist[i].getName().endsWith("."+FILE_EXTENSION)){
                list.add(filelist[i]);
            }
        }

        //remove unneccesary parts of the list depending on type
        if (type==VALUES_ONLY || type==SENSORS_ONLY){
            for (File file : list){
                if (getType(file)==type){
                    filteredList.add(file);
                }
            }
            return filteredList;
        }
        return list;

    }

    //get file type wrapper (call this method!)
    private static int getType(File file) {
        try {
            InputStream stream = new FileInputStream(file);
            xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();

            myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES
                    , false);
            myparser.setInput(stream, null);
            int ret_val = getTypeFromXML(myparser);
            stream.close();
            return ret_val;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    //get file type
    private static int getTypeFromXML(XmlPullParser myParser) {
        int event;
        String text=null;

        int presetType=DEFAULT_PRESET_TYPE;
        try {
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name=myParser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(name.equals(ROOT_TITLE)){
                            break;
                        }
                        else if (name.equals(PRESET_TYPE_TITLE)){
                            presetType = Integer.parseInt(myParser.getAttributeValue(null,PRESET_ATTRIBUTE));
                            return presetType;
                        }
                        break;
                }
                event = myParser.next();

            }
            //parsingComplete = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    //save a preset using current values, sensor assignments and using a given save mode/type
    public void savePreset(String filename, int mode, float[] knobValues, List<List<RoundKnobButton>> sensorSubscribers){
        try{
            //MainSynthActivity.showSpinner();
            //Create instance of DocumentBuilderFactory
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            //Get the DocumentBuilder
            DocumentBuilder parser = factory.newDocumentBuilder();
            //Create blank DOM Document
            Document doc=parser.newDocument();
            //create the root element
            Element root=doc.createElement(ROOT_TITLE);
            //all it to the xml tree
            doc.appendChild(root);
            Element presetType = doc.createElement(PRESET_TYPE_TITLE);
            presetType.setAttribute(PRESET_ATTRIBUTE,String.valueOf(mode));
            root.appendChild(presetType);

            if (mode!=SENSORS_ONLY){
                Element values=doc.createElement(VALUES_TITLE);
                for (int i=0;i<MainSynthActivity.SYNTH_NUM_OF_KNOBS;i++){
                    values.setAttribute(KNOB_ATTRIBUTE_PREFIX+String.valueOf(i),String.valueOf(knobValues[i]));
                }
                root.appendChild(values);
            }
            if (mode!=VALUES_ONLY){
                Element sensors=doc.createElement(SENSORS_TITLE);
                for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
                    //add knob
                    Element sensor=doc.createElement(SENSOR_ATTRIBUTE_PREFIX+String.valueOf(i));
                    for (RoundKnobButton subscriber : sensorSubscribers.get(i)){
                        sensor.setAttribute(KNOB_ATTRIBUTE_PREFIX+String.valueOf(subscriber.getID()),CONNECTED_ATT_VALUE);
                    }
                    sensors.appendChild(sensor);
                }
                root.appendChild(sensors);
            }

            TransformerFactory transformerfactory=
                    TransformerFactory.newInstance();
            Transformer transformer=
                    transformerfactory.newTransformer();

            DOMSource source=new DOMSource(doc);

            File dir = getDefaultDir();
            dir.mkdirs();
            File file = new File(dir, filename+"."+FILE_EXTENSION);

            FileOutputStream _stream = new FileOutputStream(file);
            StreamResult result=new StreamResult(_stream);
            transformer.transform(source, result);

        }catch(Exception ex){
            ex.printStackTrace();
        }finally {
            //MainSynthActivity.hideSpinner();
        }
    }

    //load XML from file
    private void parseXMLAndStoreIt(XmlPullParser myParser,SensorHandler sensorHandler) throws XmlPullParserException, IOException {
        int event;
        String text=null;

        int presetType=DEFAULT_PRESET_TYPE;
//        try {
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name=myParser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(name.equals(ROOT_TITLE)){
                            break;
                        }
                        else if (name.equals(PRESET_TYPE_TITLE)){
                            presetType = Integer.parseInt(myParser.getAttributeValue(null,PRESET_ATTRIBUTE));
                            if (presetType!=VALUES_ONLY){   //sensor are participants in this
                                sensorHandler.unsubscribeAll();
                            }
                        }
                        else if(name.equals(VALUES_TITLE)){
                            for (int i=0;i<MainSynthActivity.SYNTH_NUM_OF_KNOBS;i++){
                                RoundKnobButton.getButtonByID(i).setRotorPercentage(Float.parseFloat(myParser.getAttributeValue(null,KNOB_ATTRIBUTE_PREFIX+String.valueOf(i))));
                                //knobValues[i] = Float.parseFloat(myParser.getAttributeValue(null,KNOB_ATTRIBUTE_PREFIX+String.valueOf(i)));
                            }
                        }
                        else if(name.startsWith(SENSOR_ATTRIBUTE_PREFIX) && !name.equals(SENSORS_TITLE)){

                            for (int i=0;i< MainSynthActivity.SENSOR_AXIS_NUM;i++){
                                //add knob
                                if (name.equals(SENSOR_ATTRIBUTE_PREFIX+String.valueOf(i))){
                                    for (int j=0;j<myParser.getAttributeCount();j++){
                                        String attName = myParser.getAttributeName(j);
                                        int knobNum= Integer.parseInt(attName.substring(KNOB_ATTRIBUTE_PREFIX.length()));
                                        sensorHandler.subscribe(i,RoundKnobButton.getButtonByID(knobNum));
                                    }
                                }
                            }
                        }
                        break;
                }
                event = myParser.next();

            }
            //parsingComplete = false;
//        } catch (Exception e) {
//        }

    }

    //load XML from filename.
    //use this method for loading a file
    public int loadPreset(final String filename, SensorHandler sensorHandler){
        try {
            //MainSynthActivity.showSpinner();
            File dir = getDefaultDir();
            dir.mkdirs();
            //File file = new File(dir, filename+"."+FILE_EXTENSION);
            File file = new File(dir, filename);

            InputStream stream = new FileInputStream(file);

            //InputStream stream = conn.getInputStream();

            xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();

            myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES
                    , false);
            myparser.setInput(stream, null);
            parseXMLAndStoreIt(myparser,sensorHandler);
            stream.close();
        } catch (Exception e) {
            //MainSynthActivity.hideSpinner();
            return CANNOT_LOAD_PRESET;
            //e.printStackTrace();
        } finally {
            //MainSynthActivity.hideSpinner();
        }
        //sensorHandler = result;
        return LOAD_OK;

    }

    //check if a cerain file exists in default path
    public boolean isFileExist(String filename) {
        File dir = getDefaultDir();
        dir.mkdirs();
        //File file = new File(dir, filename+"."+FILE_EXTENSION);
        File file = new File(dir, filename);
        return (file.exists());
    }
}
