package com.hels.elements;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SyncThread implements Runnable {

    public static final int TASK_NO_TASK = 99;
    public static final int TASK_DISCOVER = 1;
    public static final int TASK_GET_PAIRED_INFO = 2;
    public static final int TASK_MUG_THREAD = 3;

    private final int STEP_IDLE = 0;
    private final int STEP_START_BLE_SCAN = 50;
    private final int STEP_BLE_SCAN_DONE = 51;
    private final int STEP_BLE_GATT_CONNECT = 52;
    private final int STEP_BLE_READ_CHAR = 53;
    private final int STEP_BLE_WRITE_CHAR = 54;
    private final int STEP_QUIT = 13;
    private final int STEP_STANDBY = 14;
    private final int STEP_WAIT_BT = 15;        // BT isn't enabled

    private final int STEP_BT_START_DISCOVERY = 20;
    private final int STEP_BT_DISCOVERING = 21;
    private final int STEP_BT_STOP_DISCOVERY = 22;
    private final int STEP_BT_DISCOVERING_DONE = 23;
    private final int STEP_BT_WAITING_FOR_BOND = 24;

    private final int STEP_PAIRED_INFO = 97;
    private final int STEP_WAITING_FOR_TASK = 98;
    //private final int STEP_TEST = 99;


    private final int WTS_CURRENT_TEMP = 0x01;
    private final int WTS_TARGET_TEMP = 0x02;
    private final int WTS_BATTERY = 0x04;
    private final int WTS_LIQUID_STATE = 0x08;
    private final int WTS_MUG_COLOR = 0x10;
    private final int WTS_MUG_NAME = 0x20;

    private final int WTS_SET_NOTIFICATION = 0x80;

    private final int DISCOVERING_NEW       = 0x01;
    private final int DISCOVERING_DONE      = 0x02;
    private final int DISCOVERING_PAIRED    = 0x03;


    private Boolean isRunning = false;
    private Boolean onTheGo = false;
    private Boolean stop = false;
    private int[] processSteps;
    private int step;
    private int psIdx;

    Context appContext;
    Messenger msgClient = null;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothDevice mugBleDevice;
    BluetoothGatt mugGatt;
    private List<BluetoothGattCharacteristic> ReadQueue;

    private volatile boolean requestToReRead = false;
    private volatile boolean requestToSet = false;
    private volatile boolean requestToBond = false;
    private volatile boolean isBonded = false;

    private volatile boolean needsToSync = false;
    private int whatToSync = 0;

    private String mugAddress = "F1:33:A8:30:97:BF";
    private String macToBond = "";

    private boolean mugFound = false;
    private boolean mugGattConnected = false;
    private boolean mugGattDiscovered = false;
    private boolean mugGattCharsRead = false;
    private boolean mugGattCharWritten = false;
    private boolean mugGattDescriptorWritten = false;

    private boolean mugTargetTempSet = false;
    private boolean mugColorSet = false;
    private boolean mugNameSet = false;

    private boolean btIsEnabled = false;

    long tsLongLast = 0;
    long tsWaitBTLastTime = 0;

    int widgetId = 0;
    int task;

    ArrayList<MugParameters> mugsList;
    int mugsListIdx = 0;

    MugParameters mugParametersToSet = null;

//    class Characteristic {
//        private Integer id, index;
//        private String name;
//        private String uuid;
//        public Characteristic(Integer id, Integer index, String name, String uuid) {
//            this.id = id;
//            this.index = index;
//            this.name = name;
//        }
//
//        public Integer getId() { return  id; }
//        public Integer getIndex() { return  index; }
//        public String getName() { return name; }
//    }
//
//    ArrayList<Characteristic> deviceCharacteristics = new ArrayList<Characteristic>();

    public Boolean isRunning() {
        return isRunning;
    }

    public void setRequestToReRead() {
        requestToReRead = true;
    }

    public void setRequestToSetParameters(MugParameters mugParameters) {
        mugParametersToSet = mugParameters;
        requestToSet = true;
    }
    public void setRequestToBond( String mac) {
        macToBond = mac;
        requestToBond = true;
    }

    private boolean startTask = false;

    public void setStartTask() {
        startTask = true;
    }

    public SyncThread(Context context, String macAddress, int widgetId, int task) {

        mugsList = new ArrayList<>();
        mugsList.add(new MugParameters(macAddress, null, null, null, null, null, null, MugParameters.TYPE_PAIRED, false));
        mugsListIdx = 0;

        this.appContext = context;

        mugAddress = macAddress;
        this.widgetId = widgetId;
        this.task = task;

        if(btManager == null)
            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if(btAdapter == null) btAdapter = btManager.getAdapter();

        context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public SyncThread(Context context, ArrayList<MugParameters> mugsParameters, int task) {

        this.appContext = context;

        this.mugsList = mugsParameters;
        this.widgetId = 0;
        this.task = task;
        this.startTask = false;

        mugsListIdx = 0;

        if(btManager == null)
            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if(btAdapter == null) btAdapter = btManager.getAdapter();

        context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }


    public void syncInit() {
        processSteps = new int[]{
                //STEP_START_BLE_SCAN,
                //STEP_BLE_SCAN_DONE,
                STEP_BLE_GATT_CONNECT,
                STEP_BLE_READ_CHAR,
                //STEP_QUIT
                STEP_STANDBY,
                STEP_IDLE
        };
        psIdx = 0;
        step = processSteps[psIdx];

        //whatToSync = (WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_LIQUID_STATE | WTS_MUG_COLOR | WTS_MUG_NAME | WTS_SET_NOTIFICATION);
        whatToSync = (WTS_MUG_NAME | WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_MUG_COLOR | WTS_SET_NOTIFICATION);
        needsToSync = true;
    }

    public void setParametersInit() {
        processSteps = new int[]{
                STEP_BLE_GATT_CONNECT,
                STEP_BLE_WRITE_CHAR,
                STEP_STANDBY,
                STEP_IDLE
        };
        psIdx = 0;
        step = processSteps[psIdx];

//        whatToSync = (WTS_MUG_NAME | WTS_CURRENT_TEMP | WTS_BATTERY | WTS_SET_NOTIFICATION);
//        needsToSync = true;
    }


    public void pairedInfoInit() {
        processSteps = new int[]{
                STEP_PAIRED_INFO,
                STEP_BT_START_DISCOVERY,
                STEP_BT_DISCOVERING,
                STEP_BT_STOP_DISCOVERY,
                STEP_BT_WAITING_FOR_BOND
        };
        psIdx = 0;
        step = processSteps[psIdx];

        //whatToSync = (WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_LIQUID_STATE | WTS_MUG_COLOR | WTS_MUG_NAME | WTS_SET_NOTIFICATION);
        whatToSync = (WTS_MUG_NAME | WTS_CURRENT_TEMP | WTS_BATTERY | WTS_SET_NOTIFICATION);
        needsToSync = true;

    }


    public void discoverInit() {
        processSteps = new int[]{
                STEP_BT_START_DISCOVERY,
                STEP_BT_DISCOVERING,
                STEP_BT_STOP_DISCOVERY
        };
        psIdx = 0;
        step = processSteps[psIdx];

        //whatToSync = (WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_LIQUID_STATE | WTS_MUG_COLOR | WTS_MUG_NAME | WTS_SET_NOTIFICATION);
        //whatToSync = (WTS_MUG_NAME | WTS_CURRENT_TEMP | WTS_BATTERY | WTS_SET_NOTIFICATION);
        //needsToSync = true;

    }


    //public void updateClients(ArrayList<Messenger> mClients) {
    //    this.mClients = mClients;
    //}
    public void updateMsgClient(Messenger m) {
        this.msgClient = m;
        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : msg client udated " + mugAddress), true);
    }

    public void removeMsgClient() {
        this.msgClient = null;
    }


    @Override
    public void run() {

        int pauseLength = 300;

        IntentFilter filter;
        EventsTimer timeToConnect = null;
        //EventsTimer timeToReconnect = null;

        //mugParameters = new MugParameters(mugAddress, null,null,null, null, null, false);

        isRunning = true;
        long id = Thread.currentThread().getId();
        //Log.d("THREAD", String.format("Started #%d", id));
        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Started #%d", id), true);

        switch(task) {
            case TASK_DISCOVER:
                discoverInit();
                break;
            case TASK_GET_PAIRED_INFO:
                //pairedInfoInit();
                step = STEP_WAITING_FOR_TASK;
                break;
            case TASK_MUG_THREAD:
                syncInit();
                break;

            case TASK_NO_TASK:
                step = STEP_WAITING_FOR_TASK;
                break;
            default:
                step = STEP_WAITING_FOR_TASK;
        }

        onTheGo = true;
        final int maxAttempts = 5;
        int attempts = maxAttempts;

        for(; ; ) {
            if(onTheGo) {
                switch(step) {
                    //--------------------------------------------------------------------------
                    case STEP_WAITING_FOR_TASK:

                        if(startTask) {
                            startTask = false;
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Task %d has started", task), true);
                            switch(task) {
                                case TASK_GET_PAIRED_INFO:
                                    pairedInfoInit();
                                    break;
                            }
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch(InterruptedException e) {
                                //  throw new RuntimeException(e);
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : getting paired info terminating msgClient"), true);
                                break;
                            }
                        }
                        break;
                    //--------------------------------------------------------------------------
                    case STEP_WAIT_BT:
//                            if(timeToReconnect != null) {
//                                //timeToReconnect.cancel();
//                                timeToReconnect = null;
//                            }
                        if(mugGatt != null) mugGatt = null;
                        try {
                            Thread.sleep(5000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }

                        long timeNow = System.currentTimeMillis() / 1000;
                        if((timeNow - tsWaitBTLastTime) > 15 * 60) {
                            MLogger.logToFile(appContext, "service.txt", "TH : BT is disabled", true);
                            tsWaitBTLastTime = timeNow;
                            updateWidget();
                        }

                        if(btIsEnabled) {
                            tsWaitBTLastTime = 0;
                            syncInit();
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is ON - Sync enabling", true);
                        }

                        break;
                    case STEP_IDLE:

                        if(checkStop()) {
                            step = STEP_QUIT;
                            break;
                        }
                        if(!checkBTEnabled()) {
                            break;
                        }

                        long tsLongNow = System.currentTimeMillis() / 1000;
                        if((tsLongNow - tsLongLast) > 15 * 60) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Alive " + mugBleDevice.getAddress(), true);
                            tsLongLast = tsLongNow;
                        }

                        if(!mugGattConnected) {
                            try {
                                Thread.sleep(3000);
                            } catch(InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }

                        if(requestToSet) {

                            MLogger.logToFile(appContext, "service.txt", "TH : request to set " + mugBleDevice.getAddress(), true);
                            processSteps = new int[]{
                                    STEP_BLE_GATT_CONNECT,
                                    STEP_BLE_WRITE_CHAR,
                                    STEP_STANDBY,
                                    STEP_IDLE
                            };

                            psIdx = 0;
                            step = processSteps[psIdx];
                            requestToSet = false;
                            break;
                        }

                        if(requestToReRead) {
                            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Request to reread %s", id, mugAddress), true);
                            syncInit();
                            requestToReRead = false;
                        }

                        if(needsToSync) {
                            id = Thread.currentThread().getId();
                            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Needs to sync " + mugAddress, id), true);

                            if(ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }

                            List<BluetoothDevice> connectedBLEDevices = btManager.getConnectedDevices(BluetoothProfile.GATT);
//                                String sss = "";
//                                for(int h = 0; h < connectedBLEDevices.size(); h++) {
//                                    sss+= String.format("\n %s", connectedBLEDevices.get(h).getName());
//                                }

//                                MessageToActivity.sendMessageToActivity(msgHandler,
//                                        ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                        sss);

                            if(connectedBLEDevices.contains(mugBleDevice) == false) {
//                                    for(int i = 0; i < processSteps.length; i++) {
//                                        if(processSteps[i] == STEP_BLE_GATT_CONNECT) {
//                                            psIdx = i;
//                                            step = processSteps[psIdx];
//                                            break;
//                                        }
//                                    }
                                MLogger.logToFile(appContext, "service.txt", "TH : Needs to sync - not connected " + mugBleDevice.getAddress(), true);
                                processSteps = new int[]{
                                        STEP_BLE_GATT_CONNECT,
                                        STEP_BLE_READ_CHAR,
                                        STEP_STANDBY,
                                        STEP_IDLE
                                };
                                psIdx = 0;
                                step = processSteps[psIdx];

                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Needs to sync - connected", true);
                                processSteps = new int[]{
                                        STEP_BLE_READ_CHAR,
                                        STEP_STANDBY,
                                        STEP_IDLE
                                };
                                psIdx = 0;
                                step = processSteps[psIdx];

//                                    for(int i = 0; i < processSteps.length; i++) {
//                                        if(processSteps[i] == STEP_BLE_READ_CHAR) {
//                                            psIdx = i;
//                                            step = processSteps[psIdx];
//                                            break;
//                                        }
//                                    }
                            }
                        }
                        else {
                            try {
                                Thread.sleep(300);
                            } catch(InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    //--------------------------------------------------------------------------
//                        case STEP_START_BLE_SCAN:
//                            mugFound = false;
//                            mugGattConnected = false;
//                            mugGattDiscovered = false;
//
//                            btManager = (BluetoothManager)appContext.getSystemService(Context.BLUETOOTH_SERVICE);
//                            btAdapter = btManager.getAdapter();
//                            bleScanner = btAdapter.getBluetoothLeScanner();
//
//                            bleScanner.startScan(leScanCallback);
//                            setTimeout(30000);
//                            step = processSteps[++psIdx];
//                            break;
                    //--------------------------------------------------------------------------
//                        case STEP_BLE_SCAN_DONE:
//                            while(!timeout) {
//                                if( checkStop() ) {
//                                    timeoutHandler.removeCallbacksAndMessages(null);
//                                    MessageToActivity.sendMessageToActivity(msgHandler,
//                                            ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                            "Terminated");
//                                    break;
//                                }
//                                if(mugFound) {
//                                    timeoutHandler.removeCallbacksAndMessages(null);
//                                    MessageToActivity.sendMessageToActivity(msgHandler,
//                                            ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                            "Mug found");
//                                    break;
//                                }
//                            }
//
//                            bleScanner.stopScan(leScanCallback);
//                            bleScanner.flushPendingScanResults(leScanCallback);
//
//                            MessageToActivity.sendMessageToActivity(msgHandler,
//                                    ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                    String.format("End of scanning", txName));
//
//                            step = processSteps[++psIdx];
//                            break;
                    //--------------------------------------------------------------------------
                    case STEP_BLE_GATT_CONNECT:

//                            if( timeToReconnect != null) {
//                                if( timeToReconnect.isReady() ) {
//                                    MLogger.logToFile(appContext, "service.txt", "TH : GATT reconnecting...", true);
//                                    Log.d("TH", String.format("GATT reconnecting..."));
//                                    //timeToReconnect.cancel();
//                                    timeToReconnect = null;
//                                }
//                                else {
//                                    threadSleep(1000);
//                                    //Log.d("TH", String.format("GATT connect sleeping..."));
//                                    break;
//                                }
//                            }

                        if(btManager == null)
                            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
                        if(btAdapter == null) btAdapter = btManager.getAdapter();

                        if(btAdapter == null) {
                            // BT isn't supported
                            btIsEnabled = false;
                            step = STEP_WAIT_BT;
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is NULL", true);

                            break;
                        } else if(!btAdapter.isEnabled()) {
                            btIsEnabled = false;
                            step = STEP_WAIT_BT;
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is Disabled", true);

                            break;
                        } else {
                            btIsEnabled = true;
                        }

                        if(mugBleDevice == null)
                            mugBleDevice = btAdapter.getRemoteDevice(mugAddress);


                        if(checkStop() || !checkBTEnabled()) {
                            break;
                        }

                        if( isMugGattConnected(mugBleDevice) && (mugGatt != null) ) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Mug is already connected " + mugAddress, true);
                        } else {

                            MLogger.logToFile(appContext, "service.txt", "TH : Mug isn't connected " + mugAddress, true);

                            mugGattConnected = false;
                            mugGattDiscovered = false;
/*
                                (timeToConnect = new EventsTimer()).start(10000);
                                while(!mugGattConnected && !timeToConnect.isReady()) {
                                    if(checkStop()) {
                                        break;
                                    }
                                    if(!checkBTEnabled()) {
                                        break;
                                    }
                                    //                                MessageToActivity.sendMessageToActivity(msgHandler,
                                    //                                        ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
                                    //                                        String.format("GATT connecting(%d)...", counter++));
                                    Log.d("BLE", String.format("Connecting..."));


                                    try {
                                        if(mugGatt == null) {
                                            MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null"), true);
                                            Log.d("BLE", String.format("TH : GATT is null"));
                                            mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);

                                        } else {
                                            mugGatt.connect();
                                        }

                                        threadSleep(100);
                                    } catch(Exception e) {
                                        String s = String.format("TH : GATT connect exception %s", e.toString());
                                        MLogger.logToFile(appContext, "service.txt", s, true);
                                        Log.d("BLE", s);
                                    }
                                }
*/
                            //------------------------------------------------------------------
                            // (timeToConnect = new EventsTimer()).start(10000);

                            try {
                                if(mugGatt == null) {
                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null. connectGATT..."), true);
                                    mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);

                                } else {
                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT isn't null. connectGATT..."), true);
                                    mugGatt.connect();
                                }

                                //threadSleep(100);
                            } catch(Exception e) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connect exception %s", e.toString()), true);
                            }

                            (timeToConnect = new EventsTimer()).start(10000);

                            while(!mugGattConnected && !checkStop() && checkBTEnabled()) {
                                // will stay here until GATT is connected or BT is disabled,
                                // or user terminates everything
//                                    if(checkStop()) {
//                                        break;
//                                    }
//                                    if(!checkBTEnabled()) {
//                                        break;
//                                    }

                                if(timeToConnect.isReady())
                                    threadSleep(10000);             // increase sleep time if Mug wasn't found in first 10 sec
                                else threadSleep(1000);    //

                                MLogger.logToFile(appContext, "service.txt",
                                        String.format(Locale.getDefault(), "TH : [ %d ] Waiting for GATT connected %s", Thread.currentThread().getId(), mugAddress), true);
                            }
                            timeToConnect = null;
                            if(!mugGattConnected) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connection is terminated."), true);
                                break; // connecting was terminated, exit from GATT connect step
                            }
                            //------------------------------------------------------------------

//                                if(timeToConnect.isReady()) {
//                                    String s = String.format("TH : GATT connect timeout");
//                                    MLogger.logToFile(appContext, "service.txt", s, true);
//                                    (timeToReconnect = new EventsTimer()).start(60000);
//                                    break;
//                                }
                        }

                        if(checkStop()) {
                            break;
                        }
                        if(!checkBTEnabled()) {
                            break;
                        }
                        //if( !mugGattConnected ) { psIdx = 0; step = processSteps[psIdx]; break; }

                        MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT Connected. Service discovering..."), true);

                        mugGatt.discoverServices();

                        while(!mugGattDiscovered) {
                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }
                        }
                        if(checkStop()) {
                            break;
                        }
                        if(!checkBTEnabled()) {
                            break;
                        }

                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Services discovered."), true);

//                            mugGatt.disconnect();
//                            //
//
//
//                            while(mugGattConnected);
//                            mugGatt.close();
//                            mugGatt = null;
//                            //setTimeout(5000);
//
//                            MessageToActivity.sendMessageToActivity(msgHandler,
//                                    ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                    String.format("DONE", txName));


                        step = processSteps[++psIdx];

                        break;
                    case STEP_BLE_READ_CHAR:
                        MLogger.logToFile(appContext, "service.txt", "TH : Reading", true);
                        if(ReadQueue != null) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Queue not empty", true);

                            if(mugGattConnected == false) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Queue not empty BUT GATT disconnected. Cancel reading.", true);
                                whatToSync = 0; needsToSync = false;
                                step = processSteps[++psIdx];
                                break;
                            }

//                            if((whatToSync & WTS_MUG_NAME) != 0) {
//
////                                mugNameSet = false;
////                                ReadQueue.get(0).setValue("Test");
////                                //ReadQueue.get(0).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
////                                ReadQueue.get(0).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
////                                mugGatt.writeCharacteristic(ReadQueue.get(0));
////
////                                while(!mugNameSet) ;
//
//                                mugGattCharsRead = false;
//                                mugGatt.readCharacteristic(ReadQueue.get(0));
//                                while(!mugGattCharsRead) {
//                                }
//
//                                whatToSync &= ~WTS_MUG_NAME;
//                            }

                            //---- Mug name ------------------------------------------------------
                            if((whatToSync & WTS_MUG_NAME) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Mug name " + mugAddress, true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(0));
                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(  Locale.getDefault(),
                                                        "TH : Reading Mug name TIMEOUT %d",timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    whatToSync &= ~WTS_MUG_NAME;
                                    attempts = maxAttempts;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Reading Mug name Attempts left: %d %s", attempts, mugAddress), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Mug name UNABLE to read %s",  mugAddress), true);
                                        whatToSync &= ~WTS_MUG_NAME;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }

                            //---- End of Mug name -----------------------------------------------

                            if((whatToSync & WTS_CURRENT_TEMP) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Tcurrent", true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(1));
                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                            String.format(  Locale.getDefault(),
                                                                            "TH : Reading Tcurrent TIMEOUT %d",timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    whatToSync &= ~WTS_CURRENT_TEMP;
                                    attempts = maxAttempts;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Tcurrent Attempts left: %d", attempts), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Tcurrent UNABLE to read"), true);
                                        whatToSync &= ~WTS_CURRENT_TEMP;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }

                            //---- Target temperature ---------------------------------------------
                            if((whatToSync & WTS_TARGET_TEMP) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Ttarget", true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(2));
                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(  Locale.getDefault(),
                                                        "TH : Reading Ttarget TIMEOUT %d",timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    whatToSync &= ~WTS_TARGET_TEMP;
                                    attempts = maxAttempts;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Ttarget Attempts left: %d", attempts), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Ttarget UNABLE to read"), true);
                                        whatToSync &= ~WTS_TARGET_TEMP;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }

                            //---- End of Target temperature --------------------------------------

                            //---- Mug color ------------------------------------------------------
                            if((whatToSync & WTS_MUG_COLOR) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Mug color " + mugAddress, true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(6));
                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(  Locale.getDefault(),
                                                        "TH : Reading Mug color TIMEOUT %d",timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    whatToSync &= ~WTS_MUG_COLOR;
                                    attempts = maxAttempts;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Reading Mug color Attempts left: %d %s", attempts, mugAddress), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Mug color UNABLE to read %s",  mugAddress), true);
                                        whatToSync &= ~WTS_MUG_COLOR;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }

                            //---- End of Mug color -----------------------------------------------

                            //------------------------------------------------


