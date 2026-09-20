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
        root.addView(DuoUi.label(this,"DUO ENGINE • 1.2.0",12,Color.rgb(155,178,255),true));
        TextView title=DuoUi.label(this,"iPhone Duo 1.2",34,Color.WHITE,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=DuoUi.dp(this,8);root.addView(title,tp);
        TextView sub=DuoUi.label(this,"Galaxy Z Fold용 Duo 홈 · 실제 Galaxy 스와이프 제스처 · 아이콘/배치/폴더/위젯 · 잠금화면 프리뷰 · 알림 허브를 설정합니다.",16,Color.rgb(202,210,226),false);sub.setPadding(0,DuoUi.dp(this,8),0,DuoUi.dp(this,22));root.addView(sub);
        steps=new LinearLayout(this);steps.setOrientation(LinearLayout.VERTICAL);root.addView(steps,new LinearLayout.LayoutParams(-1,-2));rebuildSteps();
        TextView start=actionButton("Duo 홈 미리보기 시작","현재 설정 상태와 관계없이 Duo Home을 엽니다.");start.setOnClickListener(v->startActivity(new Intent(this,HomeActivity.class)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));sp.topMargin=DuoUi.dp(this,18);root.addView(start,sp);
        TextView diag=actionButton("QE 1.0 진단 열기","FPS · 화면 프로필 · 권한 · 메모리 · 전환 엔진 상태");diag.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));dp.topMargin=DuoUi.dp(this,10);root.addView(diag,dp);
        TextView lock=actionButton("Duo 잠금화면 프리뷰","대형 시계 · 알림 · 스와이프 해제 · 퀵 액션");lock.setOnClickListener(v->startActivity(new Intent(this,LockScreenActivity.class)));LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));lp2.topMargin=DuoUi.dp(this,10);root.addView(lock,lp2);
        TextView lab=actionButton("Duo Lab 100","1.2 구현 기능과 확장 상태 100개");lab.setOnClickListener(v->startActivity(new Intent(this,DuoLabActivity.class)));LinearLayout.LayoutParams labp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));labp.topMargin=DuoUi.dp(this,10);root.addView(lab,labp);
        TextView customize=actionButton("홈 화면 사용자화","Duo 아이콘 · 앱 순서 · 폴더 · 위젯 · 그리드 · OLED");customize.setOnClickListener(v->startActivity(new Intent(this,HomeCustomizationActivity.class)));LinearLayout.LayoutParams cp2=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));cp2.topMargin=DuoUi.dp(this,10);root.addView(customize,cp2);
        TextView note=DuoUi.label(this,"1.2는 하단에 가짜 제스처 바를 덧씌우지 않습니다. Galaxy 설정의 실제 스와이프 제스처를 사용하며, 일반 APK가 해당 보안 설정을 사용자 확인 없이 강제로 바꾸지는 않습니다.",12,Color.rgb(139,151,173),false);note.setPadding(0,DuoUi.dp(this,22),0,0);root.addView(note);
        setContentView(scroll);
    }
    private void rebuildSteps(){
        steps.removeAllViews();
        addStep("1","기본 홈 앱",isHomeRoleHeld()?"설정 완료":"Duo를 홈 화면으로 지정",v->requestHomeRole());
        addStep("2","Galaxy 시스템 스와이프 제스처",DuoNavigation.mode(this),"버튼 내비게이션 대신 실제 시스템 제스처 사용",v->DuoNavigation.openGestureSettings(this));
        addStep("3","알림 허브",isNotificationAccessEnabled()?"사용 중":"알림 접근 허용",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        addStep("4","밝기 제어",Settings.System.canWrite(this)?"사용 가능":"시스템 설정 수정 권한",v->startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()))));
        boolean cam=android.os.Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED;
        addStep("5","플래시 제어",cam?"사용 가능":"카메라 권한 허용",v->requestPermissions(new String[]{Manifest.permission.CAMERA},100));
    }
    private void addStep(String n,String title,String status,View.OnClickListener click){addStep(n,title,status,null,click);}
    private void addStep(String n,String title,String status,String detail,View.OnClickListener click){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(DuoUi.dp(this,16),DuoUi.dp(this,12),DuoUi.dp(this,16),DuoUi.dp(this,12));card.setBackground(DuoUi.stroke(Color.rgb(24,30,43),Color.rgb(50,61,82),22,this));card.setOnClickListener(click);DuoUi.clickScale(card);
        TextView num=DuoUi.label(this,n,14,Color.WHITE,true);num.setGravity(Gravity.CENTER);num.setBackground(DuoUi.rounded(Color.rgb(78,105,190),18,this));card.addView(num,new LinearLayout.LayoutParams(DuoUi.dp(this,36),DuoUi.dp(this,36)));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.addView(DuoUi.label(this,title,16,Color.WHITE,true));box.addView(DuoUi.label(this,status,12,status.contains("완료")||status.contains("사용")||status.contains("제스처")?Color.rgb(129,226,171):Color.rgb(174,187,214),false));if(detail!=null)box.addView(DuoUi.label(this,detail,10,Color.rgb(145,158,184),false));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1f);bp.leftMargin=DuoUi.dp(this,14);card.addView(box,bp);
        TextView chev=DuoUi.label(this,"›",28,Color.rgb(150,161,183),false);card.addView(chev,new LinearLayout.LayoutParams(DuoUi.dp(this,24),-2));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,68));cp.bottomMargin=DuoUi.dp(this,10);steps.addView(card,cp);
    }
    private TextView actionButton(String title,String subtitle){TextView t=DuoUi.label(this,title+"\n"+subtitle,15,Color.WHITE,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(DuoUi.dp(this,18),0,DuoUi.dp(this,18),0);t.setBackground(DuoUi.rounded(Color.rgb(52,77,155),22,this));DuoUi.clickScale(t);return t;}
    private boolean isHomeRoleHeld(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);return rm!=null&&rm.isRoleHeld(RoleManager.ROLE_HOME);}return false;}
    private void requestHomeRole(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),101);return;}}startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
    private boolean isAccessibilityEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return enabled!=null&&enabled.toLowerCase().contains(getPackageName().toLowerCase());}
    private boolean isNotificationAccessEnabled(){String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return enabled!=null&&enabled.toLowerCase().contains(getPackageName().toLowerCase());}
}
