package com.sycompany.iphoneduo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public final class NotificationHub {
    public static final class Item {
        public final String app,title,text; public final long when;
        public Item(String app,String title,String text,long when){this.app=app;this.title=title;this.text=text;this.when=when;}
    }
    private static final List<Item> items=Collections.synchronizedList(new ArrayList<>());
    private NotificationHub(){}
    public static void add(Item item){synchronized(items){items.add(0,item);while(items.size()>20)items.remove(items.size()-1);}}
    public static List<Item> snapshot(){synchronized(items){return new ArrayList<>(items);}}
}
