package com.sycompany.iphoneduo;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {
    static final class AppEntry {
        final String label,pkg; final ComponentName component; final Drawable icon;
        AppEntry(String l,String p,ComponentName c,Drawable i){label=l;pkg=p;component=c;icon=i;}
    }

    private final List<AppEntry> apps=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root; private GridView grid; private TextView clock;
    private float downX,downY; private Runnable clockTick; private boolean torchOn=false;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);immersive();loadApps();buildHome(false);startClock();}
    @Override protected void onResume(){super.onResume();immersive();if(grid!=null)grid.invalidateViews();}
    @Override protected void onDestroy(){if(clockTick!=null)handler.removeCallbacks(clockTick);super.onDestroy();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);animateFoldTransition();}

    private void immersive(){
        if(android.os.Build.VERSION.SDK_INT>=30){
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        }else{
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void loadApps(){
        apps.clear();Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);PackageManager pm=getPackageManager();
        for(ResolveInfo r:pm.queryIntentActivities(i,PackageManager.MATCH_ALL)){
            if(r.activityInfo==null||r.activityInfo.packageName.equals(getPackageName()))continue;
            apps.add(new AppEntry(String.valueOf(r.loadLabel(pm)),r.activityInfo.packageName,new ComponentName(r.activityInfo.packageName,r.activityInfo.name),r.loadIcon(pm)));
        }
        Collections.sort(apps,Comparator.comparing(a->a.label.toLowerCase(Locale.getDefault())));
    }

    private void buildHome(boolean animated){
        root=new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(11,19,38),Color.rgb(39,31,73),Color.rgb(10,14,24)}));
        root.setOnTouchListener((v,e)->gesture(e));
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(DuoUi.dp(this,18),DuoUi.dp(this,20),DuoUi.dp(this,18),DuoUi.dp(this,10));root.addView(page,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        clock=DuoUi.label(this,"",16,Color.WHITE,true);top.addView(clock,new LinearLayout.LayoutParams(0,DuoUi.dp(this,44),1f));
        TextView profile=DuoUi.label(this,profileText(),11,Color.rgb(205,216,245),true);profile.setGravity(Gravity.CENTER);profile.setPadding(DuoUi.dp(this,10),0,DuoUi.dp(this,10),0);profile.setBackground(DuoUi.rounded(Color.argb(70,255,255,255),18,this));profile.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));top.addView(profile,new LinearLayout.LayoutParams(-2,DuoUi.dp(this,32)));page.addView(top,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,48)));

        TextView date=DuoUi.label(this,new SimpleDateFormat("M월 d일 EEEE",Locale.KOREAN).format(new Date()),28,Color.WHITE,true);date.setPadding(0,DuoUi.dp(this,8),0,DuoUi.dp(this,10));page.addView(date);

        TextView search=DuoUi.label(this,"⌕  검색",15,Color.rgb(230,234,245),false);search.setGravity(Gravity.CENTER_VERTICAL);search.setPadding(DuoUi.dp(this,16),0,DuoUi.dp(this,16),0);search.setBackground(DuoUi.stroke(Color.argb(55,255,255,255),Color.argb(60,255,255,255),22,this));search.setOnClickListener(v->showSearch());DuoUi.clickScale(search);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,44));sp.bottomMargin=DuoUi.dp(this,12);page.addView(search,sp);

        grid=new GridView(this);grid.setNumColumns(columns());grid.setVerticalSpacing(DuoUi.dp(this,8));grid.setHorizontalSpacing(DuoUi.dp(this,4));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);grid.setAdapter(new AppAdapter(this,apps));grid.setOnItemClickListener((p,v,pos,id)->launch(apps.get(pos)));page.addView(grid,new LinearLayout.LayoutParams(-1,0,1f));

        page.addView(buildDock(),new LinearLayout.LayoutParams(-1,DuoUi.dp(this,82)));
        FrameLayout pillBox=new FrameLayout(this);View pill=new View(this);pill.setBackground(DuoUi.rounded(Color.argb(235,255,255,255),4,this));pillBox.addView(pill,new FrameLayout.LayoutParams(DuoUi.dp(this,126),DuoUi.dp(this,5),Gravity.CENTER));page.addView(pillBox,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,20)));
        TextView hint=DuoUi.label(this,"↙ 알림     위쪽 모서리에서 아래로 스와이프     제어센터 ↘",10,Color.argb(180,255,255,255),false);hint.setGravity(Gravity.CENTER);page.addView(hint,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,18)));

        setContentView(root);
        if(animated){root.setAlpha(0f);root.setScaleX(.95f);root.setScaleY(.95f);root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420).start();}
    }

    private int columns(){int sw=getResources().getConfiguration().screenWidthDp;return sw>=900?8:sw>=720?7:sw>=600?6:4;}
    private String profileText(){int sw=getResources().getConfiguration().screenWidthDp;String m=sw>=720?"ULTRA/WIDE":sw>=600?"UNFOLDED":"COVER";return"DUO • "+m+" • "+columns()+"×";}

    private View buildDock(){
        LinearLayout dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);dock.setPadding(DuoUi.dp(this,8),DuoUi.dp(this,8),DuoUi.dp(this,8),DuoUi.dp(this,8));dock.setBackground(DuoUi.stroke(Color.argb(80,255,255,255),Color.argb(60,255,255,255),28,this));
        List<AppEntry> chosen=chooseDock();for(AppEntry a:chosen){ImageView iv=new ImageView(this);iv.setImageDrawable(a.icon);iv.setPadding(DuoUi.dp(this,7),DuoUi.dp(this,7),DuoUi.dp(this,7),DuoUi.dp(this,7));iv.setBackground(DuoUi.rounded(Color.argb(35,255,255,255),18,this));iv.setOnClickListener(v->launch(a));DuoUi.clickScale(iv);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,DuoUi.dp(this,58),1f);p.leftMargin=DuoUi.dp(this,5);p.rightMargin=DuoUi.dp(this,5);dock.addView(iv,p);}
        return dock;
    }

    private List<AppEntry> chooseDock(){
        ArrayList<AppEntry> out=new ArrayList<>();String[] keys={"dialer","phone","message","chrome","browser","camera"};
        for(String k:keys){for(AppEntry a:apps){if(!out.contains(a)&&(a.pkg.toLowerCase(Locale.ROOT).contains(k)||a.label.toLowerCase(Locale.ROOT).contains(k))){out.add(a);break;}}if(out.size()>=4)break;}
        for(AppEntry a:apps){if(out.size()>=4)break;if(!out.contains(a))out.add(a);}return out;
    }

    private void launch(AppEntry a){
        try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setComponent(a.component);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);startActivity(i);}catch(Exception ignored){}
    }

    private boolean gesture(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return false;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            float dy=e.getY()-downY;int w=root.getWidth(),h=root.getHeight();
            if(downY<DuoUi.dp(this,90)&&dy>DuoUi.dp(this,90)){if(downX>w*.55f)showControlCenter();else showNotifications();return true;}
            if(downY>h-DuoUi.dp(this,140)&&dy<-DuoUi.dp(this,100)){showSearch();return true;}
        }
        return false;
    }

    private void showSearch(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(DuoUi.dp(this,18),DuoUi.dp(this,18),DuoUi.dp(this,18),DuoUi.dp(this,18));box.setBackground(DuoUi.rounded(Color.rgb(20,25,36),28,this));
        EditText input=new EditText(this);input.setHint("앱 검색");input.setHintTextColor(Color.rgb(150,160,180));input.setTextColor(Color.WHITE);input.setSingleLine(true);input.setTextSize(17);input.setBackground(DuoUi.rounded(Color.rgb(34,41,57),20,this));input.setPadding(DuoUi.dp(this,16),0,DuoUi.dp(this,16),0);box.addView(input,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,52)));
        ListView list=new ListView(this);list.setDividerHeight(0);AppListAdapter adapter=new AppListAdapter(new ArrayList<>(apps));list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->{launch(adapter.data.get(pos));d.dismiss();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1f);lp.topMargin=DuoUi.dp(this,12);box.addView(list,lp);
        input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){adapter.filter(s.toString());}public void afterTextChanged(android.text.Editable e){}});
        d.setContentView(box);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-1);WindowManager.LayoutParams p=w.getAttributes();p.width=-1;p.height=-1;p.dimAmount=.55f;w.setAttributes(p);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}input.requestFocus();
    }

    private void showNotifications(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=panelBox();box.addView(DuoUi.label(this,"알림 센터",24,Color.WHITE,true));
        TextView day=DuoUi.label(this,new SimpleDateFormat("M월 d일 EEEE",Locale.KOREAN).format(new Date()),13,Color.rgb(170,183,210),false);box.addView(day);
        List<NotificationHub.Item> notes=NotificationHub.snapshot();
        if(notes.isEmpty()){TextView e=DuoUi.label(this,"표시할 알림이 없습니다.\n설치 마법사에서 알림 접근을 허용하면 실제 알림이 여기에 모입니다.",14,Color.rgb(182,192,213),false);e.setPadding(0,DuoUi.dp(this,28),0,DuoUi.dp(this,28));box.addView(e);}
        else for(NotificationHub.Item n:notes){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,12),DuoUi.dp(this,14),DuoUi.dp(this,12));c.setBackground(DuoUi.rounded(Color.rgb(36,43,58),20,this));c.addView(DuoUi.label(this,n.app+" • "+n.title,14,Color.WHITE,true));c.addView(DuoUi.label(this,n.text,13,Color.rgb(199,208,225),false));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=DuoUi.dp(this,10);box.addView(c,cp);}
        TextView settings=DuoUi.label(this,"알림 접근 설정",14,Color.rgb(170,190,255),true);settings.setPadding(0,DuoUi.dp(this,18),0,0);settings.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));box.addView(settings);
        d.setContentView(box);d.show();sizeDialog(d,.90f);
    }

    private void showControlCenter(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=panelBox();box.addView(DuoUi.label(this,"제어 센터",22,Color.WHITE,true));
        LinearLayout tiles=new LinearLayout(this);
        tiles.addView(tile("인터넷",v->{try{startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));}catch(Exception e){startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}}),weight());
        tiles.addView(tile("Bluetooth",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))),weight());
        tiles.addView(tile("플래시",this::toggleTorch),weight());
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,72));tp.topMargin=DuoUi.dp(this,14);box.addView(tiles,tp);

        TextView bl=DuoUi.label(this,"밝기",13,Color.rgb(182,193,215),true);bl.setPadding(0,DuoUi.dp(this,16),0,0);box.addView(bl);SeekBar bright=new SeekBar(this);bright.setMax(255);try{bright.setProgress(Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS));}catch(Exception e){bright.setProgress(128);}bright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){if(f&&Settings.System.canWrite(HomeActivity.this))Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(bright);

        TextView vl=DuoUi.label(this,"볼륨",13,Color.rgb(182,193,215),true);box.addView(vl);AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);SeekBar vol=new SeekBar(this);vol.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));vol.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){if(f)am.setStreamVolume(AudioManager.STREAM_MUSIC,p,0);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(vol);

        LinearLayout bottom=new LinearLayout(this);bottom.addView(tile("QE 1.0",v->startActivity(new Intent(this,DiagnosticsActivity.class))),weight());bottom.addView(tile("설정",v->startActivity(new Intent(this,SetupActivity.class))),weight());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,64));bp.topMargin=DuoUi.dp(this,12);box.addView(bottom,bp);
        d.setContentView(box);d.show();sizeDialog(d,.92f);
    }

    private LinearLayout panelBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(DuoUi.dp(this,16),DuoUi.dp(this,16),DuoUi.dp(this,16),DuoUi.dp(this,16));b.setBackground(DuoUi.rounded(Color.rgb(22,27,38),28,this));return b;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);p.leftMargin=DuoUi.dp(this,4);p.rightMargin=DuoUi.dp(this,4);return p;}
    private TextView tile(String text,View.OnClickListener l){TextView t=DuoUi.label(this,text,13,Color.WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(DuoUi.rounded(Color.rgb(48,58,78),20,this));t.setOnClickListener(l);DuoUi.clickScale(t);return t;}
    private void sizeDialog(Dialog d,float ratio){Window w=d.getWindow();if(w==null)return;w.setBackgroundDrawableResource(android.R.color.transparent);WindowManager.LayoutParams p=w.getAttributes();p.width=(int)(getResources().getDisplayMetrics().widthPixels*ratio);p.height=-2;p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=DuoUi.dp(this,16);p.dimAmount=.45f;w.setAttributes(p);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}

    private void toggleTorch(View v){
        try{CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);String pick=null;for(String id:cm.getCameraIdList()){Boolean f=cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer facing=cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(f)&&(facing==null||facing==CameraCharacteristics.LENS_FACING_BACK)){pick=id;break;}}if(pick!=null){torchOn=!torchOn;cm.setTorchMode(pick,torchOn);if(v instanceof TextView)((TextView)v).setText(torchOn?"플래시 ON":"플래시");}}catch(Exception ignored){}
    }

    private void animateFoldTransition(){if(root==null){buildHome(false);return;}root.animate().alpha(.35f).scaleX(.93f).scaleY(.93f).setDuration(160).withEndAction(()->buildHome(true)).start();}
    private void startClock(){clockTick=new Runnable(){public void run(){if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));handler.postDelayed(this,1000);}};handler.post(clockTick);}

    final class AppAdapter extends BaseAdapter {
        final Context c; final List<AppEntry> data;AppAdapter(Context c,List<AppEntry>d){this.c=c;data=d;}
        public int getCount(){return data.size();}public Object getItem(int p){return data.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View old,ViewGroup parent){LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(DuoUi.dp(c,3),DuoUi.dp(c,5),DuoUi.dp(c,3),DuoUi.dp(c,4));ImageView iv=new ImageView(c);iv.setImageDrawable(data.get(p).icon);iv.setPadding(DuoUi.dp(c,6),DuoUi.dp(c,6),DuoUi.dp(c,6),DuoUi.dp(c,6));iv.setBackground(DuoUi.rounded(Color.argb(30,255,255,255),18,c));box.addView(iv,new LinearLayout.LayoutParams(DuoUi.dp(c,58),DuoUi.dp(c,58)));TextView t=DuoUi.label(c,data.get(p).label,11,Color.WHITE,false);t.setGravity(Gravity.CENTER);t.setMaxLines(1);t.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,DuoUi.dp(c,24));tp.topMargin=DuoUi.dp(c,2);box.addView(t,tp);DuoUi.clickScale(box);return box;}
    }

    final class AppListAdapter extends BaseAdapter {
        final ArrayList<AppEntry> all,data;AppListAdapter(ArrayList<AppEntry>a){all=new ArrayList<>(a);data=a;}
        void filter(String q){data.clear();String s=q.toLowerCase(Locale.getDefault());for(AppEntry a:all)if(a.label.toLowerCase(Locale.getDefault()).contains(s))data.add(a);notifyDataSetChanged();}
        public int getCount(){return data.size();}public Object getItem(int p){return data.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup g){LinearLayout row=new LinearLayout(HomeActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(DuoUi.dp(HomeActivity.this,8),DuoUi.dp(HomeActivity.this,6),DuoUi.dp(HomeActivity.this,8),DuoUi.dp(HomeActivity.this,6));ImageView i=new ImageView(HomeActivity.this);i.setImageDrawable(data.get(p).icon);row.addView(i,new LinearLayout.LayoutParams(DuoUi.dp(HomeActivity.this,42),DuoUi.dp(HomeActivity.this,42)));TextView t=DuoUi.label(HomeActivity.this,data.get(p).label,15,Color.WHITE,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,DuoUi.dp(HomeActivity.this,52),1f);lp.leftMargin=DuoUi.dp(HomeActivity.this,12);row.addView(t,lp);return row;}
    }
}