//                                List<BluetoothGattDescriptor> list = ReadQueue.get(6).getDescriptors();
//
//                                try {
//                                    BluetoothGattDescriptor descriptor = ReadQueue.get(6).getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                    mugGatt.writeDescriptor(descriptor);
//                                }
//                                catch(Exception e) {
//                                    String s = e.toString();
//                                }
//                                while(!mugGattDescriptorWritten);

//                                mugTargetTempSet = false;
//                                ReadQueue.get(2).setValue(5000, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
//                                ReadQueue.get(2).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                mugGatt.writeCharacteristic(ReadQueue.get(2));
//                                while(!mugTargetTempSet);


//                                mugColorSet = false;
//                                ReadQueue.get(4).setValue(new byte[]{(byte)0xA0, (byte)0x20, (byte)0xF0, (byte)0xFF});
//                                ReadQueue.get(4).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                mugGatt.writeCharacteristic(ReadQueue.get(4));
//                                while(!mugColorSet);


//                                ReadQueue.get(0).setValue("Test");
//                                //ReadQueue.get(0).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//                                ReadQueue.get(0).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                mugGatt.writeCharacteristic(ReadQueue.get(0));

                           //     while(!mugTargetTempSet);
                            //------------------------------------------------
//                            if((whatToSync & WTS_TARGET_TEMP) != 0) {
//                                mugGattCharsRead = false;
//                                mugGatt.readCharacteristic(ReadQueue.get(2));
//                                while(!mugGattCharsRead) ;
//                                whatToSync &= ~WTS_TARGET_TEMP;
//                            }

                            if((whatToSync & WTS_BATTERY) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery", true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(3));
                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery TIMEOUT", true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    whatToSync &= ~WTS_BATTERY;
                                    attempts = maxAttempts;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Battery Attempts left: %d", attempts), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Battery UNABLE to read"), true);
                                        whatToSync &= ~WTS_BATTERY;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if(checkStop()) {
                                break;
                            }
                            if(!checkBTEnabled()) {
                                break;
                            }

                            if((whatToSync & WTS_LIQUID_STATE) != 0) {
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(4));
                                while(!mugGattCharsRead) ;
                                whatToSync &= ~WTS_LIQUID_STATE;
                            }

                            if((whatToSync & WTS_MUG_COLOR) != 0) {
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(6));
                                while(!mugGattCharsRead) ;
                                whatToSync &= ~WTS_MUG_COLOR;
                            }

                            if((whatToSync & WTS_SET_NOTIFICATION) != 0) {
                                mugGattDescriptorWritten = false;
                                mugGatt.setCharacteristicNotification(ReadQueue.get(5), true);

                                UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                                BluetoothGattDescriptor desc = ReadQueue.get(5).getDescriptor(CONFIG_DESCRIPTOR);
                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mugGatt.writeDescriptor(desc);

                                while(!mugGattDescriptorWritten) ;
                                whatToSync &= ~WTS_SET_NOTIFICATION;

                                MLogger.logToFile(appContext, "service.txt", "TH : Notifictions set...", true);
                            }

                            if(whatToSync == 0) {
                                needsToSync = false;
                                MLogger.logToFile(appContext, "service.txt", "TH : sync is done", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : still need to sync", true);
                            }
                        } else {
                            MLogger.logToFile(appContext, "service.txt", "TH : ReadQueue is null", true);
                        }
                        //else mugGattCharsRead = true;

                        //while(!mugGattCharsRead);

                        //  mugGatt.disconnect();
                        //while(mugGattConnected);
