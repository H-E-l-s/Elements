package com.hels.elements;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Locale;

public class MugFragment extends Fragment {

    final String tTargetEmpty = "Target temperature: ---";
    final String tTargetC = "Target temperature: %d \u2103";

    final String tCurrentEmpty = "Current temperature: ---";
    final String tCurrentC = "Current temperature: %d \u2103";

    final String batteryEmpty = "Battery: ---";
    final String batteryPercent = "Battery: %d %%";

    final String mugColorEmpty = "Mug color: ---";
    final String mugColorHex = "Mug color: #%06X";


    SwitchMaterial sw_enable;

    Messenger syncServiceMessenger = null;
    boolean syncServiceIsBound = false;
    Boolean mugIsEnabled = false;
    Context context = null;
    String mac;

    TextView tv_devName, tv_devMAC, tv_mugName, tv_tCurrent, tv_tTarget, tv_battery, tv_mugColor;
    ListView lv_settings;
    Button btn_setParameters;
    ImageView iv;
    ProgressBar pb_BusyMug;

    MugSettingsListAdapter settingsListAdapter;
    ArrayList<MugSettings> mugSettings;

    int widgetID = 0;

    View layoutView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        context = getActivity().getApplicationContext();

        View contentView = inflater.inflate(R.layout.fragment_mug, container, false);
        //ListView lv_settings = contentView.findViewById(R.id.lv_mug_settings);

        mugSettings  = new ArrayList<>();
        mugSettings.add(new MugSettings( (Integer)null, MugSettings.TYPE_TEMP_TARGET));
        mugSettings.add(new MugSettings( (String)null, MugSettings.TYPE_MUG_NAME));
        mugSettings.add(new MugSettings( (Long)null, MugSettings.TYPE_MUG_COLOR));
        //mugSettings.add(new MugSettings("Mug color: ", MugSettings.TYPE_MUG_COLOR));

        settingsListAdapter = new MugSettingsListAdapter(context, mugSettings);
        lv_settings = (ListView) contentView.findViewById(R.id.lv_mug_settings);
        lv_settings.setAdapter(settingsListAdapter);
        layoutView = contentView;
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        //context = view.getContext();
        widgetID = requireArguments().getInt("WidgetID");
        //tv_widgetId = view.findViewById(R.id.tv_widget_id);

        MLogger.logToFile(this.getActivity(), "service.txt", String.format(Locale.getDefault(), "MUG: Fragment: onViewCreated. Widget %d", widgetID), true);

        //if(tv_widgetId != null) tv_widgetId.setText(String.format("Widget ID: %d", widgetID));
        //else Log.d("Mug Fragment", "text view is null");

        tv_mugName = view.findViewById(R.id.tv_mug_name);
        tv_tCurrent = view.findViewById(R.id.tv_tCurrent);
        tv_tTarget = view.findViewById(R.id.tv_tTarget);
        tv_battery = view.findViewById(R.id.tv_battery);
        tv_devMAC = view.findViewById(R.id.tv_devMac);
        tv_mugColor = view.findViewById(R.id.tv_mug_color);

        iv = (ImageView) view.findViewById(R.id.iv_mug_color_current);

        pb_BusyMug = view.findViewById(R.id.pb_mug_is_busy);

        btn_setParameters = view.findViewById(R.id.btn_setParameters);
        btn_setParameters.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String s = "MUG: Set: ";
                String name = null;
                Integer targetTemperature = null;
                Long color = null;

