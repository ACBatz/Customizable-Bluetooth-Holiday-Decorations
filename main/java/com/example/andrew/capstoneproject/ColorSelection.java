package com.example.andrew.capstoneproject;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Andrew on 1/28/2016.
 * This class stores all the data the user selects while changing the colors in the lights layout
 */
public class ColorSelection implements Serializable {
    private String starColor = "";
    private String ornamentColor = "";
    private String sequenceNum = "";
    private HashMap<ImageView, String> trueColors;
    private ArrayList<ImageView> colors = null;

    public void setStarColor(String color) {
        starColor = color;
    }
    @NonNull
    public String getStarColor() {
        return starColor;
    }

    public void setOrnamentColor(String color) {
        ornamentColor = color;
    }

    public String getOrnamentColor() {
        return ornamentColor;
    }

    public ColorSelection() {
        colors = new ArrayList<>();
        trueColors = new HashMap<>();
        starColor = "000000";
        ornamentColor = "000000";
    }

    public void setColors(ArrayList<ImageView> colors) {
        this.colors = colors;
    }

    public ArrayList<ImageView> getColors() {
        return colors;
    }

    public HashMap<ImageView, String> getTrueColors() {
        return trueColors;
    }

    public void addColor(ImageView imageView, String color) {
        colors.add(imageView);
        trueColors.put(imageView, color);
    }

    public void removeColor(ImageView imageView) {
        colors.remove(imageView.getId());
        trueColors.remove(imageView);
        int id = 0;
        for (ImageView view:colors) {
            view.setId(id++);
        }
    }

    public void setSequenceNum(String str) {
        sequenceNum = str;
    }

    public String getSequenceNum() {
        return sequenceNum;
    }
}
