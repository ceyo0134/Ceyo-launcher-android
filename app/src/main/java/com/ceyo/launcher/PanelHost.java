package com.ceyo.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Paneli kaplayan, yukarı kaydırınca ya da boşluğa dokununca kapanan katman. */
public class PanelHost extends FrameLayout {
    private final Runnable onClose;
    private final PanelView panel;
    private float downY;
    private final int threshold;

    public PanelHost(Context c, Runnable onClose) {
        super(c);
        this.onClose = onClose;
        threshold = Util.dp(c, 56);
        setBackgroundColor(0x99000000);
        panel = new PanelView(c, onClose);
        addView(panel, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        setFocusableInTouchMode(true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downY = e.getY();
        } else if (a == MotionEvent.ACTION_MOVE && downY - e.getY() > threshold) {
            onClose.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downY = e.getY();
        } else if (a == MotionEvent.ACTION_MOVE && downY - e.getY() > threshold) {
            onClose.run();
        } else if (a == MotionEvent.ACTION_UP && e.getY() > panel.getBottom()) {
            onClose.run();
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) onClose.run();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }
}
