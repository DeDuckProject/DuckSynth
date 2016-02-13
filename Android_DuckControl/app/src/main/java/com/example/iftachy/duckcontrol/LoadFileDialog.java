package com.example.iftachy.duckcontrol;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static com.example.iftachy.duckcontrol.R.drawable.*;

/**
 * This is a custom dialog class that will hold a tab view with 2 tabs.
 * Tab 1 will be a list view. Tab 2 will be a list view.
 *
 */
public class LoadFileDialog extends Dialog
{
    /**
     * Our custom list view adapter for tab 1 listView (listView01).
     */
    ListView01Adapter listView01Adapter = null;

    /**
     * Our custom list view adapter for tab2 listView (listView02).
     */
    ListView02Adapter listView02Adapter = null;
    ListView02Adapter listView03Adapter = null;
    ArrayList<File> allFiles;// = FileInOut.getFileList(FileInOut.VALUES_AND_SENSORS);
    ArrayList<File> onlyValueFiles;// = FileInOut.getFileList(FileInOut.VALUES_ONLY);
    ArrayList<File> onlySensorFiles;// = FileInOut.getFileList(FileInOut.SENSORS_ONLY);
    Context mContext;
    /**
     * Default constructor.
     *  @param context
     * @param sensorHandler
     * @param spinner
     * @param fileInOut
     */
    public LoadFileDialog(Context context, SensorHandler sensorHandler, FileInOut fileInOut)
    {
        super(context);
        mContext=context;
        // get this window's layout parameters so we can change the position
        WindowManager.LayoutParams params = getWindow().getAttributes();

        // change the position. 0,0 is center
        params.x = 0;
        params.y = 0;//250;
        this.getWindow().setAttributes(params);

        // no title on this dialog
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.custom_dialog_layout);

        // instantiate our list views for each tab
        ListView listView01 = (ListView)findViewById(R.id.listView01);
        ListView listView02 = (ListView)findViewById(R.id.listView02);
        ListView listView03 = (ListView)findViewById(R.id.listView03);

        // register a context menu for all our listView02 items
        registerForContextMenu(listView02);
        //registerForContextMenu(listView03);

        allFiles = FileInOut.getFileList(FileInOut.VALUES_AND_SENSORS);
        onlyValueFiles = FileInOut.getFileList(FileInOut.VALUES_ONLY);
        onlySensorFiles = FileInOut.getFileList(FileInOut.SENSORS_ONLY);

        // instantiate and set our custom list view adapters
        listView01Adapter = new ListView01Adapter(context,allFiles);
        listView01.setAdapter(listView01Adapter);

        listView02Adapter = new ListView02Adapter(context,onlyValueFiles);
        listView02.setAdapter(listView02Adapter);

        listView03Adapter = new ListView02Adapter(context,onlySensorFiles);
        listView03.setAdapter(listView03Adapter);


