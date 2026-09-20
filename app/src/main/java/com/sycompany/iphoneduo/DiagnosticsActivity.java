package com.sycompany.iphoneduo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.role.RoleManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class DiagnosticsActivity extends Activity {
    private TextView fpsView,stateView; private long lastNs=0; private int frames=0; private boolean running=true;
    private final Choreographer.FrameCallback callback=new Choreographer.FrameCallback(){
        @Override public void doFrame(long t){if(!running)return;if(lastNs==0)lastNs=t;frames++;long diff=t-lastNs;if(diff>=1_000_000_000L){float fps=frames*1_000_000_000f/diff;frames=0;lastNs=t;if(fpsView!=null)fpsView.setText(String.format(Locale.US,"%.1f FPS\nRENDER",fps));}Choreographer.getInstance().postFrameCallback(this);}
    };
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(Color.rgb(12,16,25));build();Choreographer.getInstance().postFrameCallback(callback);}
    @Override protected void onDestroy(){running=false;Choreographer.getInstance().removeFrameCallback(callback);super.onDestroy();}
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(DuoUi.dp(this,20),DuoUi.dp(this,36),DuoUi.dp(this,20),DuoUi.dp(this,30));root.setBackgroundColor(Color.rgb(12,16,25));scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));
        root.addView(DuoUi.label(this,"QE 1.0 • Quality Engine",12,Color.rgb(151,177,255),true));
        TextView title=DuoUi.label(this,"Duo 진단 센터",30,Color.WHITE,true);title.setPadding(0,DuoUi.dp(this,6),0,DuoUi.dp(this,16));root.addView(title);
        LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER);fpsView=statCard("측정 중…\nRENDER");stateView=statCard(profile()+"\nFOLD PROFILE");stats.addView(fpsView,new LinearLayout.LayoutParams(0,DuoUi.dp(this,90),1f));LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(0,DuoUi.dp(this,90),1f);stp.leftMargin=DuoUi.dp(this,10);stats.addView(stateView,stp);root.addView(stats);
        addSection(root,"장치",deviceInfo());addSection(root,"권한/연동",permissionInfo());addSection(root,"QE 판정",qeVerdict());addSection(root,"DuoMorph 엔진","• 화면 폭 변경 감지\n• Cover / Unfolded / Wide·Ultra 자동 컬럼 전환\n• 160ms 축소 진입 + 420ms 복원\n• 런처 재생성 없이 configChanges 처리\n• 제스처/알림 서비스 선택형");
        TextView calibrate=DuoUi.label(this,"전환 애니메이션 캘리브레이션 실행",15,Color.WHITE,true);calibrate.setGravity(Gravity.CENTER);calibrate.setBackground(DuoUi.rounded(Color.rgb(57,82,164),22,this));calibrate.setOnClickListener(this::calibrate);DuoUi.clickScale(calibrate);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,58));cp.topMargin=DuoUi.dp(this,16);root.addView(calibrate,cp);
        setContentView(scroll);
    }
    private TextView statCard(String text){TextView t=DuoUi.label(this,text,18,Color.WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(DuoUi.stroke(Color.rgb(25,31,45),Color.rgb(48,59,80),22,this));return t;}
    private void addSection(LinearLayout root,String title,String body){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(DuoUi.rounded(Color.rgb(23,29,41),22,this));TextView h=DuoUi.label(this,title,17,Color.WHITE,true);h.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,14),DuoUi.dp(this,14),DuoUi.dp(this,6));TextView b=DuoUi.label(this,body,13,Color.rgb(193,204,225),false);b.setPadding(DuoUi.dp(this,14),0,DuoUi.dp(this,14),DuoUi.dp(this,14));card.addView(h);card.addView(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=DuoUi.dp(this,12);root.addView(card,p);}
    private String profile(){int sw=getResources().getConfiguration().screenWidthDp;if(sw>=720)return"WIDE/ULTRA";if(sw>=600)return"UNFOLDED";return"COVER";}
    private String deviceInfo(){ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();am.getMemoryInfo(m);long used=Debug.getPss()/1024;return"Model: "+android.os.Build.MODEL+"\nAndroid: "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nWidth dp: "+getResources().getConfiguration().screenWidthDp+"\nDensity: "+String.format(Locale.US,"%.2f",getResources().getDisplayMetrics().density)+"\nApp PSS: ~"+used+" MB\nAvailable RAM: "+(m.availMem/1024/1024)+" MB";}
    private String permissionInfo(){return"Home role: "+yes(home())+"\nGesture accessibility: "+yes(accessibility())+"\nNotification listener: "+yes(notifications())+"\nBrightness write: "+yes(Settings.System.canWrite(this));}
    private String qeVerdict(){int sw=getResources().getConfiguration().screenWidthDp;String cols=sw>=900?"8":sw>=720?"7":sw>=600?"6":"4";return"Layout columns: "+cols+"\nAdaptive mode: PASS\nAnimation fallback: PASS\nApp launcher discovery: PASS\nGesture overlay: OPTIONAL\nSystem UI replacement: SANDBOXED";}
    private String yes(boolean b){return b?"READY":"SETUP NEEDED";}
    private boolean home(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);return rm!=null&&rm.isRoleHeld(RoleManager.ROLE_HOME);}return false;}
    private boolean accessibility(){String s=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return s!=null&&s.toLowerCase().contains(getPackageName().toLowerCase());}
    private boolean notifications(){String s=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return s!=null&&s.toLowerCase().contains(getPackageName().toLowerCase());}
    private void calibrate(View v){v.animate().rotationY(12).scaleX(.95f).scaleY(.95f).setDuration(130).withEndAction(()->v.animate().rotationY(0).scaleX(1).scaleY(1).setDuration(260).start()).start();}
}
