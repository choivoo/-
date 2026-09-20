package com.sycompany.iphoneduo;
import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
public class DuoNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        Notification n=sbn.getNotification();
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE,"알림");
        CharSequence text=n.extras.getCharSequence(Notification.EXTRA_TEXT,"");
        String app=sbn.getPackageName();
        try{app=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(sbn.getPackageName(),0)).toString();}catch(Exception ignored){}
        NotificationHub.add(new NotificationHub.Item(app,title==null?"알림":title.toString(),text==null?"":text.toString(),sbn.getPostTime()));
    }
}
