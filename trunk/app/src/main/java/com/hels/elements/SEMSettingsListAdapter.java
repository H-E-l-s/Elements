package com.hels.elements;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class SEMSettingsListAdapter extends BaseAdapter {

    Context context;
    ArrayList<SEMSettings> semSettings;
    //TextView tv_settingName;
    TextView tv_upperSettingName, tv_middleSettingName, tv_bottomSettingName;

    Integer numberOfSettingTabs = 1;

    public SEMSettingsListAdapter(Context context, ArrayList<SEMSettings> semSettings) {
        super();
        this.context = context;
        this.semSettings = semSettings;
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
        ViewHolder  viewHolder;
        if(view == null ) {
            view = LayoutInflater.from(context).inflate(R.layout.listview_settings_3textedits, viewGroup, false);
            viewHolder = new ViewHolder();
            //viewHolder.et_settingValue = view.findViewById(R.id.et_setting_value);
            viewHolder.et_upperValue = view.findViewById(R.id.et_upper_setting_value);
            viewHolder.et_middleValue = view.findViewById(R.id.et_middle_setting_value);
            viewHolder.et_bottomValue = view.findViewById(R.id.et_bottom_setting_value);
            view.setTag(viewHolder);
        }
        else viewHolder = (ViewHolder) view.getTag();
/*
            int listViewItemType = getItemViewType(i);
            switch( listViewItemType ) {
                case MugSettings.TYPE_TEMP_TARGET:
                    et_targetTemp = view.findViewById(R.id.et_setting_value);
                    et_targetTemp.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            mugSettings.get(i).setTargetTemperature(0);
                        }
                    });
                    break;
                case MugSettings.TYPE_MUG_NAME:
                    et_mugName = view.findViewById(R.id.et_setting_value);
                    et_mugName.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            //mugSettings.get(i).setMugName( editable.toString());
                        }
                    });
                    break;
                case MugSettings.TYPE_MUG_COLOR:
                    et_mugColor = view.findViewById(R.id.et_setting_value);
                    et_mugColor.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                         //   Long c = Long.parseLong( editable.toString(), 16);
                         //   mugSettings.get(i).setMugColor( c );
                        }
                    });
                    break;
            }

        }
        else {
//            holder = (RecyclerView.ViewHolder)view.getTag();
        }
*/
        //tv_settingName = view.findViewById(R.id.tv_setting_name);
        tv_upperSettingName = view.findViewById(R.id.tv_upper_setting_name);
        tv_middleSettingName = view.findViewById(R.id.tv_middle_setting_name);
        tv_bottomSettingName = view.findViewById(R.id.tv_bottom_setting_name);

//        viewHolder.et_settingValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean hasFocus) {
//                if(!hasFocus) {
//                    final int position = view.getId();
//                    switch(position) {
//                        case MugSettings.TYPE_MUG_COLOR:
//
//                            final EditText et = (EditText) view;
//                            try {
//                                final Long c = Long.parseLong(et.toString(), 16);
//                                mugSettings.get(position).setMugColor(c);
//                                Log.d("DEBUG", String.format("color set: %08X", c));
//                            } catch(Exception e) {
//                                Log.d("DEBUG", String.format("color set: %s", e.toString()));
//                            }
//                            break;
//                    }
//                }
//            }
//        });

        viewHolder.et_upperValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final int position = viewHolder.et_upperValue.getId();
                final EditText et = (EditText) viewHolder.et_upperValue;
                switch(position) {
                    case SEMSettings.TYPE_LOAD:
                        try {
                            final String s0 = et.getText().toString();
                            if(!"".equals(s0)) {
                                final Integer t = Integer.parseInt(s0);
                                semSettings.get(position).setLoadOnTime(t);
                                //Log.d("DEBUG", String.format("load ON time set: %d", t));
                            }
                        } catch(Exception e) {
                            //Log.d("DEBUG", String.format("Load ON time set: %s", e.toString()));
                        }
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        viewHolder.et_middleValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final int position = viewHolder.et_middleValue.getId();
                final EditText et = (EditText) viewHolder.et_middleValue;
                switch(position) {
                    case SEMSettings.TYPE_LOAD:
                        try {
                            final String s0 = et.getText().toString();
                            if(!"".equals(s0)) {
                                final Integer t = Integer.parseInt(s0);
                                semSettings.get(position).setLoadPeriod(t);
                                //Log.d("DEBUG", String.format("load ON period set: %d", t));
                            }
                        } catch(Exception e) {
                            //Log.d("DEBUG", String.format("Load ON period set: %s", e.toString()));
                        }
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        viewHolder.et_bottomValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final int position = viewHolder.et_bottomValue.getId();
                final EditText et = (EditText) viewHolder.et_bottomValue;
                switch(position) {
                    case SEMSettings.TYPE_LOAD:
                        try {
                            final String s0 = et.getText().toString();
                            if(!"".equals(s0)) {
                                final Integer t = Integer.parseInt(s0);
                                semSettings.get(position).setLoadCurrent(t);
                                //Log.d("DEBUG", String.format("load current set: %d", t));
                            }
                        } catch(Exception e) {
                            //Log.d("DEBUG", String.format("Load current set: %s", e.toString()));
                        }
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        //Log.d("DEBUG", String.format("view: %d", i));
        int listViewItemType = getItemViewType(i);
        if(listViewItemType >= 0) {
            switch(listViewItemType) {
                case SEMSettings.TYPE_LOAD:
                    tv_upperSettingName.setText("Load ON time, ms: ");
                    viewHolder.et_upperValue.setId(i);
                    Integer t = semSettings.get(i).getLoadOnTime();
                    if(t == null) viewHolder.et_upperValue.setText("");
                    else {
                        viewHolder.et_upperValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    tv_middleSettingName.setText("Load ON period time, ms: ");
                    viewHolder.et_middleValue.setId(i);
                    t = semSettings.get(i).getLoadPeriod();
                    if(t == null) viewHolder.et_middleValue.setText("");
                    else {
                        viewHolder.et_middleValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    tv_bottomSettingName.setText("Load current, ma: ");
                    viewHolder.et_bottomValue.setId(i);
                    t = semSettings.get(i).getLoadCurrent();
                    if(t == null) viewHolder.et_bottomValue.setText("");
                    else {
                        viewHolder.et_bottomValue.setText(String.format(Locale.getDefault(), "%d", t));
                    }
                    break;
            }
        }
        return view;
    }

    class ViewHolder {
        //EditText et_settingValue;
        EditText et_upperValue, et_middleValue, et_bottomValue;
    }
}
