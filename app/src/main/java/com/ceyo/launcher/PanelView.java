package com.ceyo.launcher;

import android.accessibilityservice.AccessibilityService;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

/** Kendi hızlı ayarlar panelimiz: 3 kart (bağlantılar, parlaklık/ses, düğmeler). */
public class PanelView extends Card {
    private final Runnable close;
    private final AudioManager audio;
    private final ContentResolver cr;
    private Card connCard, sliderCard, tileCard;
    private final TextView[] connIcon = new TextView[3];
    private final TextView[] connSub = new TextView[3];
    private final TextView[] tileIcon = new TextView[8];
    private VSlider bright, vol;
    private CameraManager cam;
    private String torchId;
    private boolean torchOn;
    private CameraManager.TorchCallback torchCb;

    private static final String[] TILE_GLYPH = {"🔦", "✈", "☀", "🔕", "📡", "✂", "⟳", "📍"};
    private static final String[] TILE_LABEL = {"Flaş ışığı", "Uçak Modu", "Uyarlanabilir Parlaklık",
            "Sessiz", "Erişim Noktası", "Ekran görüntüsü", "Otomatik döndür", "Konum"};

    public PanelView(Context c, Runnable close) {
        super(c, 0);
        this.close = close;
        audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        cr = c.getContentResolver();
        apply(Prefs.PANEL);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Util.dp(c, 14);
        col.setPadding(pad, Util.statusBar(c) + Util.dp(c, 8), pad, pad + Util.dp(c, 8));
        addView(col, new FrameLayout_LP());

        col.addView(buildHeader(c));

        LinearLayout row = new LinearLayout(c);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, Util.dp(c, 200));
        rp.topMargin = Util.dp(c, 12);
        col.addView(row, rp);

