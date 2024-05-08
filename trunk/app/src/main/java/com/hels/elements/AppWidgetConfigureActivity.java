package com.hels.elements;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.core.app.ActivityCompat;

import com.hels.elements.databinding.AppWidgetConfigureBinding;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/**
 * The configuration screen for the {@link AppWidget AppWidget} AppWidget.
 */
public class AppWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "com.hels.elements.AppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    Activity activity;

    Messenger syncServiceMessenger = null;
    boolean syncServiceIsBound = false;

    BTDeviceListAdapter listAdapter;

    ArrayList<SolarEnergyMeterParameters> semParametersToShow, semParametersToCheck;

    ProgressBar pb_progress;
    //EditText mAppWidgetText;
    /*
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = AppWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
//            String widgetText = mAppWidgetText.getText().toString();
//            saveTitlePref(context, mAppWidgetId, widgetText);

            if(deviceAddress != null)
                AppPreferences.save(AppWidgetConfigureActivity.this, String.format("widget_%d", mAppWidgetId), "mac", deviceAddress );

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            AppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };
    */

    private AppWidgetConfigureBinding binding;

    public AppWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
//    static void saveTitlePref(Context context, int appWidgetId, String text) {
//        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
//        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
//        prefs.apply();
//    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if(titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MLogger.logToFile(this, "service.txt", "WCF: onCreate", true);
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
//        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }

        activity = this;

        setResult(RESULT_CANCELED);

        binding = AppWidgetConfigureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        mAppWidgetText = binding.appwidgetText;
