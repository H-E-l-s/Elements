package com.hels.elements;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Intent syncServiceIntent = new Intent(getContext(), SyncService.class);
        syncServiceIntent.putExtra("task", "on_main_fragment");
        getContext().startService(syncServiceIntent);

        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}