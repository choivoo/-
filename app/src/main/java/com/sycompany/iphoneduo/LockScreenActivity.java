package com.sycompany.iphoneduo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView clock,seconds;
    private float downY;
    private boolean torchOn=false;
    private final Runnable tick=new Runnable(){@Override public void run(){updateClock();handler.postDelayed(this,1000);}};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if(android.os.Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        immersive();build();handler.post(tick);
    }
    @Override protected void onDestroy(){handler.removeCallbacks(tick);super.onDestroy();}
    private void immersive(){
        if(android.os.Build.VERSION.SDK_INT>=30){
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null)c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
        }
    }
    private void build(){
        FrameLayout root=new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(8,17,34),Color.rgb(43,38,87),Color.rgb(12,18,32)}));
        root.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downY=e.getY();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                if(downY-e.getY()>DuoUi.dp(this,90)){startActivity(new Intent(this,HomeActivity.class));finish();return true;}
            }
            return true;
        });

        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(DuoUi.dp(this,20),DuoUi.dp(this,42),DuoUi.dp(this,20),DuoUi.dp(this,22));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));

        TextView island=DuoUi.label(this,"●  DUO 1.1  •  SECURE VIEW",11,Color.WHITE,true);
        island.setGravity(Gravity.CENTER);island.setBackground(DuoUi.rounded(Color.argb(220,10,12,18),22,this));
        page.addView(island,new LinearLayout.LayoutParams(DuoUi.dp(this,190),DuoUi.dp(this,38)));

        TextView date=DuoUi.label(this,new SimpleDateFormat("M월 d일 EEEE",Locale.KOREAN).format(new Date()),16,Color.rgb(225,230,242),true);
        date.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=DuoUi.dp(this,42);page.addView(date,dp);

        clock=DuoUi.label(this,"",72,Color.WHITE,true);clock.setGravity(Gravity.CENTER);
        page.addView(clock,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,104)));
        seconds=DuoUi.label(this,"",13,Color.rgb(185,198,226),false);seconds.setGravity(Gravity.CENTER);
        page.addView(seconds,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,26)));

        LinearLayout widget=new LinearLayout(this);widget.setOrientation(LinearLayout.VERTICAL);widget.setPadding(DuoUi.dp(this,16),DuoUi.dp(this,14),DuoUi.dp(this,16),DuoUi.dp(this,14));
        widget.setBackground(DuoUi.stroke(Color.argb(62,255,255,255),Color.argb(70,255,255,255),26,this));
        widget.addView(DuoUi.label(this,"DUO GLASS",12,Color.rgb(168,190,255),true));
        widget.addView(DuoUi.label(this,"Fold adaptive lock preview",20,Color.WHITE,true));
        widget.addView(DuoUi.label(this,profile()+"  •  "+NotificationHub.snapshot().size()+" notifications",13,Color.rgb(206,214,232),false));
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,104));wp.topMargin=DuoUi.dp(this,26);page.addView(widget,wp);

        LinearLayout notes=new LinearLayout(this);notes.setOrientation(LinearLayout.VERTICAL);
        List<NotificationHub.Item> ns=NotificationHub.snapshot();
        int max=Math.min(3,ns.size());
        for(int i=0;i<max;i++){
            NotificationHub.Item n=ns.get(i);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,10),DuoUi.dp(this,14),DuoUi.dp(this,10));
            card.setBackground(DuoUi.rounded(Color.argb(90,30,36,50),22,this));
            card.addView(DuoUi.label(this,n.app+"  •  "+n.title,13,Color.WHITE,true));
            card.addView(DuoUi.label(this,n.text,12,Color.rgb(205,212,228),false));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=DuoUi.dp(this,8);notes.addView(card,cp);
        }
        page.addView(notes,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        TextView flash=roundAction("⌁","FLASH");flash.setOnClickListener(this::toggleTorch);
        TextView camera=roundAction("◎","CAMERA");camera.setOnClickListener(v->{try{startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));}catch(Exception ignored){}});
        actions.addView(flash,new LinearLayout.LayoutParams(0,DuoUi.dp(this,70),1f));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,DuoUi.dp(this,70),1f);ap.leftMargin=DuoUi.dp(this,14);actions.addView(camera,ap);
        page.addView(actions,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,74)));

        TextView swipe=DuoUi.label(this,"⌃  위로 쓸어올려 Duo Home",13,Color.WHITE,true);swipe.setGravity(Gravity.CENTER);
        page.addView(swipe,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,34)));

        FrameLayout pillBox=new FrameLayout(this);View pill=new View(this);pill.setBackground(DuoUi.rounded(Color.WHITE,4,this));
        pillBox.addView(pill,new FrameLayout.LayoutParams(DuoUi.dp(this,132),DuoUi.dp(this,5),Gravity.CENTER));
        page.addView(pillBox,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,20)));
        setContentView(root);
    }
    private TextView roundAction(String icon,String label){
        TextView t=DuoUi.label(this,icon+"   "+label,13,Color.WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(DuoUi.stroke(Color.argb(75,255,255,255),Color.argb(70,255,255,255),30,this));DuoUi.clickScale(t);return t;
    }
    private void updateClock(){
        Date d=new Date();if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(d));
        if(seconds!=null)seconds.setText(new SimpleDateFormat("ss초  •  EEEE",Locale.KOREAN).format(d));
    }
    private String profile(){int sw=getResources().getConfiguration().screenWidthDp;return sw>=720?"WIDE / ULTRA":sw>=600?"UNFOLDED":"COVER";}
    private void toggleTorch(View v){
        try{
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);String pick=null;
            for(String id:cm.getCameraIdList()){
                Boolean f=cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing=cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if(Boolean.TRUE.equals(f)&&(facing==null||facing==CameraCharacteristics.LENS_FACING_BACK)){pick=id;break;}
            }
            if(pick!=null){torchOn=!torchOn;cm.setTorchMode(pick,torchOn);if(v instanceof TextView)((TextView)v).setText(torchOn?"●   FLASH ON":"⌁   FLASH");}
        }catch(Exception ignored){}
    }
}
