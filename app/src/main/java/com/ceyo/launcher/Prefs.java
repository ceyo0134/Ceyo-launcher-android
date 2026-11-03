package com.ceyo.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Prefs {
    private Prefs() {}

    public static final int PANEL = 0, CONN = 1, SLIDERS = 2, TILES = 3;
    public static final String[] NAMES = {
            "Panel arka planı", "Bağlantılar kartı", "Parlaklık / ses kartı", "Düğmeler kartı"};
    private static final int[] DEFAULTS = {0xF01A0B2E, 0xFF6A1B9A, 0xFF7B1FA2, 0xFF8E24AA};

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("theme", Context.MODE_PRIVATE);
    }

    public static int color(Context c, int i) {
        return sp(c).getInt("color" + i, DEFAULTS[i]);
    }

    public static File imageFile(Context c, int i) {
        return new File(c.getFilesDir(), "card_" + i + ".jpg");
    }

    public static boolean hasImage(Context c, int i) {
        return sp(c).getBoolean("img" + i, false) && imageFile(c, i).exists();
    }

    public static void setColor(Context c, int i, int color) {
        imageFile(c, i).delete();
        sp(c).edit().putInt("color" + i, color).putBoolean("img" + i, false).apply();
    }

    public static void setImage(Context c, int i, Bitmap b) throws IOException {
        FileOutputStream out = new FileOutputStream(imageFile(c, i));
        try {
            b.compress(Bitmap.CompressFormat.JPEG, 88, out);
        } finally {
            out.close();
        }
        sp(c).edit().putBoolean("img" + i, true).apply();
    }

    public static void reset(Context c, int i) {
        imageFile(c, i).delete();
        sp(c).edit().remove("color" + i).putBoolean("img" + i, false).apply();
    }

}
