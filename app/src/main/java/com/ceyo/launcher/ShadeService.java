package com.ceyo.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;

/**
 * Sistemin bildirim/hızlı ayarlar çubuğu açılınca onu geri kapatıp yerine kendi panelimizi gösterir.
 */
public class ShadeService extends AccessibilityService {
    static ShadeService instance;
    private long lastShow;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        PanelOverlay.hide();
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        if (pkg == null || !pkg.toString().equals("com.android.systemui")) return;
        String cls = e.getClassName() == null ? "" : e.getClassName().toString();
        Prefs.logWindow(this, cls);
        if (!Prefs.shadeEnabled(this) || PanelOverlay.isShown()) return;

        String c = cls.toLowerCase();
        boolean shade = c.contains("shade") || c.contains("notificationpanel")
                || c.contains("statusbarwindow") || c.contains("qspanel") || c.contains("quicksettings");
        if (!shade) return;

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) return;

        long now = System.currentTimeMillis();
        if (now - lastShow < 800) return;
        lastShow = now;

        performGlobalAction(GLOBAL_ACTION_BACK);
        PanelOverlay.show(this);
    }

    @Override
    public void onInterrupt() {
    }
}
