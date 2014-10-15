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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import org.runnerup.R;
import org.runnerup.db.DBHelper;
import org.runnerup.gpstracker.GpsInformation;
import org.runnerup.gpstracker.GpsTracker;
import org.runnerup.hr.MockHRProvider;
import org.runnerup.notification.GpsBoundState;
import org.runnerup.notification.GpsSearchingState;
import org.runnerup.notification.NotificationManagerDisplayStrategy;
import org.runnerup.notification.NotificationStateManager;
import org.runnerup.util.Formatter;
import org.runnerup.util.TickListener;
import org.runnerup.workout.HeadsetButtonReceiver;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

import java.util.HashMap;

@TargetApi(Build.VERSION_CODES.FROYO)
public class StartFragment extends Fragment implements TickListener, GpsInformation {
    final static String TAB_BASIC = "basic";
    final static String TAB_INTERVAL = "interval";
    final static String TAB_ADVANCED = "advanced";
    final static String TAB_MANUAL = "manual";

    boolean skipStopGps = false;
    GpsTracker mGpsTracker = null;
    org.runnerup.gpstracker.GpsStatus mGpsStatus = null;

    Button startButton = null;
    TextView gpsInfoView1 = null;
    TextView gpsInfoView2 = null;
    View gpsInfoLayout = null;
    TextView hrInfo = null;

    ImageButton hrButton = null;
    TextView hrValueText = null;
    FrameLayout hrLayout = null;

    DBHelper mDBHelper = null;
    private SQLiteDatabase mDB = null;

    Formatter formatter = null;
    BroadcastReceiver catchButtonEvent = null;
    boolean allowHardwareKey = false;
    private NotificationStateManager notificationStateManager;
    private GpsSearchingState gpsSearchingState;
    private GpsBoundState gpsBoundState;
    private HashMap<String,FragmentFactory> fragmentFactories = new HashMap<String, FragmentFactory>();

    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBHelper = new DBHelper(getActivity());
        mDB = mDBHelper.getWritableDatabase();
        formatter = new Formatter(getActivity());

        bindGpsTracker();
        mGpsStatus = new org.runnerup.gpstracker.GpsStatus(getActivity());
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationStateManager = new NotificationStateManager(new NotificationManagerDisplayStrategy(notificationManager));
        gpsSearchingState = new GpsSearchingState(getActivity(), this);
        gpsBoundState = new GpsBoundState(getActivity());

        // if (getAllowStartStopFromHeadsetKey()) {
        // registerHeadsetListener();
        // }
                
