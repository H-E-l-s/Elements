package com.hels.elements;

import static com.hels.elements.ShowAlertDialog.showFinalAlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    final int PERM_REQ_MULTIPLE = 11;
    Context appContext = null;
    MLogger logger;

    public static final int HM_ID_USER_MSG_INFO         = 1;
    public static final int HM_ID_USER_MSG_WARN         = 2;
    public static final int HM_ID_USER_MSG_ERR          = 3;
    public static final int HM_ID_USER_MSG_DONE         = 4;
    public static final int HM_ID_USER_MSG_QUIT         = 5;
    public static final int HM_ID_UPD_PROGRESS          = 9;
    public static final int HM_ID_USER_MSG_CANCELLED    = 10;
    public static final int HM_ID_USER_MSG_TO_LOG       = 11;
    public static final int HM_ID_UPD_STATUS            = 12;


    Messenger mService = null;
    boolean isBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        View v = findViewById(android.R.id.content).getRootView();
        logger = new MLogger(getApplicationContext(), v, "log.txt");

        checkPermissions();

    //    if (!Settings.canDrawOverlays(getApplicationContext())) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }

//        TextView tv_widgetId = findViewById(R.id.tv_id33);
//        tv_widgetId.setText(String.format("#%d", widgetId));

        Button button = (Button)v.findViewById(R.id.btn_getWorkers);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        //        Log.d("BUTTONS", "User tapped the Supabutton");
                //WorkManager.getInstance(MainActivity.this).get
                //WorkManager.getInstance(MainActivity.this).get
                addLogLine("Checking 1...");
                ListenableFuture<List<WorkInfo>> info = WorkManager.getInstance(MainActivity.this).getWorkInfosByTag("mug");
                try {
                    List<WorkInfo> infoList = info.get();
                    for(int i = 0; i < infoList.size(); i++) {
                        WorkInfo wi = infoList.get(i);
                        addLogLine(String.format( "%d %s", wi.getRunAttemptCount(), wi.getState().toString()) );
                        Set<String> st = wi.getTags();
                        String a[] = st.toArray(new String[st.size()]);
                        for(int k = 0; k < a.length; k++) {
                            addLogLine(a[k]);

                           // Operation op = WorkManager.getInstance(MainActivity.this).cancelAllWorkByTag(a[k]);


                        }
                    }
                } catch(ExecutionException e) {
                    throw new RuntimeException(e);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                addLogLine("Done");
                addLogLine("Checking 2...");
                info = WorkManager.getInstance(MainActivity.this).getWorkInfosByTag("com.hels.elements.WidgetsUpdateWorker");
                try {
                    List<WorkInfo> infoList = info.get();
                    for(int i = 0; i < infoList.size(); i++) {
                        WorkInfo wi = infoList.get(i);
                        addLogLine(String.format( "%d %s", wi.getRunAttemptCount(), wi.getState().toString()) );
                        Set<String> st = wi.getTags();
                        String a[] = st.toArray(new String[st.size()]);
                        for(int k = 0; k < a.length; k++) {
                            addLogLine(a[k]);

                          //  Operation op = WorkManager.getInstance(MainActivity.this).cancelAllWorkByTag(a[k]);


                        }
                    }
                } catch(ExecutionException e) {
                    throw new RuntimeException(e);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //----
                addLogLine("Threads");
                Set<Thread> thSet = Thread.getAllStackTraces().keySet();
                for (Thread x : thSet) {
                    //System.out.println(x.getName());
                    addLogLine( String.format("%s #%d", x.getName(), x.getId()) );
                }
                //----
                addLogLine("Done");
            }
        });


        final TextView text = findViewById(R.id.tv_serviceButton);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (isMyServiceRunning(SyncService.class)) {
//                    text.setText("Stoped");
//                    //stopService(new Intent(MainActivity.this, SyncService.class));
//                } else {
//                  text.setText("Started");
//                }

                text.setText("Started");
                //startService(new Intent(MainActivity.this, SyncService.class));
                Intent intent = new Intent(MainActivity.this, SyncService.class);
                //intent.putExtra("mac", "BB:BB:BB:BB:BB:BB");
                intent.putExtra("task", "on_app_start");
                startService(intent);

                //}
            }
        });

        Button btn_sync = (Button)v.findViewById(R.id.btn_sync);
        btn_sync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                addLogLine("Syncing...");
                sayHello(v);
            }
        });
        Button btn_bind = (Button)v.findViewById(R.id.btn_bind);
        btn_bind.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                addLogLine("Binding...");
                Log.d("ACTIVITY", "Binding...");
                bindService(new Intent(appContext, SyncService.class), mConnection,
                        Context.BIND_AUTO_CREATE);

            }
        });

        Button btn_widgetUpdate = (Button)v.findViewById(R.id.btn_widgetUpdate);
        btn_sync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(appContext, AppWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

                int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AppWidget.class));

                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
            }
        });

        //Button btn_widgetUpdate = (Button)v.findViewById(R.id.btn_widgetUpdate);
        ((Button)v.findViewById(R.id.btn_show_services)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getRunningServices();
            }
        });


        File f = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                + File.separator + "logs");
                //+ File.separator + getResources().getString(R.string.default_folder)
                //+ File.separator + getResources().getString(R.string.default_subfolder) );

        if(!f.isDirectory()) {
            if( f.mkdir() == false )
                Toast.makeText(this, "Couldn't create working folder", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, String.format("Working folder: %s", f.getAbsolutePath().toString()), Toast.LENGTH_LONG).show();
        }

        if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                Log.d("ACTIVITY", "Extras is null");
                showFragment(new MainFragment(), -1);
            } else {
                int widgetId = extras.getInt("WidgetID");
                String s = String.format("widget id %d", widgetId);
                Log.d("ACTIVITY", s);
                //String action = extras.getString("Action");
                String action = getIntent().getAction();
                Log.d("ACTIVITY", "Action :" + action);
                if("mug".equals(action)) {
                    Log.d("ACTIVITY", "Mug fragment");
                    showFragment(new MugFragment(), widgetId);
                }
                else {
                    Log.d("ACTIVITY", "Widget fragment");
                    showFragment(new WidgetConfigFragment(), widgetId);
                }
            }
        }

        forceWidgetUpdate(appContext);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service.
