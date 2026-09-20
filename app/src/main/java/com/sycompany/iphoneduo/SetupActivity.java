package com.sycompany.iphoneduo;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SetupActivity extends Activity {
    private LinearLayout steps;
    @Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);build();}
    @Override protected void onResume(){super.onResume();if(steps!=null)rebuildSteps();}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(DuoUi.dp(this,24),DuoUi.dp(this,48),DuoUi.dp(this,24),DuoUi.dp(this,32));root.setBackgroundColor(Color.rgb(12,16,25));scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));
        root.addView(DuoUi.label(this,"DUO ENGINE • 1.0.0",12,Color.rgb(155,178,255),true));
        TextView title=DuoUi.label(this,"iPhone Duo 1.0",34,Color.WHITE,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=DuoUi.dp(this,8);root.addView(title,tp);
        TextView sub=DuoUi.label(this,"Galaxy Z Fold용 런처 · 전역 제스처 · 알림 허브 · Fold 전환 · QE 진단을 한 번에 설정합니다.",16,Color.rgb(202,210,226),false);sub.setPadding(0,DuoUi.dp(this,8),0,DuoUi.dp(this,22));root.addView(sub);
        steps=new LinearLayout(this);steps.setOrientation(LinearLayout.VERTICAL);root.addView(steps,new LinearLayout.LayoutParams(-1,-2));rebuildSteps();
        TextView start=actionButton("Duo 홈 미리보기 시작","현재 설정 상태와 관계없이 Duo Home을 엽니다.");start.setOnClickListener(v->startActivity(new Intent(this,HomeActivity.class)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));sp.topMargin=DuoUi.dp(this,18);root.addView(start,sp);
        TextView diag=actionButton("QE 1.0 진단 열기","FPS · 화면 프로필 · 권한 · 메모리 · 전환 엔진 상태");diag.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));dp.topMargin=DuoUi.dp(this,10);root.addView(diag,dp);
        TextView note=DuoUi.label(this,"보안 경계: 일반 APK는 Samsung의 실제 보안 잠금화면이나 Android 시스템 UI 자체를 교체하지 않습니다. 대신 기본 홈 런처와 선택형 접근성 제스처 레이어로 동일한 사용 흐름을 구성합니다.",12,Color.rgb(139,151,173),false);note.setPadding(0,DuoUi.dp(this,22),0,0);root.addView(note);
        setContentView(scroll);
    }
    private void rebuildSteps(){
        steps.removeAllViews();
        addStep("1","기본 홈 앱",isHomeRoleHeld()?"설정 완료":"Duo를 홈 화면으로 지정",v->requestHomeRole());
        addStep("2","Duo 전역 제스처",isAccessibilityEnabled()?"사용 중":"접근성에서 iPhone Duo 켜기",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addStep("3","알림 허브",isNotificationAccessEnabled()?"사용 중":"알림 접근 허용",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        addStep("4","밝기 제어",Settings.System.canWrite(this)?"사용 가능":"시스템 설정 수정 권한",v->startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()))));
        boolean cam=android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED;
        addStep("5","플래시 제어",cam?"사용 가능":"카메라 권한 허용",v->requestPermissions(new String[]{Manifest.permission.CAMERA},100));
    }
    private void addStep(String n,String title,String status,View.OnClickListener click){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(DuoUi.dp(this,16),DuoUi.dp(this,12),DuoUi.dp(this,16),DuoUi.dp(this,12));card.setBackground(DuoUi.stroke(Color.rgb(24,30,43),Color.rgb(50,61,82),22,this));card.setOnClickListener(click);DuoUi.clickScale(card);
        TextView num=DuoUi.label(this,n,14,Color.WHITE,true);num.setGravity(Gravity.CENTER);num.setBackground(DuoUi.rounded(Color.rgb(78,105,190),18,this));card.addView(num,new LinearLayout.LayoutParams(DuoUi.dp(this,36),DuoUi.dp(this,36)));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.addView(DuoUi.label(this,title,16,Color.WHITE,true));box.addView(DuoUi.label(this,status,12,status.contains("완료")||status.contains("사용")?Color.rgb(129,226,171):Color.rgb(174,187,214),false));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1f);bp.leftMargin=DuoUi.dp(this,14);card.addView(box,bp);
        TextView chev=DuoUi.label(this,"›",28,Color.rgb(150,161,183),false);card.addView(chev,new LinearLayout.LayoutParams(DuoUi.dp(this,24),-2));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,68));cp.bottomMargin=DuoUi.dp(this,10);steps.addView(card,cp);
    }
    private TextView actionButton(String title,String subtitle){TextView t=DuoUi.label(this,title+"\n"+subtitle,15,Color.WHITE,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(DuoUi.dp(this,18),0,DuoUi.dp(this,18),0);t.setBackground(DuoUi.rounded(Color.rgb(52,77,155),22,this));DuoUi.clickScale(t);return t;}
    private boolean isHomeRoleHeld(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);return rm!=null&&rm.isRoleHeld(RoleManager.ROLE_HOME);}return false;}
    private void requestHomeRole(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),101);return;}}startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
    private boolean isAccessibilityEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return enabled!=null&&enabled.toLowerCase().contains(getPackageName().toLowerCase());}
    private boolean isNotificationAccessEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return enabled!=null&&enabled.toLowerCase().contains(getPackageName().toLowerCase());}
}
