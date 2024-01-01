package com.hels.elements;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AppWidgetUpdate extends BroadcastReceiver {

    //public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle b = intent.getExtras();
            Integer wid =  b.getInt("wid");



//            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG");
//            //Acquire the lock
//            wl.acquire();

            //You can do the processing here update the widget/remote views.
            //RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
            //        R.layout.time_widget_layout);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
            //remoteViews.setTextViewText(R.id.tv_threadStatus, Utility.getCurrentTime("hh:mm:ss a"));
            //ComponentName thiswidget = new ComponentName(context, AppWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            //manager.updateAppWidget(thiswidget, remoteViews);
            //manager.updateAppWidget(wid, remoteViews);
            //Release the lock
//            wl.release();

            Boolean isThreadRunning = true;// SyncService.isThreadRunning(context, String.format("*%s_%s", context.getPackageName(), mac));
            if(isThreadRunning == null) isThreadRunning = false;

            String ts = String.valueOf(new SimpleDateFormat("HH:mm:ss:SSS").format((new Date()).getTime()));
            if( isThreadRunning ) {
                ts = "UR:" + ts;
            }
            else ts = "UN:" + ts;

            remoteViews.setTextViewText(R.id.tv_threadStatus, ts);
            manager.updateAppWidget(wid, remoteViews);
            MLogger.logToFile(context, "service.txt", String.format( "MUG: ALRM Widget(%d) Update", wid), true);
        }
}
