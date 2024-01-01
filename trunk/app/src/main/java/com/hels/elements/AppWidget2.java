package com.hels.elements;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget2 extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget2);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        Intent launchActivity = new Intent(context, MainActivity.class);
        launchActivity.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(appWidgetId)));
        launchActivity.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  // Identifies the particular widget...
        launchActivity.putExtra("WidgetID", appWidgetId);
/*
        launchActivity.addCategory(Intent.CATEGORY_LAUNCHER);
        // first param is app package name, second is package.class of the main activity
        ComponentName cn = new ComponentName("com.hels.elements","com.hels.elements.WidgetActivity");
        launchActivity.setComponent(cn);
 */
        // launchActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget2_layout, pendingIntent);;

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for(int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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