        // bind a click listener to the listView01 list
        listView01.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id)
            {
                File file = (File) listView01Adapter.getItem(position);
                MainSynthActivity.loadPreset(file.getName());
                //fileInOut.loadPreset(file.getName(),sensorHandler,spinner);
                dismiss();
            }
        });

        // bind a click listener to the listView02 list
        listView02.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id)
            {
                File file = (File) listView02Adapter.getItem(position);
                MainSynthActivity.loadPreset(file.getName());
                // will dismiss the dialog
                dismiss();
            }
        });

        // bind a click listener to the listView02 list
        listView03.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parentView, View childView, int position, long id)
            {
                File file = (File) listView03Adapter.getItem(position);
                MainSynthActivity.loadPreset(file.getName());
                // will dismiss the dialog
                dismiss();
            }
        });

        // get our tabHost from the xml
        TabHost tabs = (TabHost)findViewById(R.id.TabHost01);
        tabs.setup();

        // create tab 1
        TabHost.TabSpec tab1 = tabs.newTabSpec("tab1");
        tab1.setContent(R.id.listView01);
        tab1.setIndicator("All presets");
        tabs.addTab(tab1);

        // create tab 2
        TabHost.TabSpec tab2 = tabs.newTabSpec("tab2");
        tab2.setContent(R.id.listView02);
        tab2.setIndicator("Only Values");
        tabs.addTab(tab2);

        // create tab 3
        TabHost.TabSpec tab3 = tabs.newTabSpec("tab3");
        tab3.setContent(R.id.listView03);
        tab3.setIndicator("Only Sensors");
        tabs.addTab(tab3);
    }

    /**
     * A custom list adapter for the listView01
     */
    private class ListView01Adapter extends BaseAdapter
    {
        ArrayList<File> mList;
        public ListView01Adapter(Context context,ArrayList<File> list)
        {
            mList=list;
        }

        /**
         * This is used to return how many rows are in the list view
         */
        public int getCount()
        {
            // add code here to determine how many results we have, hard coded for now

            return mList.size();
        }

        /**
         * Should return whatever object represents one row in the
         * list.
         */
        public Object getItem(int position)
        {
            return mList.get(position);
        }

        /**
         * Used to return the id of any custom data object.
         */
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * This is used to define each row in the list view.
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;

            // our custom holder will represent the view on each row. See class below.
            ListView01Holder holder = null;

            if (true)//(row == null)
            {
                LayoutInflater inflater = getLayoutInflater();

                // inflate our row from xml
                row = inflater.inflate(R.layout.list_view_01_row, parent, false);


                // instantiate our holder
                holder = new ListView01Holder(row,position);

                // set our holder to the row
                row.setTag(holder);
            }
            else
            {
                holder = (ListView01Holder)row.getTag();
            }

            return row;
        }

        // our custom holder
        class ListView01Holder
        {
            // text view
            private TextView text = null;

            // image view
            private ImageView image = null;
            private ImageView image2 = null;

            ListView01Holder(View row, int position)
            {
                // get out text view from xml
                //text = (TextView)row.findViewById(R.id.image);
                text = (TextView)row.findViewById(R.id.list_view_01_row_text_view);

                // add code here to set the text
                text.setText(mList.get(position).getName());

                // get our image view from xml
                image = (ImageView)row.findViewById(R.id.list_view_01_row_image_view);

                // add code here to determine which image to load
                int id = mContext.getResources().getIdentifier("com.example.iftachy.duckcontrol:drawable/" + "load_values_indicator", null, null);
                image.setImageResource(id);
                image.setAdjustViewBounds(true);
                image.setScaleX(image.getScaleY());

                // get our image view from xml
                image2 = (ImageView)row.findViewById(R.id.list_view_01_row_image_view2);

                // add code here to determine which image to load
                int id2 = mContext.getResources().getIdentifier("com.example.iftachy.duckcontrol:drawable/" + "load_sensor_indicator", null, null);
                image2.setImageResource(id2);
                image2.setAdjustViewBounds(true);
                image2.setScaleX(image2.getScaleY());

                //set indicators visibility according to file type:
                if (onlyValueFiles.contains(mList.get(position))){
                    image2.setVisibility(View.INVISIBLE);
                }else if (onlySensorFiles.contains(mList.get(position))){
                    image.setVisibility(View.INVISIBLE);
                }

            }
        }
    }

    /**
     * A custom list adapter for listView02
     */
    private class ListView02Adapter extends BaseAdapter
    {
        ArrayList<File> mList;
        public ListView02Adapter(Context context,ArrayList<File> list)
        {
            mList=list;
        }

        /**
         * This is used to return how many rows are in the list view
         */
        public int getCount()
        {
            // add code here to determine how many results we have, hard coded for now

            return mList.size();
        }

        /**
         * Should return whatever object represents one row in the
         * list.
         */
        public Object getItem(int position)
        {
            return mList.get(position);
        }

        /**
         * Used to return the id of any custom data object.
         */
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * This is used to define each row in the list view.
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            ListView02Holder holder = null;

            if(row == null)
            {
                LayoutInflater inflater = getLayoutInflater();

                row=inflater.inflate(R.layout.list_view_02_row, parent, false);
                holder = new ListView02Holder(row,position);
                row.setTag(holder);
            }
            else
            {
                holder = (ListView02Holder)row.getTag();
            }

            return row;
        }

        class ListView02Holder
        {
            private TextView text = null;

            ListView02Holder(View row, int position)
            {
                text = (TextView)row.findViewById(R.id.list_view_02_row_text_view);
                text.setText(mList.get(position).getName());
            }
        }
    }

    /**
     * This is called when a long press occurs on our listView02 items.
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Context Menu");
        menu.add(0, v.getId(), 0, "Delete");
    }

    /**
     * This is called when an item in our context menu is clicked.
     */
    public boolean onContextItemSelected(MenuItem item)
    {
        if(item.getTitle() == "Delete")
        {

        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * A custom list adapter for listView02
     */
    private class ListView03Adapter extends BaseAdapter
    {
        public ListView03Adapter(Context context)
        {

        }

        /**
         * This is used to return how many rows are in the list view
         */
        public int getCount()
        {
            // add code here to determine how many results we have, hard coded for now

            return 5;
        }

        /**
         * Should return whatever object represents one row in the
         * list.
         */
        public Object getItem(int position)
        {
            return position;
        }

        /**
         * Used to return the id of any custom data object.
         */
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * This is used to define each row in the list view.
         */
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            ListView02Holder holder = null;

            if(row == null)
            {
                LayoutInflater inflater = getLayoutInflater();

                row=inflater.inflate(R.layout.list_view_02_row, parent, false);
                holder = new ListView02Holder(row);
                row.setTag(holder);
            }
            else
            {
                holder = (ListView02Holder)row.getTag();
            }

            return row;
        }

        class ListView02Holder
        {
            private TextView text = null;

            ListView02Holder(View row)
            {
                text = (TextView)row.findViewById(R.id.list_view_02_row_text_view);
                text.setText("");
            }
        }
    }
}