//                            mugGatt.close();
//                            mugGatt = null;
//                            //setTimeout(5000);
//

                        if(needsToSync == false) step = processSteps[++psIdx];

                        break;
                    //---- Write chracteristics ---------------------------------------------------
                    case STEP_BLE_WRITE_CHAR:
                        if(mugParametersToSet == null) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Writing - NOTHING", true);
                            step = processSteps[++psIdx];
                            break;
                        }
                        //---- Mug name -----------------------------------------------------------
                        attempts = maxAttempts;
                        String name = mugParametersToSet.getMugName();
                        if( name != null ) {
                            // ??? Need to check GATT connected ???
                            while(attempts > 0) {

                                MLogger.logToFile(appContext, "service.txt", "TH : Writing Name " + name, true);
                                mugGattCharWritten = false;

                                ReadQueue.get(0).setValue(name);
                                ReadQueue.get(0).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                mugGatt.writeCharacteristic(ReadQueue.get(0));
                                //while(!mugGattCharWritten);

                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharWritten) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Writing Name TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    //whatToSync &= ~WTS_CURRENT_TEMP;
                                    MLogger.logToFile(appContext, "service.txt", "TH : Writing Name - DONE", true);
                                    attempts = maxAttempts;
                                    break;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Writing Name Attempts left: %d", attempts), true);
                                        //break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Writing Name UNABLE to write"), true);
                                        //whatToSync &= ~WTS_CURRENT_TEMP;
                                        //attempts = maxAttempts;
                                    }
                                }
                            }
                        }
                        //---- Mug name ----------------------------------------------------------
                        //---- Target temperature -------------------------------------------------
                        attempts = maxAttempts;
                        Integer t = mugParametersToSet.getTargetTemperature();
                        if( t != null ) {
                            // ??? Need to check GATT connected ???
                            while(attempts > 0) {

                                MLogger.logToFile(appContext, "service.txt", "TH : Writing Tcurrent " + t.toString(), true);
                                mugGattCharWritten = false;

                                ReadQueue.get(2).setValue(t, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                                ReadQueue.get(2).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                mugGatt.writeCharacteristic(ReadQueue.get(2));
                                //while(!mugGattCharWritten);

                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharWritten) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Writing Ttrgt TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    //whatToSync &= ~WTS_CURRENT_TEMP;
                                    MLogger.logToFile(appContext, "service.txt", "TH : Writing Tcurrent - DONE", true);
                                    attempts = maxAttempts;
                                    break;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Writing Ttrgt Attempts left: %d", attempts), true);
                                        //break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Ttrgt UNABLE to write"), true);
                                        //whatToSync &= ~WTS_CURRENT_TEMP;
                                        //attempts = maxAttempts;
                                    }
                                }
                            }
                        }
                        //---- Target temperature -------------------------------------------------
                        //---- Mug color ----------------------------------------------------------
                        attempts = maxAttempts;
                        Long color = mugParametersToSet.getMugColor();
                        if( color != null ) {

                            while(attempts > 0) {

                                MLogger.logToFile(appContext, "service.txt", String.format( "TH : Writing Color %02X%02X%02X",
                                                                                            (byte)(color >>> 16), (byte)(color >>> 8), (byte)(color & 0xFF) ),
                                                                                            true);
                                mugGattCharWritten = false;

                                //ReadQueue.get(6).setValue(new byte[]{(byte)(color >>> 16), (byte)(color >>> 8), (byte)(color & 0xFF), (byte)0xFF });
                                ReadQueue.get(6).setValue(new byte[]{(byte)(color >>> 16), (byte)(color >>> 8), (byte)(color & 0xFF), (byte)0xFF });

                                //ReadQueue.get(6).setValue(t, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                                ReadQueue.get(6).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                mugGatt.writeCharacteristic(ReadQueue.get(6));
                                //while(!mugGattCharWritten);

                                int timeout = 5000 / 10;  //5 sec
                                while(!mugGattCharWritten) {
                                    try {
                                        Thread.sleep(10);
                                    } catch(InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if(--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Writing Color TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if(timeout > 0) {
                                    //whatToSync &= ~WTS_CURRENT_TEMP;
                                    MLogger.logToFile(appContext, "service.txt", "TH : Writing Color - DONE", true);
                                    attempts = maxAttempts;
                                    break;
                                } else {
                                    if(attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Writing Color Attempts left: %d", attempts), true);
                                        //break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Color UNABLE to write"), true);
                                        //whatToSync &= ~WTS_CURRENT_TEMP;
                                        //attempts = maxAttempts;
                                    }
                                }
                            }
                        }
                        //---- Mug color ----------------------------------------------------------

                        sendMessage(SyncService.MSG_SET_PARAMETERS_DONE);

                        //if(mugGattCharWritten == false)
                        step = processSteps[++psIdx];

                    break;
                    //---- End of Write chracteristics --------------------------------------------
                    case STEP_STANDBY:
                        MLogger.logToFile(appContext, "service.txt", "TH : Standby", true);
                        step = processSteps[++psIdx];
                        break;
                    case STEP_QUIT:
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : QUIT !!! %s", mugAddress), true);

                        onTheGo = false;

                        if(isMugGattConnected(mugBleDevice)) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Disconnecting %s...", mugAddress), true);
                            mugGatt.disconnect();
                            mugGatt.close();
                        }

                        break;
                    //-------------------------------------------------------------------------
                    //-------------------------------------------------------------------------
                    case STEP_BT_START_DISCOVERY:
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Starting discovering..."), true);

                        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

                        appContext.registerReceiver(mReceiver, filter);

                        if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

                        btAdapter.startDiscovery();

                        step = processSteps[++psIdx];

                        break;
                    case STEP_BT_DISCOVERING:
                        if(Thread.currentThread().isInterrupted()) {
                            step = processSteps[++psIdx];
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Terminating discovering"), true);
                            if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
                            //while(btAdapter.isDiscovering());
                            //MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovering stopping"), true);
                        }

                        if( requestToBond && !"".equals(macToBond)) {
                            step = processSteps[++psIdx];
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Request to bond. Terminating discovering"), true);
                            if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();


//                            while(!isBonded) {};
//                            onTheGo = false;
                        }
                        break;
                    case STEP_BT_STOP_DISCOVERY:
                        break;
                    case STEP_BT_DISCOVERING_DONE:
                        //updateBTDevices(null, null, DISCOVERING_DONE);
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovering done"), true);
                        //onTheGo = false;
                        //step = processSteps[++psIdx];

                        if( requestToBond && !"".equals(macToBond)) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Request to bond %s", macToBond), true);
                            requestToBond = false;
                            step = STEP_BT_WAITING_FOR_BOND;
                            BluetoothDevice bleDevice =  btAdapter.getRemoteDevice(macToBond);
                            bleDevice.createBond();

//                            try {
//                                if(mugGatt == null) {
//                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null. connectGATT..."), true);
//                                    mugGatt = bleDevice.connectGatt(appContext, false, bleGattCallback);
//
//                                } else {
//                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT isn't null. connectGATT..."), true);
//                                    mugGatt.connect();
//                                }
//
//                                //threadSleep(100);
//                            } catch(Exception e) {
//                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connect exception %s", e.toString()), true);
//                            }

                        }

                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovering done - QUIT"), true);
                        onTheGo = false;


                        break;
                    case STEP_BT_WAITING_FOR_BOND:
//                        if( requestToBond && !"".equals(macToBond)) {
//                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Request to bond %s", macToBond), true);
//                            requestToBond = false;
//                            BluetoothDevice bleDevice =  btAdapter.getRemoteDevice(macToBond);
//                            bleDevice.createBond();
//                            while(!isBonded) {};
//                            onTheGo = false;
//                        }

                        while(!isBonded) {};
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Bonded %s", macToBond), true);
                        updateBTDevices("WWWW", macToBond, DISCOVERING_PAIRED);
                        onTheGo = false;

                        break;
                    //-------------------------------------------------------------------------
                    case STEP_PAIRED_INFO:
                        if(btAdapter == null) {
                            // BT isn't supported
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is NULL", true);
                            onTheGo = false;
                            // add error processing
                            break;
                        } else if(!btAdapter.isEnabled()) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is Disabled", true);
                            onTheGo = false;
                            // add error processing
                            break;
                        }

                        while(mugsListIdx < mugsList.size() ) {

                            mugBleDevice = btAdapter.getRemoteDevice(mugsList.get(mugsListIdx).getMACAddress());

                            MLogger.logToFile(  appContext,
                                                "service.txt",
                                                String.format( Locale.getDefault(),
                                                                "TH : collecting data for %s (%d/%d)",
                                                                mugsList.get(mugsListIdx).getMACAddress(),
                                                                mugsListIdx,
                                                                mugsList.size() - 1 ) , true);

                            mugGattConnected = false;
                            mugGattDiscovered = false;

                            try {

                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null. connectGATT..."), true);
                                mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);

                            } catch(Exception e) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connect exception %s", e.toString()), true);
                            }

                            (timeToConnect = new EventsTimer()).start(5000);

                            while(!mugGattConnected && !Thread.currentThread().isInterrupted()) {

                                if(timeToConnect.isReady()) {
                                    MLogger.logToFile(appContext, "service.txt",
                                            String.format(Locale.getDefault(), "TH : [ %d ] GATT connection timeout %s",
                                                                                Thread.currentThread().getId(),
                                                                                mugsList.get(mugsListIdx).getMACAddress()), true);
                                    updatePairedInfo(false);

                                    break;
                                }
                            }

                            timeToConnect = null;

                            if(Thread.currentThread().isInterrupted()) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : interrupted"), true);
                                break;
                            }

                            if(!mugGattConnected) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : switching to next device"), true);
                                mugsListIdx++;
                                continue;
                            }

                            MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT Connected. Service discovering..."), true);

                            mugGatt.discoverServices();

                            while(!mugGattDiscovered && !Thread.currentThread().isInterrupted()) { }

                            if(!mugGattDiscovered) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : Couldn't discover services, switching to next device"), true);
                            }

                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Services discovered."), true);


                            //----

                            if( readCharacteristic(ReadQueue.get(0)) ) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Name - OK", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Name - ERR", true);
                            }

                            if( readCharacteristic(ReadQueue.get(1)) ) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Tcurrent - OK", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Tcurrent - ERR", true);
                            }

                            if( readCharacteristic(ReadQueue.get(2)) ) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Ttarget - OK", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Ttarget - ERR", true);
                            }

                            if( readCharacteristic(ReadQueue.get(3)) ) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery - OK", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery - ERR", true);
                            }

                            if( readCharacteristic(ReadQueue.get(6)) ) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Color - OK", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Color - ERR", true);
                            }
                            mugsList.get(mugsListIdx).setType(MugParameters.TYPE_PAIRED);
                            updatePairedInfo(false);

                            //------------------------------------------------
                            mugsListIdx++;
                        }
                        MLogger.logToFile(appContext, "service.txt", "TH : Paired info - DONE", true);

                        if(Thread.currentThread().isInterrupted()) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : paired - interrupted"), true);
                            //updatePairedInfo(true);
                            onTheGo = false;
                        }
                        else {
                            step = processSteps[++psIdx];
                        }
                        break;
                    //-------------------------------------------------------------------------
                }
            } else break;
        }
        MLogger.logToFile(appContext, "service.txt", "TH : Thread EXIT", true);
    }

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                mugGattConnected = true;
                mugsList.get(mugsListIdx).setConnected(true);
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connected %s (%d)", gatt.getDevice().getAddress(), step), true);
                if(step == STEP_IDLE) {
                    requestToReRead = true;
                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connected - ReRead %s (%d)", gatt.getDevice().getAddress(), step), true);
                }
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Disconnected %s (%d) ", gatt.getDevice().getAddress(), step), true);
                mugGattConnected = false;
                mugsList.get(mugsListIdx).setConnected(false);
            } else if(newState == BluetoothProfile.STATE_CONNECTING) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connecting %s (%d) ", gatt.getDevice().getAddress(), step), true);
            } else if(newState == BluetoothProfile.STATE_DISCONNECTING) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Disconnecting %s (%d)", gatt.getDevice().getAddress(), step), true);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if(status == BluetoothGatt.GATT_SUCCESS) {
                String s = "";
                List<BluetoothGattService> list = gatt.getServices();

                for(int i = 0; i < list.size(); i++) {
                    s += list.get(i).getUuid().toString().toUpperCase() + "\n";

                    if(UUID.fromString("fc543622-236c-4c94-8fa9-944a3e5353fa").equals(list.get(i).getUuid())) {  // Mug service
                        ReadQueue = new ArrayList<>(0x20);
                        s += "yes\n";
                        List<BluetoothGattCharacteristic> charList = list.get(i).getCharacteristics();
                        for(int k = 0; k < charList.size(); k++) {
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa")))   // 0 Mug name
                                ReadQueue.add(charList.get(k));
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540002-236c-4c94-8fa9-944a3e5353fa")))   // 1 Current temp
                                ReadQueue.add(charList.get(k));
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540003-236c-4c94-8fa9-944a3e5353fa")))   // 2 Target temp
                                ReadQueue.add(charList.get(k));
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540007-236c-4c94-8fa9-944a3e5353fa")))   // 3 battery
                                ReadQueue.add(charList.get(k));
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540008-236c-4c94-8fa9-944a3e5353fa")))   // 4 Liquid state
                                ReadQueue.add(charList.get(k));

                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540012-236c-4c94-8fa9-944a3e5353fa"))) {   // 5 notifications
                                ReadQueue.add(charList.get(k));
                            }
                            if(charList.get(k).getUuid().equals(UUID.fromString("fc540014-236c-4c94-8fa9-944a3e5353fa")))   // 6 color
                                ReadQueue.add(charList.get(k));


                            List<BluetoothGattDescriptor> descrList = charList.get(k).getDescriptors();
                            if(descrList.size() != 0) {
                                s += String.format("\n\n#%d# %s %d", k, charList.get(k).getUuid().toString(), descrList.size());
                                for(int j = 0; j < descrList.size(); j++) {
                                    s += String.format("\n*%s", descrList.get(j).getUuid().toString());
                                }
                            }

                            //s += "+\n";
                        }
                    }
