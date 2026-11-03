package com.ceyo.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Dikey kaydırıcı (parlaklık / ses). */
public class VSlider extends View {
    public interface Listener {
        void onChange(float value, boolean done);
    }

    private float value;
    private boolean dragging;
    private Listener listener;
    private final String glyph;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public VSlider(Context c, String glyph) {
        super(c);
        this.glyph = glyph;
        bg.setColor(0x55FFFFFF);
        fill.setColor(0xFFFFFFFF);
        txt.setColor(0xFF333333);
        txt.setTextSize(Util.dp(c, 22));
        txt.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) { listener = l; }
    public boolean isDragging() { return dragging; }

    public void setValue(float v) {
        value = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float rad = Util.dp(getContext(), 22);
        rect.set(0, 0, w, h);
        cv.drawRoundRect(rect, rad, rad, bg);
        cv.save();
        path.reset();
        path.addRoundRect(rect, rad, rad, Path.Direction.CW);
        cv.clipPath(path);
        cv.drawRect(0, h * (1f - value), w, h, fill);
        cv.restore();
        cv.drawText(glyph, w / 2f, h - Util.dp(getContext(), 22), txt);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            dragging = true;
        }
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) {
            setValue(1f - e.getY() / Math.max(1, getHeight()));
            if (listener != null) listener.onChange(value, false);
        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            if (listener != null) listener.onChange(value, true);
        }
        return true;
    }
}
