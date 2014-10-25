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

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import org.runnerup.R;
import org.runnerup.db.DBHelper;
import org.runnerup.util.Constants.DB;
import org.runnerup.util.FileUtil;
import org.runnerup.util.Formatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
    private ViewPager pager;
    private ImageButton startRun;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setPreferences();

        pager = (ViewPager) findViewById(R.id.pager);
        startRun = (ImageButton) findViewById(R.id.btn_start_run);
        startRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StartActivity.class);
                startActivity(intent);
            }
        });

        List<FragmentPage> pages = new ArrayList<FragmentPage>();
        pages.add(new FragmentPage("FEED", "feed", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new FeedFragment();
            }
        }));
        pages.add(new FragmentPage("HISTORY", "history", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new HistoryFragment();
            }
        }));
        pager.setAdapter(new FragmentPageAdapter(getSupportFragmentManager(), pages));
        pager.setOnPageChangeListener(this);

        int currentTab = savedInstanceState != null ? savedInstanceState.getInt("currentTab") : 0;

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        addTabs(pages, currentTab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("currentTab", pager.getCurrentItem());
    }

    private void addTabs(List<FragmentPage> pages, int currentTab) {
        ActionBar ab = getSupportActionBar();
        int i=0;
        for(FragmentPage page : pages) {
            ab.addTab(ab.newTab()
                    .setText(page.getTitle())
                    .setTag(page.getTag())
                    .setTabListener(this), currentTab == i++);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private enum UpgradeState {
        UNKNOWN, NEW, UPGRADE, DOWNGRADE, SAME;
    }

    private void setPreferences() {
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
        boolean km = Formatter.getUseKilometers(pref, editor);

        if (upgradeState == UpgradeState.NEW) {
            editor.putString(getResources().getString(R.string.pref_autolap),
                    Double.toString(km ? Formatter.km_meters : Formatter.mi_meters));
        }
        editor.commit();



        // clear basicTargetType between application startup/shutdown
        pref.edit().remove("basicTargetType").commit();

        System.err.println("app-version: " + versionCode + ", upgradeState: " + upgradeState
                + ", km: " + km);

        PreferenceManager.setDefaultValues(this, R.layout.settings, false);
        PreferenceManager.setDefaultValues(this, R.layout.audio_cue_settings, true);

        handleBundled(getApplicationContext().getAssets(), "bundled", getFilesDir().getPath()+ "/..");

        if (upgradeState == UpgradeState.UPGRADE) {
            whatsNew();
        }
    }

    void handleBundled(AssetManager mgr, String src, String dst) {
        String list[] = null;
        try {
            list = mgr.list(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (list != null) {
            for (int i = 0; i < list.length; ++i) {
                boolean isFile = false;
                String add = list[i];
                try {
                    InputStream is = mgr.open(src + File.separator + add);
                    is.close();
                    isFile = true;
                } catch (Exception ex) {
                }

                System.err.println("Found: " + dst + ", " + add + ", isFile: " + isFile);
                if (isFile == false) {
                    File dstDir = new File(dst + File.separator + add);
                    dstDir.mkdir();
                    if (!dstDir.isDirectory()) {
                        System.err.println("Failed to copy " + add + " as \"" + dst
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
                        System.err.println("Skip: " + tmp +
                                ", isDirectory(): " + dstFile.isDirectory() +
                                ", isFile(): " + dstFile.isFile());
                        continue;
                    }

                    String key = "install_bundled_" + add;
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                    if (pref.contains(key)) {
                        System.err.println("Skip: " + key);
                        continue;

                    }

                    pref.edit().putBoolean(key, true).commit();
                    System.err.println("Copying: " + tmp);
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

    public void whatsNew() {
        WhatsNewFragment dialog = new WhatsNewFragment();
        dialog.show(getSupportFragmentManager(), "whatsnewdialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if(item.getItemId() == R.id.menu_rate) {
            try {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        }
        else if(item.getItemId() == R.id.menu_whatsnew) {
            WhatsNewFragment whatsNewFragment = new WhatsNewFragment();
            whatsNewFragment.show(getSupportFragmentManager(), "whatsnewdialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