//        bindService(new Intent(this, SyncService.class), mConnection,
//                Context.BIND_AUTO_CREATE);

        Log.d("ACTIVITY", "On start - NO bind");

        //showAlertDialog( this, "AAAAA", "BBBB");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service.
        if (isBound) {

            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

         //   unbindService(mConnection);
            isBound = false;
            Log.d("ACTIVITY", "On stop - unbind");
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void getRunningServices() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);

        String s = String.format("Services: %d", list.size());
        Log.d("Activity", s);
        MLogger.logToFile(appContext, "service.txt", s, true);
        logger.addLogLine(s, MLogger.LOG_BOX);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d("Activity", service.service.getClassName());
            MLogger.logToFile(appContext, "service.txt", service.service.getClassName(), true);
            logger.addLogLine(service.service.getClassName(), MLogger.LOG_BOX);

            //if (serviceClass.getName().equals(service.service.getClassName())) {
            //    return true;
            //}
        }
        //return false;
    }


    private boolean checkPermissions() {

        ArrayList<String> permissionsToAsk = new ArrayList<>();

        ArrayList<String> necessaryPermissions = new ArrayList<>();
        //necessaryPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        //necessaryPermissions.add(Manifest.permission.READ_PHONE_STATE);
        //necessaryPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //necessaryPermissions.add(Manifest.permission.INTERNET);
        necessaryPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        necessaryPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        necessaryPermissions.add(Manifest.permission.BLUETOOTH);



        for(int i = 0; i < necessaryPermissions.size(); i++) {
            if(ContextCompat.checkSelfPermission(MainActivity.this,
                    necessaryPermissions.get(i)) != PackageManager.PERMISSION_GRANTED) {

                permissionsToAsk.add(necessaryPermissions.get(i));
            }
        }

        if(permissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    permissionsToAsk.toArray(new String[permissionsToAsk.size()]), PERM_REQ_MULTIPLE);
        }

        if(permissionsToAsk.size() > 0 ) return false;
        else return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int denied = 0;
        switch(requestCode) {
            case PERM_REQ_MULTIPLE: {
                //Logger.logToFile(getApplicationContext(), "perms.txt", String.format("num: %d", grantResults.length));
                if(grantResults.length > 0) {
                    String permName = "";
                    for(int i = 0; i < grantResults.length; i++) {

                        if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            //Logger.logToFile(getApplicationContext(), "perms.txt", String.format("%s denied", permissions[i]));
                            try {
                                permName += "\r\n" + permissions[i].substring(permissions[i].lastIndexOf(".") + 1).replace("_", " ");
                            } catch(Exception e) {
                            }
                            denied++;
                        }
                        //else Logger.logToFile(getApplicationContext(), "perms.txt", String.format("%s allowed", permissions[i]));
                    }
                    if(denied == 1)
                        showFinalAlertDialog(this, "Permission denied", String.format("Please allow permission:\r\n%s", permName));
                    else if(denied > 1)
                        showFinalAlertDialog(this, "Permissions denied", String.format("Please allow  permissions:\r\n%s", permName));
                }
            }
        }
    }


    public void addLogLine(String s) {
        logger.addLogLine(s, MLogger.LOG_BOX);
    }

    public Handler userMessengerHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {

            switch(msg.what) {
//                case HM_ID_USER_MSG_QUIT:
//                    updateProgress(0);
//                    restoreControls();
//                    if( !((String)msg.obj).equals("") )
//                        setUserMessage( (String) msg.obj, UM_ID_DONE );
//                    break;
//                case HM_ID_USER_MSG_DONE:
//                    setUserMessage( (String) msg.obj, UM_ID_DONE );
//                    break;
//                case HM_ID_USER_MSG_INFO:
//                    setUserMessage( (String) msg.obj, UM_ID_INFO );
//                    break;
//                case HM_ID_USER_MSG_WARN:
//                    setUserMessage( (String) msg.obj, UM_ID_WARN );
//                    break;
//                case HM_ID_USER_MSG_ERR:
//                    setUserMessage( (String) msg.obj, UM_ID_ERR );
//                    updateProgress(PROGRESS_INDETERMINATE_DIS);
//                    restoreControls();
//                    break;
//                case HM_ID_USER_MSG_CANCELLED:
//                    setUserMessage( (String) msg.obj, UM_ID_WARN );
//                    updateProgress(0);
//                    break;
//                case HM_ID_UPD_PROGRESS:
//                    try {
//                        setUserMessage( ((ProgressInfo)msg.obj).getInfo(), UM_ID_INFO );
//                        updateProgress((int)( (ProgressInfo)msg.obj).getProgress() ) ;
//                    }
//                    catch(Exception e) {
//                        String s = e.toString();
//                    }
//                    break;
                case HM_ID_USER_MSG_TO_LOG:
                    logger.addLogLine((String) msg.obj, MLogger.LOG_BOX);
                    break;
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            isBound = true;
            try {
                Message msg = Message.obtain(null,
                        SyncService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null,
                        SyncService.MSG_SET_VALUE, 33, 0);
                mService.send(msg);
                logger.addLogLine(  String.format("Set %d", SyncService.MSG_SET_VALUE), MLogger.LOG_BOX);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            Log.d("ACTIVITY", "Service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected&mdash;that is, its process crashed.
            mService = null;
            isBound = false;
            Log.d("ACTIVITY", "Service disconnected");
        }
    };

    public void sayHello(View v) {
        if (!isBound) return;
        // Create and send a message to the service, using a supported 'what' value.
        //Message msg = Message.obtain(null, SyncService.MSG_SAY_HELLO, 0, 0);
        Message msg = Message.obtain(null,
                SyncService.MSG_SET_VALUE, 33, 0);
        try {
            Log.d("ACTIVITY", "Sending request");
            mService.send(msg);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //---- Service to activity communication ------------------------------------------------------
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SyncService.MSG_SET_VALUE:
                    //mCallbackText.setText("Received from service: " + msg.arg1);
                    logger.addLogLine(  String.format("got %d", msg.arg1), MLogger.LOG_BOX);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    //final Messenger mMessenger = new Messenger(new IncomingHandler());

    //---------------------------------------------------------------------------------------------

    //---- Fragments management -------------------------------------------------------------------
    private void showFragment(Fragment fragment, int widgetId) {

        Bundle bundle = new Bundle();
        bundle.putInt("WidgetID", widgetId);
        fragment.setArguments(bundle);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }
    //---------------------------------------------------------------------------------------------

    public static void forceWidgetUpdate(Context context) {
        // update receiver widgets
        MLogger.logToFile(context, "service.txt", String.format("MA : Forced Widget update"), true);

        Intent intent = new Intent(context, AppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(context.getApplicationContext())
                .getAppWidgetIds(new ComponentName(context.getApplicationContext(), AppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}