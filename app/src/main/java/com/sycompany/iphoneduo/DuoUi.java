package com.sycompany.iphoneduo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public final class DuoUi {
    private DuoUi() {}
    public static int dp(Context c, float v){ return Math.round(v*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable rounded(int color,float radiusDp,Context c){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(c,radiusDp)); return g;
    }
    public static GradientDrawable stroke(int color,int strokeColor,float radiusDp,Context c){
        GradientDrawable g=rounded(color,radiusDp,c); g.setStroke(dp(c,1),strokeColor); return g;
    }
    public static TextView label(Context c,String text,float sp,int color,boolean bold){
        TextView t=new TextView(c); t.setText(text); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    public static void clickScale(View v){
        v.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()){
                case android.view.MotionEvent.ACTION_DOWN: view.animate().scaleX(.96f).scaleY(.96f).setDuration(70).start(); break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: view.animate().scaleX(1f).scaleY(1f).setDuration(100).start(); break;
            }
            return false;
        });
    }
}
