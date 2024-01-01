package com.hels.elements;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by DF on 2/16/2017.
 */

class MLogger {

    public static final int LOG_BOX    = 0x01;
    public static final int LOG_FILE   = 0x02;

    public static final String PARAM_LOG_LINE       = "log_line";
    public static final String PARAM_LOG_LINE_ID    = "log_line_id";

    private String logFileName = "";
    private boolean logEnabled = false;
    private Context context = null;

    private TextView tw_Log = null;
    private ScrollView sw_Log = null;

    private boolean boxEnabled = true;

    DataReceiver dataReceiver = null;

    public MLogger(Context context, View view, String logFileName/*, ScrollView scrollView*/) {
        this.context = context;
        this.logFileName = logFileName;
//        this.sw_Log = scrollView;

        if( view != null) {
            boxEnabled = true;
            tw_Log = (TextView) view.findViewById(R.id.tv_logger);

            tw_Log.setSelected(true);
            tw_Log.setMovementMethod(new ScrollingMovementMethod());

            this.sw_Log = (ScrollView) view.findViewById(R.id.log_scroll_view);

            //------------------------------------------------------------------------------------------
            IntentFilter dataFilter = new IntentFilter(DataReceiver.DATA_MSG);
            dataFilter.addCategory(Intent.CATEGORY_DEFAULT);
            dataReceiver = new DataReceiver();
            context.registerReceiver(dataReceiver, dataFilter);
            //------------------------------------------------------------------------------------------
        }
    }

    public void addLogLine(String s, int dest) {

        //dest &= ~LOG_BOX;
        //dest = 0;
        if( (sw_Log != null) && ((dest & LOG_BOX) != 0) && boxEnabled  ) {
            /*
            Intent messageIntent = new Intent();
            messageIntent.setAction(DataReceiver.DATA_MSG);
            messageIntent.addCategory(Intent.CATEGORY_DEFAULT);
            messageIntent.putExtra(PARAM_LOG_LINE, s);
            context.sendBroadcast(messageIntent);*/
            tw_Log.append(s + "\n");

            //int scrollAmount = dataText.getLayout().getLineTop(dataText.getLineCount()) - dataText.getHeight();
            //if (scrollAmount > 0) dataText.scrollTo(0, scrollAmount);
            //else dataText.scrollTo(0, 0);
            sw_Log.post(new Runnable()
            {
                public void run()
                {
                    sw_Log.fullScroll(View.FOCUS_DOWN);
                }
            });

        }
        if( (dest & LOG_FILE) != 0 ) {
            //File tDir = new File(logFileName);
            //File tFile = new File(tDir, "hs.txt");


            File logDir = new File(/*context.getFilesDir()*/getExternalStorageDirectory(), "logs");
            //File dir = context.getFilesDir();
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            File tFile = new File(logDir, logFileName);

            FileWriter fw=null;
            long logFileTimeStamp = 0;
            SimpleDateFormat dateFormat;

            dateFormat = new SimpleDateFormat("d HH:mm:ss:SSS");
            logFileTimeStamp = (new Date()).getTime();
            String ts = String.valueOf( dateFormat.format(logFileTimeStamp) );

            try {
                fw = new FileWriter(tFile, true);
                fw.write(String.valueOf( dateFormat.format(logFileTimeStamp) ) + " " + s + "\n");
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if( dataReceiver != null) context.unregisterReceiver(dataReceiver);
    }

    public class DataReceiver extends BroadcastReceiver {
        public static final String DATA_MSG = "com.hels.DATA_TO_DISPLAY";

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(PARAM_LOG_LINE);
            int id = intent.getIntExtra(PARAM_LOG_LINE_ID, 0)             ;
            //dataText.append(text + "\n");
            if(tw_Log != null) {
                tw_Log.append(text + "\n");

                //int scrollAmount = dataText.getLayout().getLineTop(dataText.getLineCount()) - dataText.getHeight();
                //if (scrollAmount > 0) dataText.scrollTo(0, scrollAmount);
                //else dataText.scrollTo(0, 0);
                sw_Log.post(new Runnable()
                {
                    public void run()
                    {
                        sw_Log.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }
    }


    public static void logToFile(Context context, String logFileName, String line, boolean logEnabled) {

        if( ":".equals(line.substring(3,4) ) ) {
            Log.d(line.substring(0,3) , line);
        }

        if(logEnabled == false) return;
/*
        File logDir = new File(getExternalStorageDirectory(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        File tFile = new File(logDir, logFileName);
*/


        try {

            FileWriter fw=null;

            long logFileTimeStamp = 0;
            //SimpleDateFormat dateFormat;

            //dateFormat = new SimpleDateFormat("d:HH:mm:ss:SSS");
            logFileTimeStamp = (new Date()).getTime();
            //String ts = String.valueOf( dateFormat.format(logFileTimeStamp) );

            //logFileName = "antiember_"
            File tFile = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    + File.separator + "logs" + File.separator + "antiember_" + new SimpleDateFormat("y-MM-d", Locale.US).format(logFileTimeStamp) + ".txt" );


            fw = new FileWriter(tFile, true);
            fw.write( new SimpleDateFormat("HH:mm:ss:SSS", Locale.US).format(logFileTimeStamp)  + " " + line + "\n");
            fw.flush();
            fw.close();
        } catch (IOException e) {
            //e.printStackTrace();
            Log.d("LOG", String.format( "Logger exception %s", e.toString()) );
        }
    }

}