        catchButtonEvent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startButton.performClick();
            }
        };

        fragmentFactories.put(TAB_BASIC, new FragmentFactory() {
            @Override
            public Fragment build() {
                return new BasicSettingsFragment();
            }
        });
        fragmentFactories.put(TAB_INTERVAL, new FragmentFactory() {
            @Override
            public Fragment build() {
                return new IntervalSettingsFragment();
            }
        });
        fragmentFactories.put(TAB_ADVANCED, new FragmentFactory() {
            @Override
            public Fragment build() {
                return new AdvancedSettingsFragment();
            }
        });
        fragmentFactories.put(TAB_MANUAL, new FragmentFactory() {
            @Override
            public Fragment build() {
                return new ManualSettingsFragment();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start, container, false);

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









        Intent intent = getActivityIntent();
        if(intent != null && intent.hasExtra("mode") && intent.getStringExtra("mode").equals(TAB_ADVANCED)) {
            showFragment(TAB_ADVANCED);
            intent.removeExtra("mode");
        }
        else {
            showFragment(TAB_BASIC);
        }

        return view;
    }

    private Intent getActivityIntent() {
        Activity activity = getActivity();
        if(activity == null)
            return null;

        return activity.getIntent();
    }

    private void showFragment(String tag) {
        Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.run_settings);
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        if(currentFragment != null && currentFragment == fragment)
            return;

        FragmentFactory factory = fragmentFactories.get(tag);
        if(factory == null)
            throw new IllegalStateException("no factory for tag " + tag);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.run_settings, factory.build(), tag)
                .commit();
    }

    private String getCurrentTabTag() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.run_settings);
        if(fragment == null)
            return null;

        return fragment.getTag();
    }

    private StartSettingsFragment getCurrentSettings() {
        return (StartSettingsFragment) getChildFragmentManager().findFragmentById(R.id.run_settings);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getAutoStartGps()) {
            /**
             * If autoStartGps, then stop it during pause
             */
            stopGps();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsBound || mGpsTracker == null) {
            bindGpsTracker();
        } else {
            onGpsTrackerBound();
        }
        if (getAllowStartStopFromHeadsetKey()) {
            unregisterHeadsetListener();
            registerHeadsetListener();
        }
    }

    private void registerHeadsetListener() {
        ComponentName mMediaReceiverCompName = new ComponentName(
                getActivity().getPackageName(), HeadsetButtonReceiver.class.getName());
        AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager
                .registerMediaButtonEventReceiver(mMediaReceiverCompName);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(2147483647);
        intentFilter.addAction("org.runnerup.START_STOP");
        getActivity().registerReceiver(catchButtonEvent, intentFilter);
    }

    private void unregisterHeadsetListener() {
        ComponentName mMediaReceiverCompName = new ComponentName(
                getActivity().getPackageName(), HeadsetButtonReceiver.class.getName());
        AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager
                .unregisterMediaButtonEventReceiver(mMediaReceiverCompName);
        try {
            getActivity().unregisterReceiver(catchButtonEvent);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
            } else {
                // unexpected, re-throw
                throw e;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGps();
        unbindGpsTracker();
        mGpsStatus = null;
        mGpsTracker = null;

        mDB.close();
        mDBHelper.close();
    }

    void onGpsTrackerBound() {
        if (getAutoStartGps()) {
            startGps();
        } else {
        }
        updateView();
    }

    boolean getAutoStartGps() {
        Context ctx = getActivity().getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getBoolean("pref_startgps", false);
    }

    boolean getAllowStartStopFromHeadsetKey() {
        Context ctx = getActivity().getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getBoolean("pref_keystartstop_active", true);
    }

    private void startGps() {
        System.err.println("StartActivity.startGps()");
        if (mGpsStatus != null && !mGpsStatus.isLogging())
            mGpsStatus.start(this);
        if (mGpsTracker != null && !mGpsTracker.isLogging())
            mGpsTracker.startLogging();

        notificationStateManager.displayNotificationState(gpsSearchingState);
    }

    private void stopGps() {
        System.err.println("StartActivity.stopGps() skipStop: " + this.skipStopGps);
        if (skipStopGps)
            return;

        if (mGpsStatus != null)
            mGpsStatus.stop(this);

        if (mGpsTracker != null)
            mGpsTracker.stopLogging();

        notificationStateManager.cancelNotification();
    }

    OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {

        @Override
        public void onTabChanged(String tabId) {
            if (tabId.contentEquals(TAB_BASIC))
                startButton.setVisibility(View.VISIBLE);
            else if (tabId.contentEquals(TAB_INTERVAL))
                startButton.setVisibility(View.VISIBLE);
            else if (tabId.contentEquals(TAB_ADVANCED)) {
                startButton.setVisibility(View.VISIBLE);
                //TODO: loadAdvanced(null);
            } else if (tabId.contentEquals(TAB_MANUAL)) {
                startButton.setText("Save activity");
            }
            updateView();
        }
    };

    OnClickListener startButtonClick = new OnClickListener() {
        public void onClick(View v) {

            if (getCurrentTabTag().contentEquals(TAB_MANUAL)) {
                Fragment fragment = getFragmentManager().findFragmentById(R.id.run_settings);
                if(fragment instanceof ManualSettingsFragment) {
                    ManualSettingsFragment manualSettingsFragment = (ManualSettingsFragment) fragment;
                    manualSettingsFragment.save();
                }
                return;
            } else if (!mGpsStatus.isEnabled()) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (!mGpsTracker.isLogging()) {
                startGps();
            } else if (mGpsStatus.isFixed()) {
                Context ctx = getActivity().getApplicationContext();
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
                StartSettingsFragment settingsFragment = getCurrentSettings();
                if(settingsFragment == null)
                    throw new IllegalStateException("settingsFragment is null!");

                SharedPreferences audioPref = settingsFragment.getAudioPreferences(pref);
                Workout w = settingsFragment.getWorkout(pref);
                
                skipStopGps = true;
                WorkoutBuilder.prepareWorkout(getResources(), pref, w,
                        TAB_BASIC.contentEquals(getCurrentTabTag()));
                WorkoutBuilder.addAudioCuesToWorkout(getResources(), w, audioPref);
                mGpsStatus.stop(StartFragment.this);
                mGpsTracker.setWorkout(w);

                Intent intent = new Intent(getActivity(), RunActivity.class);
                StartFragment.this.startActivityForResult(intent, 112);
                if (getAllowStartStopFromHeadsetKey()) {
                    unregisterHeadsetListener();
                }
                return;
            }
            updateView();
        }
    };

    OnClickListener hrButtonClick = new OnClickListener() {
        @Override
        public void onClick(View arg0) {

        }
    };

    private void updateView() {
        {
            int cnt0 = mGpsStatus.getSatellitesFixed();
            int cnt1 = mGpsStatus.getSatellitesAvailable();
            gpsInfoView1.setText("" + cnt0 + "/" + cnt1);
        }

        gpsInfoView2.setText(getGpsAccuracy());

        StartSettingsFragment fragment = getCurrentSettings();

        if (getCurrentTabTag().contentEquals(TAB_MANUAL)) {
            gpsInfoLayout.setVisibility(View.GONE);
            startButton.setEnabled(fragment != null && fragment.isStartButtonEnabled());
            startButton.setText("Save activity");
            return;
        } else if (!mGpsStatus.isEnabled()) {
            startButton.setEnabled(true);
            startButton.setText("Enable GPS");
        } else if (!mGpsStatus.isLogging()) {
            startButton.setEnabled(true);
            startButton.setText("Start GPS");
        } else if (!mGpsStatus.isFixed()) {
            startButton.setEnabled(false);
            startButton.setText("Waiting for GPS");
            notificationStateManager.displayNotificationState(gpsSearchingState);
        } else {
            startButton.setText("Start activity");
            startButton.setEnabled(fragment != null && fragment.isStartButtonEnabled());
            notificationStateManager.displayNotificationState(gpsBoundState);
        }
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

        if (mGpsTracker != null && mGpsTracker.isHRConfigured()) {
            hrLayout.setVisibility(View.VISIBLE);
            Integer hrVal = null;
            if (mGpsTracker.isHRConnected()) {
                hrVal = mGpsTracker.getCurrentHRValue();
            }
            if (hrVal != null) {
                hrButton.setEnabled(false);
                hrValueText.setText(Integer.toString(hrVal));
            } else {
                hrButton.setEnabled(true);
                hrValueText.setText("?");
            }
        }
        else {
            hrLayout.setVisibility(View.GONE);
        }

        if(fragment != null)
            fragment.update();
    }

    @Override
    public String getGpsAccuracy() {
        if (mGpsTracker != null) {
            Location l = mGpsTracker.getLastKnownLocation();

            if (l != null && l.getAccuracy() > 0) {
                return String.format(", %s m", l.getAccuracy());
            }
        }

        return "";
    }

    private boolean mIsBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mGpsTracker = ((GpsTracker.LocalBinder) service).getService();
            // Tell the user about this for our demo.
            StartFragment.this.onGpsTrackerBound();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mGpsTracker = null;
        }
    };

    void bindGpsTracker() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().getApplicationContext().bindService(new Intent(getActivity(), GpsTracker.class),
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
        if (data != null) {
            if (data.getStringExtra("url") != null)
                System.err.println("data.getStringExtra(\"url\") => " + data.getStringExtra("url"));
            if (data.getStringExtra("ex") != null)
                System.err.println("data.getStringExtra(\"ex\") => " + data.getStringExtra("ex"));
            if (data.getStringExtra("obj") != null)
                System.err.println("data.getStringExtra(\"obj\") => " + data.getStringExtra("obj"));
        }
        if (requestCode == 112) {
            skipStopGps = false;
            if (!mIsBound || mGpsTracker == null) {
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

    @Override
    public int getSatellitesAvailable() {
        return mGpsStatus.getSatellitesAvailable();
    }

    @Override
    public int getSatellitesFixed() {
        return mGpsStatus.getSatellitesFixed();
    }

    public SQLiteDatabase getDB() {
        return mDB;
    }





}

