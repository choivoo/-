package com.sycompany.iphoneduo;
import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

public class DuoAccessibilityService extends AccessibilityService {
    private WindowManager wm; private View overlay; private float downX,downY; private long downAt;
    @Override protected void onServiceConnected(){super.onServiceConnected();showGesturePill();}
    private void showGesturePill(){
        if(overlay!=null)return; wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        FrameLayout box=new FrameLayout(this); box.setPadding(DuoUi.dp(this,12),DuoUi.dp(this,10),DuoUi.dp(this,12),DuoUi.dp(this,8));
        View pill=new View(this); pill.setBackground(DuoUi.rounded(Color.argb(230,255,255,255),6,this));
        box.addView(pill,new FrameLayout.LayoutParams(DuoUi.dp(this,126),DuoUi.dp(this,5),Gravity.CENTER));
        box.setOnTouchListener((v,e)->gesture(e));
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(DuoUi.dp(this,176),DuoUi.dp(this,42),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;lp.y=DuoUi.dp(this,2);
        try{wm.addView(box,lp);overlay=box;}catch(Exception ignored){}
    }
    private boolean gesture(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();downAt=SystemClock.uptimeMillis();return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            float dx=e.getRawX()-downX,dy=e.getRawY()-downY;long dt=SystemClock.uptimeMillis()-downAt;
            if(overlay!=null)overlay.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if(Math.abs(dx)>DuoUi.dp(this,70))performGlobalAction(GLOBAL_ACTION_RECENTS);
            else if(dy<-DuoUi.dp(this,42)){if(dt>420)performGlobalAction(GLOBAL_ACTION_RECENTS);else performGlobalAction(GLOBAL_ACTION_HOME);}
            else performGlobalAction(GLOBAL_ACTION_HOME);
            return true;
        }
        return true;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(wm!=null&&overlay!=null)try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;super.onDestroy();}
}
