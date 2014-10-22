package org.runnerup.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

import org.runnerup.R;
import org.runnerup.view.list.ListViewModel;
import org.runnerup.view.list.ListViewModelFragment;
import org.runnerup.view.list.TwoLineViewModel;
import org.runnerup.widget.DisabledEntriesAdapter;
import org.runnerup.workout.Dimension;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

import java.util.ArrayList;
import java.util.List;

public class BasicSettingsFragment extends ListViewModelFragment implements StartSettings {

    private AudioSchemeListAdapter simpleAudioListAdapter;
    private DatabaseProvider databaseProvider;
    private HRZonesListAdapter hrZonesAdapter;
    private DisabledEntriesAdapter targetEntriesAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //databaseProvider = (DatabaseProvider) getParentFragment();
    }

    @Override
    protected List<ListViewModel> onCreateListItems(LayoutInflater inflater) {
        List<ListViewModel> items = new ArrayList<ListViewModel>();
        items.add(new TwoLineViewModel(inflater, 1, true, getString(R.string.basic_audio_cue_spinner), "Default"));
        items.add(new TwoLineViewModel(inflater, 2, true, "Sport", "Running"));
        items.add(new TwoLineViewModel(inflater, 3, true, "Target", "Pace"));
        items.add(new TwoLineViewModel(inflater, 4, true, "Target pace (HH:MM:SS)", "00:05:00"));
        return items;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //simpleAudioListAdapter = new AudioSchemeListAdapter(databaseProvider.getDatabase(), getActivity().getLayoutInflater(), false);
        //simpleAudioListAdapter.reload();
        //audioCueSpinner.setAdapter(simpleAudioListAdapter);
        hrZonesAdapter = new HRZonesListAdapter(getActivity(), getActivity().getLayoutInflater());
        targetEntriesAdapter = new DisabledEntriesAdapter(getActivity(), R.array.targetEntries);

        //targetHrz.setAdapter(hrZonesAdapter);
        /*targetType.setOnCloseDialogListener(new TitleSpinner.OnCloseDialogListener() {

            @Override
            public void onClose(TitleSpinner spinner, boolean ok) {
                if (ok) {
                    updateTargetView();
                }
            }
        });*/

    }

    @Override
    public void onResume() {
        super.onResume();

        //simpleAudioListAdapter.reload();

        hrZonesAdapter.reload();
        //targetHrz.setAdapter(hrZonesAdapter);
        if (!hrZonesAdapter.hrZones.isConfigured()) {
            targetEntriesAdapter.addDisabled(2);
        } else {
            targetEntriesAdapter.clearDisabled();
        }
        //targetType.setAdapter(targetEntriesAdapter);

    }
/*
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
*/
    @Override
    public Workout getWorkout(SharedPreferences pref) {
        Dimension target = null;
        /*
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
        */
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
        //simpleAudioListAdapter.reload();
    }
}
