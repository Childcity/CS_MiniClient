package com.cardsystems.stopnet4.monitoring;

import android.graphics.Bitmap;

/**
 * Created by Maxwell on 21.03.2018.
 */


public class EventData{
    private Bitmap bmp_ = null;
    private String info_ = null;

    public EventData(Bitmap bmp, String info){
        bmp_ = bmp;
        info_ = info;
    }

    public String getInfo(){ return info_; }

    public Bitmap getBmp(){ return bmp_; }
}
