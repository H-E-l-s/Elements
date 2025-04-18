package com.hels.elements;

import android.Manifest;
import android.annotation.SuppressLint;
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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.os.IResultReceiver;
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
    public static final int TASK_ELEMENT_THREAD = 3;

    private final int STEP_IDLE = 0;
    private final int STEP_START_BLE_SCAN = 50;
    private final int STEP_BLE_SCAN_DONE = 51;
    private final int STEP_BLE_GATT_CONNECT = 52;
    private final int STEP_BLE_READ_CHAR = 53;
    private final int STEP_BLE_WRITE_CHAR = 54;
    private final int STEP_QUIT = 13;
    private final int STEP_STANDBY = 14;
    private final int STEP_WAIT_BT = 15;        // BT isn't enabled
    private final int STEP_LOST_CONNECTION = 16;        // BT isn't enabled

    private final int STEP_BT_START_DISCOVERY = 20;
    private final int STEP_BT_DISCOVERING = 21;
    private final int STEP_BT_STOP_DISCOVERY = 22;
    private final int STEP_BT_DISCOVERING_DONE = 23;
    private final int STEP_BT_WAITING_FOR_BOND = 24;

    //private final int STEP_PAIRED_INFO = 97;
    private final int STEP_WAITING_FOR_TASK = 98;
    //private final int STEP_TEST = 99;


    //private final int WTS_CURRENT_TEMP = 0x01;
    //private final int WTS_TARGET_TEMP = 0x02;
    //private final int WTS_BATTERY = 0x04;
    //private final int WTS_LIQUID_STATE = 0x08;
    private final int WTS_PARAMETERS = 0x04;
    //private final int WTS_MUG_NAME = 0x20;
    private final int WTS_LAST_LOG_REC = 0x02;
    private final int WTS_DATETIME = 0x01;


    private final int WTS_SET_NOTIFICATION = 0x80;
    private final int WTS_LAST = WTS_SET_NOTIFICATION;

    private final int DISCOVERING_NEW = 0x01;
    private final int DISCOVERING_DONE = 0x02;
    private final int DISCOVERING_PAIRED = 0x03;

    private final int WRITE_STRING          = 0x01;
    private final int WRITE_UINT16          = 0x02;
    private final int WRITE_ARRAY           = 0x03;

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
    //private List<BluetoothGattCharacteristic> ReadQueue;

    private volatile boolean requestToReRead = false;
    private volatile boolean requestToSet = false;
    private volatile boolean requestToBond = false;
    private volatile boolean isBonded = false;

    private volatile boolean needsToSync = false;
    private int whatToSync = 0;

    private volatile boolean needsToReConnect = false;

    private String mugAddress = "F8:8A:5E:54:19:72";
    private String macToBond = "";

//    private boolean mugFound = false;
    private volatile boolean mugGattConnected = false;
    private volatile boolean mugGattDiscovered = false;
    private volatile boolean mugGattCharsRead = false;
    private volatile boolean mugGattCharWritten = false;
    private volatile boolean mugGattDescriptorWritten = false;
    private volatile boolean gattRSSIReady = false;
    private volatile boolean gattMTUUpdated = false;

    private boolean btIsEnabled = false;