        connCard = new Card(c, 24);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1.25f);
        cp.rightMargin = Util.dp(c, 10);
        row.addView(connCard, cp);
        buildConn(c);

        sliderCard = new Card(c, 24);
        row.addView(sliderCard, new LinearLayout.LayoutParams(0, -1, 1f));
        buildSliders(c);

        tileCard = new Card(c, 24);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = Util.dp(c, 12);
        col.addView(tileCard, tp);
        buildTiles(c);

        applyTheme();
    }

    private static class FrameLayout_LP extends android.widget.FrameLayout.LayoutParams {
        FrameLayout_LP() { super(-1, -2); }
    }

    public void applyTheme() {
        apply(Prefs.PANEL);
        connCard.apply(Prefs.CONN);
        sliderCard.apply(Prefs.SLIDERS);
        tileCard.apply(Prefs.TILES);
    }

    private View buildHeader(final Context c) {
        LinearLayout h = new LinearLayout(c);
        h.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        TextClock t = new TextClock(c);
        t.setFormat24Hour("HH:mm");
        t.setFormat12Hour("hh:mm");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        t.setTextColor(Color.WHITE);
        TextClock d = new TextClock(c);
        d.setFormat24Hour("dd MMM, EEE");
        d.setFormat12Hour("dd MMM, EEE");
        d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        d.setTextColor(0xDDFFFFFF);
        left.addView(t);
        left.addView(d);
        h.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView gear = new TextView(c);
        gear.setText("⚙");
        gear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        gear.setTextColor(Color.WHITE);
        gear.setPadding(Util.dp(c, 12), Util.dp(c, 8), Util.dp(c, 12), Util.dp(c, 8));
        gear.setOnClickListener(v -> {
            close.run();
            c.startActivity(new Intent(c, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
        h.addView(gear);
        return h;
    }

    private void buildConn(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Util.dp(c, 10);
        col.setPadding(p, p, p, p);
        connCard.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -1));
        String[] glyph = {"📶", "⇅", "B"};
        String[] title = {"Wi-Fi", "Mobil veri", "Bluetooth"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout r = new LinearLayout(c);
            r.setGravity(Gravity.CENTER_VERTICAL);
            TextView ic = new TextView(c);
            ic.setText(glyph[i]);
            ic.setGravity(Gravity.CENTER);
            ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            ic.setTextColor(Color.WHITE);
            r.addView(ic, new LinearLayout.LayoutParams(Util.dp(c, 44), Util.dp(c, 44)));
            LinearLayout tx = new LinearLayout(c);
            tx.setOrientation(LinearLayout.VERTICAL);
            TextView tt = new TextView(c);
            tt.setText(title[i]);
            tt.setTextColor(Color.WHITE);
            tt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tt.setMaxLines(1);
            TextView st = new TextView(c);
            st.setTextColor(0xCCFFFFFF);
            st.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tx.addView(tt);
            tx.addView(st);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
            tlp.leftMargin = Util.dp(c, 8);
            r.addView(tx, tlp);
            r.setOnClickListener(v -> connClick(idx));
            col.addView(r, new LinearLayout.LayoutParams(-1, 0, 1f));
            connIcon[i] = ic;
            connSub[i] = st;
        }
    }

    private void buildSliders(final Context c) {
        LinearLayout row = new LinearLayout(c);
        int p = Util.dp(c, 12);
        row.setPadding(p, p, p, p);
        sliderCard.addView(row, new android.widget.FrameLayout.LayoutParams(-1, -1));
        bright = new VSlider(c, "☀");
        vol = new VSlider(c, "🔊");
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -1, 1f);
        a.rightMargin = Util.dp(c, 6);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, -1, 1f);
        b.leftMargin = Util.dp(c, 6);
        row.addView(bright, a);
        row.addView(vol, b);
        bright.setListener((v, done) -> {
            if (!Settings.System.canWrite(c)) {
                if (done) needWrite();
                return;
            }
            try {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, Math.max(1, Math.round(v * 255)));
            } catch (Exception ignored) {
            }
        });
        vol.setListener((v, done) -> {
            try {
                int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(v * max), 0);
            } catch (Exception ignored) {
            }
        });
    }

    private void buildTiles(Context c) {
        LinearLayout grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        int p = Util.dp(c, 8);
        grid.setPadding(p, p, p, p);
        tileCard.addView(grid, new android.widget.FrameLayout.LayoutParams(-1, -2));
        for (int r = 0; r < 2; r++) {
            LinearLayout line = new LinearLayout(c);
            for (int k = 0; k < 4; k++) {
                final int i = r * 4 + k;
                LinearLayout cell = new LinearLayout(c);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(Util.dp(c, 2), Util.dp(c, 8), Util.dp(c, 2), Util.dp(c, 8));
                TextView ic = new TextView(c);
                ic.setText(TILE_GLYPH[i]);
                ic.setGravity(Gravity.CENTER);
                ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                ic.setTextColor(Color.WHITE);
                cell.addView(ic, new LinearLayout.LayoutParams(Util.dp(c, 58), Util.dp(c, 58)));
                TextView lb = new TextView(c);
                lb.setText(TILE_LABEL[i]);
                lb.setGravity(Gravity.CENTER);
                lb.setTextColor(Color.WHITE);
                lb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                lb.setMaxLines(2);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = Util.dp(c, 4);
                cell.addView(lb, lp);
                cell.setOnClickListener(v -> tileClick(i));
                line.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
                tileIcon[i] = ic;
            }
            grid.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    // ---------- durum ----------

    private static void paint(TextView v, boolean on, int onColor, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Util.dp(v.getContext(), radiusDp));
        g.setColor(on ? onColor : 0x55FFFFFF);
        v.setBackground(g);
    }

    private boolean tileActive(int i) {
        try {
            switch (i) {
                case 0: return torchOn;
                case 1: return Settings.Global.getInt(cr, "airplane_mode_on", 0) == 1;
                case 2: return Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1;
                case 3: return audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
                case 6: return Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
                case 7: return locationOn();
                default: return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean locationOn() {
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 28) return lm.isLocationEnabled();
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressWarnings("deprecation")
    public void refresh() {
        Context c = getContext();
        boolean wifi = false, mobile = false, bt = false;
        try {
            wifi = ((WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
        } catch (Exception ignored) {
        }
        try {
            NetworkInfo ni = ((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            mobile = ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE;
        } catch (Exception ignored) {
        }
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            bt = a != null && a.isEnabled();
        } catch (Exception ignored) {
        }
        setConn(0, wifi, wifi ? "Açık" : "Kapalı");
        setConn(1, mobile, mobile ? "Açık" : "Dokun");
        setConn(2, bt, bt ? "Açık" : "Kapalı");
        for (int i = 0; i < 8; i++) paint(tileIcon[i], tileActive(i), 0xFF2979FF, 18);
        try {
            if (!bright.isDragging())
                bright.setValue(Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f);
            if (!vol.isDragging())
                vol.setValue(audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        / (float) audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        } catch (Exception ignored) {
        }
    }

    private void setConn(int i, boolean on, String sub) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(on ? 0xFF2ECC71 : 0x66FFFFFF);
        connIcon[i].setBackground(g);
        connSub[i].setText(sub);
    }

    // ---------- eylemler ----------

    private void open(Intent i) {
        close.run();
        try {
            getContext().startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            toast("Bu ayar ekranı açılamadı");
        }
    }

    private void toast(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    private boolean needWrite() {
        Context c = getContext();
        if (Settings.System.canWrite(c)) return false;
        toast("Ceyo Launcher için 'ayarları değiştirme' iznini aç");
        open(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.getPackageName())));
        return true;
    }

    private void connClick(int i) {
        if (i == 0) {
            open(new Intent(Build.VERSION.SDK_INT >= 29 ? Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                    : Settings.ACTION_WIFI_SETTINGS));
        } else if (i == 1) {
            open(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
        } else {
            open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        }
    }

    private void tileClick(int i) {
        try {
            boolean on = tileActive(i);
            switch (i) {
                case 0:
                    if (cam != null && torchId != null) cam.setTorchMode(torchId, !torchOn);
                    else toast("Flaş bulunamadı");
                    break;
                case 1:
                    open(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
                    return;
                case 2:
                    if (needWrite()) return;
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, on ? 0 : 1);
                    break;
                case 3:
                    audio.setRingerMode(on ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_VIBRATE);
                    break;
                case 4:
                    try {
                        open(new Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"));
                    } catch (Exception e) {
                        open(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                    return;
                case 5:
                    screenshot();
                    return;
                case 6:
                    if (needWrite()) return;
                    Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, on ? 0 : 1);
                    break;
                case 7:
                    open(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    return;
            }
        } catch (Exception e) {
            toast("İşlem yapılamadı");
        }
        refresh();
    }

    private void screenshot() {
        final ShadeService svc = ShadeService.instance;
        if (svc == null || Build.VERSION.SDK_INT < 28) {
            toast("Önce ayarlardan erişilebilirlik hizmetini aç");
            return;
        }
        close.run();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT), 500);
    }

    // ---------- yaşam döngüsü ----------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            cam = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            for (String id : cam.getCameraIdList()) {
                Boolean f = cam.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (f != null && f) {
                    torchId = id;
                    break;
                }
            }
            torchCb = new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String id, boolean enabled) {
                    if (id.equals(torchId)) {
                        torchOn = enabled;
                        refresh();
                    }
                }
            };
            cam.registerTorchCallback(torchCb, new Handler(Looper.getMainLooper()));
        } catch (Exception ignored) {
        }
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            if (cam != null && torchCb != null) cam.unregisterTorchCallback(torchCb);
        } catch (Exception ignored) {
        }
    }
}
