package com.hels.elements;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WidgetConfigFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_widget_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        int widgetID = requireArguments().getInt("WidgetID");

        Log.d("Widget config Fragment", String.format("widget id: %d", widgetID));
        TextView tv_widgetId =  view.findViewById(R.id.tv_widget_id);
        if(tv_widgetId != null) tv_widgetId.setText(String.format("Widget ID: %d", widgetID));
        else Log.d("Widget config Fragment", "text view is null");

    }

}