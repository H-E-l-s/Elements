package com.hels.elements;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Log.d("ACTIVITY", "ACTIVITY: ACTION_BOOT_COMPLETED");
            MLogger.logToFile( context, "service.txt", "ACTIVITY: ACTION_BOOT_COMPLETED", true);

            Intent syncServiceIntent = new Intent(context, SyncService.class);
            syncServiceIntent.putExtra("task", "on_boot");
            context.startService(syncServiceIntent);
        }
    }
}


