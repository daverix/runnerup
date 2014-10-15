/*
 * Copyright (C) 2012 - 2014 jonas.oreland@gmail.com
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import org.runnerup.R;
import org.runnerup.util.FileUtil;

import java.io.IOException;

public class SettingsFragment extends PreferenceFragment {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.settings);

        {
            Preference btn = findPreference("exportdb");
            btn.setOnPreferenceClickListener(onExportClick);
        }
        {
            Preference btn = findPreference("importdb");
            btn.setOnPreferenceClickListener(onImportClick);
        }

        Preference whatsnew = findPreference("whats_new");
        whatsnew.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                WhatsNewFragment whatsNewFragment = new WhatsNewFragment();
                whatsNewFragment.show(getFragmentManager(), "whatsnewdialog");
                return true;
            }
        });

        if (!hasHR(getActivity())) {
            Preference pref = findPreference("cue_configure_hrzones");
            getPreferenceScreen().removePreference(pref);
        }
    }

    public static boolean hasHR(Context ctx) {
        Resources res = ctx.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String btAddress = prefs.getString(res.getString(R.string.pref_bt_address), null);
        String btProviderName = prefs.getString(res.getString(R.string.pref_bt_provider), null);
        if (btProviderName != null && btAddress != null)
            return true;
        return false;
    }

    private String getDbFile() {
        String from = getActivity().getFilesDir().getPath() + "/../databases/runnerup.db";
        return from;
    }

    OnPreferenceClickListener onExportClick = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String dstdir = Environment.getExternalStorageDirectory().getPath();
            builder.setTitle("Export runnerup.db to " + dstdir);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }

            };
            String from = getDbFile();
            String to = dstdir + "/runnerup.db.export";
            try {
                int cnt = FileUtil.copyFile(to, from);
                builder.setMessage("Copied " + cnt + " bytes");
                builder.setPositiveButton("Great!", listener);
            } catch (IOException e) {
                builder.setMessage("Exception: " + e.toString());
                builder.setNegativeButton("Darn!", listener);
            }
            builder.show();
            return false;
        }
    };

    OnPreferenceClickListener onImportClick = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String srcdir = Environment.getExternalStorageDirectory().getPath();
            builder.setTitle("Import runnerup.db from " + srcdir);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }

            };
            String to = getDbFile();
            String from = srcdir + "/runnerup.db.export";
            try {
                int cnt = FileUtil.copyFile(to, from);
                builder.setMessage("Copied " + cnt + " bytes");
                builder.setPositiveButton("Great!", listener);
            } catch (IOException e) {
                builder.setMessage("Exception: " + e.toString());
                builder.setNegativeButton("Darn!", listener);
            }
            builder.show();
            return false;
        }
    };
}
