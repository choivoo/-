package com.sycompany.iphoneduo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DuoPrefs {
    private static final String FILE="duo_home_12";
    private DuoPrefs(){}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    public static boolean duoIcons(Context c){return p(c).getBoolean("duo_icons",true);}
    public static void setDuoIcons(Context c,boolean v){p(c).edit().putBoolean("duo_icons",v).apply();}
    public static boolean labels(Context c){return p(c).getBoolean("labels",true);}
    public static void setLabels(Context c,boolean v){p(c).edit().putBoolean("labels",v).apply();}
    public static boolean oled(Context c){return p(c).getBoolean("oled",false);}
    public static void setOled(Context c,boolean v){p(c).edit().putBoolean("oled",v).apply();}
    public static boolean widgets(Context c){return p(c).getBoolean("widgets",true);}
    public static void setWidgets(Context c,boolean v){p(c).edit().putBoolean("widgets",v).apply();}
    public static int iconSize(Context c){return p(c).getInt("icon_size",58);}
    public static void setIconSize(Context c,int v){p(c).edit().putInt("icon_size",v).apply();}
    public static int grid(Context c){return p(c).getInt("grid_add",0);}
    public static void setGrid(Context c,int v){p(c).edit().putInt("grid_add",v).apply();}
    public static int glass(Context c){return p(c).getInt("glass",72);}
    public static void setGlass(Context c,int v){p(c).edit().putInt("glass",v).apply();}
    public static int anim(Context c){return p(c).getInt("anim",100);}
    public static void setAnim(Context c,int v){p(c).edit().putInt("anim",v).apply();}
    public static String folderName(Context c){return p(c).getString("folder_name","즐겨찾기");}
    public static void setFolderName(Context c,String v){p(c).edit().putString("folder_name",v).apply();}
    public static List<String> folder(Context c){return decode(p(c).getString("folder_apps",""));}
    public static void setFolder(Context c,List<String> v){p(c).edit().putString("folder_apps",encode(v)).apply();}
    public static List<String> order(Context c){return decode(p(c).getString("app_order",""));}
    public static void setOrder(Context c,List<String> v){p(c).edit().putString("app_order",encode(v)).apply();}
    public static List<String> hidden(Context c){return decode(p(c).getString("hidden_apps",""));}
    public static void setHidden(Context c,List<String> v){p(c).edit().putString("hidden_apps",encode(v)).apply();}
    private static String encode(List<String> l){StringBuilder b=new StringBuilder();for(String s:l){if(s==null)continue;if(b.length()>0)b.append("\n");b.append(s.replace("\n",""));}return b.toString();}
    private static List<String> decode(String s){if(s==null||s.isEmpty())return new ArrayList<>();return new ArrayList<>(Arrays.asList(s.split("\n")));}
}