//                    List<BluetoothGattCharacteristic> charList = list.get(i).getCharacteristics();
//
//                    for(int k = 0; k < charList.size(); k++) {
//                        s += "* " + charList.get(k).getUuid().toString() + "\n";
//                        if( charList.get(k).getUuid().equals( UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa") ) ){
//                            s += "yes\n";
//                        }
//                        else s += "no \n";
//
//                    }

                    //   for(int k = 0; k < charList.size(); k++) {
                    //       s += "* " + charList.get(i).getUuid().toString().toUpperCase() + "\n";
//                        MessageToActivity.sendMessageToActivity(msgHandler,
//                                ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                                "* " + charList.get(i).getUuid().toString().toUpperCase()  );

                    // }

                }
//                MessageToActivity.sendMessageToActivity(msgHandler,
//                        ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                        s );


                mugGattDiscovered = true;
                //              broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
//                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                if(characteristic.getUuid().equals(UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa"))) {
                    String s = characteristic.getStringValue(0);
                    mugsList.get(mugsListIdx).setMugName(s);
                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Mug name: %s", s), true);
                }
                if(characteristic.getUuid().equals(UUID.fromString("fc540002-236c-4c94-8fa9-944a3e5353fa"))) {
                    //    String s = characteristic.getStringValue(0);
                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    long id = Thread.currentThread().getId();
                    mugsList.get(mugsListIdx).setCurrentTemperature((float) t / 100.0f);
                    //updateWidget(new MugParameters(mugAddress, ((float)t)/100.0f, null, null, true));

                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d)Current temp: %.2fC", id, (float) t / 100.00), true);
                }
                if(characteristic.getUuid().equals(UUID.fromString("fc540003-236c-4c94-8fa9-944a3e5353fa"))) {
                    //    String s = characteristic.getStringValue(0);
                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    long id = Thread.currentThread().getId();
                    mugsList.get(mugsListIdx).setTargetTemperature(t);
                    //updateWidget(new MugParameters(mugAddress, ((float)t)/100.0f, null, null, true));

                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d)Target temp: %.2fC", id, (float) t / 100.00), true);

                }
                if(characteristic.getUuid().equals(UUID.fromString("fc540007-236c-4c94-8fa9-944a3e5353fa"))) {
                    //    String s = characteristic.getStringValue(0);
                    String charging = "no";
                    int p = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    int c = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
                    int v = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
                    if(c == 1) charging = "yes";

                    //updateWidget( new MugParameters( mugAddress, null, null, p, true ) );
                    mugsList.get(mugsListIdx).setBatteryCharge(p);

                    MLogger.logToFile(appContext, "service.txt",
                            String.format(Locale.getDefault(), "TH : Battery: %d%% charging: %s %.2fC %d", p, charging, (float) t / 100.00, v),
                            true);
                }

                if(characteristic.getUuid().equals(UUID.fromString("fc540008-236c-4c94-8fa9-944a3e5353fa"))) {
                    byte[] state = characteristic.getValue();
                    String[] states = new String[]{"NA", "Empty", "Filling", "Unknown", "Cooling", "Heating", "OK"};
                    if(state[0] >= states.length) state[0] = 0;
                }

                if(characteristic.getUuid().equals(UUID.fromString("fc540014-236c-4c94-8fa9-944a3e5353fa"))) {
                    byte[] color = new byte[8];
                    byte[] tmp = characteristic.getValue();
                    System.arraycopy(tmp, 0, color, 0, tmp.length);

                    long id = Thread.currentThread().getId();

                    //byte[] data = new byte[] {50, -106, 40, -22, 0, 0, 0, 0};
                    /*
                    ByteBuffer buffer = ByteBuffer.wrap(color);
//                    buffer.order(ByteOrder.BIG_ENDIAN);
//                    System.out.println(buffer.getLong()); // 3645145933890453504
//                    buffer = ByteBuffer.wrap(color);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    //System.out.println(buffer.getLong()); // 3928528434
                    Long l = buffer.getLong();
*/
                    Long l = (long) ((((long)tmp[0] & 0xFF) << 16) | (((long)tmp[1] & 0xFF) << 8) | (((long)tmp[2] & 0xFF)));

                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Mug color: %06X", id, l ), true);

                    mugsList.get(mugsListIdx).setMugColor(l);



                }

                mugGattCharsRead = true;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: onCharacteristicWrite: %s  written", characteristic.getUuid()), true);


            if(characteristic.getUuid().equals(UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa"))) {
                mugNameSet = true;
                mugGattCharWritten = true;
            }

            if(characteristic.getUuid().equals(UUID.fromString("fc540003-236c-4c94-8fa9-944a3e5353fa"))) {
                //if(characteristic.getUuid().equals(UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa"))) {
                mugTargetTempSet = true;
                mugGattCharWritten = true;
            }
            if(characteristic.getUuid().equals(UUID.fromString("fc540014-236c-4c94-8fa9-944a3e5353fa"))) {
                mugColorSet = true;
                mugGattCharWritten = true;
            }

        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {

            byte[] data = characteristic.getValue();
            String[] info = new String[]{"NA", "Battery", "Charging", "Not charging", "Target temp", "Current temp", "NA", "Level", "State"};

            long id = Thread.currentThread().getId();
            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: [%d][%d]Notification %s(%02X) G:%s T:%s", id, step, info[data[0]], data[0], gatt.getDevice().getAddress(), mugAddress), true);

            if(needsToSync == false) {
                switch(data[0]) {
                    case 0x01:
                        whatToSync |= WTS_BATTERY;
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Battery notification"), true);
                        break;
                    case 0x04:
                        // whatToSync |= WTS_TARGET_TEMP;
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Target temperature notification"), true);
                        break;
                    case 0x05:
                        whatToSync |= WTS_CURRENT_TEMP;
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Current temperature notification"), true);
                        break;
                    case 0x08:
                        //whatToSync |= WTS_LIQUID_STATE;
                        break;

                }
                if(whatToSync != 0) needsToSync = true;
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {

//            MessageToActivity.sendMessageToActivity(msgHandler,
//                    ButtonsFragment.HM_ID_USER_MSG_TO_LOG,
//                    String.format("Descriptor was written: %d", status ));
            MLogger.logToFile(appContext, "service.txt", "BLE: onDescriptorWrite", true);
            mugGattDescriptorWritten = true;

        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // It means the user has changed his bluetooth state.
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                switch(btAdapter.getState()) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        // The user bluetooth is turning off yet, but it is not disabled yet.
                        MLogger.logToFile(appContext, "service.txt", "BLE: Adapter is turning OFF", true);
                        btIsEnabled = false;
                        mugsList.get(mugsListIdx).setConnected(false);
                        return;
                    case BluetoothAdapter.STATE_ON:
                        if(btAdapter != null) {
                            if(btAdapter.isEnabled()) {
                                btIsEnabled = true;
                                MLogger.logToFile(appContext, "service.txt", "BLE: Adapter is ON", true);
                            }
                        }
                        break;
                }

            }
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                switch(step) {
                    case STEP_BT_DISCOVERING:
//                        Message.sendMessageToActivity(txBrowserMsgHandler,
//                                TxBrowserFragment.HM_ID_ADD_TX_TO_LIST,
//                                new BTDeviceInfo(deviceName, deviceHardwareAddress, device));
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovered: %s %s", deviceName, deviceHardwareAddress), true);
                        if( deviceName != null) updateBTDevices(deviceName, deviceHardwareAddress, DISCOVERING_NEW);
                        break;
//                    case STEP_BT_SEARCHING:
//
//                        if(txName.equals(deviceName)) {
//
//                            Message.sendMessageToActivity(processingMsgHandler,
//                                    ProcessingFragment.HM_ID_USER_MSG_INFO,
//                                    "Transmitter was found");
//
//                            BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
//                            if(bta.isDiscovering() ) bta.cancelDiscovery();
//
//                            txBTDevice = device;
//                            step = processSteps[++psIdx];   //STEP_BT_CONNECT
//
//                            break;
//                        }
                }
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                switch(step) {
                    case STEP_BT_DISCOVERING:
                        //BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                        if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
                        btAdapter.startDiscovery();
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovery finished - restarting"), true);
                        break;
                    case STEP_BT_STOP_DISCOVERY:
                        step = STEP_BT_DISCOVERING_DONE;
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovery done"), true);
                        break;
//                    case STEP_BT_STOP_SEARCH:
//                        step = STEP_BT_SEARCHING_DONE;
//                        break;
//                    case STEP_BT_SEARCHING:
//                        step = STEP_BT_SEARCHING_FAIL;
//                        break;
                }
            }
            if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                MLogger.logToFile(appContext, "service.txt", String.format("TH : pairing request %s", deviceName), true);
//                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
//                if( type == BluetoothDevice.PAIRING_VARIANT_PIN) {
//                    device.setPin(new byte[]{0, 0, 0 ,0 });
//                }
            }
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                MLogger.logToFile(appContext, "service.txt", String.format("TH : bond state changed"), true);
                switch(step) {
                    case STEP_BT_WAITING_FOR_BOND:
                        final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                        if(bondState == BluetoothDevice.BOND_BONDING) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : bond state: bonding"), true);
//                            BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                            int s = d.getBondState();
//                            if(s == BluetoothDevice.BOND_BONDED) {
//
//                            }
                        }

                        if(bondState == BluetoothDevice.BOND_BONDED) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : bond state: bonded"), true);
                            isBonded = true;
                        }
                        if(bondState == BluetoothDevice.BOND_NONE) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : bond state: NONE"), true);
                        }
                        break;
                }
            }

            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals((action))) {
//                try {
//                    Method m = txBTDevice.getClass()
//                            .getMethod("removeBond", (Class[]) null);
//                    m.invoke(txBTDevice, (Object[]) null);
//                } catch (Exception e) {
//                 ///   Log.e(TAG, e.getMessage());
//                }
            }

            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals((action))) {
                MLogger.logToFile(appContext, "service.txt", String.format("TH : ACL connected"), true);
//                try {
//                    Method m = txBTDevice.getClass()
//                            .getMethod("removeBond", (Class[]) null);
//                    m.invoke(txBTDevice, (Object[]) null);
//                } catch (Exception e) {
//                    ///   Log.e(TAG, e.getMessage());
//                }

            }

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                MLogger.logToFile(appContext, "service.txt", String.format("TH : State changed"), true);
//                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
//                    try {
//                        Method m = txBTDevice.getClass()
//                                .getMethod("removeBond", (Class[]) null);
//                        m.invoke(txBTDevice, (Object[]) null);
//                    } catch (Exception e) {
//                        ///   Log.e(TAG, e.getMessage());
//                    }
//                }
//                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
//                    Log.v("", "");
//                }
            }

        }
    };

    public void setStop() {
        stop = true;
    }

    private boolean checkStop() {

        if(AppPreferences.readBoolean(appContext, String.format("mug_%s", mugAddress), AppPreferences.NAME_ENABLED) == null) {
            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : it seems Thread %s shouldn't exist anymore", mugAddress), true);
            step = STEP_QUIT;
            return true;
        }

        if(stop) {
            step = STEP_QUIT;
            //  logMsg("Stopped by user");
            return true;
        } else return false;
    }

    private boolean checkBTEnabled() {

        if(!btIsEnabled) {
            step = STEP_WAIT_BT;

            MLogger.logToFile(appContext, "service.txt", String.format("TH : Going to STEP_WAIT_BT"), true);

            return false;
        }
        return true;
    }

    public void setOnTheGo(boolean value) {
        onTheGo = value;
    }

    public boolean getOnTheGo() {
        return onTheGo;
    }

    private void updateWidget() { //(int tempCurrent, int batteryCharge) {
        String ts = String.valueOf(new SimpleDateFormat("HH:mm:ss:SSS").format((new Date()).getTime()));
        long id = Thread.currentThread().getId();

        if(widgetId != 0) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
            RemoteViews remoteViews = new RemoteViews(appContext.getPackageName(), R.layout.app_widget);
            //ComponentName thisWidget = new ComponentName(appContext, AppWidget.class);

            //        int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            //        if(widgetIds != null) {
            //            String s = String.format("TH : Widget update %s", Arrays.toString(widgetIds));
            //            MLogger.logToFile(appContext, "service.txt", s, true);
            //            remoteViews.setTextViewText(R.id.tv_widgetId, Arrays.toString(widgetIds));
            //        }
            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Widget %d update @%s", widgetId, String.format("#%d %s", id, ts)), true);
            remoteViews.setTextViewText(R.id.tv_widgetId, String.format("[%d]", widgetId));
            remoteViews.setTextViewText(R.id.tv_timeStamp, String.format("#%d %s", id, ts));

            if(mugsList.get(mugsListIdx).getConnected()) {
                if(mugsList.get(mugsListIdx).getCurrentTemperature() != null)
                    remoteViews.setTextViewText(R.id.tv_currentTemperature, String.format(Locale.getDefault(), "%.02fC", mugsList.get(mugsListIdx).getCurrentTemperature()));
                //remoteViews.setTextViewText(R.id.tv_currentTemperature, String.format(Locale.getDefault(), "%.00fC", mugParameters.getCurrentTemperature() ));
                if(mugsList.get(mugsListIdx).getBatteryCharge() != null)
                    remoteViews.setTextViewText(R.id.tv_battery, String.format("%d%%", mugsList.get(mugsListIdx).getBatteryCharge()));
            } else {
                remoteViews.setTextViewText(R.id.tv_currentTemperature, "--C");
                remoteViews.setTextViewText(R.id.tv_battery, "--%");
            }
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        } else {
            MLogger.logToFile(appContext, "service.txt", "TH : Widget update. ID is NULL", true);
        }

        if(msgClient != null) {
            try {
                msgClient.send(Message.obtain(null,
                        SyncService.MSG_REFRESH_PARAMETERS, 0, 0, mugsList.get(mugsListIdx)));
                MLogger.logToFile(appContext, "service.txt", "TH : Widget update. Msg to activity - Value sent", true);
            } catch(RemoteException e) {
                MLogger.logToFile(appContext, "service.txt", "TH : Widget update. Remote exception " + e.toString(), true);
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                msgClient = null;
            }

        } else {
            MLogger.logToFile(appContext, "service.txt", "TH : Widget update. Msg to activity - NO CLIENT", true);
        }