//    private int charIdxDateTime = 0;
//    private int charIdxLastRecord = 0;
//    private int charIdxParameters = 0;
//    private int charIdxEvents = 0;

    long tsLongLast = 0;
    long tsWaitBTLastTime = 0;

    int widgetId = 0;
    int task;

    ArrayList<SolarEnergyMeterParameters> semPList;
    int semPListIdx = 0;

    SolarEnergyMeterParameters semParametersToSet = null;

    ArrayList <Bluetooth.BLECharacteristic> bleCharacteristics;

    public Boolean isRunning() {
        return isRunning;
    }

    public void setRequestToReRead() {
        requestToReRead = true;
    }

    public void setRequestToSetParameters(SolarEnergyMeterParameters sepParameters) {
        semParametersToSet = sepParameters;
        requestToSet = true;
    }

    public void setRequestToBond(String mac) {
        macToBond = mac;
        requestToBond = true;
    }

    private boolean startTask = false;

    public void setStartTask() {
        startTask = true;
    }

    public SyncThread(Context context, String macAddress, int widgetId, int task) {

        semPList = new ArrayList<>();
        semPList.add(new SolarEnergyMeterParameters(macAddress, null, null, null, null, null, null, null, null, null, null, null, null, null, SolarEnergyMeterParameters.TYPE_PAIRED, false, null, null));
        semPListIdx = 0;

        this.appContext = context;

        mugAddress = macAddress;
        this.widgetId = widgetId;
        this.task = task;

        if (btManager == null)
            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btAdapter == null) btAdapter = btManager.getAdapter();

        context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public SyncThread(Context context, ArrayList<SolarEnergyMeterParameters> semParameters, int task) {

        this.appContext = context;

        this.semPList = semParameters;
        this.widgetId = 0;
        this.task = task;
        this.startTask = false;

        semPListIdx = 0;

        if (btManager == null)
            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btAdapter == null) btAdapter = btManager.getAdapter();

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
        //whatToSync = (WTS_MUG_NAME | WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_MUG_COLOR | WTS_SET_NOTIFICATION);
        whatToSync = /*WTS_DATETIME |*/ WTS_LAST_LOG_REC /*| WTS_PARAMETERS*/ | WTS_SET_NOTIFICATION;
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
//                STEP_PAIRED_INFO,
                STEP_BT_START_DISCOVERY,
                STEP_BT_DISCOVERING,
                STEP_BT_STOP_DISCOVERY,
                STEP_BT_WAITING_FOR_BOND
        };
        psIdx = 0;
        step = processSteps[psIdx];

        //whatToSync = (WTS_CURRENT_TEMP | WTS_TARGET_TEMP | WTS_BATTERY | WTS_LIQUID_STATE | WTS_MUG_COLOR | WTS_MUG_NAME | WTS_SET_NOTIFICATION);
        whatToSync = (WTS_DATETIME | WTS_SET_NOTIFICATION);
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


    @SuppressLint("MissingPermission")
    @Override
    public void run() {

        bleCharacteristics = new ArrayList<Bluetooth.BLECharacteristic>();
        bleCharacteristics.add(new Bluetooth.BLECharacteristic(null, UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),  "DateTime", Bluetooth.BLECharacteristic.optionRead | Bluetooth.BLECharacteristic.optionWrite, WTS_DATETIME) );
        bleCharacteristics.add(new Bluetooth.BLECharacteristic(null, UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"),  "LastRecord", Bluetooth.BLECharacteristic.optionRead, WTS_LAST_LOG_REC) );
        bleCharacteristics.add(new Bluetooth.BLECharacteristic(null, UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"),  "DeviceSettings", Bluetooth.BLECharacteristic.optionRead | Bluetooth.BLECharacteristic.optionWrite, WTS_PARAMETERS) );
        bleCharacteristics.add(new Bluetooth.BLECharacteristic(null, UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"),  "EventsNotifications", Bluetooth.BLECharacteristic.optionNotification, WTS_SET_NOTIFICATION) );


        int pauseLength = 300;

        IntentFilter filter;
        EventsTimer timeToConnect = null;
        //EventsTimer timeToReconnect = null;

        isRunning = true;
        long id = Thread.currentThread().getId();
        //Log.d("THREAD", String.format("Started #%d", id));
        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Started #%d", id), true);

        switch (task) {
            case TASK_DISCOVER:
                discoverInit();
                break;
            case TASK_GET_PAIRED_INFO:
                //pairedInfoInit();
                step = STEP_WAITING_FOR_TASK;
                break;
            case TASK_ELEMENT_THREAD:
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

        for (; ; ) {
            if (onTheGo) {
                switch (step) {
                    //--------------------------------------------------------------------------
                    case STEP_WAITING_FOR_TASK:

                        if (startTask) {
                            startTask = false;
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Task %d has started", task), true);
                            switch (task) {
                                case TASK_GET_PAIRED_INFO:
                                    pairedInfoInit();
                                    break;
                            }
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
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
                        if (mugGatt != null) mugGatt = null;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        long timeNow = System.currentTimeMillis() / 1000;
                        if ((timeNow - tsWaitBTLastTime) > 15 * 60) {
                            MLogger.logToFile(appContext, "service.txt", "TH : BT is disabled", true);
                            tsWaitBTLastTime = timeNow;
                            updateWidget();
                        }

                        if (btIsEnabled) {
                            tsWaitBTLastTime = 0;
                            syncInit();
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is ON - Sync enabling", true);
                        }

                        break;
                    case STEP_IDLE:

                        if (checkStop()) {
                            step = STEP_QUIT;
                            break;
                        }
                        if (!checkBTEnabled()) {
                            break;
                        }

                        long tsLongNow = System.currentTimeMillis() / 1000;
                        if ((tsLongNow - tsLongLast) > 15 * 60) {

                            String status = "unknown";
                            if(mugBleDevice != null) {
                                if (isMugGattConnected(mugBleDevice) && (mugGatt != null)) {
                                    status = "connected";
                                }
                                else status = "not connected";
                            }

                            MLogger.logToFile(appContext, "service.txt", "TH : Alive " + mugBleDevice.getAddress() + " " + status, true);
                            tsLongLast = tsLongNow;
                        }

                        if (needsToReConnect) {
                            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) %s GATT was disconnected -> need to restart everything", id, mugAddress), true);
                            mugGatt.close();
                            syncInit();
                            needsToReConnect = false;
                            break;
                        }

                        if (!mugGattConnected) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }

                        if (requestToSet) {

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

                        if (requestToReRead) {
                            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Request to reread %s", id, mugAddress), true);
                            syncInit();
                            requestToReRead = false;
                        }

                        if (needsToSync) {
                            id = Thread.currentThread().getId();
                            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Needs to sync " + mugAddress, id), true);

                            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

                            if (connectedBLEDevices.contains(mugBleDevice) == false) {
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
                        } else {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
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

                        if (btManager == null)
                            btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
                        if (btAdapter == null) btAdapter = btManager.getAdapter();

                        if (btAdapter == null) {
                            // BT isn't supported
                            btIsEnabled = false;
                            step = STEP_WAIT_BT;
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is NULL", true);

                            break;
                        } else if (!btAdapter.isEnabled()) {
                            btIsEnabled = false;
                            step = STEP_WAIT_BT;
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is Disabled", true);

                            break;
                        } else {
                            btIsEnabled = true;
                        }

                        if (mugBleDevice == null)
                            mugBleDevice = btAdapter.getRemoteDevice(mugAddress);


                        if (checkStop() || !checkBTEnabled()) {
                            break;
                        }

                        if (isMugGattConnected(mugBleDevice) && (mugGatt != null)) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Mug is already connected " + mugAddress, true);
                        } else {

                            MLogger.logToFile(appContext, "service.txt", "TH : Mug isn't connected " + mugAddress, true);

                            mugGattConnected = false;
                            mugGattDiscovered = false;

                            try {
                                if (mugGatt == null) {
                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null. connectGATT..."), true);
                                    mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);
                                    refreshDeviceCache(mugGatt);

                                } else {
                                    MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT isn't null. connectGATT..."), true);
                                    //mugGatt.connect();
                                    mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);
                                    refreshDeviceCache(mugGatt);
                                }

                            } catch (Exception e) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connect exception %s", e.toString()), true);
                            }

                            (timeToConnect = new EventsTimer()).start(10000);

                            MLogger.logToFile(appContext, "service.txt",
                                    String.format(Locale.getDefault(), "TH : [ %d ] Waiting for GATT connected %s", Thread.currentThread().getId(), mugAddress), true);

                            int logMsgCntr = 0;
                            while (!mugGattConnected && !checkStop() && checkBTEnabled()) {
//                                if (timeToConnect.isReady())
//                                    threadSleep(10000);             // increase sleep time if Mug wasn't found in first 10 sec
//                                else threadSleep(1000);    //

                                threadSleep(100);

                                if(logMsgCntr >= 100) { // log every 10 sec
                                    MLogger.logToFile(appContext, "service.txt",
                                            String.format(Locale.getDefault(), "TH : [ %d ] Waiting for GATT connected %s", Thread.currentThread().getId(), mugAddress), true);
                                }
                            }
                            timeToConnect = null;
                            if (!mugGattConnected) {
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

                        if (checkStop()) {
                            break;
                        }
                        if (!checkBTEnabled()) {
                            break;
                        }
                        //if( !mugGattConnected ) { psIdx = 0; step = processSteps[psIdx]; break; }

                        MLogger.logToFile(appContext, "service.txt", String.format("TH : MTU setting..."), true);

                        if(!setMTU(48)) break;
                        if(!mugGattConnected) break;

                        MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT Connected. Service discovering..."), true);

                        mugGatt.discoverServices();

                        while (!mugGattDiscovered) {
                            if (checkStop()) {
                                break;
                            }
                            if (!checkBTEnabled()) {
                                break;
                            }
                            if(!mugGattConnected) break;
                        }
                        if (checkStop()) {
                            break;
                        }
                        if (!checkBTEnabled()) {
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
                     //   if (ReadQueue != null) {
                        //   MLogger.logToFile(appContext, "service.txt", "TH : Queue not empty", true);

                            if (mugGattConnected == false) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Queue not empty BUT GATT disconnected. Cancel reading.", true);
                                whatToSync = 0;
                                needsToSync = false;
                                step = processSteps[++psIdx];   // ?? step reconnect
                                break;
                            }

//                            if(!setMTU(48)) break;
//                            if(!mugGattConnected) break;
                            if(!requestRSSI()) break;
                            if(!mugGattConnected) break;
                            //---------------------------------------------------------------------
                            int toDoMask = 0x01;
                            boolean opIsOK = true;
                            while( (toDoMask <= WTS_LAST) && opIsOK ) {
                                if((whatToSync & toDoMask) != 0 ) {
                                    opIsOK = readCharacteristic(toDoMask);
                                    if(opIsOK) updateWidget();
                                }
                                toDoMask <<= 1;
                            }
                            //updateWidget();

                            if(checkStop() || !checkBTEnabled() || !mugGattConnected )  { break; }

                            toDoMask = 0x01;
                            opIsOK = true;
                            while( (toDoMask <= WTS_LAST) && opIsOK ) {
                                if((whatToSync & toDoMask) != 0 ) {
                                    opIsOK = setNotification(toDoMask);
                                }
                                toDoMask <<= 1;
                            }

                            if(checkStop() || !checkBTEnabled() || !mugGattConnected )  { updateWidget(); break; }

                            if(whatToSync == 0) {
                                needsToSync = false;
                                MLogger.logToFile(appContext, "service.txt", "TH : sync is done", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : still need to sync", true);
                            }

                            if(needsToSync == false) step = processSteps[++psIdx];

                            //---------------------------------------------------------------------
                            /*
                            //---- read DateTime --------------------------------------------------
                            if ((whatToSync & WTS_DATETIME) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading DateTime", true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(charIdxDateTime));   // DateTime
                                int timeout = 5000 / 10;  //5 sec
                                while (!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Reading DateTime TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if (timeout > 0) {
                                    whatToSync &= ~WTS_DATETIME;
                                    attempts = maxAttempts;
                                } else {
                                    if (attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading DateTime Attempts left: %d", attempts), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading DateTime UNABLE to read"), true);
                                        whatToSync &= ~WTS_DATETIME;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }
                            //---- read DateTime --------------------------------------------------
                            if (checkStop()) {
                                break;
                            }
                            if (!checkBTEnabled()) {
                                break;
                            }
                            if(!mugGattConnected) break;

                            //---- read Last Log record -----------------------------------------------
                            if ((whatToSync & WTS_LAST_LOG_REC) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading LLR", true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(charIdxLastRecord));
                                int timeout = 5000 / 10;  //5 sec
                                while (!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Reading LLR TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if (timeout > 0) {
                                    whatToSync &= ~WTS_LAST_LOG_REC;
                                    attempts = maxAttempts;
                                } else {
                                    if (attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading LLR Attempts left: %d", attempts), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading LLR UNABLE to read"), true);
                                        whatToSync &= ~WTS_LAST_LOG_REC;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if (checkStop()) {
                                break;
                            }
                            if (!checkBTEnabled()) {
                                break;
                            }
                            if(!mugGattConnected) break;
                            //---- Last Log record -----------------------------------------------


                            //---- Reading Parameters ---------------------------------------------
                            if ((whatToSync & WTS_PARAMETERS) != 0) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Parameters " + mugAddress, true);
                                mugGattCharsRead = false;
                                mugGatt.readCharacteristic(ReadQueue.get(charIdxParameters));
                                int timeout = 5000 / 10;  //5 sec
                                while (!mugGattCharsRead) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (--timeout <= 0) {
                                        MLogger.logToFile(appContext, "service.txt",
                                                String.format(Locale.getDefault(),
                                                        "TH : Reading Parameters TIMEOUT %d", timeout), true);
                                        break;
                                    }
                                }
                                if (timeout > 0) {
                                    whatToSync &= ~WTS_PARAMETERS;
                                    attempts = maxAttempts;
                                } else {
                                    if (attempts > 0) {
                                        attempts--;
                                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Reading Parameters Attempts left: %d %s", attempts, mugAddress), true);
                                        break;
                                    } else {
                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading Parameters UNABLE to read %s", mugAddress), true);
                                        whatToSync &= ~WTS_PARAMETERS;
                                        attempts = maxAttempts;
                                    }
                                }
                                updateWidget();
                            }

                            if (checkStop()) {
                                break;
                            }
                            if (!checkBTEnabled()) {
                                break;
                            }
                            if(!mugGattConnected) break;
                            //---- End of Reading Parameters --------------------------------------

                            if(!mugGattConnected) break;
                            if ((whatToSync & WTS_SET_NOTIFICATION) != 0) {
                                mugGattDescriptorWritten = false;
                                mugGatt.setCharacteristicNotification(ReadQueue.get(charIdxEvents), true);

                                UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                                BluetoothGattDescriptor desc = ReadQueue.get(charIdxEvents).getDescriptor(CONFIG_DESCRIPTOR);
                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mugGatt.writeDescriptor(desc);

                                while (!mugGattDescriptorWritten) ;
                                whatToSync &= ~WTS_SET_NOTIFICATION;

                                MLogger.logToFile(appContext, "service.txt", "TH : Notifictions set...", true);
                            }

                            if (whatToSync == 0) {
                                needsToSync = false;
                                MLogger.logToFile(appContext, "service.txt", "TH : sync is done", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : still need to sync", true);
                            }
                        } else {
                            MLogger.logToFile(appContext, "service.txt", "TH : ReadQueue is null", true);
                        }

                        if (needsToSync == false) step = processSteps[++psIdx];
*/
                        break;
                    //---- Write chracteristics ---------------------------------------------------
                    case STEP_BLE_WRITE_CHAR:
                        if(checkStop() || !checkBTEnabled() )  { break; }
                        if(!mugGattConnected) { step = processSteps[++psIdx]; break; }

                        if ( semParametersToSet == null) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Writing - NOTHING", true);
                            step = processSteps[++psIdx];
                            break;
                        }
                        //---- DateTime -----------------------------------------------------------
                        Long dateTime = semParametersToSet.getDateTime();
                        if( dateTime != null ) {
                            String timeString = new SimpleDateFormat("yy-MM-dd h:mm:ss").format(new Date(dateTime * 1000));
                            MLogger.logToFile(null, "", String.format("TH : Writing DateTime: #%06X %s", dateTime, timeString), true);
                            if(writeCharacteristic(WTS_DATETIME, new byte[]{(byte) (dateTime >>> 24), (byte) (dateTime >>> 16), (byte) (dateTime >> 8), (byte) (dateTime & 0xFF)}, WRITE_ARRAY))
                                MLogger.logToFile(null, "", String.format("TH : Writing DateTime %08X %s - OK", dateTime, timeString), true);
                            else
                                MLogger.logToFile(null, "", "TH : Writing DatTime - ERROR", true);
                        }

                        if(checkStop() || !checkBTEnabled() )  { break; }
                        if(!mugGattConnected) { step = processSteps[++psIdx]; break; }

                        //---- DateTime -----------------------------------------------------------
//                        attempts = maxAttempts;
//                        Long dateTime = semParametersToSet.getDateTime();
//                        if (dateTime != null) {
//
//                            while (attempts > 0) {
//
//                                MLogger.logToFile(appContext, "service.txt", String.format("TH : Writing DateTime %08X %s ",
//                                                dateTime,
//                                                new SimpleDateFormat("yy-MM-dd h:mm:ss").format(new Date(dateTime * 1000))),
//                                        true);
//                                mugGattCharWritten = false;
//
//                                ReadQueue.get(charIdxDateTime).setValue(new byte[]{(byte) (dateTime >>> 24), (byte) (dateTime >>> 16), (byte) (dateTime >> 8), (byte) (dateTime & 0xFF)});
//                                ReadQueue.get(charIdxDateTime).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                mugGatt.writeCharacteristic(ReadQueue.get(charIdxDateTime));
//
//                                int timeout = 5000 / 10;  //5 sec
//                                while (!mugGattCharWritten) {
//                                    try {
//                                        Thread.sleep(10);
//                                    } catch (InterruptedException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                    if (--timeout <= 0) {
//                                        MLogger.logToFile(appContext, "service.txt",
//                                                String.format(Locale.getDefault(),
//                                                        "TH : Writing DateTime TIMEOUT %d", timeout), true);
//                                        break;
//                                    }
//                                }
//                                if (timeout > 0) {
//
//                                    MLogger.logToFile(appContext, "service.txt", "TH : Writing DateTime - DONE", true);
//                                    attempts = maxAttempts;
//                                    break;
//                                } else {
//                                    if (attempts > 0) {
//                                        attempts--;
//                                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Writing DateTime Attempts left: %d", attempts), true);
//                                    } else {
//                                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Reading DateTime UNABLE to write"), true);
//                                    }
//                                }
//                            }
//                        }
                        //---- DateTime ----------------------------------------------------------

                        //---- Load parameters ----------------------------------------------------
                        Integer loadOnTime = semParametersToSet.getLoadOnTime();
                        Integer loadOnPeriod = semParametersToSet.getLoadOnPeriod();
                        Integer loadCurrent  = semParametersToSet.getLoadCurrent();

                        if ((loadOnTime != null) && (loadOnPeriod != null) && (loadCurrent != null) ){
                            byte[] a = new byte[32];
                            a[0] = 0x01; a[1] = loadOnTime.byteValue(); a[2] = loadOnPeriod.byteValue(); a[3] = loadCurrent.byteValue();

                            MLogger.logToFile(null, "", String.format("TH : Writing Settings: Load current: %dmA Duration: %ds Period: %ds", a[3], a[1], a[2]), true);
                            if(writeCharacteristic(WTS_PARAMETERS, a, WRITE_ARRAY))
                                MLogger.logToFile(null, "", "TH : Writing Settings - OK", true);
                            else
                                MLogger.logToFile(null, "", "TH : Writing Settings - ERROR", true);
                        }

                        if(checkStop() || !checkBTEnabled() )  { break; }
                        if(!mugGattConnected) { step = processSteps[++psIdx]; break; }
                        //---- Load parameters ----------------------------------------------------

                        //---- Load parameters ----------------------------------------------------
//                        attempts = maxAttempts;
//                        Integer loadOnTime = semParametersToSet.getLoadOnTime();
//                        Integer loadOnPeriod = semParametersToSet.getLoadOnPeriod();
//                        Integer loadCurrent  = semParametersToSet.getLoadCurrent();
//                        if ((loadOnTime != null) && (loadOnPeriod != null) && (loadCurrent != null) ){
//
//                            while (attempts > 0) {
//
//                                MLogger.logToFile(appContext, "service.txt",
//                                        String.format("TH : Writing Load Parameters: ON Time: %d ms, Period %d ms Current: %dmA", loadOnTime, loadOnPeriod, loadCurrent), true);
//
//                                mugGattCharWritten = false;
//
//                                byte[] a = new byte[32];
//                                a[0] = 0x01; a[1] = loadOnTime.byteValue(); a[2] = loadOnPeriod.byteValue(); a[3] = loadCurrent.byteValue();
//                                ReadQueue.get(charIdxParameters).setValue(a );
//                                ReadQueue.get(charIdxParameters).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                mugGatt.writeCharacteristic(ReadQueue.get(charIdxParameters));
//
//                                int timeout = 5000 / 10;  //5 sec
//                                while (!mugGattCharWritten) {
//                                    try {
//                                        Thread.sleep(10);
//                                    } catch (InterruptedException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                    if (--timeout <= 0) {
//                                        MLogger.logToFile(appContext, "service.txt",
//                                                String.format(Locale.getDefault(),
//                                                        "TH : Writing Load Parameters TIMEOUT %d", timeout), true);
//                                        break;
//                                    }
//                                }
//                                if (timeout > 0) {
//                                    MLogger.logToFile(appContext, "service.txt", "TH : Writing Load Parameters - DONE", true);
//                                    attempts = maxAttempts;
//                                    break;
//                                } else {
//                                    if (attempts > 0) {
//                                        attempts--;
//                                    }
//                                }
//                            }
//                        }


                        sendMessage(SyncService.MSG_SET_PARAMETERS_DONE);
                        step = processSteps[++psIdx];

                        break;


                    //---- End of Write chracteristics --------------------------------------------
                    case STEP_STANDBY:

                        String status = "unknown";
                        if(mugBleDevice != null) {
                            if (isMugGattConnected(mugBleDevice) && (mugGatt != null)) {
                                status = "connected";
                            }
                            else status = "not connected";
                        }
                        tsLongLast = 0;
                        MLogger.logToFile(appContext, "service.txt", "TH : Standby - " + status, true);
                        step = processSteps[++psIdx];
                        break;
                    case STEP_QUIT:
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : QUIT !!! %s", mugAddress), true);

                        onTheGo = false;

                        if (isMugGattConnected(mugBleDevice)) {
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

                        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

                        btAdapter.startDiscovery();

                        step = processSteps[++psIdx];

                        break;
                    case STEP_BT_DISCOVERING:
                        if (Thread.currentThread().isInterrupted()) {
                            step = processSteps[++psIdx];
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Terminating discovering"), true);
                            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
                            //while(btAdapter.isDiscovering());
                            //MLogger.logToFile(appContext, "service.txt", String.format("TH : Discovering stopping"), true);
                        }

                        if (requestToBond && !"".equals(macToBond)) {
                            step = processSteps[++psIdx];
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Request to bond. Terminating discovering"), true);
                            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();


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

                        if (requestToBond && !"".equals(macToBond)) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Request to bond %s", macToBond), true);
                            requestToBond = false;
                            step = STEP_BT_WAITING_FOR_BOND;
                            BluetoothDevice bleDevice = btAdapter.getRemoteDevice(macToBond);
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

                        while (!isBonded) {
                        }
                        ;
                        MLogger.logToFile(appContext, "service.txt", String.format("TH : Bonded %s", macToBond), true);
                        updateBTDevices("WWWW", macToBond, DISCOVERING_PAIRED);
                        onTheGo = false;

                        break;
                    //-------------------------------------------------------------------------
                    /*
                    case STEP_PAIRED_INFO:
                        if (btAdapter == null) {
                            // BT isn't supported
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is NULL", true);
                            onTheGo = false;
                            // add error processing
                            break;
                        } else if (!btAdapter.isEnabled()) {
                            MLogger.logToFile(appContext, "service.txt", "TH : Adapter is Disabled", true);
                            onTheGo = false;
                            // add error processing
                            break;
                        }

                        while (semPListIdx < semPList.size()) {

                            mugBleDevice = btAdapter.getRemoteDevice(semPList.get(semPListIdx).getMACAddress());

                            MLogger.logToFile(appContext,
                                    "service.txt",
                                    String.format(Locale.getDefault(),
                                            "TH : collecting data for %s (%d/%d)",
                                            semPList.get(semPListIdx).getMACAddress(),
                                            semPListIdx,
                                            semPList.size() - 1), true);

                            mugGattConnected = false;
                            mugGattDiscovered = false;

                            try {

                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT is null. connectGATT..."), true);
                                mugGatt = mugBleDevice.connectGatt(appContext, true, bleGattCallback);

                            } catch (Exception e) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT connect exception %s", e.toString()), true);
                            }

                            (timeToConnect = new EventsTimer()).start(5000);

                            while (!mugGattConnected && !Thread.currentThread().isInterrupted()) {

                                if (timeToConnect.isReady()) {
                                    MLogger.logToFile(appContext, "service.txt",
                                            String.format(Locale.getDefault(), "TH : [ %d ] GATT connection timeout %s",
                                                    Thread.currentThread().getId(),
                                                    semPList.get(semPListIdx).getMACAddress()), true);
                                    updatePairedInfo(false);

                                    break;
                                }
                            }

                            timeToConnect = null;

                            if (Thread.currentThread().isInterrupted()) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : interrupted"), true);
                                break;
                            }

                            if (!mugGattConnected) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : switching to next device"), true);
                                semPListIdx++;
                                continue;
                            }

                            MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT Connected. Service discovering..."), true);

                            mugGatt.discoverServices();

                            while (!mugGattDiscovered && !Thread.currentThread().isInterrupted()) {
                            }

                            if (!mugGattDiscovered) {
                                MLogger.logToFile(appContext, "service.txt", String.format("TH : Couldn't discover services, switching to next device"), true);
                            }

                            MLogger.logToFile(appContext, "service.txt", String.format("TH : Services discovered."), true);


                            //----

                            if (readCharacteristic(ReadQueue.get(0))) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Name - OK", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Name - ERR", true);
                            }

                            if (readCharacteristic(ReadQueue.get(1))) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Tcurrent - OK", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Tcurrent - ERR", true);
                            }

                            if (readCharacteristic(ReadQueue.get(2))) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Ttarget - OK", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Ttarget - ERR", true);
                            }

                            if (readCharacteristic(ReadQueue.get(3))) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery - OK", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Battery - ERR", true);
                            }

                            if (readCharacteristic(ReadQueue.get(6))) {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Color - OK", true);
                            } else {
                                MLogger.logToFile(appContext, "service.txt", "TH : Reading Color - ERR", true);
                            }
                            semPList.get(semPListIdx).setType(SolarEnergyMeterParameters.TYPE_PAIRED);
                            updatePairedInfo(false);

                            //------------------------------------------------
                            semPListIdx++;
                        }
                        MLogger.logToFile(appContext, "service.txt", "TH : Paired info - DONE", true);

                        if (Thread.currentThread().isInterrupted()) {
                            MLogger.logToFile(appContext, "service.txt", String.format("TH : paired - interrupted"), true);
                            //updatePairedInfo(true);
                            onTheGo = false;
                        } else {
                            step = processSteps[++psIdx];
                        }
                        break;*/
                    //-------------------------------------------------------------------------
                }
            } else break;
        }
        MLogger.logToFile(appContext, "service.txt", "TH : Thread EXIT", true);
    }

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connected %s (%d)", gatt.getDevice().getAddress(), step), true);
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                //gatt.requestMtu(100);
                //gatt.requestMtu(50);

                mugGattConnected = true;
                semPList.get(semPListIdx).setConnected(true);

                if(step == STEP_IDLE) {
                    requestToReRead = true;
                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connected - ReRead %s (%d)", gatt.getDevice().getAddress(), step), true);
                }

            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Disconnected %s (%d) ", gatt.getDevice().getAddress(), step), true);
                mugGattConnected = false;
                semPList.get(semPListIdx).setConnected(false);
                if((step == STEP_IDLE) || (step == STEP_STANDBY) || (step == STEP_BLE_READ_CHAR)) { needsToReConnect = true; }
            } else if(newState == BluetoothProfile.STATE_CONNECTING) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Connecting %s (%d) ", gatt.getDevice().getAddress(), step), true);
            } else if(newState == BluetoothProfile.STATE_DISCONNECTING) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT Disconnecting %s (%d)", gatt.getDevice().getAddress(), step), true);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT MTU  %s (%d)", gatt.getDevice().getAddress(), mtu), true);

                /*
                mugGattConnected = true;
                semPList.get(semPListIdx).setConnected(true);

                if(step == STEP_IDLE) {
                    requestToReRead = true;
                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT MTU Changed - ReRead %s (%d)", gatt.getDevice().getAddress(), step), true);
                }*/

            }
            else {
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: GATT MTU Error %s (%d)", gatt.getDevice().getAddress(), status), true);
            }
            gattMTUUpdated = true;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if(status == BluetoothGatt.GATT_SUCCESS) {
                String s = "";
                List<BluetoothGattService> list = gatt.getServices();

                Integer idx = 0;
                for(int i = 0; i < list.size(); i++) {
                    //s += list.get(i).getUuid().toString().toUpperCase() + "\n";

                    if(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb").equals(list.get(i).getUuid())) {
                        List<BluetoothGattCharacteristic> charList = list.get(i).getCharacteristics();

                        for(int k = 0; k < charList.size(); k++) {
                            for(int m = 0; m < bleCharacteristics.size(); m++) {
                                if(bleCharacteristics.get(m).getUUID().equals(charList.get(k).getUuid())) {
                                    bleCharacteristics.get(m).setGattCharacteristic(charList.get(k));
                                    MLogger.logToFile(null, "", String.format(Locale.getDefault(), "BLE: Discovered %s characteristic %s",
                                            bleCharacteristics.get(m).getDescription(),
                                            bleCharacteristics.get(m).getUUID().toString().toUpperCase()), true);
                                    s += "added: " + bleCharacteristics.get(m).getUUID().toString().toUpperCase() + " : " + bleCharacteristics.get(m).getDescription() + "\n";

//                                    List<BluetoothGattDescriptor> descrList = charList.get(k).getDescriptors();
//                                    if(descrList.size() != 0) {
//                                        for(int j = 0; j < descrList.size(); j++) {
//                                            s += String.format("*%s\n", descrList.get(j).getUuid().toString());
//                                        }
//                                    }

                                    break;
                                }
                            }
//                            MLogger.logToFile(null, "", String.format(Locale.getDefault(), "BLE: %s", s), true);
                        }
                        MLogger.logToFile(null, "", String.format(Locale.getDefault(), "BLE: %s", s), true);

//                        ReadQueue = new ArrayList<>(0x20);
//                        s += "yes\n";
//                        List<BluetoothGattCharacteristic> charList = list.get(i).getCharacteristics();
//                        for(int k = 0; k < charList.size(); k++) {
//                            if(charList.get(k).getUuid().equals(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))) {   //  DateTime
//                                ReadQueue.add(charList.get(k));
//                                charIdxDateTime = idx++;
//                            }
//
//                            if(charList.get(k).getUuid().equals(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))) {   //  Last Record
//                                ReadQueue.add(charList.get(k));
//                                charIdxLastRecord = idx++;
//                            }
//
//                            if(charList.get(k).getUuid().equals(UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"))) {  //  Events/Notifications
//                                ReadQueue.add(charList.get(k));
//                                charIdxParameters = idx++;
//                            }
//
//                            if(charList.get(k).getUuid().equals(UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"))) {  //  Events/Notifications
//                                ReadQueue.add(charList.get(k));
//                                charIdxEvents = idx++;
//                            }
//
//                            List<BluetoothGattDescriptor> descrList = charList.get(k).getDescriptors();
//                            if(descrList.size() != 0) {
//                                s += String.format("\n\n#%d# %s %d", k, charList.get(k).getUuid().toString(), descrList.size());
//                                for(int j = 0; j < descrList.size(); j++) {
//                                    s += String.format("\n*%s", descrList.get(j).getUuid().toString());
//                                }
//                            }
//                        }
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
                //Log.w(TAG, "onServicesDiscovered received: " + status);
                MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : onServicesDiscovered %s", status), true);
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

                long id = Thread.currentThread().getId();

                //---- DateTime -------------------------------------------------------------------
                if(characteristic.getUuid().equals(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))) {  // DateTime

                    //long dateTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                    long dateTime;
                    byte[] t = characteristic.getValue();
                    dateTime = t[0] & 0xFF; dateTime <<=8;
                    dateTime |= t[1] & 0xFF; dateTime <<=8;
                    dateTime |= t[2] & 0xFF; dateTime <<=8;
                    dateTime |= t[3] & 0xFF;

                    semPList.get(semPListIdx).setDateTime(dateTime);
                    String dateString = new SimpleDateFormat("MM/dd/yyyy").format(new Date(dateTime * 1000));
                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : SoH's DateTime: %08X", dateTime), true);
                }
                //---- DateTime -------------------------------------------------------------------

                //---- Last Log Record ------------------------------------------------------------
                if(characteristic.getUuid().equals(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))) {
//                    long timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
//                    long vBatMain = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4);
//                    long vBatBkp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6);
//                    long tCPU = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 8);
                    byte c[] = characteristic.getValue();
                    //long vBatBkp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 12);

                    long dateTime = (long)c[0] & 0xFF; dateTime <<= 8;
                    dateTime |= (long)c[1] & 0xFF; dateTime <<= 8;
                    dateTime |= (long)c[2] & 0xFF; dateTime <<= 8;
                    dateTime |= (long)c[3] & 0xFF;


                    long vBatBkp = (long)c[18] & 0xFF;
                    vBatBkp <<= 8;
                    vBatBkp |= (long)c[19] & 0xFF;

                    long vBat = (long)c[6] & 0xFF;
                    vBat <<= 8;
                    vBat |= (long)c[7] & 0xFF;


                    long iBat = (long)c[8];
                    iBat <<= 8;
                    iBat |= (long)c[9] & 0xFF;

                    long wBat = (long)c[10];
                    wBat <<= 8;
                    wBat |= (long)c[11] & 0xFF;

                    long vLoad = (long)c[12] & 0xFF;
                    vLoad <<= 8;
                    vLoad |= (long)c[13] & 0xFF;

                    long iLoad = (long)c[14];
                    iLoad <<= 8;
                    iLoad |= (long)c[15] & 0xFF;

                    long eLoad = (long)c[16];
                    eLoad <<= 8;
                    eLoad |= (long)c[17] & 0xFF;

                    long tc = (long)c[22];
                    tc <<= 8;
                    tc |= (long)c[23] & 0xFF;

                    semPList.get(semPListIdx).setDateTime(dateTime);

                    semPList.get(semPListIdx).setCPUBatteryCharge((int)vBatBkp);
                    semPList.get(semPListIdx).setBatteryVolts((int)vBat);
                    semPList.get(semPListIdx).setBatteryAmps((int)iBat);
                    semPList.get(semPListIdx).setBatterymW((int)wBat);

                    semPList.get(semPListIdx).setLoadVolts((int)vLoad);
                    semPList.get(semPListIdx).setLoadAmps((int)iLoad);
                    semPList.get(semPListIdx).setLoadmW((int)eLoad);

                    semPList.get(semPListIdx).setTemperature((int)tc);

                    //gatt.readRemoteRssi();

                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d)Bat CPU: %dmV Bat: %dmV %dmA", id, vBatBkp, vBat, iBat), true);
                }
                //---- Last Log Record ------------------------------------------------------------

//                if(characteristic.getUuid().equals(UUID.fromString("fc540001-236c-4c94-8fa9-944a3e5353fa"))) {
//                    String s = characteristic.getStringValue(0);
//                    semPList.get(semPListIdx).setMugName(s);
//                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Mug name: %s", s), true);
//                }
//                if(characteristic.getUuid().equals(UUID.fromString("fc540002-236c-4c94-8fa9-944a3e5353fa"))) {
//                    //    String s = characteristic.getStringValue(0);
//                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
//                    long id = Thread.currentThread().getId();
//                    semPList.get(semPListIdx).setCurrentTemperature((float) t / 100.0f);
//
//                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d)Current temp: %.2fC", id, (float) t / 100.00), true);
//                }
//                if(characteristic.getUuid().equals(UUID.fromString("fc540003-236c-4c94-8fa9-944a3e5353fa"))) {
//                    //    String s = characteristic.getStringValue(0);
//                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
//                    long id = Thread.currentThread().getId();
//                    semPList.get(semPListIdx).setTargetTemperature(t);
//
//                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d)Target temp: %.2fC", id, (float) t / 100.00), true);
//
//                }
                if(characteristic.getUuid().equals(UUID.fromString("fc540007-236c-4c94-8fa9-944a3e5353fa"))) {
                    //    String s = characteristic.getStringValue(0);
                    String charging = "no";
                    int p = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    int c = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                    int t = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
                    int v = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
                    if(c == 1) charging = "yes";

                    semPList.get(semPListIdx).setCPUBatteryCharge(p);

                    MLogger.logToFile(appContext, "service.txt",
                            String.format(Locale.getDefault(), "TH : Battery: %d%% charging: %s %.2fC %d", p, charging, (float) t / 100.00, v),
                            true);
                }

//                if(characteristic.getUuid().equals(UUID.fromString("fc540008-236c-4c94-8fa9-944a3e5353fa"))) {
//                    byte[] state = characteristic.getValue();
//                    String[] states = new String[]{"NA", "Empty", "Filling", "Unknown", "Cooling", "Heating", "OK"};
//                    if(state[0] >= states.length) state[0] = 0;
//                }

                if(characteristic.getUuid().equals(UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"))) {

                    byte[] tmp = characteristic.getValue();

                    semPList.get(semPListIdx).setLoadOnTime((int) tmp[1]);
                    semPList.get(semPListIdx).setLoadOnPeriod((int) tmp[2]);
                    semPList.get(semPListIdx).setLoadCurrent((int) tmp[3]);

                    MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : (#%d) Load: %dms %dms %dmA", tmp[1], tmp[2], tmp[3] ), true);
                }

                mugGattCharsRead = true;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: onCharacteristicWrite: %s  written", characteristic.getUuid()), true);

            // DateTime
            if(characteristic.getUuid().equals(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))) {
                mugGattCharWritten = true;
            }
            // Parameters
            if(characteristic.getUuid().equals(UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"))) {
                mugGattCharWritten = true;
            }
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {

            byte[] data = characteristic.getValue();

            long id = Thread.currentThread().getId();
            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: [%d][%d]Notification %02X %02X G:%s T:%s", id, step, data[0], data[1], gatt.getDevice().getAddress(), mugAddress), true);

            if(needsToSync == false) {
                switch(data[0]) {
                    case 0x01:
                        whatToSync |= WTS_LAST_LOG_REC;
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Last log record notification"), true);
                        break;
//                    case 0x04:
//                        // whatToSync |= WTS_TARGET_TEMP;
//                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Target temperature notification"), true);
//                        break;
//                    case 0x05:
//                        whatToSync |= WTS_CURRENT_TEMP;
//                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "BLE: Current temperature notification"), true);
//                        break;
//                    case 0x08:
//                        //whatToSync |= WTS_LIQUID_STATE;
//                        break;

                }
                if(whatToSync != 0) needsToSync = true;
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {

            MLogger.logToFile(appContext, "service.txt", "BLE: onDescriptorWrite", true);
            mugGattDescriptorWritten = true;

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                MLogger.logToFile(appContext, "service.txt", String.format("BLE: RSSI %d", rssi), true);
                semPList.get(semPListIdx).setRSSI(rssi);
                gattRSSIReady = true;
            }
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
                        semPList.get(semPListIdx).setConnected(false);
                        mugGattConnected = false;
                        if(mugGatt == null) {
                            MLogger.logToFile(appContext, "service.txt", "BLE: GATT is NULL", true);
                        }
                        else {
                            MLogger.logToFile(appContext, "service.txt", "BLE: GATT is NOT NULL", true);
                        }
                        return;
                    case BluetoothAdapter.STATE_OFF:
                        MLogger.logToFile(appContext, "service.txt", "BLE: Adapter is OFF", true);
                            if(mugGatt == null) {
                                MLogger.logToFile(appContext, "service.txt", "BLE: GATT is NULL", true);
                            }
                            else {
                                MLogger.logToFile(appContext, "service.txt", "BLE: GATT is NOT NULL", true);
                            }
                    break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        MLogger.logToFile(appContext, "service.txt", "BLE: Adapter is turning ON", true);
                        break;
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

//            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                MLogger.logToFile(appContext, "service.txt", String.format("TH : State changed"), true);

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
//            }

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

    private void updateWidget() {
        String ts = String.valueOf(new SimpleDateFormat("HH:mm:ss:SSS").format((new Date()).getTime()));
        long id = Thread.currentThread().getId();

        if(widgetId != 0) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
            RemoteViews remoteViews = new RemoteViews(appContext.getPackageName(), R.layout.app_widget);

            MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Widget %d update @%s", widgetId, String.format("#%d %s", id, ts)), true);
            remoteViews.setTextViewText(R.id.tv_widgetId, String.format("[%d]", widgetId));
            remoteViews.setTextViewText(R.id.tv_timeStamp, String.format("#%d %s", id, ts));

            if(semPList.get(semPListIdx).getConnected()) {
                if(semPList.get(semPListIdx).getBatteryVolts() != null)
                    //remoteViews.setTextViewText(R.id.tv_vbat, String.format(Locale.getDefault(), "%.02fC", (float)semPList.get(semPListIdx).getBatteryVolts() * 1.25/1000.0 ) );
                    remoteViews.setTextViewText(R.id.tv_vbat, String.format(Locale.getDefault(), "%dmV", semPList.get(semPListIdx).getBatteryVolts() ) );
                if(semPList.get(semPListIdx).getBatteryAmps() != null)
                    remoteViews.setTextViewText(R.id.tv_ibat, String.format(Locale.getDefault(), "%.02fmA", (float)semPList.get(semPListIdx).getBatteryAmps()/100.0));
                if(semPList.get(semPListIdx).getBatterymW() != null)
                    remoteViews.setTextViewText(R.id.tv_wbat, String.format(Locale.getDefault(), "%.02fmAh", (float)semPList.get(semPListIdx).getBatterymW()/100.0));

                if(semPList.get(semPListIdx).getLoadVolts() != null)
                    remoteViews.setTextViewText(R.id.tv_vload, String.format(Locale.getDefault(), "%dmV", semPList.get(semPListIdx).getLoadVolts() ) );
                if(semPList.get(semPListIdx).getLoadAmps() != null)
                    remoteViews.setTextViewText(R.id.tv_iload, String.format(Locale.getDefault(), "%.02fmA", (float)semPList.get(semPListIdx).getLoadAmps()/100.0));
                if(semPList.get(semPListIdx).getLoadmW() != null)
                    remoteViews.setTextViewText(R.id.tv_wload, String.format(Locale.getDefault(), "%.02fmAh", (float)semPList.get(semPListIdx).getLoadmW()/100.0));


                if(semPList.get(semPListIdx).getCPUBatteryCharge() != null)
                    remoteViews.setTextViewText(R.id.tv_battery, String.format(Locale.getDefault(),"%.03fV", semPList.get(semPListIdx).getCPUBatteryCharge()/1000.0f));
                if(semPList.get(semPListIdx).getTemperature() != null)
                    remoteViews.setTextViewText(R.id.tv_currentTemperature, String.format(Locale.getDefault(),"%.2f\u2103", (float)semPList.get(semPListIdx).getTemperature()/100.0f));

                if(semPList.get(semPListIdx).getDateTime() != null) {
                    String dateString = new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(new Date(semPList.get(semPListIdx).getDateTime() * 1000));
                    remoteViews.setTextViewText(R.id.tv_timeStamp, dateString);
                }
                if(semPList.get(semPListIdx).getRSSI() != null) {
                    String s = String.format("%ddB", semPList.get(semPListIdx).getRSSI());
                    remoteViews.setTextViewText(R.id.tv_rssi, s);
                }

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
                        SyncService.MSG_REFRESH_PARAMETERS, 0, 0, semPList.get(semPListIdx)));
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
                                new SolarEnergyMeterParameters(mac, name, null, null, null, null, null, null, null, null, null, null, null, null, SolarEnergyMeterParameters.TYPE_DISCOVERED, false, null, null)
                        ));
                    break;
                    case DISCOVERING_PAIRED:
                        MLogger.logToFile(appContext, "service.txt", String.format(Locale.getDefault(), "TH : Paired. Msg to activity.", mac), true);
                        msgClient.send(Message.obtain(null,
                                SyncService.MSG_NEW_PAIRED_DEVICE_INFO, 0, 0,
                                new SolarEnergyMeterParameters(mac, name, null, null, null, null, null, null, null, null, null, null, null, null, SolarEnergyMeterParameters.TYPE_PAIRED, false, null, null)));
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

        MLogger.logToFile(appContext, "service.txt", String.format("TH : update %d", semPListIdx), true);

        if(msgClient != null) {

            try {
                if(done) {
                    msgClient.send(Message.obtain(null,
                            SyncService.MSG_PAIRED_DEVICE_INFO, 1, 0, null ));
                }
                else {
                    msgClient.send(Message.obtain(null,
                            SyncService.MSG_PAIRED_DEVICE_INFO, 0, 0, semPList.get(semPListIdx)));
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

//        for(BluetoothDevice d : connectedBLEDevices) {
//            MLogger.logToFile(appContext, "service.txt", "TH : Connected: " + d.getAddress(), true);
//        }

        if(connectedBLEDevices.contains(mugBleDevice) == false) return false;

        return true;
    }

//    boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
//
//        MLogger.logToFile(appContext, "service.txt", "TH : Reading characteristic", true);
//
//        mugGattCharsRead = false;
//
//        if(ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return false;
//        }
//
//        mugGatt.readCharacteristic(characteristic);
//        int timeout = 3000/10;  //3 sec
//        while(!mugGattCharsRead) {
//            try {
//                Thread.sleep(10);
//            } catch(InterruptedException e) {
//                return false;
//            }
//            if(timeout-- <= 0) {
//                MLogger.logToFile(appContext, "service.txt", "TH : Reading characteristic TIMEOUT", true);
//                return false;
//            }
//        }
//
//        return true;
//    }

    @SuppressLint("MissingPermission")
    boolean readCharacteristic(int id) {
        int attempts = 5;
        int i;

        MLogger.logToFile(null, "", String.format("TH : Characteristic Read. ID: %d ", id), true);

        for(i = 0; i < bleCharacteristics.size(); i++) {
            MLogger.logToFile(null, "", String.format("TH : Characteristic Read. Checking %s ", bleCharacteristics.get(i).getDescription()), true);
            if(bleCharacteristics.get(i).getId() == id) {
                MLogger.logToFile(null, "", String.format("TH : Characteristic Read. Found %s ", bleCharacteristics.get(i).getDescription()), true);
                break;
            }
        }
        if( i == bleCharacteristics.size()) {
            MLogger.logToFile(null, "", String.format("TH : Couldn't find characteristic id: %d", id), true);
            return false;
        }

        if( (bleCharacteristics.get(i).getOptions() & Bluetooth.BLECharacteristic.optionRead ) == 0 ) {
            MLogger.logToFile(null, "", String.format("TH : Characteristic %s doesn't support reading", bleCharacteristics.get(i).getDescription()), true);
            return false;
        }

        while(/*!checkStop() &&*/ checkBTEnabled() && mugGattConnected) {
            MLogger.logToFile(null, "", String.format("TH : Reading %s, %s ", bleCharacteristics.get(i).getDescription(), mugAddress), true);
            mugGattCharsRead = false;
            mugGatt.readCharacteristic(bleCharacteristics.get(i).getGattCharacteristic());

            int timeout = 5000 / 10;  //5 sec
            while(!mugGattCharsRead /*&& !checkStop()*/ && checkBTEnabled() && mugGattConnected) {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(--timeout <= 0) {
                    MLogger.logToFile(null, "",
                            String.format(Locale.getDefault(),
                                    "TH : Reading %s TIMEOUT %d", bleCharacteristics.get(i).getDescription(), timeout), true);
                    break;
                }
            }

            if(/*checkStop() ||*/ !checkBTEnabled() || !mugGattConnected) {
                return false;
            }

            if(timeout > 0) {
                whatToSync &= ~bleCharacteristics.get(i).getId();
                return true;
            } else {
                if(attempts > 0) {
                    attempts--;
                    MLogger.logToFile(null, "", String.format(Locale.getDefault(), "TH : Reading %s Attempts left: %d %s", bleCharacteristics.get(i).getDescription(), attempts, mugAddress), true);
                } else {
                    MLogger.logToFile(null, "", String.format("TH : Reading %s UNABLE to read %s", bleCharacteristics.get(i).getDescription(), mugAddress), true);
                    whatToSync &= ~bleCharacteristics.get(i).getId();
                    return false;
                }
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    boolean writeCharacteristic(int id, Object value, int type ) {
        int attempts = 5;
        int i;
        for(i = 0; i < bleCharacteristics.size(); i++) {
            if(bleCharacteristics.get(i).getId() == id) break;
        }
        if( i == bleCharacteristics.size()) {
            MLogger.logToFile(null, "", String.format("TH : Couldn't find characteristic id: %d", id), true);
            return false;
        }

        if( (bleCharacteristics.get(i).getOptions() & Bluetooth.BLECharacteristic.optionWrite ) == 0 ) {
            MLogger.logToFile(null, "", String.format("TH : Characteristic %s doesn't support writing", bleCharacteristics.get(i).getDescription()), true);
            return false;
        }

        String string = "";
        switch(type) {
            case WRITE_STRING:
                string = (String)value;
                break;
            case WRITE_UINT16:
                string = String.format("%d", (int)value);
                break;
            case WRITE_ARRAY:
                int j = 0;
                while(j < ((byte[])value).length) {
                    string += String.format("%02X ", ((byte[])value)[j++] );
                }
                break;
        }

        while(!checkStop() && checkBTEnabled() && mugGattConnected) {

            MLogger.logToFile(null, "", String.format("TH : Writing '%s' to %s, %s ", string, bleCharacteristics.get(i).getDescription(), mugAddress), true);

            mugGattCharWritten = false;

            bleCharacteristics.get(i).getGattCharacteristic().setValue(string);

            switch(type) {
                case WRITE_STRING:
                    bleCharacteristics.get(i).getGattCharacteristic().setValue((String)value );
                    break;
                case WRITE_UINT16:
                    bleCharacteristics.get(i).getGattCharacteristic().setValue((int)value , BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    break;
                case WRITE_ARRAY:
                    bleCharacteristics.get(i).getGattCharacteristic().setValue((byte[])value );
                    break;
            }

            bleCharacteristics.get(i).getGattCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mugGatt.writeCharacteristic(bleCharacteristics.get(i).getGattCharacteristic());

            int timeout = 5000 / 10;  //5 sec
            while(!mugGattCharWritten && !checkStop() && checkBTEnabled() && mugGattConnected) {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(--timeout <= 0) {
                    MLogger.logToFile(null, "",
                            String.format(Locale.getDefault(),
                                    "TH : Writing %s TIMEOUT %d", bleCharacteristics.get(i).getDescription(), timeout), true);
                    break;
                }
            }

            if(checkStop() || !checkBTEnabled() || !mugGattConnected) {
                return false;
            }

            if(timeout > 0) {
                //whatToSync &= ~bleCharacteristics.get(i).getId();
                return true;
            } else {
                if(attempts > 0) {
                    attempts--;
                    MLogger.logToFile(null, "", String.format(Locale.getDefault(), "TH : Writing %s Attempts left: %d %s", bleCharacteristics.get(i).getDescription(), attempts, mugAddress), true);
                } else {
                    MLogger.logToFile(null, "", String.format("TH : Writing %s UNABLE to read %s", bleCharacteristics.get(i).getDescription(), mugAddress), true);
                    //whatToSync &= ~bleCharacteristics.get(i).getId();
                    return false;
                }
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    boolean setNotification(int id) {
        int attempts = 5;
        int i;
        for(i = 0; i < bleCharacteristics.size(); i++) {
            if(bleCharacteristics.get(i).getId() == id) break;
        }
        if( i == bleCharacteristics.size()) {
            MLogger.logToFile(null, "", String.format("TH : Couldn't find characteristic id: %d", id), true);
            return false;
        }

        if( (bleCharacteristics.get(i).getOptions() & Bluetooth.BLECharacteristic.optionNotification ) == 0 ) {
            MLogger.logToFile(null, "", String.format("TH : Characteristic %s doesn't support notification setting", bleCharacteristics.get(i).getDescription()), true);
            return false;
        }

        while(!checkStop() && checkBTEnabled() && mugGattConnected) {
            MLogger.logToFile(null, "", String.format("TH : Notification setting %s, %s ", bleCharacteristics.get(i).getDescription(), mugAddress), true);

            mugGattDescriptorWritten = false;
            mugGatt.setCharacteristicNotification(bleCharacteristics.get(i).getGattCharacteristic(), true);

            UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            BluetoothGattDescriptor desc = bleCharacteristics.get(i).getGattCharacteristic().getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mugGatt.writeDescriptor(desc);

            int timeout = 5000 / 10;  //5 sec
            while(!mugGattDescriptorWritten && !checkStop() && checkBTEnabled() && mugGattConnected) {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(--timeout <= 0) {
                    MLogger.logToFile(null, "",
                            String.format(Locale.getDefault(),
                                    "TH : Notification setting %s TIMEOUT %d", bleCharacteristics.get(i).getDescription(), timeout), true);
                    break;
                }
            }

            if(checkStop() || !checkBTEnabled() || !mugGattConnected) {
                return false;
            }

            if(timeout > 0) {
                whatToSync &= ~bleCharacteristics.get(i).getId();
                return true;
            } else {
                if(attempts > 0) {
                    attempts--;
                    MLogger.logToFile(null, "", String.format(Locale.getDefault(), "TH : Notification setting %s Attempts left: %d %s", bleCharacteristics.get(i).getDescription(), attempts, mugAddress), true);
                } else {
                    MLogger.logToFile(null, "", String.format("TH : %s UNABLE to set notification %s", bleCharacteristics.get(i).getDescription(), mugAddress), true);
                    whatToSync &= ~bleCharacteristics.get(i).getId();
                    return false;
                }
            }
        }
        return false;
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

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            //Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    boolean setMTU(Integer mtu) {

        int attempts = 5;

        gattMTUUpdated = false;
        mugGatt.requestMtu(mtu);

        int timeout = 5000 / 10;  //5 sec
        while (!gattMTUUpdated) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if( !mugGattConnected) {
                MLogger.logToFile(appContext, "service.txt", String.format("TH : GATT disconnected while MTU is negotiated"), true);
                return false;
            }

            if (--timeout <= 0) {
                MLogger.logToFile(null, "",
                        String.format(Locale.getDefault(), "TH : Setting MTU TIMEOUT %d", timeout), true);
                break;
            }
        }
        if (timeout > 0) {
            MLogger.logToFile(appContext, "service.txt", String.format("TH : MTU has been updated"), true);
            return true;
        } else {
            if (attempts > 0) {
                attempts--;
                MLogger.logToFile(appContext, "service.txt", String.format("TH : Setting MTU left: %d attempts", attempts), true);
                return false;
            } else {
                MLogger.logToFile(appContext, "service.txt", String.format("TH : Setting MTU - Unable to set"), true);
                return false;
            }
        }
    }

    @SuppressLint("MissingPermission")
    boolean requestRSSI() {

        int attempts = 5;

        gattRSSIReady = false;
        mugGatt.readRemoteRssi();

        int timeout = 5000 / 10;  //5 sec
        while (!gattRSSIReady) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if( !mugGattConnected) {
                MLogger.logToFile(null, "service.txt", String.format("TH : GATT disconnected while RSSI is requested"), true);
                return false;
            }

            if (--timeout <= 0) {
                MLogger.logToFile(null, "",
                        String.format(Locale.getDefault(), "TH : Getting RSSI TIMEOUT %d", timeout), true);
                break;
            }
        }
        if (timeout > 0) {
            MLogger.logToFile(null, "", String.format("TH : RSSI has been updated"), true);
            return true;
        } else {
            if (attempts > 0) {
                attempts--;
                MLogger.logToFile(null, "", String.format("TH : Getting RSSI left: %d ateempts", attempts), true);
                return false;
            } else {
                MLogger.logToFile(null, "", String.format("TH : Unable to get RSSI"), true);
                return false;
            }
        }
    }



}
