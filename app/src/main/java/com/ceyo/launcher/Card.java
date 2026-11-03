package com.ceyo.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Yuvarlak köşeli, rengi ya da fotoğrafı seçilebilen kart. */
public class Card extends FrameLayout {
    private final ImageView img;
    private final int radius;

    public Card(Context c, int radiusDp) {
        super(c);
        radius = Util.dp(c, radiusDp);
        img = new ImageView(c);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setVisibility(GONE);
        addView(img, new FrameLayout.LayoutParams(-1, -1));
        if (radius > 0) {
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                }
            });
        }
    }

    public void apply(int idx) {
        Context c = getContext();
        if (Prefs.hasImage(c, idx)) {
            Bitmap b = BitmapFactory.decodeFile(Prefs.imageFile(c, idx).getPath());
            if (b != null) {
                img.setImageBitmap(b);
                img.setColorFilter(0x44000000, PorterDuff.Mode.SRC_ATOP);
                img.setVisibility(VISIBLE);
                setBackgroundColor(Color.BLACK);
                return;
            }
        }
        img.setImageDrawable(null);
        img.setVisibility(GONE);
        setBackgroundColor(Prefs.color(c, idx));
    }
}
