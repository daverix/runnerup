package org.runnerup.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.runnerup.R;
import org.runnerup.widget.DisabledEntriesAdapter;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.workout.Dimension;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

public class BasicSettingsFragment extends Fragment implements StartSettingsFragment {
    private TitleSpinner audioCueSpinner;
    private TitleSpinner sportCueSpinner;
    private TitleSpinner targetType;
    private TitleSpinner targetPaceMax;
    private TitleSpinner targetHrz;
    private AudioSchemeListAdapter simpleAudioListAdapter;
    private StartFragment startFragment;
    private HRZonesListAdapter hrZonesAdapter;
    private DisabledEntriesAdapter targetEntriesAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startFragment = (StartFragment) getParentFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_basic, container, false);

        audioCueSpinner = (TitleSpinner) view.findViewById(R.id.basic_audio_cue_spinner);
        sportCueSpinner = (TitleSpinner) view.findViewById(R.id.basic_sport);
        targetType = (TitleSpinner) view.findViewById(R.id.tab_basic_target_type);
        targetPaceMax = (TitleSpinner) view.findViewById(R.id.tab_basic_target_pace_max);
        targetHrz = (TitleSpinner) view.findViewById(R.id.tab_basic_target_hrz);

        simpleAudioListAdapter = new AudioSchemeListAdapter(startFragment.getDB(), inflater, false);
        simpleAudioListAdapter.reload();
        audioCueSpinner.setAdapter(simpleAudioListAdapter);
        hrZonesAdapter = new HRZonesListAdapter(getActivity(), inflater);
        targetEntriesAdapter = new DisabledEntriesAdapter(getActivity(), R.array.targetEntries);

        targetHrz.setAdapter(hrZonesAdapter);
        targetType.setOnCloseDialogListener(new TitleSpinner.OnCloseDialogListener() {

            @Override
            public void onClose(TitleSpinner spinner, boolean ok) {
                if (ok) {
                    updateTargetView();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        simpleAudioListAdapter.reload();

        hrZonesAdapter.reload();
        targetHrz.setAdapter(hrZonesAdapter);
        if (!hrZonesAdapter.hrZones.isConfigured()) {
            targetEntriesAdapter.addDisabled(2);
        } else {
            targetEntriesAdapter.clearDisabled();
        }
        targetType.setAdapter(targetEntriesAdapter);

    }

    private void updateTargetView() {
        switch (targetType.getValueInt()) {
            case 0:
            default:
                targetPaceMax.setEnabled(false);
                targetHrz.setEnabled(false);
                break;
            case 1:
                targetPaceMax.setEnabled(true);
                targetPaceMax.setVisibility(View.VISIBLE);
                targetHrz.setVisibility(View.GONE);
                break;
            case 2:
                targetPaceMax.setVisibility(View.GONE);
                targetHrz.setEnabled(true);
                targetHrz.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Workout getWorkout(SharedPreferences pref) {
        Dimension target = null;
        switch (targetType.getValueInt()) {
            case 0: // none
                break;
            case 1:
                target = Dimension.PACE;
                break;
            case 2:
                target = Dimension.HRZ;
                break;
        }
        return WorkoutBuilder.createDefaultWorkout(getResources(), pref, target);
    }

    @Override
    public SharedPreferences getAudioPreferences(SharedPreferences pref) {
        return WorkoutBuilder.getAudioCuePreferences(getActivity(), pref, "basicAudio");
    }

    @Override
    public boolean isStartButtonEnabled() {
        return true;
    }

    @Override
    public void update() {
        simpleAudioListAdapter.reload();
    }
}
