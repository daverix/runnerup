package org.runnerup.view;

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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.runnerup.R;
import org.runnerup.db.DBHelper;
import org.runnerup.gpstracker.GpsInformation;
import org.runnerup.gpstracker.GpsStatus;
import org.runnerup.gpstracker.GpsTracker;
import org.runnerup.hr.MockHRProvider;
import org.runnerup.notification.GpsBoundState;
import org.runnerup.notification.GpsSearchingState;
import org.runnerup.notification.NotificationManagerDisplayStrategy;
import org.runnerup.notification.NotificationStateManager;
import org.runnerup.util.Formatter;
import org.runnerup.util.TickListener;
import org.runnerup.workout.HeadsetButtonReceiver;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends ActionBarActivity implements GpsInformation, TickListener {
    private Toolbar toolbar;
    private SlidingTabLayout slidingTabLayout;
    private ViewPager pager;
    private ImageButton hrButton;
    private TextView hrValueText;
    private FrameLayout hrLayout;
    private View gpsInfoLayout;
    private Button startButton;
    private TextView gpsInfoText;
    private TextView hrInfo;

    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;
    private Formatter formatter;
    private GpsStatus mGpsStatus;
    private NotificationStateManager notificationStateManager;
    private GpsSearchingState gpsSearchingState;
    private GpsBoundState gpsBoundState;
    private GpsTracker mGpsTracker;
    private boolean skipStopGps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start_run);

        pager = (ViewPager) findViewById(R.id.pager);
        toolbar = (Toolbar) findViewById(R.id.actionbar);
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.tabs);
        hrButton = (ImageButton) findViewById(R.id.hr_button);
        hrValueText = (TextView) findViewById(R.id.hr_value_text);
        hrLayout = (FrameLayout) findViewById(R.id.hr_layout);
        gpsInfoLayout = findViewById(R.id.gpsinfo);
        startButton = (Button) findViewById(R.id.start_button);
        gpsInfoText = (TextView) findViewById(R.id.gps_info_text);
        hrInfo = (TextView) findViewById(R.id.hrinfo);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRun();
            }
        });
        hrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setTitle("Start run");
        setSupportActionBar(toolbar);

        List<FragmentPage> pages = new ArrayList<FragmentPage>();
        pages.add(new FragmentPage("BASIC", "basic", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new BasicSettingsFragment();
            }
        }));
        pages.add(new FragmentPage("INTERVAL", "interval", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new IntervalSettingsFragment();
            }
        }));
        pages.add(new FragmentPage("ADVANCED", "advanced", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new AdvancedSettingsFragment();
            }
        }));
        pages.add(new FragmentPage("MANUAL", "manual", new FragmentFactory() {
            @Override
            public Fragment build() {
                return new ManualSettingsFragment();
            }
        }));

        pager.setAdapter(new FragmentPageAdapter(getSupportFragmentManager(), pages));
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateView();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        slidingTabLayout.setViewPager(pager);


        mDBHelper = new DBHelper(this);
        mDB = mDBHelper.getWritableDatabase();
        formatter = new Formatter(this);

        bindGpsTracker();
        mGpsStatus = new org.runnerup.gpstracker.GpsStatus(this);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationStateManager = new NotificationStateManager(new NotificationManagerDisplayStrategy(notificationManager));
        gpsSearchingState = new GpsSearchingState(this, this);
        gpsBoundState = new GpsBoundState(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    final BroadcastReceiver catchButtonEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startButton.performClick();
        }
    };

    void onGpsTrackerBound() {
        if (getAutoStartGps()) {
            startGps();
        } else {
        }
        updateView();
    }


    boolean getAutoStartGps() {
        Context ctx = getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getBoolean("pref_startgps", false);
    }

    boolean getAllowStartStopFromHeadsetKey() {
        Context ctx = getApplicationContext();
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

    private void registerHeadsetListener() {
        ComponentName mMediaReceiverCompName = new ComponentName(
                getPackageName(), HeadsetButtonReceiver.class.getName());
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.registerMediaButtonEventReceiver(mMediaReceiverCompName);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(2147483647);
        intentFilter.addAction("org.runnerup.START_STOP");
        registerReceiver(catchButtonEvent, intentFilter);
    }

    private void unregisterHeadsetListener() {
        ComponentName mMediaReceiverCompName = new ComponentName(
                getPackageName(), HeadsetButtonReceiver.class.getName());
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.unregisterMediaButtonEventReceiver(mMediaReceiverCompName);
        try {
            unregisterReceiver(catchButtonEvent);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
            } else {
                // unexpected, re-throw
                throw e;
            }
        }
    }

    private void startRun() {
        /*
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
            StartSettings settingsFragment = getCurrentSettings();
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
        */
    }

    private void updateView() {
        switch (pager.getCurrentItem()) {
            case 0:
            case 1:
            case 2:
                startButton.setVisibility(View.VISIBLE);
                break;
            case 3:
            default:
                startButton.setVisibility(View.VISIBLE);
                startButton.setText("Save activity");
                break;
        }

        gpsInfoText.setText(String.format("%s %d/%d %s",
                getString(R.string.gps_info_text_txt),
                mGpsStatus.getSatellitesFixed(),
                mGpsStatus.getSatellitesAvailable(),
                getGpsAccuracy()));

        int currentItem = pager.getCurrentItem();
        if (currentItem == 4) {
            gpsInfoLayout.setVisibility(View.GONE);
            startButton.setEnabled(false); //TODO: get current fragment to check if it can be enabled
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
            startButton.setEnabled(false);  //TODO: get current fragment to check if it can be enabled
            notificationStateManager.displayNotificationState(gpsBoundState);
        }
        gpsInfoLayout.setVisibility(View.VISIBLE);


        Resources res = getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String btDeviceName = prefs.getString(res.getString(R.string.pref_bt_name), null);
        if (btDeviceName != null) {
            hrInfo.setText(btDeviceName);
            hrInfo.setVisibility(View.VISIBLE);
        } else {
            if (MockHRProvider.NAME.contentEquals(prefs.getString(
                    res.getString(R.string.pref_bt_provider), ""))) {
                final String btAddress = "mock: "
                        + prefs.getString(res.getString(R.string.pref_bt_address), "???");
                hrInfo.setText(btAddress);
                hrInfo.setVisibility(View.VISIBLE);
            }
            else {
                hrInfo.setVisibility(View.GONE);
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

        //TODO: update current fragment
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
            onGpsTrackerBound();
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
        getApplicationContext().bindService(new Intent(this, GpsTracker.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void unbindGpsTracker() {
        if (mIsBound) {
            // Detach our existing connection.
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }
}
