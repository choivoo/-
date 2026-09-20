package com.sycompany.iphoneduo;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * 1.2: no visible gesture overlay.
 * Galaxy system gesture navigation handles Home / Back / Recents.
 * This optional service remains non-visual for future automation hooks only.
 */
public class DuoAccessibilityService extends AccessibilityService {
    @Override protected void onServiceConnected(){ super.onServiceConnected(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
}
