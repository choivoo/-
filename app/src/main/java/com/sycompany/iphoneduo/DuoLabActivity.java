package com.sycompany.iphoneduo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DuoLabActivity extends Activity {
    private static final String[] FEATURES={
        "Adaptive Cover layout",
        "Adaptive Unfolded layout",
        "Wide/Ultra 7–8 column grid",
        "Fold transition animation",
        "Dynamic Island-style status capsule",
        "Glass search bar",
        "Glass Dock",
        "Real installed-app discovery",
        "Alphabetic app sorting",
        "Direct app launching",
        "Spotlight-style app search",
        "Bottom swipe search gesture",
        "Top-left notification gesture",
        "Top-right Control Center gesture",
        "Global Home gesture overlay",
        "Global Recents gesture overlay",
        "Notification listener hub",
        "Notification center cards",
        "Brightness control",
        "Media volume control",
        "Internet settings tile",
        "Bluetooth settings tile",
        "Flashlight tile",
        "QE live FPS meter",
        "QE device profile",
        "QE memory readout",
        "QE permission audit",
        "QE adaptive-layout verdict",
        "DuoMorph calibration",
        "Setup Wizard",
        "Home-role setup",
        "Accessibility setup shortcut",
        "Notification-access setup shortcut",
        "Write-settings setup shortcut",
        "Camera permission setup",
        "Duo Glass lock preview",
        "Large lock clock",
        "Lock notification cards",
        "Swipe-up unlock flow",
        "Lock flashlight action",
        "Lock camera action",
        "Lock-screen fold profile",
        "Duo Lab 100 dashboard",
        "Versioned update install path",
        "Same applicationId update support",
        "Immersive status/navigation hide",
        "Transient system-bar reveal",
        "Real app icon rendering",
        "Rounded icon containers",
        "Touch press-scale feedback",
        "Custom icon pack engine",
        "Per-app icon override",
        "Home edit mode",
        "Drag-and-drop app rearrange",
        "Persistent home page positions",
        "App folders",
        "Folder rename",
        "Dock customization",
        "Widget framework",
        "Clock widget variants",
        "Calendar widget",
        "Battery widget",
        "Weather widget connector",
        "Music now-playing widget",
        "Focus modes",
        "Do Not Disturb tile",
        "Rotation lock tile",
        "Screen recording tile",
        "QR scanner tile",
        "Calculator quick tile",
        "Theme color engine",
        "Wallpaper picker",
        "Wallpaper depth effect",
        "Light/Dark automatic theme",
        "OLED black theme",
        "Glass intensity slider",
        "Icon size slider",
        "Label visibility toggle",
        "Grid density slider",
        "Animation speed slider",
        "Fold hinge progress interpolation",
        "Half-fold tabletop layout",
        "Cover-to-inner continuity map",
        "Per-display wallpaper",
        "Per-display icon positions",
        "Recent-app card skin",
        "Back gesture visual indicator",
        "Volume HUD replacement layer",
        "Charging animation overlay",
        "Low-battery overlay",
        "Private app hide list",
        "App lock gate",
        "Search history controls",
        "Notification grouping",
        "Notification clear controls",
        "Control Center reorder",
        "Duo backup/export",
        "Duo restore/import",
        "QE crash log viewer",
        "QE safe mode"
    };
    private static final int LIVE_COUNT=50;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(10,14,24));build();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(DuoUi.dp(this,18),DuoUi.dp(this,34),DuoUi.dp(this,18),DuoUi.dp(this,28));root.setBackgroundColor(Color.rgb(10,14,24));
        scroll.addView(root,new ViewGroup.LayoutParams(-1,-2));

        root.addView(DuoUi.label(this,"DUO LAB • 100 FEATURE SPEC",11,Color.rgb(155,181,255),true));
        TextView title=DuoUi.label(this,"iPhone Duo 1.1 → 2.0",30,Color.WHITE,true);title.setPadding(0,DuoUi.dp(this,6),0,DuoUi.dp(this,6));root.addView(title);
        TextView sub=DuoUi.label(this,"1.1은 기반 엔진과 핵심 UX 50개를 실사용 가능한 상태로 묶고, 나머지 50개는 2.0 확장을 위한 내장 로드맵으로 관리합니다.",14,Color.rgb(192,204,228),false);sub.setPadding(0,0,0,DuoUi.dp(this,16));root.addView(sub);

        LinearLayout progress=new LinearLayout(this);progress.setGravity(Gravity.CENTER);progress.setBackground(DuoUi.stroke(Color.rgb(25,32,47),Color.rgb(55,68,92),24,this));
        TextView p=DuoUi.label(this,LIVE_COUNT+" / 100\nLIVE / FOUNDATION",20,Color.WHITE,true);p.setGravity(Gravity.CENTER);progress.addView(p,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,86)));root.addView(progress);

        for(int i=0;i<FEATURES.length;i++){
            boolean live=i<LIVE_COUNT;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(DuoUi.dp(this,12),DuoUi.dp(this,9),DuoUi.dp(this,12),DuoUi.dp(this,9));
            row.setBackground(DuoUi.rounded(live?Color.rgb(25,36,55):Color.rgb(20,25,36),18,this));
            TextView n=DuoUi.label(this,String.format("%02d",i+1),12,live?Color.rgb(139,226,183):Color.rgb(132,145,169),true);n.setGravity(Gravity.CENTER);
            row.addView(n,new LinearLayout.LayoutParams(DuoUi.dp(this,38),DuoUi.dp(this,40)));
            TextView name=DuoUi.label(this,FEATURES[i],14,Color.WHITE,true);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,DuoUi.dp(this,40),1f);np.leftMargin=DuoUi.dp(this,8);row.addView(name,np);
            TextView state=DuoUi.label(this,live?"LIVE":"2.0",11,live?Color.rgb(126,235,177):Color.rgb(166,179,208),true);state.setGravity(Gravity.CENTER);
            state.setBackground(DuoUi.rounded(live?Color.rgb(28,75,59):Color.rgb(44,50,68),14,this));row.addView(state,new LinearLayout.LayoutParams(DuoUi.dp(this,54),DuoUi.dp(this,28)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,58));rp.topMargin=DuoUi.dp(this,7);root.addView(row,rp);
        }
        setContentView(scroll);
    }
}
