package com.hels.elements;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Messenger;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link AppWidgetConfigureActivity AppWidgetConfigureActivity}
 */
public class AppWidget extends AppWidgetProvider {

    Messenger syncServiceMessenger = null;
    boolean syncServiceIsBound = false;


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

       // CharSequence widgetText = AppWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object

        MLogger.logToFile(context, "service.txt", String.format("WID: onUpdate (%d)", appWidgetId), true);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        views.setTextViewText(R.id.tv_widgetId, String.valueOf(appWidgetId));

        //---- Intent: open Mug fragment by clicking on Current temperature -----------------------
        Intent showMugSettingsIntent = new Intent(context, MainActivity.class);
        showMugSettingsIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(appWidgetId)));
        showMugSettingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  // Identifies the particular widget...
        showMugSettingsIntent.putExtra("WidgetID", appWidgetId);
        //showMugSettingsIntent.putExtra("Action", "mug");
        showMugSettingsIntent.setAction("mug");
        PendingIntent showMugSettingsPendingIntent = PendingIntent.getActivity(context, 0, showMugSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        //views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);;
        views.setOnClickPendingIntent(R.id.tv_currentTemperature, showMugSettingsPendingIntent);;
        appWidgetManager.updateAppWidget(appWidgetId, views);
        //-----------------------------------------------------------------------------------------
        //---- Intent: open Mug Widget setting fragment by clicking on Mug image ------------------
        Intent showWidgetConfigIntent = new Intent(context, MainActivity.class);
        showWidgetConfigIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(appWidgetId)));
        showWidgetConfigIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  // Identifies the particular widget...
        showWidgetConfigIntent.putExtra("WidgetID", appWidgetId);
        showWidgetConfigIntent.setAction("widget");
        PendingIntent showWidgetConfigPendingIntent = PendingIntent.getActivity(context, 0, showWidgetConfigIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        //views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);;
        views.setOnClickPendingIntent(R.id.iv_mug, showWidgetConfigPendingIntent);;
        //-----------------------------------------------------------------------------------------

        String mac =AppPreferences.readString(context,  String.format(Locale.getDefault(), "widget_%d", appWidgetId), "mac");
        if(mac != null) views.setTextViewText(R.id.tv_mac, mac);

        Boolean isThreadRunning = SyncService.isThreadRunning(context, String.format("*%s_%s", context.getPackageName(), mac));
        if(isThreadRunning == null) isThreadRunning = false;

        String ts = String.valueOf(new SimpleDateFormat("HH:mm:ss:SSS").format((new Date()).getTime()));
        if( isThreadRunning ) {
            ts = "R:" + ts;
        }
        else ts = "N:" + ts;

        views.setTextViewText(R.id.tv_threadStatus, ts);

        MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onUpdate (%d). Thread %s is running: %B", appWidgetId, mac, isThreadRunning), true);

        /* Disabled - widget isn't allowed to start the service.
        Boolean isEnabled =  AppPreferences.readBoolean( context,  String.format("mug_%s", mac), AppPreferences.NAME_ENABLED);
        if(isEnabled == null) isEnabled = false;

        MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onUpdate (%d). Thread %s is enabled: %B", appWidgetId, mac, isEnabled), true);

        if( isEnabled & !isThreadRunning ) {
            try {
                MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onUpdate (%d). Restarting %s", appWidgetId, mac), true);
                Intent intent = new Intent(context, SyncService.class);

                intent.putExtra("mac", mac);
                intent.putExtra("widgetID", appWidgetId);
                context.startService(intent);
            }
            catch(Exception e) {
                MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onUpdate (%d). Restarting %s. Exception %s", appWidgetId, mac, e.toString()), true);
            }
        }

        views.setTextViewText(R.id.tv_threadStatus, "r");

         */

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        //MLogger.logToFile(context, "service.txt", "WID: On Update", true);
        for(int appWidgetId : appWidgetIds) {
            String mac =AppPreferences.readString(context,  String.format(Locale.getDefault(), "widget_%d", appWidgetId), "mac");
            if( mac == null) mac = "NA";
            MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onUpdate widget #%d %s", appWidgetId, mac), true);
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.

        for(int appWidgetId : appWidgetIds) {
            MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onDeleted (%d)", appWidgetId), true);
            // to stop the Thread will check if preferences files are exist or not.
            String mac = AppPreferences.readString( context,  String.format(Locale.getDefault(), "widget_%d", appWidgetId), "mac");
            AppPreferences.remove(context, String.format(Locale.getDefault(), "widget_%d", appWidgetId));
            if( mac != null) {
                //MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onDeleted (%d). Removing %s", appWidgetId, String.format("mug_%s", mac)), true);
                Boolean r =  AppPreferences.remove( context,  String.format("mug_%s", mac));
                MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onDeleted (%d). Removing %s: %B", appWidgetId, String.format("mug_%s", mac), r), true);

            }

            Boolean r =  AppPreferences.remove( context,  String.format(Locale.getDefault(), "widget_%d", appWidgetId) );
            MLogger.logToFile(context, "service.txt", String.format(Locale.getDefault(), "WID: onDeleted (%d). Removing %s: %B", appWidgetId, String.format("mug_%s", mac), r), true);

        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}