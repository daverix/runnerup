/*
 * Copyright (C) 2012 - 2013 jonas.oreland@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.runnerup.view;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import org.runnerup.R;
import org.runnerup.common.util.Constants.DB;
import org.runnerup.db.DBHelper;
import org.runnerup.util.FileUtil;
import org.runnerup.util.Formatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainLayout extends AppCompatActivity {

    private enum UpgradeState {
        UNKNOWN, NEW, UPGRADE, DOWNGRADE, SAME
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        int versionCode = 0;
        UpgradeState upgradeState = UpgradeState.UNKNOWN;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = pref.edit();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
            int version = pref.getInt("app-version", -1);
            if (version == -1) {
                upgradeState = UpgradeState.NEW;
            } else if (versionCode == version) {
                upgradeState = UpgradeState.SAME;
            } else if (versionCode > version) {
                upgradeState = UpgradeState.UPGRADE;
            } else if (versionCode < version) {
                upgradeState = UpgradeState.DOWNGRADE;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        editor.putInt("app-version", versionCode);
        boolean km = Formatter.getUseKilometers(getResources(), pref, editor);

        if (upgradeState == UpgradeState.NEW) {
            editor.putString(getResources().getString(R.string.pref_autolap),
                    Double.toString(km ? Formatter.km_meters : Formatter.mi_meters));
        }
        editor.commit();

        // clear basicTargetType between application startup/shutdown
        pref.edit().remove(getString(R.string.pref_basic_target_type)).commit();

        Log.e(getClass().getName(), "app-version: " + versionCode + ", upgradeState: " + upgradeState
                + ", km: " + km);

        PreferenceManager.setDefaultValues(this, R.layout.settings, false);
        PreferenceManager.setDefaultValues(this, R.layout.audio_cue_settings, true);

        setupBottomNavigation();

        FragmentManager fm = getSupportFragmentManager();
        if(fm.findFragmentById(R.id.content) == null) {
            fm.beginTransaction().add(R.id.content, new StartFragment()).commit();
        }

        if (upgradeState == UpgradeState.UPGRADE) {
            whatsNew();
        }

        handleBundled(getApplicationContext().getAssets(), "bundled", getFilesDir().getPath()
                + "/..");

        // if we were called from an intent-filter because user opened "runnerup.db.export", load it
        final Uri data = getIntent().getData();
        if (data != null) {
            String filePath = null;
            if ("content".equals(data.getScheme())) {
                Cursor cursor = this.getContentResolver().query(data, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
                cursor.moveToFirst();
                filePath = cursor.getString(0);
                cursor.close();
            } else {
                filePath = data.getPath();
            }
            Log.i(getClass().getSimpleName(), "Importing database from " + filePath);
            DBHelper.importDatabase(MainLayout.this, filePath);
        }
    }

    private void setupBottomNavigation() {
        TabLayout navigationBar = (TabLayout) findViewById(R.id.navigationBar);
        navigationBar.addTab(navigationBar.newTab()
            .setIcon(R.drawable.ic_tab_main_unselected)
            .setText(R.string.Start));

        navigationBar.addTab(navigationBar.newTab()
                .setIcon(R.drawable.ic_tab_feed_unselected)
                .setText(R.string.feed));

        navigationBar.addTab(navigationBar.newTab()
                .setIcon(R.drawable.ic_tab_history_unselected)
                .setText(R.string.History));

        navigationBar.addTab(navigationBar.newTab()
                .setIcon(R.drawable.ic_tab_setup_unselected)
                .setText(R.string.Settings));

        navigationBar.setTabTextColors(ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.green));
        navigationBar.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTabs(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                switchTabs(tab.getPosition());
            }
        });
    }

    private void switchTabs(int position) {
        switch (position) {
            case 0:
                replace(new StartFragment());
                break;
            case 1:
                replace(new FeedFragment());
                break;
            case 2:
                replace(new HistoryFragment());
                break;
            case 3:
                //TODO: should be converted to a fragment
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
    }

    private void replace(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .commit();
    }

    public void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content, fragment)
                .commit();
    }

    public void navigateUp() {
        getSupportFragmentManager().popBackStack();
    }

    void handleBundled(AssetManager mgr, String src, String dst) {
        String list[] = null;
        try {
            list = mgr.list(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (list != null) {
            for (String aList : list) {
                boolean isFile = false;
                String add = aList;
                try {
                    InputStream is = mgr.open(src + File.separator + add);
                    is.close();
                    isFile = true;
                } catch (Exception ex) {
                }

                Log.e(getClass().getName(), "Found: " + dst + ", " + add + ", isFile: " + isFile);
                if (isFile == false) {
                    File dstDir = new File(dst + File.separator + add);
                    dstDir.mkdir();
                    if (!dstDir.isDirectory()) {
                        Log.e(getClass().getName(), "Failed to copy " + add + " as \"" + dst
                                + "\" is not a directory!");
                        continue;
                    }
                    if (dst == null)
                        handleBundled(mgr, src + File.separator + add, add);
                    else
                        handleBundled(mgr, src + File.separator + add, dst + File.separator + add);
                } else {
                    String tmp = dst + File.separator + add;
                    File dstFile = new File(tmp);
                    if (dstFile.isDirectory() || dstFile.isFile()) {
                        Log.e(getClass().getName(), "Skip: " + tmp +
                                ", isDirectory(): " + dstFile.isDirectory() +
                                ", isFile(): " + dstFile.isFile());
                        continue;
                    }

                    String key = "install_bundled_" + add;
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                    if (pref.contains(key)) {
                        Log.e(getClass().getName(), "Skip: " + key);
                        continue;

                    }

                    pref.edit().putBoolean(key, true).commit();
                    Log.e(getClass().getName(), "Copying: " + tmp);
                    InputStream input = null;
                    try {
                        input = mgr.open(src + File.separator + add);
                        FileUtil.copy(input, tmp);
                        handleHooks(src, add);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtil.close(input);
                    }
                }
            }
        }
    }

    private void handleHooks(String path, String file) {
        if (file.contains("_audio_cues.xml")) {
            String name = file.substring(0, file.indexOf("_audio_cues.xml"));

            DBHelper mDBHelper = new DBHelper(this);
            SQLiteDatabase mDB = mDBHelper.getWritableDatabase();

            ContentValues tmp = new ContentValues();
            tmp.put(DB.AUDIO_SCHEMES.NAME, name);
            tmp.put(DB.AUDIO_SCHEMES.SORT_ORDER, 0);
            mDB.insert(DB.AUDIO_SCHEMES.TABLE, null, tmp);

            mDB.close();
            mDBHelper.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public final OnClickListener onRateClick = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            try {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    public void whatsNew() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.whatsnew, null);
        WebView wv = (WebView) view.findViewById(R.id.web_view1);
        builder.setTitle(getString(R.string.Whats_new));
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.Rate_RunnerUp), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onRateClick.onClick(null);
            }

        });
        builder.setNegativeButton(getString(R.string.Dismiss), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
        wv.loadUrl("file:///android_asset/changes.html");
    }
}