                name  = mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_NAME)).getMugName();
                targetTemperature  = mugSettings.get(findMugSetting(MugSettings.TYPE_TEMP_TARGET)).getTargetTemperature();
                color  = mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_COLOR)).getMugColor();

                s += ( (name == null) ? "name: NA" : "name: NA" + name );
                s += ((targetTemperature == null) ? (" Ttrgt: NA") : String.format(Locale.getDefault(), " Ttrgt: %d \u2103", targetTemperature / 100) ) ;
                s += String.format(Locale.getDefault(), " Color: %08X", color );

                setViewAndChildrenEnabled(layoutView, false);
                pb_BusyMug.setVisibility(View.VISIBLE);

                MLogger.logToFile(context, "service.txt", s, true);
                requestToSetParameters(new MugParameters(mac, null, name, color, null, targetTemperature, null, null, null));

                clearInfo();

            }
        });


        sw_enable = view.findViewById(R.id.sw_enable);

        mac = AppPreferences.readString(requireActivity(),  String.format(Locale.getDefault(), "widget_%d", widgetID), "mac");

        mugIsEnabled = AppPreferences.readBoolean( requireActivity(),  String.format("mug_%s", mac), "enabled");
        if( mugIsEnabled == null) mugIsEnabled = false;

        String threadName = String.format("*%s_%s", context.getPackageName(), mac);

        Boolean threadIsRunning = SyncService.isThreadRunning( context, threadName );

        MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onViewCreated: Thread %s enabled: %B; is running: %B", mac, mugIsEnabled, threadIsRunning), true);

        sw_enable.setOnCheckedChangeListener (null);
        if(mugIsEnabled) {
            sw_enable.setChecked(true);
//            if(threadIsRunning) {
//                MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onViewCreated: Thread is running"), true);
//                //tv_widgetId.setText("ON");
//                sw_enable.setText("ON");
//
//                context.bindService(new Intent(context, SyncService.class), mConnection, 0);
//            }
//            else {
//                MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onViewCreated: Thread isn't running"), true);
//                //tv_widgetId.setText("ON");
//                sw_enable.setText("ON");
//                Intent intent = new Intent(getActivity(), SyncService.class);
//                intent.putExtra("mac", mac);
//                intent.putExtra("widgetID", widgetID);
//                intent.putExtra("task", "on_mug_enabled");
//                getActivity().startService(intent);
//
//                context.bindService(new Intent(context, SyncService.class), mConnection, 0);
//
//            }
        }
        else {
            sw_enable.setChecked(false);
        }

        sw_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onCheckChanged: checked %s enabled: %B; is running: %B", mac, mugIsEnabled, threadIsRunning), true);
                    mugIsEnabled = true;
                    AppPreferences.save( getActivity(),  String.format("mug_%s", mac), AppPreferences.NAME_ENABLED, mugIsEnabled);
                    //stateOnOff.setText("On");
                    //tv_widgetId.setText("ON");
                    sw_enable.setText("Enable");
                    Intent intent = new Intent(getActivity(), SyncService.class);
                    intent.putExtra("mac", mac);
                    intent.putExtra("widgetID", widgetID);
                    intent.putExtra("task", "on_mug_enabled");
                    getActivity().startService(intent);


                    Boolean b =  context.bindService(new Intent(context, SyncService.class), mConnection, 0);
                    MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onCheckChanged: checked binding %B", b), true);


                    //Bundle b = new Bundle();
                    //b.putInt("wid", widgetID);
                    AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    Intent intentAlrm = new Intent(context, AppWidgetUpdate.class);
                    intentAlrm.putExtra("wid", widgetID);
                    PendingIntent pi = PendingIntent.getBroadcast(context, 0, intentAlrm, PendingIntent.FLAG_IMMUTABLE);
                    am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000 * 60* 15, 60 * 1000 * 15 , pi);

                }else{
                    MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onCheckChanged: NOT checked %s enabled: %B; is running: %B", mac, mugIsEnabled, threadIsRunning), true);
                    mugIsEnabled = false;
                    AppPreferences.save( getActivity(),  String.format("mug_%s", mac), "enabled", mugIsEnabled);

                    //tv_widgetId.setText("OFF");
                    sw_enable.setText("Enable");


                    Message msg = Message.obtain(null,
                            SyncService.MSG_ENABLE_DISABLE, 0, 0, (Object) mac);

                    try {
                        syncServiceMessenger.send(msg);
                    } catch(RemoteException e) {
                        throw new RuntimeException(e);
                    }

                    unBindService();

                    //  getActivity().stopService(new Intent(getActivity(), SyncService.class));
//                    if (mConnection != null) {
//                        context.unbindService(mConnection);
//                        syncServiceIsBound = false;
//                        MLogger.logToFile(context, "service.txt", "MUG: Fragment: onCheckChanged: NOT checked Unbounding service", true);
//                    }

                }

            }
        });



    }

    @Override
    public void onStart() {

        super.onStart();
        MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onStart", true);
//        context.bindService(new Intent(context, SyncService.class), mConnection,
//                Context.BIND_AUTO_CREATE);


        String threadName = String.format("*%s_%s", context.getPackageName(), mac);
        Boolean threadIsRunning = SyncService.isThreadRunning( context, threadName );

        clearInfo();
        //sw_enable.setOnCheckedChangeListener (null);
        if(mugIsEnabled) {
          //  sw_enable.setChecked(true);
            if(threadIsRunning) {
                MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onStart: Bind Service"), true);
                //sw_enable.setText("ON");
                context.bindService(new Intent(context, SyncService.class), mConnection, 0);
            }
            else {
                MLogger.logToFile(getActivity(), "service.txt", String.format("MUG: onStart: Start Service"), true);
                //tv_widgetId.setText("ON");
                //sw_enable.setText("ON");
                Intent intent = new Intent(getActivity(), SyncService.class);
                intent.putExtra("mac", mac);
                intent.putExtra("widgetID", widgetID);
                intent.putExtra("task", "on_mug_enabled");
                getActivity().startService(intent);

                context.bindService(new Intent(context, SyncService.class), mConnection, 0);

            }
        }
//        else {
//            sw_enable.setChecked(false);
//        }
    }

    @Override
    public void onStop() {

        super.onStop();
        MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onStop", true);
        unBindService();
//        // Unbind from the service.
//        if (syncServiceIsBound) {
//            if ( syncServiceMessenger!= null) {
//
//                String mac = AppPreferences.readString(requireActivity(),  String.format(Locale.getDefault(), "widget_%d", widgetID), "mac");
//
//                try {
//                    Message msg = Message.obtain(null,
//                            SyncService.MSG_UNREGISTER_CLIENT);
//                    msg.replyTo = mMessenger;
//                    msg.obj = mac;
//                    syncServiceMessenger.send(msg);
//
//                    MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onStop: Unbounding service " + mac, true);
//
//                } catch (RemoteException e) {
//                    // There is nothing special we need to do if the service
//                    // has crashed.
//                    MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onStop: Unbounding service " + e.toString(), true);
//                }
//            }
//
//            //   unbindService(mConnection);
//            //syncServiceIsBound = false;
//            if (mConnection != null) {
//                context.unbindService(mConnection);
//                syncServiceIsBound = false;
//                MLogger.logToFile(context, "service.txt", "MUG: Fragment: onStop: Unbounding service", true);
//            }
//
//        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

            String mac = AppPreferences.readString(requireActivity(),  String.format(Locale.getDefault(), "widget_%d", widgetID), "mac");

            syncServiceMessenger = new Messenger(service);
            syncServiceIsBound = true;
            try {
                Message msg = Message.obtain(null,
                        SyncService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                msg.obj = mac;
                syncServiceMessenger.send(msg);
                MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onServiceConnected: Connected. Registering client for " + mac, true);

                requestToReadMugParameters(mac);

            } catch (RemoteException e) {
                MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onServiceConnected: !E! " + e.toString(), true);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            syncServiceMessenger = null;
            syncServiceIsBound = false;
            MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onServiceConnected: Service DISconnected", true);
        }
    };

    class SyncServiceIncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Boolean updateListView = false;
            switch (msg.what) {
                case SyncService.MSG_REFRESH_PARAMETERS:
                    MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: parameters updating", true);
                    if(msg.obj != null) {
                        MugParameters mp = (MugParameters)msg.obj;
                        Float ct = mp.getCurrentTemperature();

                        if(ct != null) {
                            MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: current temp: %.02fC",ct), true);
                            //tv_tCurrent.setText(String.format(Locale.getDefault(), "Current temperature: %.02fC", ct));
                            tv_tCurrent.setText(String.format(Locale.getDefault(), tCurrentC, ct.intValue()));
                        }
                        else {
                            tv_tCurrent.setText(String.format(Locale.getDefault(), tCurrentEmpty));
                        }

                        Integer tt = mp.getTargetTemperature();
                        if(tt != null) {
                            MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: target temp: %.02fC",tt.floatValue()/100.0f), true);
                            //tv_tCurrent.setText(String.format(Locale.getDefault(), "Current temperature: %.02fC", ct));
                            //tv_tTarget.setText(String.format(Locale.getDefault(), "Target temperature: %d C", tt/100));
                            tv_tTarget.setText(String.format(Locale.getDefault(), tTargetC, tt/100));

                            //mugSettings.get(findMugSetting(MugSettings.TYPE_TEMP_TARGET)).setTargetTemperature(tt);
                            //updateListView = true;
                        }
                        else {
                            tv_tTarget.setText(String.format(Locale.getDefault(), tTargetEmpty));
                            //mugSettings.get(findMugSetting(MugSettings.TYPE_TEMP_TARGET)).setTargetTemperature(null);
                        }

                        Integer charge = mp.getBatteryCharge();
                        if(charge != null) {
                            MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: battery: %d%%",charge), true);
                            tv_battery.setText(String.format(Locale.getDefault(), batteryPercent, charge));
                        }
                        else {
                            tv_battery.setText(String.format(Locale.getDefault(), batteryEmpty));
                        }

                        String name = mp.getMugName();
                        if( name != null) {
                            tv_mugName.setText(name);
                            //mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_NAME)).setMugName(name);
                        }
                        else {
                            tv_mugName.setText("---");
                            //mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_NAME)).setMugName("---");
                        }

                        tv_devMAC.setText( "MAC: " + mp.getMACAddress());

                        Long color = mp.getMugColor();
                        if(color != null) {
                            MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: color: %06X", color), true);
                            tv_mugColor.setText(String.format(Locale.getDefault(), mugColorHex, color));
                            //mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_COLOR)).setMugColor(color);

                            //Paint mugColorPaint;
                            GradientDrawable shape = new GradientDrawable();
                            shape.setShape(GradientDrawable.OVAL);
                            //shape.setCornerRadii(new float[]{0,0,0,0,0,0,0,0,});

                            //String s = String.format("#FF%06X", (color & 0x00FFFFFF));
                            String s = String.format("#%06X", (color & 0x00FFFFFF));
                            shape.setColor(Color.parseColor(s));
                            //int c = Color.parseColor("red");
                            //c &= 0x00FFFFFF;
                            //shape.setColor(c);

                            //shape.setColor(Color.rgb(0x50,0x50,0x00));
                            //shape.setColor(Color.rgb(255,00,00));
                            shape.setStroke(5, Color.rgb(255,255,255));



                            iv.setBackground(shape);
                            iv.setAlpha(1.0f);
                            //updateListView = true;

                        }
                        else {
                            tv_mugColor.setText(String.format(Locale.getDefault(), mugColorEmpty));
                            //mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_COLOR)).setMugColor(null);
                        }

                        //if(updateListView) settingsListAdapter.notifyDataSetChanged();

                    }
                    break;
                case SyncService.MSG_SET_PARAMETERS_DONE:
                    pb_BusyMug.setVisibility(View.GONE);
                    setViewAndChildrenEnabled(layoutView, true);
                    MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: Parameters are written"), true);
                    requestToReadMugParameters(mac);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    void requestToReadMugParameters(String mac) {
        MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: ReRead parameters for %s", mac), true);
        Message msg = Message.obtain(null,
                SyncService.MSG_REREAD_PARAMETERS);
        msg.replyTo = mMessenger;
        msg.obj = mac;
        try {
            syncServiceMessenger.send(msg);
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    void requestToSetParameters(MugParameters mugParameters) {
        MLogger.logToFile(getActivity(), "service.txt",  String.format(Locale.getDefault(), "MUG: Fragment: Request to set parameters %s", mac), true);
        Message msg = Message.obtain(null,
                SyncService.MSG_SET_PARAMETERS);
        msg.replyTo = mMessenger;
        msg.obj = mugParameters;
        try {
            syncServiceMessenger.send(msg);
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    void clearInfo() {
        tv_mugName.setText("");
        tv_tCurrent.setText(tCurrentEmpty);
        tv_tTarget.setText(tTargetEmpty);
        tv_battery.setText(batteryEmpty);
//        tv_devMAC = view.findViewById(R.id.tv_devMac);
        tv_mugColor.setText(mugColorEmpty);

        iv.setAlpha(0.0f);

        mugSettings.get(findMugSetting(MugSettings.TYPE_TEMP_TARGET)).setTargetTemperature(null);
        mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_NAME)).setMugName(null);
        mugSettings.get(findMugSetting(MugSettings.TYPE_MUG_COLOR)).setMugColor(null);
        settingsListAdapter.notifyDataSetChanged();
    }


    void unBindService() {
        // Unbind from the service.
        if (syncServiceIsBound) {
            if ( syncServiceMessenger!= null) {

                String mac = AppPreferences.readString(requireActivity(),  String.format(Locale.getDefault(), "widget_%d", widgetID), "mac");

                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    msg.obj = mac;
                    syncServiceMessenger.send(msg);

                    MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: Unbinding service " + mac, true);

                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                    MLogger.logToFile(getActivity(), "service.txt", "MUG: Fragment: onStop: Unbinding service " + e.toString(), true);
                }
            }

            //   unbindService(mConnection);
            //syncServiceIsBound = false;
            if (mConnection != null) {
                context.unbindService(mConnection);
                syncServiceIsBound = false;
                MLogger.logToFile(context, "service.txt", "MUG: Fragment: onStop: Unbinding service", true);
            }

        }
    }

    Integer findMugSetting(Integer type) {
        for(int i = 0; i < mugSettings.size(); i++) {
            if(mugSettings.get(i).getType() == type) return i;
        }

        return null;
    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {

        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }
    //---- End of Fragment-Service communications -------------------------------------------------



}


