package com.sycompany.iphoneduo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class HomeCustomizationActivity extends Activity {
    private LinearLayout root;
    @Override protected void onCreate(Bundle b){super.onCreate(b);build();}

    private void build(){
        ScrollView s=new ScrollView(this);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(DuoUi.dp(this,18),DuoUi.dp(this,34),DuoUi.dp(this,18),DuoUi.dp(this,30));
        root.setBackgroundColor(Color.rgb(9,13,22));s.addView(root,new ViewGroup.LayoutParams(-1,-2));
        root.addView(DuoUi.label(this,"DUO HOME • 1.2",11,Color.rgb(151,180,255),true));
        TextView title=DuoUi.label(this,"홈 화면 사용자화",30,Color.WHITE,true);title.setPadding(0,DuoUi.dp(this,6),0,DuoUi.dp(this,8));root.addView(title);
        root.addView(DuoUi.label(this,"아이콘 · 그리드 · 위젯 · 글래스 · 애니메이션 · 폴더를 저장하고 Fold 화면마다 그대로 불러옵니다.",14,Color.rgb(194,204,226),false));

        addCheck("Duo 아이콘 프레임","앱 원본 아이콘을 둥근 Duo 스타일 타일 안에 표시",DuoPrefs.duoIcons(this),v->DuoPrefs.setDuoIcons(this,v));
        addCheck("앱 이름 표시","홈 아이콘 아래 라벨 표시",DuoPrefs.labels(this),v->DuoPrefs.setLabels(this,v));
        addCheck("Duo 위젯 스트립","시계 · 날짜 · 배터리 · Fold 프로필",DuoPrefs.widgets(this),v->DuoPrefs.setWidgets(this,v));
        addCheck("OLED Black","홈 배경을 완전 검정 계열로 변경",DuoPrefs.oled(this),v->DuoPrefs.setOled(this,v));

        addSeek("아이콘 크기",48,76,DuoPrefs.iconSize(this),DuoPrefs::setIconSize);
        addSeek("추가 그리드 열",0,2,DuoPrefs.grid(this),DuoPrefs::setGrid);
        addSeek("Glass 강도",25,110,DuoPrefs.glass(this),DuoPrefs::setGlass);
        addSeek("애니메이션 속도",50,160,DuoPrefs.anim(this),DuoPrefs::setAnim);

        TextView folder=button("폴더 이름 변경","현재: "+DuoPrefs.folderName(this));
        folder.setOnClickListener(v->renameFolder());root.addView(folder,buttonParams());

        TextView nav=button("Galaxy 시스템 스와이프 제스처","현재: "+DuoNavigation.mode(this)+"\n버튼식 내비게이션을 실제 삼성 제스처로 전환");
        nav.setOnClickListener(v->DuoNavigation.openGestureSettings(this));root.addView(nav,buttonParams());

        TextView reset=button("홈 배치 초기화","드래그로 저장된 앱 순서와 즐겨찾기 폴더를 초기화");
        reset.setOnClickListener(v->{DuoPrefs.setOrder(this,new java.util.ArrayList<>());DuoPrefs.setFolder(this,new java.util.ArrayList<>());});
        root.addView(reset,buttonParams());

        TextView done=button("Duo Home으로 돌아가기","변경 사항을 즉시 적용");
        done.setOnClickListener(v->{startActivity(new android.content.Intent(this,HomeActivity.class));finish();});root.addView(done,buttonParams());
        setContentView(s);
    }

    interface BoolSave{void save(boolean v);}
    private void addCheck(String title,String sub,boolean value,BoolSave save){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,10),DuoUi.dp(this,14),DuoUi.dp(this,10));
        card.setBackground(DuoUi.rounded(Color.rgb(23,29,42),20,this));
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);texts.addView(DuoUi.label(this,title,15,Color.WHITE,true));texts.addView(DuoUi.label(this,sub,12,Color.rgb(182,194,218),false));
        card.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));
        CheckBox check=new CheckBox(this);check.setChecked(value);check.setOnCheckedChangeListener((b,v)->save.save(v));card.addView(check);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,68));p.topMargin=DuoUi.dp(this,9);root.addView(card,p);
    }

    interface IntSave{void save(android.content.Context c,int v);}
    private void addSeek(String title,int min,int max,int value,IntSave save){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(DuoUi.dp(this,14),DuoUi.dp(this,9),DuoUi.dp(this,14),DuoUi.dp(this,9));
        card.setBackground(DuoUi.rounded(Color.rgb(23,29,42),20,this));
        TextView label=DuoUi.label(this,title+"  •  "+value,14,Color.WHITE,true);card.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,value-min));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean from){int v=min+p;label.setText(title+"  •  "+v);if(from)save.save(HomeCustomizationActivity.this,v);}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });card.addView(bar,new LinearLayout.LayoutParams(-1,DuoUi.dp(this,38)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,76));p.topMargin=DuoUi.dp(this,9);root.addView(card,p);
    }

    private void renameFolder(){
        EditText e=new EditText(this);e.setText(DuoPrefs.folderName(this));e.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("폴더 이름").setView(e).setPositiveButton("저장",(d,w)->{
            String n=e.getText().toString().trim();if(!n.isEmpty())DuoPrefs.setFolderName(this,n);recreate();
        }).setNegativeButton("취소",null).show();
    }

    private TextView button(String title,String sub){
        TextView t=DuoUi.label(this,title+"\n"+sub,14,Color.WHITE,true);t.setPadding(DuoUi.dp(this,16),0,DuoUi.dp(this,16),0);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setBackground(DuoUi.stroke(Color.rgb(28,36,54),Color.rgb(54,69,98),21,this));DuoUi.clickScale(t);return t;
    }
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,DuoUi.dp(this,68));p.topMargin=DuoUi.dp(this,10);return p;}
}
