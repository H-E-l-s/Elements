package com.hels.elements;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Locale;

public class SEMSettingsListAdapter extends BaseAdapter {

    Activity activity;
    Context context;
    ArrayList<SEMSettings> semSettings;

    TextView tv_settingName;
    TextView tv_upperSettingName, tv_middleSettingName, tv_bottomSettingName;

    Button btn_logRead24H, btn_readAll;

    Integer numberOfSettingTabs = 2;

    EventListener eventListener;

    public interface EventListener {
        void onLog24HButtonClick();
        void onLogReadAllButtonClick();
    }

    public SEMSettingsListAdapter(Activity activity, Context context, ArrayList<SEMSettings> semSettings, EventListener eventListener) {
        super();
        this.activity = activity;
        this.context = context;
        this.semSettings = semSettings;
        this.eventListener = eventListener;
    }

    @Override
    public int getCount() {
       return semSettings.size();
    }

    @Override
    public Object getItem(int i) {
        return semSettings.get(i);
    }

    @Override
    public long getItemId(int i) {
        return semSettings.get(i).hashCode();
    }

    @Override
    public int getViewTypeCount() {
        return numberOfSettingTabs;
    }

    @Override
    public int getItemViewType(int i) {
        if( i >= semSettings.size() ) { Log.d("Adapter", "semSettings size"); return -1;}
        return semSettings.get(i).getType();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Log.d("DEBUG", String.format("Getting view  %d ", i));
        ViewHolder  viewHolder;
        int listViewItemType = getItemViewType(i);
        if(view == null ) {
            switch( listViewItemType ) {
                case SEMSettings.TYPE_LOAD: {
                    Log.d("DEBUG", String.format("View is null, type %d - init load settings", listViewItemType));
                    view = LayoutInflater.from(context).inflate(R.layout.listview_settings_3textedits, viewGroup, false);
                    viewHolder = new ViewHolder();
                    //viewHolder.et_settingValue = view.findViewById(R.id.et_setting_value);
                    viewHolder.et_upperValue = view.findViewById(R.id.et_upper_setting_value);
                    viewHolder.et_middleValue = view.findViewById(R.id.et_middle_setting_value);
                    viewHolder.et_bottomValue = view.findViewById(R.id.et_bottom_setting_value);
                    view.setTag(viewHolder);
                    break;
                }
                case SEMSettings.TYPE_LOG: {
                    Log.d("DEBUG", String.format("View is null, type %d - init log readers", listViewItemType));
                    view = LayoutInflater.from(activity).inflate(R.layout.listview_settings_two_buttons, viewGroup, false);
                    viewHolder = new ViewHolder();
                    viewHolder.tv_settingName = view.findViewById(R.id.tv_setting_name);
                    viewHolder.btn_logRead24 = view.findViewById(R.id.btn_1st_of_two);
                    viewHolder.btn_logReadAll = view.findViewById(R.id.btn_2nd_of_two);
                    view.setTag(viewHolder);
                    break;
                }
                default:
                    Log.d("DEBUG", String.format("Error - Viewholder not initialized"));
                    viewHolder = new ViewHolder();
                    view.setTag(viewHolder);
            }
        }
        else viewHolder = (ViewHolder) view.getTag();

        //tv_settingName = view.findViewById(R.id.tv_setting_name);
        tv_upperSettingName = view.findViewById(R.id.tv_upper_setting_name);
        tv_middleSettingName = view.findViewById(R.id.tv_middle_setting_name);
        tv_bottomSettingName = view.findViewById(R.id.tv_bottom_setting_name);

        if(viewHolder.et_upperValue != null) {
            viewHolder.et_upperValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    final int position = viewHolder.et_upperValue.getId();
                    final EditText et = (EditText) viewHolder.et_upperValue;
                    switch (position) {
                        case SEMSettings.TYPE_LOAD:
                            try {
                                final String s0 = et.getText().toString();
                                if (!"".equals(s0)) {
                                    final Integer t = Integer.parseInt(s0);
                                    semSettings.get(position).setLoadOnTime(t);
                                    //Log.d("DEBUG", String.format("load ON time set: %d", t));
                                }
                            } catch (Exception e) {
                                //Log.d("DEBUG", String.format("Load ON time set: %s", e.toString()));
                            }
                            break;
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        if(viewHolder.et_middleValue != null) {
            viewHolder.et_middleValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    final int position = viewHolder.et_middleValue.getId();
                    final EditText et = (EditText) viewHolder.et_middleValue;
                    switch (position) {
                        case SEMSettings.TYPE_LOAD:
                            try {
                                final String s0 = et.getText().toString();
                                if (!"".equals(s0)) {
                                    final Integer t = Integer.parseInt(s0);
                                    semSettings.get(position).setLoadPeriod(t);
                                    //Log.d("DEBUG", String.format("load ON period set: %d", t));
                                }
                            } catch (Exception e) {
                                //Log.d("DEBUG", String.format("Load ON period set: %s", e.toString()));
                            }
                            break;
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        if(viewHolder.et_bottomValue != null) {
            viewHolder.et_bottomValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    final int position = viewHolder.et_bottomValue.getId();
                    final EditText et = (EditText) viewHolder.et_bottomValue;
                    switch (position) {
                        case SEMSettings.TYPE_LOAD:
                            try {
                                final String s0 = et.getText().toString();
                                if (!"".equals(s0)) {
                                    final Integer t = Integer.parseInt(s0);
                                    semSettings.get(position).setLoadCurrent(t);
                                    //Log.d("DEBUG", String.format("load current set: %d", t));
                                }
                            } catch (Exception e) {
                                //Log.d("DEBUG", String.format("Load current set: %s", e.toString()));
                            }
                            break;
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        }

        if(viewHolder.btn_logRead24 != null) {
            viewHolder.btn_logRead24.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    eventListener.onLog24HButtonClick();
                }
            });
        }

        if(viewHolder.btn_logReadAll != null) {
            viewHolder.btn_logReadAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    eventListener.onLog24HButtonClick();
                }
            });
        }


        //Log.d("DEBUG", String.format("view: %d", i));
        //int listViewItemType = getItemViewType(i);
        if(listViewItemType >= 0) {
            switch(listViewItemType) {
                case SEMSettings.TYPE_LOAD: {
                    tv_upperSettingName.setText("Load ON time, ms: ");
                    viewHolder.et_upperValue.setId(i);
                    Integer t = semSettings.get(i).getLoadOnTime();
                    if (t == null) viewHolder.et_upperValue.setText("");
                    else {
                        viewHolder.et_upperValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    tv_middleSettingName.setText("Load ON period time, ms: ");
                    viewHolder.et_middleValue.setId(i);
                    t = semSettings.get(i).getLoadPeriod();
                    if (t == null) viewHolder.et_middleValue.setText("");
                    else {
                        viewHolder.et_middleValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    tv_bottomSettingName.setText("Load current, ma: ");
                    viewHolder.et_bottomValue.setId(i);
                    t = semSettings.get(i).getLoadCurrent();
                    if (t == null) viewHolder.et_bottomValue.setText("");
                    else {
                        viewHolder.et_bottomValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    break;
                }
                case SEMSettings.TYPE_LOG: {
                    viewHolder.tv_settingName.setText("Log");
                    viewHolder.btn_logRead24.setText("24H");
                    viewHolder.btn_logReadAll.setText("ALL");

                    break;
                }
            }
        }
        return view;
    }

    class ViewHolder {
        //EditText et_settingValue;
        TextView tv_settingName;
        EditText et_upperValue, et_middleValue, et_bottomValue;
        Button btn_logRead24, btn_logReadAll;
    }
}
