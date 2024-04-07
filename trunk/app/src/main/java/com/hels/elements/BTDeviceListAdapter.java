package com.hels.elements;

import static com.hels.elements.MugParameters.TYPE_DISCOVERED;
import static com.hels.elements.MugParameters.TYPE_PAIRED;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class BTDeviceListAdapter extends BaseAdapter {

    Context context;

    ArrayList<SolarEnergyMeterParameters> semPList;
    TextView tv_devName, tv_devMAC, tv_mugName, tv_batVolts, tv_batAmps, tv_battery;

    public BTDeviceListAdapter(Context context, ArrayList<SolarEnergyMeterParameters> semPList) {
        this.context = context;
        this.semPList = semPList;
    }

    @Override
    public int getCount() {
        //return infoList.size();
        return semPList.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int i) {
        return semPList.get(i).getType();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        int listViewItemType = getItemViewType(i);

        MLogger.logToFile( context, "service.txt", String.format(Locale.getDefault(), "BLA: %d : %d : %d", i, semPList.size(), listViewItemType), true );

        switch(listViewItemType) {
            case TYPE_PAIRED:
                if(view == null ) view = LayoutInflater.from(context).inflate(R.layout.listview_bt_list_paired, viewGroup, false);

                tv_devName = view.findViewById(R.id.tv_devName);
                tv_mugName = view.findViewById(R.id.tv_mugName);
                tv_devMAC = view.findViewById(R.id.tv_devMac);
                //tv_batVolts = view.findViewById(R.id.tv_);
                tv_battery = view.findViewById(R.id.tv_battery);

                tv_devMAC.setText(semPList.get(i).getMACAddress());

                if( semPList.get(i).getBtName() != null ) {
                    tv_devName.setText(semPList.get(i).getBtName());
                    tv_devName.setVisibility(View.VISIBLE);
                }
                else {
                    tv_devName.setVisibility(View.GONE);
                }

                if(semPList.get(i).getMugName() != null ) {
                    tv_mugName.setText(semPList.get(i).getMugName());
                    tv_mugName.setVisibility(View.VISIBLE);
                }
                else {
                    tv_mugName.setVisibility(View.GONE);
                }

//                if(semPList.get(i).getCurrentTemperature() != null ) {
//                    tv_curTemp.setText( String.format("Current temperature: %.02fF", semPList.get(i).getCurrentTemperature()) );
//                    tv_curTemp.setVisibility(View.VISIBLE);
//                }
//                else tv_curTemp.setVisibility(View.GONE);

                if(semPList.get(i).getCPUBatteryCharge() != null ) {
                    tv_battery.setText( String.format("Battery: %d%%", semPList.get(i).getCPUBatteryCharge()) );
                    tv_battery.setVisibility(View.VISIBLE);
                }
                else {
                    tv_battery.setVisibility(View.GONE);
                }

                break;
            case TYPE_DISCOVERED:
                if(view == null ) view = LayoutInflater.from(context).inflate(R.layout.listview_bt_list_discovered, viewGroup, false);

                tv_devName = view.findViewById(R.id.tv_devName);
                tv_devMAC = view.findViewById(R.id.tv_devMac);

                tv_devMAC.setText(semPList.get(i).getMACAddress());

                if( semPList.get(i).getBtName() != null ) {
                    tv_devName.setText(semPList.get(i).getBtName());
                    tv_devName.setVisibility(View.VISIBLE);
                }
                else {
                    tv_devName.setVisibility(View.GONE);
                }

                break;
        }

        return view;
    }
}
