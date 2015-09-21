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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import org.runnerup.R;
import org.runnerup.common.tracker.TrackerState;
import org.runnerup.common.util.Constants;
import org.runnerup.db.DBHelper;
import org.runnerup.hr.MockHRProvider;
import org.runnerup.notification.GpsBoundState;
import org.runnerup.notification.GpsSearchingState;
import org.runnerup.notification.NotificationManagerDisplayStrategy;
import org.runnerup.notification.NotificationStateManager;
import org.runnerup.tracker.GpsInformation;
import org.runnerup.tracker.GpsStatus;
import org.runnerup.tracker.Tracker;
import org.runnerup.tracker.component.TrackerHRM;
import org.runnerup.tracker.component.TrackerWear;
import org.runnerup.util.Formatter;
import org.runnerup.util.SafeParse;
import org.runnerup.util.TickListener;
import org.runnerup.view.start.AdvancedFragment;
import org.runnerup.view.start.BasicFragment;
import org.runnerup.view.start.IntervalFragment;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.widget.TitleSpinner.OnCloseDialogListener;
import org.runnerup.widget.TitleSpinner.OnSetValueListener;
import org.runnerup.workout.Workout.StepListEntry;
import org.runnerup.workout.WorkoutBuilder;

import java.util.ArrayList;
import java.util.List;

public class StartFragment extends Fragment implements TickListener, GpsInformation {
    final static int TAB_BASIC = 0;
    final static int TAB_INTERVAL = 1;
    final static int TAB_ADVANCED = 2;
    final static int TAB_MANUAL = 3;

    boolean skipStopGps = false;
    Tracker mTracker = null;
    GpsStatus mGpsStatus = null;

    Button startButton = null;
    TextView gpsInfoView1 = null;
    TextView gpsInfoView2 = null;
    View gpsInfoLayout = null;
    TextView hrInfo = null;

    ImageButton hrButton = null;
    TextView hrValueText = null;
    FrameLayout hrLayout = null;
    boolean batteryLevelMessageShowed = false;

    ImageButton wearButton = null;
    TextView wearValueText = null;
    FrameLayout wearLayout = null;

    boolean manualSetValue = false;
    DBHelper mDBHelper = null;
    SQLiteDatabase mDB = null;

    Formatter formatter = null;
    private NotificationStateManager notificationStateManager;
    private GpsSearchingState gpsSearchingState;
    private GpsBoundState gpsBoundState;
    private boolean headsetRegistered = false;

    private ViewPager pager;
    private TabLayout tabs;
    private Toolbar toolbar;
    private DrawerOpener drawerOpener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        drawerOpener = (DrawerOpener) activity;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBHelper = new DBHelper(getActivity());
        mDB = mDBHelper.getWritableDatabase();
        formatter = new Formatter(getActivity());
        mGpsStatus = new GpsStatus(getActivity());
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationStateManager = new NotificationStateManager(new NotificationManagerDisplayStrategy(notificationManager));
        gpsSearchingState = new GpsSearchingState(getActivity(), this);
        gpsBoundState = new GpsBoundState(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start, container, false);
        bindGpsTracker();