//        binding.addButton.setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        pb_progress = (ProgressBar) findViewById(R.id.pb_progress);

        semParametersToShow = new ArrayList<>();
        semParametersToCheck = new ArrayList<>();

        listAdapter = new BTDeviceListAdapter(this, semParametersToShow);
        ListView lv_bt_list = (ListView) findViewById(R.id.lv_bt_list);
        lv_bt_list.setAdapter(listAdapter);

        lv_bt_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onClick selected device", true);
                // requestToTerminate();

                String deviceAddress = semParametersToShow.get(position).getMACAddress();

                /* obsolete
                if( MugParameters.TYPE_PAIRED ==  mugsParametersToShow.get(position).getType() ) {
                    if(deviceAddress != null)
                        AppPreferences.save(AppWidgetConfigureActivity.this, String.format(Locale.getDefault(), "widget_%d", mAppWidgetId), "mac", mugsParametersToShow.get(position).getMACAddress() );

                    // It is the responsibility of the configuration activity to update the app widget
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                    AppWidget.updateAppWidget(getApplicationContext(), appWidgetManager, mAppWidgetId);

                    // Make sure we pass back the original appWidgetId
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }

                if( MugParameters.TYPE_DISCOVERED ==  mugsParametersToShow.get(position).getType() ) {
                    showAlertDialog(activity, "AAAAA", "BBBB");
                    requestToBond(deviceAddress);
                }
                end of obsolete*/

                MLogger.logToFile(  getApplicationContext(),
                        "service.txt",
                        String.format(Locale.getDefault(),  "WCF: onItemClick: %s ", deviceAddress), true);

                AppPreferences.save(AppWidgetConfigureActivity.this, String.format(Locale.getDefault(), "widget_%d", mAppWidgetId), "mac", deviceAddress );

                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                AppWidget.updateAppWidget(getApplicationContext(), appWidgetManager, mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();


            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        MLogger.logToFile(this, "service.txt", "WCF: onStart", true);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        pb_progress.setIndeterminate(true);

        semParametersToShow.clear();
        listAdapter.notifyDataSetChanged();

        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        semParametersToCheck.clear();
        semParametersToShow.clear();
        listAdapter.notifyDataSetChanged();

        if( pairedDevices.size() > 0) {
            for(BluetoothDevice d : pairedDevices) {
                if( (d.getName() != null) && d.getName().length() >="Ember".length())
                    if("Ember".equals(d.getName().substring(0,"Ember".length())))
                        semParametersToCheck.add( new SolarEnergyMeterParameters( d.getAddress(), d.getName(), null, null, null, null, null, null, null, null, null, null, null, null, SolarEnergyMeterParameters.TYPE_PAIRED, false, null, null) );
            }
        }

        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("task", "paired_info");
        intent.putParcelableArrayListExtra("mugs_list", semParametersToCheck);
        startService(intent);

        boolean res = bindService(new Intent(this, SyncService.class), mConnection, 0);

        boolean mc = false;
        if(mConnection != null) mc = true;
        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("WCF: Bind: %B %B", res, mc), true);

    }

    @Override
    protected  void onStop() {
        super.onStop();
        MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onStop:", true);

        requestToTerminate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        if (mConnection != null) {
            if(syncServiceIsBound) {
                unbindService(mConnection);
                syncServiceIsBound = false;
                MLogger.logToFile(this, "service.txt", "WCF: onDestroy: Unbounding service", true);
            }
            else {
                MLogger.logToFile(this, "service.txt", "WCF: onDestroy: Unbounding service - service isn't bound", true);
            }
        }
    }

    //---- Fragment-Service communications --------------------------------------------------------

    final Messenger mMessenger = new Messenger(new SyncServiceIncomingHandler());
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.

            String mac = SyncService.WIDGET_CFG;

            syncServiceMessenger = new Messenger(service);
            syncServiceIsBound = true;
            try {
                Message msg = Message.obtain(null,
                        SyncService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                msg.obj = mac;
                syncServiceMessenger.send(msg);
                MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onServiceConnected: Registering client for " + mac, true);

                msg = Message.obtain(null,
                        SyncService.MSG_GET_PAIRED_DEVICE_INFO);
                msg.replyTo = mMessenger;
                msg.obj = mac;
                syncServiceMessenger.send(msg);
                MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onServiceConnected: Registering client for " + mac, true);

                MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onServiceConnected: Service connected", true);

            } catch (RemoteException e) {
                MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onServiceConnected: !E! " + e.toString(), true);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            syncServiceMessenger = null;
            syncServiceIsBound = false;
            MLogger.logToFile(getApplicationContext(), "service.txt", "WCF: onServiceDisConnected", true);
        }
    };

    class SyncServiceIncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SyncService.MSG_UPDATE_BT_DEVICE:
                    if(msg.obj != null) {
                        String name = ((String[]) msg.obj)[0];
                        String mac = ((String[]) msg.obj)[1];
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("WCF: from Service: %s %s", name, mac), true);
                        if( name != null) {
                            if(!checkBTDeviceInTheList(mac) ) {
                                //infoList.add(new BTDevicesInfo( mac, name, BTDeviceListAdapter.TYPE_DISCOVERED));
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case SyncService.MSG_PAIRED_DEVICE_INFO:
                    if(msg.arg1 == 1) { // process is done
                        pb_progress.setIndeterminate(false);
                        pb_progress.setProgress(100);
                    }
                    if(msg.obj != null) {
                        SolarEnergyMeterParameters mp = (SolarEnergyMeterParameters) msg.obj;
                        if(mp != null) {
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("WCF: from Service: paired info %s %s S%d", mp.getMugName(), mp.getMACAddress(), semParametersToShow.size()), true);
                            semParametersToShow.add(mp);
                            listAdapter.notifyDataSetChanged();
                        }
                    }

                    break;
                case SyncService.MSG_NOT_PAIRED_DEVICE_INFO:

                    if(msg.arg1 == 1) { // process is done
                        pb_progress.setIndeterminate(false);
                        pb_progress.setProgress(100);
                    }
                    if(msg.obj != null) {
                        SolarEnergyMeterParameters mp = (SolarEnergyMeterParameters) msg.obj;
                        if(mp != null) {

                            MLogger.logToFile(  getApplicationContext(),
                                                "service.txt",
                                                String.format(  Locale.getDefault(),
                                                                "WCF: from Service: discovered info %s S%d",
                                                                mp.getMACAddress(), semParametersToShow.size()), true);

                            if(!checkBTDeviceInTheList(mp.getMACAddress()) ) {
                                semParametersToShow.add(mp);
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case SyncService.MSG_NEW_PAIRED_DEVICE_INFO:

                    if(msg.obj != null) {
                        SolarEnergyMeterParameters mp = (SolarEnergyMeterParameters) msg.obj;
                        if(mp != null) {

                            MLogger.logToFile(  getApplicationContext(),
                                    "service.txt",
                                    String.format(Locale.getDefault(),  "WCF: from Service: bonded %s S%d",
                                            mp.getMACAddress(), semParametersToShow.size()), true);

                            AppPreferences.save(AppWidgetConfigureActivity.this, String.format(Locale.getDefault(), "widget_%d", mAppWidgetId), "mac", mp.getMACAddress() );

                            // It is the responsibility of the configuration activity to update the app widget
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                            AppWidget.updateAppWidget(getApplicationContext(), appWidgetManager, mAppWidgetId);

                            // Make sure we pass back the original appWidgetId
                            Intent resultValue = new Intent();
                            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                            setResult(RESULT_OK, resultValue);
                            finish();


                        }
                    }

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    void requestToTerminate() {
        if (syncServiceIsBound) {
            if(syncServiceMessenger != null) {

                String mac = SyncService.WIDGET_CFG;

                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_STOP_PAIRED_DEVICE_INFO);
                    msg.replyTo = mMessenger;
                    msg.obj = mac;
                    syncServiceMessenger.send(msg);

                    MLogger.logToFile(this, "service.txt", "WCF: onStop: Terminate getting devices info", true);

                } catch(RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                    MLogger.logToFile(this, "service.txt", "WCF: onStop: Terminate getting devices info " + e.toString(), true);
                }

                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    msg.obj = mac;
                    syncServiceMessenger.send(msg);

                    MLogger.logToFile(this, "service.txt", "WCF: onStop: Unregistring client " + mac, true);

                } catch(RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                    MLogger.logToFile(this, "service.txt", "WCF: onStop: Unregistring client " + e.toString(), true);
                }
            }

            if (mConnection != null) {
                unbindService(mConnection);
                syncServiceIsBound = false;
                MLogger.logToFile(this, "service.txt", "WCF: onStop: Unbounding service", true);
            }
        }
    }

    void requestToBond(String mac) {
        if (syncServiceIsBound) {
            if(syncServiceMessenger != null) {

                // String mac = SyncService.WIDGET_CFG;

                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_REQUEST_TO_BOND);
                    msg.replyTo = mMessenger;
                    msg.obj = mac;
                    syncServiceMessenger.send(msg);

                    MLogger.logToFile(this, "service.txt", "WCF: Request to bond", true);

                } catch(RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                    MLogger.logToFile(this, "service.txt", "WCF: Request to bond " + e.toString(), true);
                }

            }
        }
    }


    //---- End of Fragment-Service communications -------------------------------------------------

    boolean checkBTDeviceInTheList(String mac) {
        for(SolarEnergyMeterParameters mp : semParametersToShow) {
            //MLogger.logToFile(getApplicationContext(), "service.txt", String.format("WCF: %s ? %s", mac, info.getMac()), true);
            if( mac.equals( mp.getMACAddress()) ) {
                //MLogger.logToFile(getApplicationContext(), "service.txt", String.format("WCF: %s = %s", mac, info.getMac()), true);
                return true;
            }
        }
        return false;
    }
}