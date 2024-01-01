package com.hels.elements;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class SyncService extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    static final int MSG_SAY_HELLO = 1;
    static final int MSG_REGISTER_CLIENT = 2;
    static final int MSG_UNREGISTER_CLIENT = 3;
    static final int MSG_SET_VALUE = 4;
    static final int MSG_ENABLE_DISABLE = 5;
    static final int MSG_REFRESH_PARAMETERS = 6;
    static final int MSG_REREAD_PARAMETERS = 7;
    static final int MSG_UPDATE_BT_DEVICE = 8;
    static final int MSG_SET_PARAMETERS = 9;
    static final int MSG_SET_PARAMETERS_DONE = 91;


    static final int MSG_GET_PAIRED_DEVICE_INFO = 10;
    static final int MSG_PAIRED_DEVICE_INFO = 11;
    static final int MSG_NOT_PAIRED_DEVICE_INFO = 12;
    static final int MSG_STOP_PAIRED_DEVICE_INFO = 13;
    static final int MSG_NEW_PAIRED_DEVICE_INFO = 14;

    static final int MSG_REQUEST_TO_BOND = 14;

    public final static String WIDGET_CFG = "WCFG";

    int mValue = 0;
    final Messenger mMessenger = new Messenger(new IncomingHandler());


    class ThreadInfo {
        private SyncThread st = null;
        private Thread th = null;
        private String mac = null;
        private int widgetId = 0;
        private Messenger messenger = null;

        public ThreadInfo(SyncThread st, Thread th, String mac, int widgetId) {
            this.st = st;
            this.th = th;
            this.mac = mac;
            this.widgetId = widgetId;
        }

        public void addMessenger(Messenger m) {
            this.messenger = m;
        }
        public void removeMessenger() {
            this.messenger = null;
        }

    }

    ArrayList <ThreadInfo> threads;


    public SyncService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onCreate", true);

        threads = new ArrayList<>();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart", true);

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AntiEmber Foreground Service")
                .setContentText("ABC")
                .setSmallIcon(R.drawable.cup_10_128x128_neonblue)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //do heavy work on a background thread

        if(intent != null ) {

            String task = intent.getStringExtra("task");
            String mac = intent.getStringExtra("mac");
            int widgetID = intent.getIntExtra("widgetID", 0);

            if(task != null) {
                switch(task) {
                    case "on_boot":
                    case "on_app_start":
                    case "on_main_fragment":
                        if( mac == null) {
                            MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: intent is NOT NULL, but mac is NULL", true);
                            // check all widget's one by one for enabled Thread
                            int[] widgetIDs = getWidgetsList();
                            if((widgetIDs != null) &&  (widgetIDs.length != 0)) {           // there're some widgets
                                for(int i = 0; i < widgetIDs.length; i++) {
                                    mac =AppPreferences.readString(getApplicationContext(),  String.format("widget_%d", widgetIDs[i]), "mac");
                                    if(mac != null) {

                                        Boolean mugIsEnabled = AppPreferences.readBoolean( this.getApplicationContext(),  String.format("mug_%s", mac), "enabled");
                                        if(mugIsEnabled == null) mugIsEnabled = false;
                                        if(mugIsEnabled) {
                                            String threadName = String.format("*%s_%s", getPackageName(), mac);
                                            if(!isThreadRunning(getApplicationContext(), threadName)) {
                                                SyncThread st = new SyncThread(getApplicationContext(), mac, widgetIDs[i], SyncThread.TASK_MUG_THREAD);
                                                Thread th = new Thread(st, threadName);
                                                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread REstOring:" + threadName, true);

                                                //threads.add(new ThreadInfo(st, th, mac, i));
                                                addThread(new ThreadInfo(st, th, mac, i));
                                                th.start();

                                            }
                                            else {
                                                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread is already running: " + threadName, true);
                                            }
                                        }
                                        else {
                                            MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread isn't enabled: " + mac, true);
                                        }
                                    }
                                }
                            }
                            else {  // app doesn't have any widgets yet
                                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: no widgets yet", true);
                            }
                        }
                        break;
                    case "on_mug_enabled":
                        if(mac != null)  {  // mac != null - need to start Thread for the certain Widget
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format(Locale.getDefault(), "SRV: onStart: intent is NOT NULL, mac is %s. Widget %d", mac, widgetID), true);
                            String threadName = String.format("*%s_%s", getPackageName(), mac);
                            if(!isThreadRunning(getApplicationContext(), threadName)) {
                                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread starting " + mac, true);
                                SyncThread st = new SyncThread(getApplicationContext(), mac, widgetID, SyncThread.TASK_MUG_THREAD);
                                Thread th = new Thread(st, threadName);

                              // threads.add(new ThreadInfo(st, th, mac, widgetID));
                                addThread(new ThreadInfo(st, th, mac, widgetID));

                                th.start();

                            } else {
                                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread is already running " + mac, true);
                            }
                        }
                    break;
                    case "paired_info":
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format(Locale.getDefault(), "SRV: onStart: getting info for paired list"), true);
                        String threadName = String.format("*%s_paired", getPackageName());

                        //String[] macList = intent.getStringArrayExtra("mac_list");
                        ArrayList<MugParameters> mugsList = intent.getExtras().getParcelableArrayList("mugs_list");

                        SyncThread st = new SyncThread(getApplicationContext(), mugsList, SyncThread.TASK_GET_PAIRED_INFO);

                        Thread th = new Thread(st, threadName);

                        threads.add(new ThreadInfo(st, th, SyncService.WIDGET_CFG, 0));
                        //addThread(new ThreadInfo(st, th, SyncService.WIDGET_CFG, 0));

                        th.start();

                        break;
                    case "idle":
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format(Locale.getDefault(), "SRV: onStart: idle"), true);
                        break;
                }
            }




        }
        else {
            MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Intent is NULL. DO WE NEED PROCESS THIS?!", true);
//            if(!threads.isEmpty()) {
//                threads.forEach((k)->{
//                    String threadName = String.format("*%s_%s", getPackageName(), k.mac);
//                    if(!isThreadRunning( getApplicationContext(), threadName ) ) {
//                        SyncThread st = new SyncThread(getApplicationContext(), k.mac);
//                        Thread th = new Thread(st, threadName);
//                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onStart: Thread REstarting " + threadName, true);
//
//                        //threads.add(new ThreadInfo(st, th, mac));
//
//                        th.start();
//                    }
//                });
//            }

        }

        // Debug: show known threads
        /*
        Set<Thread> thSet = Thread.getAllStackTraces().keySet();
        String threadNameHeader = String.format("*%s_", getPackageName());
        for (Thread x : thSet) {

            if( x.getName().length() >= threadNameHeader.length() ) {
                if( x.getName().substring(0, threadNameHeader.length()).equals(threadNameHeader) ) {
                    MLogger.logToFile(getApplicationContext(), "service.txt", String.format( Locale.getDefault(), "SRV: onStart: % is running #%d",x.getName(), x.getId()), true);
                }
            }
            //addLogLine( String.format("%s #%d", x.getName(), x.getId()) );
        }*/

        if(!threads.isEmpty()) {
            for(int i = 0; i < threads.size(); i++) {
                MLogger.logToFile(getApplicationContext(), "service.txt", String.format(Locale.getDefault(), "SRV: [%d] : %s %s", i, threads.get(i).mac, threads.get(i).th.getState().toString()), true);
            }
        }
        else {
            //Log.d("SERVICE", "No threads");
            MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: No threads", true);
        }


        //stopSelf();
        //return START_NOT_STICKY;
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Toast.makeText(this, "Notification Service destroyed by user.", Toast.LENGTH_LONG).show();
        //Log.d("SERVICE", "DESTROYED");
        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onDestroy", true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        //Log.d("SERVICE", "onBind");
        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: onBind", true);
        //mMessenger = new Messenger(new IncomingHandler(this));
        return mMessenger.getBinder();
    }

    private void createNotificationChannel() {

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

    }

    class IncomingHandler extends Handler {
        private Context applicationContext;

//        IncomingHandler(Context context) {
//            applicationContext = context.getApplicationContext();
//        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    //Log.d("SERVICE", "Service received message");
                    Toast.makeText(applicationContext, "hello!", Toast.LENGTH_SHORT).show();
                    MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Service received message", true);
                    break;
                case MSG_REGISTER_CLIENT:
                    if( msg.obj != null) {
                        String mac = (String)msg.obj;
                        ThreadInfo ti = getThreadInfo(mac);
                        ti.addMessenger(msg.replyTo);                   // Exception!
                        ti.st.updateMsgClient(msg.replyTo);

//                        mClients.add(msg.replyTo);
//                        threads.forEach((k) -> k.st.updateClients(mClients));

                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Client registered " + mac, true);
                    }
                    else {
                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Client registering. MAC is null!", true);
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    if( msg.obj != null) {
                        String mac = (String)msg.obj;
                        ThreadInfo ti = getThreadInfo(mac);
                        if(ti != null) {
                            ti.removeMessenger();
                            ti.st.removeMsgClient();
                        }
                        else
                            MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Client unregistering. ThreadInfo is null!", true);

                        //mClients.remove(msg.replyTo);
                        //threads.forEach((k) -> k.st.updateClients(mClients));

                        //Log.d("SERVICE", "Client unregistered");
                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Client unregistered " + mac, true);
                    }
                    else {
                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Client unregistering. MAC is null!", true);
                    }
                    break;
                case MSG_SET_VALUE:

                    mValue = msg.arg1;
                    if(msg.obj != null) {
                        String mac = (String)msg.obj;
                        if( mac != null) {
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: set value '%d' to Service from %s", mValue, mac), true);
                            ThreadInfo ti = getThreadInfo(mac);
                            if((ti != null) && (ti.messenger != null) ) {
                                try {

                                    ti.messenger.send(Message.obtain(null,
                                            MSG_SET_VALUE, 99, 0));
                                } catch(RemoteException e) {
                                    // The client is dead.  Remove it from the list;
                                    // we are going through the list from back to front
                                    // so this is safe to do inside the loop.
                                    //mClients.remove(i);
                                    ti.removeMessenger();
                                    MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: return value from Service, client is dead %s ",  mac), true);
                                }
                            }
                            /*
                            if(threads != null) {
                                int idx = -1;
                                for(int k = 0; k < threads.size(); k++) {
                                    if(((String) msg.obj).equals(threads.get(k).mac)) {
                                        idx = k;
                                        break;
                                    }
                                }
                                // before sending - check if Thread is still running.
                                for(int i = mClients.size() - 1; i >= 0; i--) {
                                    try {
                                        mClients.get(i).send(Message.obtain(null,
                                                MSG_SET_VALUE, threads.get(idx).st.getTempCurrent(), 0));

                                    } catch(RemoteException e) {
                                        // The client is dead.  Remove it from the list;
                                        // we are going through the list from back to front
                                        // so this is safe to do inside the loop.
                                        mClients.remove(i);
                                    }
                                }
                            }*/
                        }
                        else {
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: set value '%d' to Service from NULL", mValue), true);
                        }
                    }

                    break;
                case MSG_REREAD_PARAMETERS:
                    if(msg.obj != null) {
                        String mac = (String)msg.obj;
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Request to ReRead %s", mac), true);
                        ThreadInfo ti = getThreadInfo(mac);
                        ti.st.setRequestToReRead();
                    }
                    break;
                case MSG_SET_PARAMETERS:
                    if(msg.obj != null) {
                        MugParameters mp = (MugParameters)msg.obj;
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Request to set parameters %s", mp.getMACAddress()), true);
                        ThreadInfo ti = getThreadInfo(mp.getMACAddress());
                        ti.st.setRequestToSetParameters(mp);
                    }
                    break;
                case MSG_ENABLE_DISABLE:

                        if(msg.obj != null) {
                            String mac = (String)msg.obj;
                            if( msg.arg1 == 0 ) {   //disable thread
                                MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Request to disable %s", mac), true);
                                ThreadInfo ti = getThreadInfo(mac);
                                ti.st.setStop();
                                removeThread(mac);
                            }
                        }
                    break;
//                case MSG_TERMINATE_DISCOVERING:
//                    if( msg.obj != null) {
//                        String mac = (String) msg.obj;
//
//                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Request to terminate discovering" + mac, true);
//
//                        ThreadInfo ti = getThreadInfo(mac);
//                        ti.th.interrupt();
//                        removeThread(mac);
//                    }
//                    break;

                case MSG_GET_PAIRED_DEVICE_INFO:
                    if(msg.obj != null) {
                        String mac = (String)msg.obj;
                        if( msg.arg1 == 0 ) {   //disable thread
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Request to start read paired parameters"), true);

                            ThreadInfo ti = getThreadInfo(mac);
                            ti.st.setStartTask();
                        }
                    }
                    break;
                case MSG_STOP_PAIRED_DEVICE_INFO:
                    if( msg.obj != null) {
                        String mac = (String) msg.obj;

                        MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: Request to stop getting paired info", true);

                        ThreadInfo ti = getThreadInfo(mac);
                        ti.th.interrupt();
                        removeThread(mac);
                    }
                    break;

                case MSG_REQUEST_TO_BOND:
                    if(msg.obj != null) {
                        String mac = (String)msg.obj;
                        if( msg.arg1 == 0 ) {   //disable thread
                            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Request to bond %s", mac), true);

                            ThreadInfo ti = getThreadInfo(WIDGET_CFG);
                            ti.st.setRequestToBond(mac);
                        }
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public static boolean isThreadRunning( Context context, String threadName) {
        Set<Thread> thSet = Thread.getAllStackTraces().keySet();
        for(Thread x : thSet) {
            //System.out.println(x.getName());
            //addLogLine(String.format("%s #%d", x.getName(), x.getId()));
            if( x.getName().equals(threadName) ) {
                MLogger.logToFile(context, "service.txt", "SRV: onStart: Thread is already running " + threadName, true);
                return true;
            }
        }
        return false;
    }

    int[] getWidgetsList() {
        Context appContext = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName thisWidget = new ComponentName(appContext, AppWidget.class);

        int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        return  widgetIds;
    }

    ThreadInfo getThreadInfo(String mac) {
        if(!threads.isEmpty()) {
            for(int i = 0; i < threads.size(); i++) {
                if( mac.equals( threads.get(i).mac ) ) return threads.get(i);
            };
        }
        return null;
    }

    void removeThread(String mac) {

        if(!threads.isEmpty()) {
            MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Removing thread %s",mac), true);
            for(int i = 0; i < threads.size(); i++) {
                if( mac.equals( threads.get(i).mac ) ) { threads.remove(i); break; }
            };

            if(!threads.isEmpty()) {
                for(int i = 0; i < threads.size(); i++) {
                    //String ss = String.format("SRV: #%d : %s", i, threads.get(i).name);
                    //Log.d("SERVICE", ss);
                    MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: [%d] : %s", i, threads.get(i).mac), true);
                }
            }
            else {
                //Log.d("SERVICE", "No threads");
                MLogger.logToFile(getApplicationContext(), "service.txt", "SRV: No threads", true);
            }

        }
    }

    void addThread(ThreadInfo t) {
        if(threads.size() > 0 ) {
            for(int i = 0; i < threads.size(); i++) {
//                Boolean m = true;
//                if( threads.get(i).messenger == null) m = false;
                Boolean a = false;
                if( threads.get(i).th.isAlive() ) a = true;

                if(threads.get(i).mac.equals(t.mac)) {
                    if(a == false) {
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Checking thread %s Alive: FALSE -> REMOVED", threads.get(i).mac), true);
                        threads.remove(i);
                    } else {
                        MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Checking thread %s Alive: TRUE !!!! Rewriting???!", threads.get(i).mac, a), true);
                    }
                }
                else {
                    MLogger.logToFile(getApplicationContext(), "service.txt", String.format("SRV: Checking thread %s Alive: %B", threads.get(i).mac, a), true);
                }
            }
        }
        threads.add(t);
    }

}