        tabs = (TabLayout) view.findViewById(R.id.tabs);
        pager = (ViewPager) view.findViewById(R.id.pager);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.start);
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerOpener.openDrawer();
            }
        });

        startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(startButtonClick);
        gpsInfoLayout = view.findViewById(R.id.gpsinfo);
        gpsInfoView1 = (TextView) view.findViewById(R.id.gps_info1);
        gpsInfoView2 = (TextView) view.findViewById(R.id.gps_info2);
        hrInfo = (TextView) view.findViewById(R.id.hr_info);

        hrButton = (ImageButton) view.findViewById(R.id.hr_button);
        hrButton.setOnClickListener(hrButtonClick);
        hrValueText = (TextView) view.findViewById(R.id.hr_value_text);
        hrLayout = (FrameLayout) view.findViewById(R.id.hr_layout);

        wearButton = (ImageButton) view.findViewById(R.id.wear_button);
        wearValueText = (TextView) view.findViewById(R.id.wear_value_text);
        wearLayout = (FrameLayout) view.findViewById(R.id.wear_layout);

        Intent i = getActivity().getIntent();
        if (i.getIntExtra("mode", 0) == TAB_ADVANCED) {
            pager.setCurrentItem(2);
            i.removeExtra("mode");
        }

        updateTargetView();
        setupPages();

        return view;
    }

    private void setupPages() {
        pager.setAdapter(new StartPagerAdapter(getChildFragmentManager()));
        tabs.setupWithViewPager(pager);
    }

    private class StartPagerAdapter extends FragmentPagerAdapter {
        public StartPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_BASIC:
                    return new BasicFragment();
                case TAB_INTERVAL:
                    return new IntervalFragment();
                case TAB_ADVANCED:
                    return new AdvancedFragment();
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_BASIC:
                    return getString(R.string.Basic);
                case TAB_INTERVAL:
                    return getString(R.string.Interval);
                case TAB_ADVANCED:
                    return getString(R.string.Advanced);
            }
            return super.getPageTitle(position);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerStartEventListener();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (pager.getCurrentItem() == TAB_ADVANCED) {
            loadAdvanced(null);
        }

        if (!mIsBound || mTracker == null) {
            bindGpsTracker();
        } else {
            onGpsTrackerBound();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getAutoStartGps()) {
            /**
             * If autoStartGps, then stop it during pause
             */
            stopGps();
        } else {
            if (mTracker != null &&
                ((mTracker.getState() == TrackerState.INITIALIZED) ||
                 (mTracker.getState() == TrackerState.INITIALIZING))) {
                Log.e(getClass().getName(), "mTracker.reset()");
                mTracker.reset();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterStartEventListener();
    }

    @Override
    public void onDestroy() {
        stopGps();
        unbindGpsTracker();
        mGpsStatus = null;
        mTracker = null;

        mDB.close();
        mDBHelper.close();
        super.onDestroy();
    }

    private final BroadcastReceiver startEventBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mTracker == null)
                        return;

                    if (!startButton.isEnabled())
                        return;

                    if (mTracker.getState() == TrackerState.INIT /* this will start gps */ ||
                            mTracker.getState() == TrackerState.INITIALIZED /* ...start a workout*/ ||
                            mTracker.getState() == TrackerState.CONNECTED) {
                        startButton.performClick();
                    }
                }
            });
        }
    };

    private void registerStartEventListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.Intents.START_WORKOUT);
        getActivity().registerReceiver(startEventBroadcastReceiver, intentFilter);

        if (StartActivityHeadsetButtonReceiver.getAllowStartStopFromHeadsetKey(getActivity())) {
            headsetRegistered = true;
            StartActivityHeadsetButtonReceiver.registerHeadsetListener(getActivity());
        }
    }

    private void unregisterStartEventListener() {
        try {
            getActivity().unregisterReceiver(startEventBroadcastReceiver);
        } catch (Exception e) {
        }
        if (headsetRegistered) {
            headsetRegistered = false;
            StartActivityHeadsetButtonReceiver.unregisterHeadsetListener(getActivity());
        }
    }

    void onGpsTrackerBound() {
        if (getAutoStartGps()) {
            startGps();
        } else {
            switch (mTracker.getState()) {
                case INIT:
                case CLEANUP:
                    mTracker.setup();
                    break;
                case INITIALIZING:
                case INITIALIZED:
                    break;
                case CONNECTING:
                case CONNECTED:
                case STARTED:
                case PAUSED:
                    assert(false);
                    return;
                case ERROR:
                    break;
            }
        }
        updateView();
    }

    boolean getAutoStartGps() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        return pref.getBoolean(getString(R.string.pref_startgps), false);
    }

    private void startGps() {
        Log.e(getClass().getName(), "StartActivity.startGps()");
        if (mGpsStatus != null && !mGpsStatus.isLogging())
            mGpsStatus.start(this);

        if (mTracker != null) {
            mTracker.connect();
        }

        notificationStateManager.displayNotificationState(gpsSearchingState);
    }

    private void stopGps() {
        Log.e(getClass().getName(), "StartActivity.stopGps() skipStop: " + this.skipStopGps);
        if (skipStopGps)
            return;

        if (mGpsStatus != null)
            mGpsStatus.stop(this);

        if (mTracker != null)
            mTracker.reset();

        notificationStateManager.cancelNotification();
    }

    protected void notificationBatteryLevel(int batteryLevel) {
        if ((batteryLevel < 0) || (batteryLevel > 100)) {
            return;
        }

        final String pref_key = getString(R.string.pref_battery_level_low_notification_discard);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int batteryLevelHighThreshold = SafeParse.parseInt(prefs.getString(getString(
                R.string.pref_battery_level_high_threshold), "75"), 75);
        if ((batteryLevel > batteryLevelHighThreshold) && (prefs.contains(pref_key))) {
            prefs.edit().remove(pref_key).commit();
            return;
        }

        int batteryLevelLowThreshold = SafeParse.parseInt(prefs.getString(getString(
                R.string.pref_battery_level_low_threshold), "15"), 15);
        if (batteryLevel > batteryLevelLowThreshold) {
            return;
        }

        if (prefs.getBoolean(pref_key, false)) {
            return;
        }

        AlertDialog.Builder prompt = new AlertDialog.Builder(getActivity());
        final CheckBox dontShowAgain = new CheckBox(getActivity());
        dontShowAgain.setText(getResources().getText(R.string.Do_not_show_again));
        prompt.setView(dontShowAgain);

        prompt.setCancelable(false);
        prompt.setMessage(getResources().getText(R.string.Low_HRM_battery_level)
            + "\n" + getResources().getText(R.string.Battery_level) + ": " + batteryLevel + "%");
        prompt.setTitle(getResources().getText(R.string.Warning));

        prompt.setPositiveButton(getResources().getText(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (dontShowAgain.isChecked()) {
                    prefs.edit().putBoolean(pref_key, true).commit();
                }
                return;
            }
        });

        prompt.show();
    }

    final OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {

        @Override
        public void onTabChanged(String tabId) {
            final int currentItem = pager.getCurrentItem();
            switch (currentItem) {
                case TAB_BASIC:
                    startButton.setVisibility(View.VISIBLE);
                    break;
                case TAB_INTERVAL:
                    startButton.setVisibility(View.VISIBLE);
                    break;
                case TAB_ADVANCED:
                    startButton.setVisibility(View.VISIBLE);
                    loadAdvanced(null);
                    break;
                case TAB_MANUAL:
                    startButton.setText(getString(R.string.Save_activity));
                    break;
            }
            updateView();
        }
    };

    final OnClickListener startButtonClick = new OnClickListener() {
        public void onClick(View v) {
            final int currentItem = pager.getCurrentItem();
            if (currentItem == 0) {
                manualSaveButtonClick.onClick(v);
                return;
            } else if (!mGpsStatus.isEnabled()) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (mTracker.getState() != TrackerState.CONNECTED) {
                startGps();
            } else {
                mGpsStatus.stop(StartFragment.this);
                /**
                 * unregister receivers
                 */
                unregisterStartEventListener();

                /**
                 * This will start the advancedWorkoutSpinner!
                 */
                //TODO: fix this
                //mTracker.setWorkout(prepareWorkout());
                //mTracker.start();

                skipStopGps = true;
                Intent intent = new Intent(getActivity(), RunActivity.class);
                StartFragment.this.startActivityForResult(intent, 112);
                notificationStateManager.cancelNotification(); // will be added by RunActivity
                return;
            }
            updateView();
        }
    };

    final OnClickListener hrButtonClick = new OnClickListener() {
        @Override
        public void onClick(View arg0) {

        }
    };

    private void updateView() {
        {
            int cnt0 = mGpsStatus.getSatellitesFixed();
            int cnt1 = mGpsStatus.getSatellitesAvailable();
            gpsInfoView1.setText(": " + cnt0 + "/" + cnt1);
        }

        gpsInfoView2.setText(getGpsAccuracy());

        int playIcon = 0;
        int currentItem = pager.getCurrentItem();
        if (currentItem == 0) {
            gpsInfoLayout.setVisibility(View.GONE);
            startButton.setEnabled(manualSetValue);
            startButton.setText(getString(R.string.Save_activity));
            return;
        } else if (!mGpsStatus.isEnabled()) {
            startButton.setEnabled(true);
            startButton.setText(getString(R.string.Enable_GPS));
        } else if (!mGpsStatus.isLogging()) {
            startButton.setEnabled(true);
            startButton.setText(getString(R.string.Start_GPS));
        } else if (!mGpsStatus.isFixed()) {
            startButton.setEnabled(false);
            startButton.setText(getString(R.string.Waiting_for_GPS));
            notificationStateManager.displayNotificationState(gpsSearchingState);
        } else {
            playIcon = R.drawable.ic_av_play_arrow;
            startButton.setText(getString(R.string.Start_activity));
            if (currentItem != 3 /* TODO: || advancedWorkout != null*/) {
                startButton.setEnabled(true);
            } else {
                startButton.setEnabled(false);
            }
            notificationStateManager.displayNotificationState(gpsBoundState);
        }
        startButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, playIcon, 0);
        gpsInfoLayout.setVisibility(View.VISIBLE);

        {
            Resources res = getResources();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String btDeviceName = prefs.getString(res.getString(R.string.pref_bt_name), null);
            if (btDeviceName != null) {
                hrInfo.setText(btDeviceName);
            } else {
                hrInfo.setText("");
                if (MockHRProvider.NAME.contentEquals(prefs.getString(
                        res.getString(R.string.pref_bt_provider), ""))) {
                    final String btAddress = "mock: "
                            + prefs.getString(res.getString(R.string.pref_bt_address), "???");
                    hrInfo.setText(btAddress);
                }
            }
        }

        boolean hideHR = true;
        boolean hideWear = true;
        if (mTracker != null) {
            if (mTracker.isComponentConfigured(TrackerHRM.NAME)) {
                hideHR = false;
                Integer hrVal = null;
                if (mTracker.isComponentConnected(TrackerHRM.NAME)) {
                    hrVal = mTracker.getCurrentHRValue();
                }
                if (hrVal != null) {
                    hrButton.setEnabled(false);
                    hrValueText.setText(String.valueOf(hrVal));

                    if (!batteryLevelMessageShowed) {
                        batteryLevelMessageShowed = true;
                        notificationBatteryLevel(mTracker.getCurrentBatteryLevel());
                    }
                } else {
                    hrButton.setEnabled(true);
                    hrValueText.setText("?");
                }
            }
            if (mTracker.isComponentConfigured(TrackerWear.NAME)) {
                hideWear = false;
                if (mTracker.isComponentConnected(TrackerWear.NAME)) {
                    wearValueText.setVisibility(View.GONE);
                } else {
                    wearValueText.setText("?");
                    wearValueText.setVisibility(View.VISIBLE);
                }
            }
        }
        if (hideHR)
            hrLayout.setVisibility(View.GONE);
        else
            hrLayout.setVisibility(View.VISIBLE);

        if (hideWear)
            wearLayout.setVisibility(View.GONE);
        else
            wearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public String getGpsAccuracy() {
        if (mTracker != null) {
            Location l = mTracker.getLastKnownLocation();

            if (l != null && l.getAccuracy() > 0) {
                return String.format(", %s m", l.getAccuracy());
            }
        }

        return "";
    }

    private boolean mIsBound = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mTracker = ((Tracker.LocalBinder) service).getService();
            // Tell the user about this for our demo.
            StartFragment.this.onGpsTrackerBound();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mTracker = null;
        }
    };

    void bindGpsTracker() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().getApplicationContext().bindService(new Intent(getActivity(), Tracker.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void unbindGpsTracker() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        registerStartEventListener();

        if (data != null) {
            if (data.getStringExtra("url") != null)
                Log.e(getClass().getName(), "data.getStringExtra(\"url\") => " + data.getStringExtra("url"));
            if (data.getStringExtra("ex") != null)
                Log.e(getClass().getName(), "data.getStringExtra(\"ex\") => " + data.getStringExtra("ex"));
            if (data.getStringExtra("obj") != null)
                Log.e(getClass().getName(), "data.getStringExtra(\"obj\") => " + data.getStringExtra("obj"));
        }
        if (requestCode == 112) {
            skipStopGps = false;
            if (!mIsBound || mTracker == null) {
                bindGpsTracker();
            } else {
                onGpsTrackerBound();
            }
        } else {
            updateView();
        }
    }

    @Override
    public void onTick() {
        updateView();
    }

    final OnCloseDialogListener simpleTargetTypeClick = new OnCloseDialogListener() {

        @Override
        public void onClose(TitleSpinner spinner, boolean ok) {
            if (ok) {
                updateTargetView();
            }
        }
    };

    void updateTargetView() {
        /* TODO:
        Dimension dim = Dimension.valueOf(simpleTargetType.getValueInt());
        if (dim == null) {
            simpleTargetPaceValue.setEnabled(false);
            simpleTargetHrz.setEnabled(false);
        } else {
            switch (dim) {
                case PACE:
                    simpleTargetPaceValue.setEnabled(true);
                    simpleTargetPaceValue.setVisibility(View.VISIBLE);
                    simpleTargetHrz.setVisibility(View.GONE);
                    break;
                case HRZ:
                    simpleTargetPaceValue.setVisibility(View.GONE);
                    simpleTargetHrz.setEnabled(true);
                    simpleTargetHrz.setVisibility(View.VISIBLE);
            }
        }
        */
    }

    final OnSetValueListener intervalTypeSetValue = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            boolean time = (newValue == 0);
            /* TODO:
            intervalTime.setVisibility(time ? View.VISIBLE : View.GONE);
            intervalDistance.setVisibility(time ? View.GONE : View.VISIBLE);
            */
            return newValue;
        }
    };

    final OnSetValueListener intervalRestTypeSetValue = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            boolean time = (newValue == 0);
            /* TODO:
            intervalRestTime.setVisibility(time ? View.VISIBLE : View.GONE);
            intervalRestDistance.setVisibility(time ? View.GONE : View.VISIBLE);
            */
            return newValue;
        }
    };

    void loadAdvanced(String name) {
        Context ctx = getActivity().getApplicationContext();
        if (name == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            name = pref.getString(getResources().getString(R.string.pref_advanced_workout), "");
        }
        //TODO: fix commented
        //advancedWorkout = null;
        if ("".contentEquals(name))
            return;
        try {
            /*
            advancedWorkout = WorkoutSerializer.readFile(ctx, name);
            advancedWorkoutStepsAdapter.steps = advancedWorkout.getStepList();
            advancedWorkoutStepsAdapter.notifyDataSetChanged();
            advancedDownloadWorkoutButton.setVisibility(View.GONE);
             */
        } catch (Exception ex) {
            ex.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.Failed_to_load_workout));
            builder.setMessage("" + ex.toString());
            builder.setPositiveButton(getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
            return;
        }
    }

    @Override
    public int getSatellitesAvailable() {
        return mGpsStatus.getSatellitesAvailable();
    }

    @Override
    public int getSatellitesFixed() {
        return mGpsStatus.getSatellitesFixed();
    }

    final class WorkoutStepsAdapter extends BaseAdapter {

        List<StepListEntry> steps = new ArrayList<StepListEntry>();

        @Override
        public int getCount() {
            return steps.size();
        }

        @Override
        public Object getItem(int position) {
            return steps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            StepListEntry entry = steps.get(position);
            StepButton button = null;
            if (convertView != null && convertView instanceof StepButton) {
                button = (StepButton) convertView;
            } else {
                button = new StepButton(getActivity(), null);
            }
            button.setStep(entry.step);
            button.setPadding(entry.level * 7, 0, 0, 0);
            button.setOnChangedListener(onWorkoutChanged);
            return button;
        }
    }

    final Runnable onWorkoutChanged = new Runnable() {
        @Override
        public void run() {
            //TODO: fix
            /*
            String name = advancedWorkoutSpinner.getValue().toString();
            if (advancedWorkout != null) {
                Context ctx = getActivity().getApplicationContext();
                try {
                    WorkoutSerializer.writeFile(ctx, name, advancedWorkout);
                } catch (Exception ex) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.Failed_to_load_workout));
                    builder.setMessage("" + ex.toString());
                    builder.setPositiveButton(getString(R.string.OK),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                    return;
                }
            }
            */
        }
    };

    final OnSetValueListener onSetTimeValidator = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {

            if (WorkoutBuilder.validateSeconds(newValue))
                return newValue;

            throw new IllegalArgumentException("Unable to parse time value: " + newValue);
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            return newValue;
        }

    };

    final OnSetValueListener onSetValueManual = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            manualSetValue = true;
            startButton.setEnabled(true);
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            manualSetValue = true;
            startButton.setEnabled(true);
            return newValue;
        }
    };

    void setManualPace(String distance, String duration) {
        //TODO: fix
        /*
        Log.e(getClass().getName(), "distance: >" + distance + "< duration: >" + duration + "<");
        double dist = SafeParse.parseDouble(distance, 0); // convert to meters
        long seconds = SafeParse.parseSeconds(duration, 0);
        if (dist == 0 || seconds == 0) {
            manualPace.setVisibility(View.GONE);
            return;
        }
        double pace = seconds / dist;
        manualPace.setValue(formatter.formatPace(Formatter.TXT_SHORT, pace));
        manualPace.setVisibility(View.VISIBLE);
        return;
        */
    }

    final OnSetValueListener onSetManualDistance = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            /* TODO: fix
            setManualPace(newValue, manualDuration.getValue().toString());
            startButton.setEnabled(true);
            */
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            startButton.setEnabled(true);
            return newValue;
        }

    };

    final OnSetValueListener onSetManualDuration = new OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            /* TODO: fix
            setManualPace(manualDistance.getValue().toString(), newValue);
            */
            startButton.setEnabled(true);
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            startButton.setEnabled(true);
            return newValue;
        }
    };

    final OnClickListener manualSaveButtonClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            /* TODO: fix
            ContentValues save = new ContentValues();
            int sport = manualSport.getValueInt();
            CharSequence date = manualDate.getValue();
            CharSequence time = manualTime.getValue();
            CharSequence distance = manualDistance.getValue();
            CharSequence duration = manualDuration.getValue();
            String notes = manualNotes.getText().toString().trim();
            long start_time = 0;

            if (notes.length() > 0) {
                save.put(DB.ACTIVITY.COMMENT, notes);
            }
            double dist = 0;
            if (distance.length() > 0) {
                dist = Double.parseDouble(distance.toString()); // convert to
                                                                // meters
                save.put(DB.ACTIVITY.DISTANCE, dist);
            }
            long secs = 0;
            if (duration.length() > 0) {
                secs = SafeParse.parseSeconds(duration.toString(), 0);
                save.put(DB.ACTIVITY.TIME, secs);
            }
            if (date.length() > 0) {
                DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                try {
                    Date d = df.parse(date.toString());
                    start_time += d.getTime() / 1000;
                } catch (ParseException e) {
                }
            }
            if (time.length() > 0) {
                DateFormat df = android.text.format.DateFormat.getTimeFormat(getActivity());
                try {
                    Date d = df.parse(time.toString());
                    start_time += d.getTime() / 1000;
                } catch (ParseException e) {
                }
            }
            save.put(DB.ACTIVITY.START_TIME, start_time);

            save.put(DB.ACTIVITY.SPORT, sport);
            long id = mDB.insert(DB.ACTIVITY.TABLE, null, save);

            ContentValues lap = new ContentValues();
            lap.put(DB.LAP.ACTIVITY, id);
            lap.put(DB.LAP.LAP, 0);
            lap.put(DB.LAP.INTENSITY, DB.INTENSITY.ACTIVE);
            lap.put(DB.LAP.TIME, secs);
            lap.put(DB.LAP.DISTANCE, dist);
            mDB.insert(DB.LAP.TABLE, null, lap);

            Intent intent = new Intent(getActivity(), DetailActivity.class);
            intent.putExtra("mode", "save");
            intent.putExtra("ID", id);
            startActivityForResult(intent, 0);
            */
        }
    };
}
