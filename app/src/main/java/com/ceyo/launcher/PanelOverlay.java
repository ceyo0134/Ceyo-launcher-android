package com.ceyo.launcher;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.WindowManager;

/** Erişilebilirlik hizmeti üzerinden her uygulamanın üstüne paneli çizer. */
final class PanelOverlay {
    private PanelOverlay() {}

    private static PanelHost view;
    private static WindowManager wm;

    static boolean isShown() { return view != null; }

    static void show(Context c) {
        if (view != null) return;
        wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        view = new PanelHost(c, PanelOverlay::hide);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        try {
            wm.addView(view, lp);
            view.requestFocus();
        } catch (Exception e) {
            view = null;
        }
    }

    static void hide() {
        if (view == null) return;
        try {
            wm.removeView(view);
        } catch (Exception ignored) {
        }
        view = null;
    }
}