/*
        if(mClients != null) {
            for(int i = mClients.size() - 1; i >= 0; i--) {
                try {
                    mClients.get(i).send(Message.obtain(null,
                            SyncService.MSG_SET_VALUE, tempCurrent, 0, mugAddress));
                    MLogger.logToFile(appContext, "service.txt", "TH : Widget update. Msg to activity - Value sent", true);
                } catch(RemoteException e) {
                    MLogger.logToFile(appContext, "service.txt", "TH : Widget update. Remote exception "  + e.toString(), true);
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
        }
*/
    }

    void updateBTDevices(String name, String mac, int state) {
        if(msgClient != null) {
            try {
                switch(state) {
//                    case DISCOVERING_DONE:
//                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Discovering DONE. Msg to activity.", mac), true);
//                        msgClient.send(Message.obtain(null,
//                                SyncService.MSG_NOT_PAIRED_DEVICE_INFO, DISCOVERING_DONE, 0, null));
//                        break;
                    case DISCOVERING_NEW:
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Device discovered %s. Msg to activity.", mac), true);
                        msgClient.send(Message.obtain(null,
                                SyncService.MSG_NOT_PAIRED_DEVICE_INFO, 0, 0,
                                new MugParameters(mac, name, null, null, null, null, null, MugParameters.TYPE_DISCOVERED, false)
                        ));
                    break;
                    case DISCOVERING_PAIRED:
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Paired. Msg to activity.", mac), true);
                        msgClient.send(Message.obtain(null,
                                SyncService.MSG_NEW_PAIRED_DEVICE_INFO, 0, 0,
                                new MugParameters(mac, name, null, null, null, null, null, MugParameters.TYPE_PAIRED, false)));
                        break;
                }
            } catch(RemoteException e) {
                MLogger.logToFile(appContext, "service.txt", "TH : Discovery. Msg to activity. Remote exception " + e.toString(), true);
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                msgClient = null;
            }

        } else {
            MLogger.logToFile(appContext, "service.txt", "TH : Discovery. Msg to activity - NO CLIENT", true);
        }


    }

    void updatePairedInfo(boolean done) {

        MLogger.logToFile(appContext, "service.txt", String.format("TH : update %d", mugsListIdx), true);

        if(msgClient != null) {

            try {
                if(done) {
                    msgClient.send(Message.obtain(null,
                            SyncService.MSG_PAIRED_DEVICE_INFO, 1, 0, null ));
                }
                else {
                    msgClient.send(Message.obtain(null,
                            SyncService.MSG_PAIRED_DEVICE_INFO, 0, 0, mugsList.get(mugsListIdx)));
                }
                MLogger.logToFile(appContext, "service.txt", "TH : Paired info. Msg to activity - Value sent", true);
            } catch(RemoteException e) {
                MLogger.logToFile(appContext, "service.txt", "TH : paired info. Remote exception " + e.toString(), true);
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                msgClient = null;
            }

        } else {
            MLogger.logToFile(appContext, "service.txt", "TH : Paired info. Msg to activity - NO CLIENT", true);
        }
    }

    private void threadSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMugGattConnected(BluetoothDevice btDevice) {
        //if(btManager == null) btManager = (BluetoothManager)appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        //if(btAdapter == null) btAdapter = btManager.getAdapter();

        if(ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }

        List<BluetoothDevice> connectedBLEDevices = ((BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE)).getConnectedDevices(BluetoothProfile.GATT);

        for(BluetoothDevice d : connectedBLEDevices) {
            MLogger.logToFile(appContext, "service.txt", "TH : Connected: " + d.getAddress(), true);
        }

        if(connectedBLEDevices.contains(mugBleDevice) == false) return false;

        return true;
    }

    boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {

        MLogger.logToFile(appContext, "service.txt", "TH : Reading characteristic", true);

        mugGattCharsRead = false;

        if(ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        mugGatt.readCharacteristic(characteristic);
        int timeout = 3000/10;  //3 sec
        while(!mugGattCharsRead) {
            try {
                Thread.sleep(10);
            } catch(InterruptedException e) {
                return false;
            }
            if(timeout-- <= 0) {
                MLogger.logToFile(appContext, "service.txt", "TH : Reading characteristic TIMEOUT", true);
                return false;
            }
        }

        return true;
    }

    void sendMessage(int msg) {
        if(msgClient != null) {
            try {
                msgClient.send(Message.obtain(null,
                        msg, 0, 0, null));
                MLogger.logToFile(appContext, "service.txt", String.format("TH : Msg to activity - %d", msg), true);
            } catch(RemoteException e) {
                MLogger.logToFile(appContext, "service.txt",  String.format("TH : Msg to activity - %d %s", msg, e.toString()), true);
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                msgClient = null;
            }

        } else {
            MLogger.logToFile(appContext, "service.txt", String.format("TH : Msg to activity -%d - NO CLIENT", msg), true);
        }
    }
}
