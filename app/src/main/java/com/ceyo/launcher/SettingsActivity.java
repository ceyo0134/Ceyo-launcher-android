package com.ceyo.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class SettingsActivity extends Activity {
    private static final int[] COLORS = {
            0xFF6A1B9A, 0xFF8E24AA, 0xFF4A148C, 0xFFAD1457, 0xFFC62828, 0xFFEF6C00, 0xFFF9A825,
            0xFF2E7D32, 0xFF00897B, 0xFF00838F, 0xFF1565C0, 0xFF283593, 0xFF37474F, 0xFF000000,
            0xFFFFFFFF, 0xB3000000};

    private PanelView preview;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF121212);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(10), dp(14), dp(24));
        sv.addView(col);
        setContentView(sv);

        col.addView(label("Önizleme (kartlara dokunarak denemeyin, aşağıdan seçin)", 13, 0x99FFFFFF));
        preview = new PanelView(this, () -> {});
        col.addView(preview, new LinearLayout.LayoutParams(-1, -2));

        for (int i = 0; i < Prefs.NAMES.length; i++) {
            final int idx = i;
            col.addView(label(Prefs.NAMES[i], 17, Color.WHITE), margin(18));
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout sw = new LinearLayout(this);
            hs.addView(sw);
            for (final int color : COLORS) {
                View v = new View(this);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(color);
                g.setStroke(dp(2), 0x66FFFFFF);
                v.setBackground(g);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
                lp.rightMargin = dp(10);
                sw.addView(v, lp);
                v.setOnClickListener(x -> {
                    Prefs.setColor(this, idx, color);
                    preview.applyTheme();
                });
            }
            col.addView(hs, margin(8));

            LinearLayout btns = new LinearLayout(this);
            btns.addView(button("Fotoğraf seç", v -> pick(idx)), new LinearLayout.LayoutParams(0, -2, 1f));
            btns.addView(button("Sıfırla", v -> {
                Prefs.reset(this, idx);
                preview.applyTheme();
            }), new LinearLayout.LayoutParams(0, -2, 1f));
            col.addView(btns);
        }

        col.addView(label("İzinler", 17, Color.WHITE), margin(26));
        col.addView(button("1) Ayarları değiştirme izni (parlaklık, döndürme)",
                v -> startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())))));
        col.addView(button("2) Varsayılan ana ekranı seç",
                v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preview != null) {
            preview.applyTheme();
            preview.refresh();
        }
    }

    private void pick(int idx) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, 100 + idx);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null || req < 100) return;
        try {
            Uri u = data.getData();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(u);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / s > 1400) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            in = getContentResolver().openInputStream(u);
            Bitmap bm = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (bm == null) throw new Exception("decode");
            Prefs.setImage(this, req - 100, bm);
            preview.applyTheme();
        } catch (Exception e) {
            Toast.makeText(this, "Fotoğraf eklenemedi", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView label(String t, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        return v;
    }

    private Button button(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    private LinearLayout.LayoutParams margin(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(top);
        return lp;
    }

    private int dp(int v) {
        return Util.dp(this, v);
    }
}
