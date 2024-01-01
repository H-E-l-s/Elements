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

public class MugSettingsListAdapter extends BaseAdapter {

    Context context;
    ArrayList<MugSettings> mugSettings;
    TextView tv_settingName;
    //EditText et_settingValue;
    //EditText et_targetTemp, et_mugName, et_mugColor;

    //ArrayList<ViewHolder> viewHolders;

    public MugSettingsListAdapter(Context context, ArrayList<MugSettings> mugSettings) {
        super();
        this.context = context;
        this.mugSettings = mugSettings;
    }

    @Override
    public int getCount() {
        //return 0;
       return mugSettings.size();
    }

    @Override
    public Object getItem(int i) {
        return mugSettings.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mugSettings.get(i).hashCode();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int i) {
        if( i >= mugSettings.size() ) { Log.d("Adapter", " mugSettings size"); return -1;}
        return mugSettings.get(i).getType();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder  viewHolder;
        //LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        //LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        if(view == null ) {
            view = LayoutInflater.from(context).inflate(R.layout.listview_settings_textedit, viewGroup, false);
            //view = inflater.inflate(R.layout.listview_settings_textedit, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.et_settingValue = view.findViewById(R.id.et_setting_value);
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
        tv_settingName = view.findViewById(R.id.tv_setting_name);

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

        viewHolder.et_settingValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final int position = viewHolder.et_settingValue.getId();
                final EditText et = (EditText) viewHolder.et_settingValue;
                switch(position) {
                    case MugSettings.TYPE_TEMP_TARGET:
                        try {
                            final String s0 = et.getText().toString();
                            if(!"".equals(s0)) {
                                final Integer t = Integer.parseInt(s0) * 100;
                                mugSettings.get(position).setTargetTemperature(t);
                                Log.d("DEBUG", String.format("temp set: %d", t));
                            }
                        } catch(Exception e) {
                            Log.d("DEBUG", String.format("temp set: %s", e.toString()));
                        }
                        break;
                    case MugSettings.TYPE_MUG_NAME:
                        final String s1 = et.getText().toString();
                        if(!"".equals(s1)) {
                            mugSettings.get(position).setMugName(s1);
                            Log.d("DEBUG", String.format("name changed: %s", s1));
                        }
                        else Log.d("DEBUG", String.format("name changed: NOT CHANGED"));
                        break;
                    case MugSettings.TYPE_MUG_COLOR:
                        try {
                            final String s2 = et.getText().toString();
                            if(!"".equals(s2)) {
                                final Long c = Long.parseLong(s2, 16);
                                mugSettings.get(position).setMugColor(c);
                                Log.d("DEBUG", String.format("color changed: %08X", c));
                            }
                        } catch(Exception e) {
                            Log.d("DEBUG", String.format("color changed: %s", e.toString()));
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
                case MugSettings.TYPE_TEMP_TARGET:
                    tv_settingName.setText("Target temperature, \u2103: ");
                    viewHolder.et_settingValue.setId(i);
                    Integer t = mugSettings.get(i).getTargetTemperature();
                    if(t == null) viewHolder.et_settingValue.setText("");
                    else {
                        viewHolder.et_settingValue.setText(String.format(Locale.getDefault(), "%d", (int) (t / 100)));
                    }
                    break;
                case MugSettings.TYPE_MUG_NAME:
                    tv_settingName.setText("Mug name: ");
                    viewHolder.et_settingValue.setId(i);
                    String n = mugSettings.get(i).getMugName();
                    if(n == null) viewHolder.et_settingValue.setText("");
                    else viewHolder.et_settingValue.setText(n);
                    break;
                case MugSettings.TYPE_MUG_COLOR:
                    tv_settingName.setText(String.format("Mug color :"));
                    viewHolder.et_settingValue.setId(i);
                    Long color = mugSettings.get(i).getMugColor();
                    String s = String.format("%06X", color);

                    //Log.d("Adapter", s);
                    //tv_settingName.setText(s);
                    if(color == null) {
                        viewHolder.et_settingValue.setText("");
                        Log.d("DEBUG", String.format("color clear"));
                    }
                    else {
                        viewHolder.et_settingValue.setText(s);
                        Log.d("DEBUG", String.format("color shown: %s",s));
                    }
                   // Log.d("Adapter", "*" + s);
                    //Log.d("DEBUG", String.format("color set: %08X", c));
                    break;
            }
        }
        return view;
    }

    class ViewHolder {
        EditText et_settingValue;
    }
}
