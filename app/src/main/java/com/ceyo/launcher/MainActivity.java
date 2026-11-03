package com.ceyo.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    static class AppItem {
        String label;
        String pkg;
        Intent launch;
        Drawable icon;
    }

    private final List<AppItem> allApps = new ArrayList<>();
    private final List<AppItem> shown = new ArrayList<>();
    private AppAdapter adapter;
    private EditText search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#66000000"));
        root.setFitsSystemWindows(true);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("hh:mm");
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        clock.setTextColor(Color.WHITE);
        clock.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(clock, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        date.setTextColor(Color.parseColor("#DDFFFFFF"));
        date.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(date, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        search = new EditText(this);
        search.setHint("Uygulama ara...");
        search.setHintTextColor(Color.parseColor("#99FFFFFF"));
        search.setTextColor(Color.WHITE);
        search.setSingleLine(true);
        search.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        search.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMargins(dp(8), dp(16), dp(8), dp(8));
        root.addView(search, sp);

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(dp(8));
        grid.setSelector(android.R.color.transparent);
        grid.setVerticalScrollBarEnabled(false);
        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> launch(shown.get(pos)));
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            showMenu(shown.get(pos));
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) { applyFilter(e.toString()); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        applyFilter(search.getText().toString());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Ana ekran tuşuna basılınca aramayı temizle
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            search.setText("");
            search.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        // Launcher'dan geri tuşuyla çıkılmaz
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        allApps.clear();
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            AppItem item = new AppItem();
            item.label = String.valueOf(ri.loadLabel(pm));
            item.pkg = pkg;
            item.icon = ri.loadIcon(pm);
            item.launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(pkg, ri.activityInfo.name)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            allApps.add(item);
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(allApps, (a, b) -> collator.compare(a.label, b.label));
    }

    private void applyFilter(String q) {
        String needle = q.trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (AppItem a : allApps) {
            if (needle.isEmpty() || a.label.toLowerCase(Locale.getDefault()).contains(needle)) {
                shown.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void launch(AppItem a) {
        try {
            startActivity(a.launch);
        } catch (Exception ignored) {
        }
    }

    private void showMenu(final AppItem a) {
        new AlertDialog.Builder(this)
                .setTitle(a.label)
                .setItems(new CharSequence[]{"Uygulama bilgisi", "Kaldır"}, (d, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", a.pkg, null)));
                    } else {
                        startActivity(new Intent(Intent.ACTION_DELETE,
                                Uri.fromParts("package", a.pkg, null)));
                    }
                })
                .show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private class AppAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convert == null) {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(4), dp(8), dp(4), dp(8));

                icon = new ImageView(MainActivity.this);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

                label = new TextView(MainActivity.this);
                label.setTextColor(Color.WHITE);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(4);
                cell.addView(label, lp);
            } else {
                cell = (LinearLayout) convert;
                icon = (ImageView) cell.getChildAt(0);
                label = (TextView) cell.getChildAt(1);
            }
            AppItem a = shown.get(i);
            icon.setImageDrawable(a.icon);
            label.setText(a.label);
            return cell;
        }
    }
}
