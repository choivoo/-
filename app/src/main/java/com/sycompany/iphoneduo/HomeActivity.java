package com.sycompany.iphoneduo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.ClipData;
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
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.DragEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends Activity {
    static final class AppEntry {
        final String label,pkg; final ComponentName component; final Drawable icon;
        AppEntry(String l,String p,ComponentName c,Drawable i){label=l;pkg=p;component=c;icon=i;}
        String key(){return component.flattenToString();}
    }

    private final List<AppEntry> apps=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private GridView grid;
    private AppAdapter gridAdapter;
    private TextView clock;
    private float downX,downY;
    private Runnable clockTick;
    private boolean torchOn=false;
    private boolean editMode=false;
    private boolean firstResume=true;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if(android.os.Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
        configureSystemBars();
        loadApps();
        applySavedOrder();
        buildHome(false);
        startClock();
    }

    @Override protected void onResume(){
        super.onResume();configureSystemBars();
        if(firstResume){firstResume=false;return;}
        loadApps();applySavedOrder();buildHome(false);
    }

    @Override protected void onDestroy(){if(clockTick!=null)handler.removeCallbacks(clockTick);super.onDestroy();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);animateFoldTransition();}

    private void configureSystemBars(){
        // 1.2 intentionally keeps the real Android/Samsung navigation bar active.
        // Only the status bar is hidden; the Galaxy gesture navigation area is not replaced by an overlay.
        if(android.os.Build.VERSION.SDK_INT>=30){
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null){
                c.hide(WindowInsets.Type.statusBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }else{
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void loadApps(){
        apps.clear();
        Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm=getPackageManager();
        List<String> hidden=DuoPrefs.hidden(this);
        for(ResolveInfo rr:pm.queryIntentActivities(i,PackageManager.MATCH_ALL)){
            if(rr.activityInfo==null||rr.activityInfo.packageName.equals(getPackageName()))continue;
            ComponentName cn=new ComponentName(rr.activityInfo.packageName,rr.activityInfo.name);
            if(hidden.contains(cn.flattenToString()))continue;
            apps.add(new AppEntry(String.valueOf(rr.loadLabel(pm)),rr.activityInfo.packageName,cn,rr.loadIcon(pm)));
        }
        Collections.sort(apps,Comparator.comparing(a->a.label.toLowerCase(Locale.getDefault())));
    }

    private void applySavedOrder(){
        List<String> saved=DuoPrefs.order(this);
        if(saved.isEmpty())return;
        Map<String,Integer> rank=new HashMap<>();
        for(int n=0;n<saved.size();n++)rank.put(saved.get(n),n);
        Collections.sort(apps,(a,b)->{
            Integer x=rank.get(a.key()),y=rank.get(b.key());
            if(x!=null&&y!=null)return Integer.compare(x,y);
            if(x!=null)return -1;if(y!=null)return 1;
            return a.label.compareToIgnoreCase(b.label);
        });
    }

    private void saveOrder(){
        ArrayList<String> keys=new ArrayList<>();
        for(AppEntry a:apps)keys.add(a.key());
        DuoPrefs.setOrder(this,keys);
    }

    private void buildHome(boolean animated){
        root=new FrameLayout(this);
        if(DuoPrefs.oled(this))root.setBackgroundColor(Color.BLACK);
        else root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{
            Color.rgb(6,14,29),Color.rgb(44,30,86),Color.rgb(13,26,52),Color.rgb(7,12,22)
        }));
        root.setOnTouchListener((v,e)->topGesture(e));

        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(DuoUi.dp(this,18),DuoUi.dp(this,18),DuoUi.dp(this,18),DuoUi.dp(this,8));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));

        TextView island=DuoUi.label(this,"●  DUO 1.2  •  "+profileText(),10,Color.WHITE,true);
        island.setGravity(Gravity.CENTER);island.setBackground(DuoUi.rounded(Color.argb(220,7,9,14),24,this));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(DuoUi.dp(this,218),DuoUi.dp(this,34));
        ip.gravity=Gravity.CENTER_HORIZONTAL;ip.bottomMargin=DuoUi.dp(this,4);page.addView(island,ip);

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        clock=DuoUi.label(this,new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),16,Color.WHITE,true);
        top.addView(clock,new LinearLayout.LayoutParams(0,DuoUi.dp(this,42),1f));
        TextView nav=DuoUi.label(this,"NAV • "+shortNavMode(),10,Color.rgb(210,220,244),true);
        nav.setGravity(Gravity.CENTER);nav.setPadding(DuoUi.dp(this,10),0,DuoUi.dp(this,10),0);
        nav.setBackground(DuoUi.rounded(Color.argb(glassAlpha(),255,255,255),18,this));
        nav.setOnClickListener(v->DuoNavigation.openGestureSettings(this));DuoUi.clickScale(nav);
        top.addView(nav,new LinearLayout.LayoutParams(-2,DuoUi.dp(this,30)));
        page.addView(top,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,44)));

        TextView date=DuoUi.label(this,new SimpleDateFormat("M월 d일 EEEE",Locale.KOREAN).format(new Date()),27,Color.WHITE,true);
        date.setPadding(0,DuoUi.dp(this,4),0,DuoUi.dp(this,7));page.addView(date);

        if(DuoPrefs.widgets(this))page.addView(buildWidgetStrip(),new LinearLayout.LayoutParams(-1,DuoUi.dp(this,78)));

        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER);
        quick.setPadding(DuoUi.dp(this,6),DuoUi.dp(this,6),DuoUi.dp(this,6),DuoUi.dp(this,6));
        quick.setBackground(DuoUi.stroke(Color.argb(glassAlpha(),255,255,255),Color.argb(58,255,255,255),22,this));
        TextView edit=tile(editMode?"편집 종료":"EDIT",v->{editMode=!editMode;buildHome(false);});
        TextView custom=tile("CUSTOM",v->startActivity(new Intent(this,HomeCustomizationActivity.class)));
        TextView lab=tile("LAB 100",v->startActivity(new Intent(this,DuoLabActivity.class)));
        TextView lock=tile("LOCK",v->startActivity(new Intent(this,LockScreenActivity.class)));
        quick.addView(edit,weight());quick.addView(custom,weight());quick.addView(lab,weight());quick.addView(lock,weight());
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,54));qp.bottomMargin=DuoUi.dp(this,8);page.addView(quick,qp);

        TextView search=DuoUi.label(this,"⌕  Spotlight 검색",15,Color.rgb(231,235,246),false);
        search.setGravity(Gravity.CENTER_VERTICAL);search.setPadding(DuoUi.dp(this,16),0,DuoUi.dp(this,16),0);
        search.setBackground(DuoUi.stroke(Color.argb(Math.min(115,glassAlpha()+15),255,255,255),Color.argb(60,255,255,255),21,this));
        search.setOnClickListener(v->showSearch());DuoUi.clickScale(search);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,43));sp.bottomMargin=DuoUi.dp(this,8);page.addView(search,sp);

        if(editMode){
            TextView editHint=DuoUi.label(this,"편집 모드 • 아이콘을 길게 누른 뒤 다른 아이콘 위로 끌어 이동",11,Color.rgb(255,220,153),true);
            editHint.setGravity(Gravity.CENTER);page.addView(editHint,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,28)));
        }

        grid=new GridView(this);grid.setNumColumns(columns());grid.setVerticalSpacing(DuoUi.dp(this,6));grid.setHorizontalSpacing(DuoUi.dp(this,3));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);
        gridAdapter=new AppAdapter(this,apps);grid.setAdapter(gridAdapter);
        page.addView(grid,new LinearLayout.LayoutParams(-1,0,1f));

        page.addView(buildDock(),new LinearLayout.LayoutParams(-1,DuoUi.dp(this,78)));

        TextView hint=DuoUi.label(this,"Galaxy 시스템 제스처 사용 • 좌우=뒤로  ↑=홈  ↑길게=최근 앱",10,Color.argb(190,255,255,255),false);
        hint.setGravity(Gravity.CENTER);hint.setOnClickListener(v->DuoNavigation.openGestureSettings(this));
        page.addView(hint,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,25)));

        setContentView(root);
        if(animated){
            int d=Math.max(120,(int)(420*(100f/Math.max(50,DuoPrefs.anim(this)))));
            root.setAlpha(0f);root.setScaleX(.95f);root.setScaleY(.95f);
            root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(d).start();
        }
    }

    private View buildWidgetStrip(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);
        TextView time=widgetCard(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),"CLOCK");
        TextView battery=widgetCard(getBattery()+"%","BATTERY");
        TextView folder=widgetCard(String.valueOf(DuoPrefs.folder(this).size()),DuoPrefs.folderName(this));
        folder.setOnClickListener(v->showFolder());DuoUi.clickScale(folder);
        TextView fold=widgetCard(String.valueOf(columns()),"GRID");
        row.addView(time,weight());row.addView(battery,weight());row.addView(folder,weight());row.addView(fold,weight());
        return row;
    }

    private TextView widgetCard(String big,String small){
        TextView t=DuoUi.label(this,big+"\n"+small,14,Color.WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(DuoUi.stroke(Color.argb(glassAlpha(),255,255,255),Color.argb(48,255,255,255),20,this));return t;
    }

    private int getBattery(){
        try{BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);}catch(Exception e){return 0;}
    }

    private int glassAlpha(){return Math.max(20,Math.min(120,DuoPrefs.glass(this)));}
    private int columns(){
        int sw=getResources().getConfiguration().screenWidthDp;
        int base=sw>=900?8:sw>=720?7:sw>=600?6:4;
        return Math.min(10,base+DuoPrefs.grid(this));
    }
    private String profileText(){
        int sw=getResources().getConfiguration().screenWidthDp;
        String m=sw>=720?"ULTRA/WIDE":sw>=600?"UNFOLDED":"COVER";
        return m+" • "+columns()+" COL";
    }
    private String shortNavMode(){
        String m=DuoNavigation.mode(this);
        if(m.contains("스와이프"))return"GESTURE";
        if(m.contains("3버튼"))return"BUTTONS";
        return"SETUP";
    }

    private View buildDock(){
        LinearLayout dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);
        dock.setPadding(DuoUi.dp(this,7),DuoUi.dp(this,7),DuoUi.dp(this,7),DuoUi.dp(this,7));
        dock.setBackground(DuoUi.stroke(Color.argb(glassAlpha(),235,242,255),Color.argb(72,255,255,255),29,this));
        List<AppEntry> chosen=chooseDock();
        for(AppEntry a:chosen){
            ImageView iv=new ImageView(this);iv.setImageDrawable(a.icon);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setPadding(DuoUi.dp(this,8),DuoUi.dp(this,8),DuoUi.dp(this,8),DuoUi.dp(this,8));
            iv.setBackground(iconBackground(a));
            iv.setOnClickListener(v->{if(!editMode)launch(a);});
            iv.setOnLongClickListener(v->{showAppMenu(a);return true;});DuoUi.clickScale(iv);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,DuoUi.dp(this,58),1f);p.leftMargin=DuoUi.dp(this,4);p.rightMargin=DuoUi.dp(this,4);dock.addView(iv,p);
        }
        return dock;
    }

    private List<AppEntry> chooseDock(){
        ArrayList<AppEntry> out=new ArrayList<>();
        List<String> saved=DuoPrefs.dock(this);
        for(String k:saved){AppEntry a=findByKey(k);if(a!=null&&!out.contains(a))out.add(a);}
        if(out.isEmpty()){
            String[] keys={"dialer","phone","message","chrome","browser","camera"};
            for(String k:keys){
                for(AppEntry a:apps){
                    if(!out.contains(a)&&(a.pkg.toLowerCase(Locale.ROOT).contains(k)||a.label.toLowerCase(Locale.ROOT).contains(k))){out.add(a);break;}
                }
                if(out.size()>=4)break;
            }
            for(AppEntry a:apps){if(out.size()>=4)break;if(!out.contains(a))out.add(a);}
            ArrayList<String> ids=new ArrayList<>();for(AppEntry a:out)ids.add(a.key());DuoPrefs.setDock(this,ids);
        }
        int max=getResources().getConfiguration().screenWidthDp>=600?6:4;
        if(out.size()>max)return new ArrayList<>(out.subList(0,max));
        return out;
    }

    private Drawable iconBackground(AppEntry a){
        boolean raw=DuoPrefs.rawIcons(this).contains(a.key());
        if(!DuoPrefs.duoIcons(this)||raw)return DuoUi.rounded(Color.argb(24,255,255,255),20,this);
        return DuoUi.stroke(DuoPrefs.oled(this)?Color.rgb(26,27,31):Color.argb(230,245,247,253),Color.argb(100,255,255,255),21,this);
    }

    private void launch(AppEntry a){
        try{
            Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setComponent(a.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);startActivity(i);
        }catch(Exception ignored){}
    }

    private boolean topGesture(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return false;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){
            float dy=e.getY()-downY;
            if(downY<DuoUi.dp(this,95)&&dy>DuoUi.dp(this,85)){
                if(downX>root.getWidth()*.55f)showControlCenter();else showNotifications();return true;
            }
        }
        return false;
    }

    private void showAppMenu(AppEntry a){
        List<String> folder=DuoPrefs.folder(this),dock=DuoPrefs.dock(this),raw=DuoPrefs.rawIcons(this);
        boolean inFolder=folder.contains(a.key()),inDock=dock.contains(a.key()),isRaw=raw.contains(a.key());
        String[] items={
            inFolder?"즐겨찾기 폴더에서 제거":"즐겨찾기 폴더에 추가",
            inDock?"Dock에서 제거":"Dock에 추가",
            isRaw?"Duo 아이콘 프레임 사용":"이 앱은 원본 아이콘 사용",
            "홈에서 숨기기",
            "앱 정보"
        };
        new AlertDialog.Builder(this).setTitle(a.label).setItems(items,(d,w)->{
            if(w==0){
                ArrayList<String> x=new ArrayList<>(folder);if(inFolder)x.remove(a.key());else if(!x.contains(a.key()))x.add(a.key());DuoPrefs.setFolder(this,x);buildHome(false);
            }else if(w==1){
                ArrayList<String> x=new ArrayList<>(dock);if(inDock)x.remove(a.key());else{if(x.size()>=6)x.remove(0);x.add(a.key());}DuoPrefs.setDock(this,x);buildHome(false);
            }else if(w==2){
                ArrayList<String> x=new ArrayList<>(raw);if(isRaw)x.remove(a.key());else x.add(a.key());DuoPrefs.setRawIcons(this,x);buildHome(false);
            }else if(w==3){
                ArrayList<String> x=new ArrayList<>(DuoPrefs.hidden(this));if(!x.contains(a.key()))x.add(a.key());DuoPrefs.setHidden(this,x);loadApps();applySavedOrder();buildHome(false);
            }else{
                try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));}catch(Exception ignored){}
            }
        }).show();
    }

    private void showFolder(){
        ArrayList<AppEntry> items=new ArrayList<>();
        for(String k:DuoPrefs.folder(this)){AppEntry a=findByKey(k);if(a!=null)items.add(a);}
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=panelBox();
        box.addView(DuoUi.label(this,DuoPrefs.folderName(this),24,Color.WHITE,true));
        if(items.isEmpty()){
            TextView empty=DuoUi.label(this,"폴더가 비어 있습니다.\n앱을 길게 눌러 이 폴더에 추가하세요.",14,Color.rgb(190,201,222),false);
            empty.setPadding(0,DuoUi.dp(this,20),0,DuoUi.dp(this,20));box.addView(empty);
        }else{
            ListView list=new ListView(this);list.setDividerHeight(0);AppListAdapter ad=new AppListAdapter(items);list.setAdapter(ad);
            list.setOnItemClickListener((p,v,pos,id)->{launch(ad.data.get(pos));d.dismiss();});
            box.addView(list,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,Math.min(420,70*items.size()))));
        }
        TextView edit=DuoUi.label(this,"폴더 설정",14,Color.rgb(170,193,255),true);edit.setPadding(0,DuoUi.dp(this,12),0,0);
        edit.setOnClickListener(v->{d.dismiss();startActivity(new Intent(this,HomeCustomizationActivity.class));});box.addView(edit);
        d.setContentView(box);d.show();sizeDialog(d,.86f);
    }

    private AppEntry findByKey(String key){for(AppEntry a:apps)if(a.key().equals(key))return a;return null;}

    private void showSearch(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=panelBox();
        EditText input=new EditText(this);input.setHint("Spotlight 앱 검색");input.setHintTextColor(Color.rgb(150,160,180));input.setTextColor(Color.WHITE);
        input.setSingleLine(true);input.setTextSize(17);input.setBackground(DuoUi.rounded(Color.rgb(34,41,57),20,this));
        input.setPadding(DuoUi.dp(this,16),0,DuoUi.dp(this,16),0);box.addView(input,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,52)));
        ListView list=new ListView(this);list.setDividerHeight(0);AppListAdapter adapter=new AppListAdapter(new ArrayList<>(apps));list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->{launch(adapter.data.get(pos));d.dismiss();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,460));lp.topMargin=DuoUi.dp(this,10);box.addView(list,lp);
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){adapter.filter(s.toString());}
            public void afterTextChanged(android.text.Editable e){}
        });
        d.setContentView(box);d.show();sizeDialog(d,.94f);input.requestFocus();
    }

    private void showNotifications(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=panelBox();box.addView(DuoUi.label(this,"알림 센터",24,Color.WHITE,true));
        box.addView(DuoUi.label(this,new SimpleDateFormat("M월 d일 EEEE",Locale.KOREAN).format(new Date()),13,Color.rgb(170,183,210),false));
        List<NotificationHub.Item> notes=NotificationHub.snapshot();
        if(notes.isEmpty()){
            TextView e=DuoUi.label(this,"표시할 알림이 없습니다.",14,Color.rgb(182,192,213),false);e.setPadding(0,DuoUi.dp(this,26),0,DuoUi.dp(this,26));box.addView(e);
        }else for(NotificationHub.Item n:notes){
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,10),DuoUi.dp(this,14),DuoUi.dp(this,10));
            c.setBackground(DuoUi.rounded(Color.rgb(36,43,58),20,this));c.addView(DuoUi.label(this,n.app+" • "+n.title,14,Color.WHITE,true));
            c.addView(DuoUi.label(this,n.text,13,Color.rgb(199,208,225),false));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=DuoUi.dp(this,8);box.addView(c,cp);
        }
        TextView settings=DuoUi.label(this,"알림 접근 설정",14,Color.rgb(170,190,255),true);settings.setPadding(0,DuoUi.dp(this,15),0,0);
        settings.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));box.addView(settings);
        d.setContentView(box);d.show();sizeDialog(d,.90f);
    }

    private void showControlCenter(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=panelBox();box.addView(DuoUi.label(this,"Duo Control Center 1.2",22,Color.WHITE,true));

        LinearLayout row1=new LinearLayout(this);
        row1.addView(tile("인터넷",v->{try{startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));}catch(Exception e){startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}}),weight());
        row1.addView(tile("Bluetooth",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))),weight());
        row1.addView(tile("플래시",this::toggleTorch),weight());
        row1.addView(tile("NAV",v->DuoNavigation.openGestureSettings(this)),weight());
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,68));rp.topMargin=DuoUi.dp(this,12);box.addView(row1,rp);

        LinearLayout row2=new LinearLayout(this);
        row2.addView(tile("회전",this::toggleRotation),weight());
        row2.addView(tile("집중",v->openSafe(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)),weight());
        row2.addView(tile("배경화면",v->openWallpaper()),weight());
        row2.addView(tile("사용자화",v->startActivity(new Intent(this,HomeCustomizationActivity.class))),weight());
        LinearLayout.LayoutParams r2p=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,62));r2p.topMargin=DuoUi.dp(this,8);box.addView(row2,r2p);

        TextView bl=DuoUi.label(this,"밝기",13,Color.rgb(182,193,215),true);bl.setPadding(0,DuoUi.dp(this,14),0,0);box.addView(bl);
        SeekBar bright=new SeekBar(this);bright.setMax(255);try{bright.setProgress(Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS));}catch(Exception e){bright.setProgress(128);}
        bright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){if(from&&Settings.System.canWrite(HomeActivity.this))Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,p));}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });box.addView(bright);

        TextView vl=DuoUi.label(this,"볼륨",13,Color.rgb(182,193,215),true);box.addView(vl);
        AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);SeekBar vol=new SeekBar(this);vol.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));vol.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));
        vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){if(from)am.setStreamVolume(AudioManager.STREAM_MUSIC,p,0);}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });box.addView(vol);

        LinearLayout bottom=new LinearLayout(this);
        bottom.addView(tile("QE",v->startActivity(new Intent(this,DiagnosticsActivity.class))),weight());
        bottom.addView(tile("LOCK",v->startActivity(new Intent(this,LockScreenActivity.class))),weight());
        bottom.addView(tile("LAB",v->startActivity(new Intent(this,DuoLabActivity.class))),weight());
        bottom.addView(tile("SETUP",v->startActivity(new Intent(this,SetupActivity.class))),weight());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,60));bp.topMargin=DuoUi.dp(this,9);box.addView(bottom,bp);
        d.setContentView(box);d.show();sizeDialog(d,.94f);
    }

    private void toggleRotation(View v){
        if(!Settings.System.canWrite(this)){startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));return;}
        try{
            int cur=Settings.System.getInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1);
            Settings.System.putInt(getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,cur==1?0:1);
            if(v instanceof TextView)((TextView)v).setText(cur==1?"회전 잠금":"자동 회전");
        }catch(Exception ignored){}
    }

    private void openWallpaper(){
        try{startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));}
        catch(Exception e){try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}catch(Exception ignored){}}
    }
    private void openSafe(String action){try{startActivity(new Intent(action));}catch(Exception ignored){}}

    private LinearLayout panelBox(){
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(DuoUi.dp(this,16),DuoUi.dp(this,16),DuoUi.dp(this,16),DuoUi.dp(this,16));
        b.setBackground(DuoUi.rounded(Color.rgb(20,25,36),28,this));return b;
    }
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);p.leftMargin=DuoUi.dp(this,3);p.rightMargin=DuoUi.dp(this,3);return p;}
    private TextView tile(String text,View.OnClickListener l){
        TextView t=DuoUi.label(this,text,12,Color.WHITE,true);t.setGravity(Gravity.CENTER);
        t.setBackground(DuoUi.rounded(Color.rgb(48,58,78),18,this));t.setOnClickListener(l);DuoUi.clickScale(t);return t;
    }
    private void sizeDialog(Dialog d,float ratio){
        Window w=d.getWindow();if(w==null)return;w.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams p=w.getAttributes();p.width=(int)(getResources().getDisplayMetrics().widthPixels*ratio);p.height=-2;p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        p.y=DuoUi.dp(this,12);p.dimAmount=.45f;w.setAttributes(p);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private void toggleTorch(View v){
        try{
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);String pick=null;
            for(String id:cm.getCameraIdList()){
                Boolean f=cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing=cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if(Boolean.TRUE.equals(f)&&(facing==null||facing==CameraCharacteristics.LENS_FACING_BACK)){pick=id;break;}
            }
            if(pick!=null){torchOn=!torchOn;cm.setTorchMode(pick,torchOn);if(v instanceof TextView)((TextView)v).setText(torchOn?"플래시 ON":"플래시");}
        }catch(Exception ignored){}
    }

    private void animateFoldTransition(){
        if(root==null){buildHome(false);return;}
        int d=Math.max(90,(int)(160*(100f/Math.max(50,DuoPrefs.anim(this)))));
        root.animate().alpha(.35f).scaleX(.94f).scaleY(.94f).setDuration(d).withEndAction(()->{
            loadApps();applySavedOrder();buildHome(true);
        }).start();
    }

    private void startClock(){
        clockTick=new Runnable(){public void run(){
            if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
            handler.postDelayed(this,1000);
        }};handler.post(clockTick);
    }

    final class AppAdapter extends BaseAdapter {
        final Context c; final List<AppEntry> data;
        AppAdapter(Context c,List<AppEntry>d){this.c=c;data=d;}
        public int getCount(){return data.size();}
        public Object getItem(int p){return data.get(p);}
        public long getItemId(int p){return p;}

        public View getView(int p,View old,ViewGroup parent){
            AppEntry a=data.get(p);
            LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(DuoUi.dp(c,2),DuoUi.dp(c,4),DuoUi.dp(c,2),DuoUi.dp(c,3));
            int size=DuoUi.dp(c,DuoPrefs.iconSize(HomeActivity.this));
            ImageView iv=new ImageView(c);iv.setImageDrawable(a.icon);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setPadding(DuoUi.dp(c,7),DuoUi.dp(c,7),DuoUi.dp(c,7),DuoUi.dp(c,7));iv.setBackground(iconBackground(a));
            box.addView(iv,new LinearLayout.LayoutParams(size,size));
            if(DuoPrefs.labels(HomeActivity.this)){
                TextView t=DuoUi.label(c,a.label,11,Color.WHITE,false);t.setGravity(Gravity.CENTER);t.setMaxLines(1);t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,DuoUi.dp(c,23));tp.topMargin=DuoUi.dp(c,1);box.addView(t,tp);
            }
            box.setOnClickListener(v->{if(!editMode)launch(a);});
            box.setOnLongClickListener(v->{
                if(editMode){
                    ClipData clip=ClipData.newPlainText("duo-app",a.key());
                    v.startDragAndDrop(clip,new View.DragShadowBuilder(v),a,0);return true;
                }
                showAppMenu(a);return true;
            });
            box.setOnDragListener((v,e)->{
                if(!editMode)return false;
                if(e.getAction()==DragEvent.ACTION_DRAG_ENTERED){v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(80).start();return true;}
                if(e.getAction()==DragEvent.ACTION_DRAG_EXITED){v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();return true;}
                if(e.getAction()==DragEvent.ACTION_DROP){
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    Object local=e.getLocalState();if(local instanceof AppEntry){
                        AppEntry from=(AppEntry)local;int fromIndex=apps.indexOf(from);int toIndex=apps.indexOf(a);
                        if(fromIndex>=0&&toIndex>=0&&fromIndex!=toIndex){apps.remove(fromIndex);if(toIndex>apps.size())toIndex=apps.size();apps.add(toIndex,from);saveOrder();notifyDataSetChanged();}
                    }return true;
                }
                if(e.getAction()==DragEvent.ACTION_DRAG_ENDED){v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();return true;}
                return e.getAction()==DragEvent.ACTION_DRAG_STARTED;
            });
            DuoUi.clickScale(box);return box;
        }
    }

    final class AppListAdapter extends BaseAdapter {
        final ArrayList<AppEntry> all,data;
        AppListAdapter(List<AppEntry>a){all=new ArrayList<>(a);data=new ArrayList<>(a);}
        void filter(String q){
            data.clear();String s=q.toLowerCase(Locale.getDefault());
            for(AppEntry a:all)if(a.label.toLowerCase(Locale.getDefault()).contains(s))data.add(a);notifyDataSetChanged();
        }
        public int getCount(){return data.size();}
        public Object getItem(int p){return data.get(p);}
        public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup g){
            AppEntry a=data.get(p);LinearLayout row=new LinearLayout(HomeActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(DuoUi.dp(HomeActivity.this,8),DuoUi.dp(HomeActivity.this,6),DuoUi.dp(HomeActivity.this,8),DuoUi.dp(HomeActivity.this,6));
            ImageView i=new ImageView(HomeActivity.this);i.setImageDrawable(a.icon);i.setBackground(iconBackground(a));i.setPadding(DuoUi.dp(HomeActivity.this,5),DuoUi.dp(HomeActivity.this,5),DuoUi.dp(HomeActivity.this,5),DuoUi.dp(HomeActivity.this,5));
            row.addView(i,new LinearLayout.LayoutParams(DuoUi.dp(HomeActivity.this,44),DuoUi.dp(HomeActivity.this,44)));
            TextView t=DuoUi.label(HomeActivity.this,a.label,15,Color.WHITE,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,DuoUi.dp(HomeActivity.this,52),1f);lp.leftMargin=DuoUi.dp(HomeActivity.this,12);row.addView(t,lp);return row;
        }
    }
}
