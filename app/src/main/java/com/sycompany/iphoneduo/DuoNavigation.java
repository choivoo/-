package com.sycompany.iphoneduo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public final class DuoNavigation {
    private DuoNavigation(){}

    public static void openGestureSettings(Context c){
        Intent[] tries=new Intent[]{
            new Intent("com.android.settings.NAVIGATION_MODE_SETTINGS"),
            component("com.android.settings","com.android.settings.Settings$NavigationBarSettingsActivity"),
            component("com.android.settings","com.samsung.android.settings.navigationbar.NavigationBarSettings"),
            new Intent(Settings.ACTION_DISPLAY_SETTINGS)
        };
        for(Intent i:tries){
            try{
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if(i.resolveActivity(c.getPackageManager())!=null){c.startActivity(i);return;}
            }catch(Exception ignored){}
        }
        c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private static Intent component(String pkg,String cls){
        Intent i=new Intent(Intent.ACTION_MAIN);i.setComponent(new ComponentName(pkg,cls));return i;
    }

    public static String mode(Context c){
        try{
            int m=Settings.Secure.getInt(c.getContentResolver(),"navigation_mode",-1);
            if(m==2)return "시스템 스와이프 제스처 감지";
            if(m==0)return "3버튼 내비게이션 감지";
            if(m==1)return "2버튼/레거시 모드 감지";
        }catch(Exception ignored){}
        return "현재 모드는 기기 설정에서 확인";
    }
}
