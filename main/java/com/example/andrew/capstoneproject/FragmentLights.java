package com.example.andrew.capstoneproject;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Andrew on 1/25/2016.
 * Inflates the layout in the Fragment with the controller for the LEDs
 */
public class FragmentLights extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.lights_fragment, container, false);

        return view;
    }
}