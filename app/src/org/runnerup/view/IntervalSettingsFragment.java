package org.runnerup.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.runnerup.R;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

public class IntervalSettingsFragment extends Fragment implements StartSettingsFragment {
    TitleSpinner intervalType = null;
    TitleSpinner intervalTime = null;
    TitleSpinner intervalDistance = null;
    TitleSpinner intervalRestType = null;
    TitleSpinner intervalRestTime = null;
    TitleSpinner intervalRestDistance = null;
    TitleSpinner intervalAudioSpinner = null;
    AudioSchemeListAdapter intervalAudioListAdapter = null;
    StartFragment startFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startFragment = (StartFragment) getParentFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_interval, container, false);

        intervalType = (TitleSpinner) view.findViewById(R.id.interval_type);
        intervalTime = (TitleSpinner) view.findViewById(R.id.interval_time);
        intervalTime.setOnSetValueListener(onSetTimeValidator);
        intervalDistance = (TitleSpinner) view.findViewById(R.id.interval_distance);
        intervalType.setOnSetValueListener(intervalTypeSetValue);

        intervalRestType = (TitleSpinner) view.findViewById(R.id.interval_rest_type);
        intervalRestTime = (TitleSpinner) view.findViewById(R.id.interval_rest_time);
        intervalRestTime.setOnSetValueListener(onSetTimeValidator);
        intervalRestDistance = (TitleSpinner) view.findViewById(R.id.interval_rest_distance);
        intervalRestType.setOnSetValueListener(intervalRestTypeSetValue);
        intervalAudioListAdapter = new AudioSchemeListAdapter(startFragment.getDB(), inflater, false);
        intervalAudioListAdapter.reload();
        intervalAudioSpinner = (TitleSpinner) view.findViewById(R.id.interval_audio_cue_spinner);
        intervalAudioSpinner.setAdapter(intervalAudioListAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        intervalAudioListAdapter.reload();
    }

    @Override
    public Workout getWorkout(SharedPreferences pref) {
        return WorkoutBuilder.createDefaultIntervalWorkout(getResources(), pref);
    }

    @Override
    public SharedPreferences getAudioPreferences(SharedPreferences pref) {
        return WorkoutBuilder.getAudioCuePreferences(getActivity(), pref,
                getResources().getString(R.string.pref_interval_audio));
    }

    @Override
    public boolean isStartButtonEnabled() {
        return false;
    }

    @Override
    public void update() {
        intervalAudioListAdapter.reload();
    }

    TitleSpinner.OnSetValueListener intervalTypeSetValue = new TitleSpinner.OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            boolean time = (newValue == 0);
            intervalTime.setVisibility(time ? View.VISIBLE : View.GONE);
            intervalDistance.setVisibility(time ? View.GONE : View.VISIBLE);
            return newValue;
        }
    };

    TitleSpinner.OnSetValueListener intervalRestTypeSetValue = new TitleSpinner.OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            boolean time = (newValue == 0);
            intervalRestTime.setVisibility(time ? View.VISIBLE : View.GONE);
            intervalRestDistance.setVisibility(time ? View.GONE : View.VISIBLE);
            return newValue;
        }
    };

    TitleSpinner.OnSetValueListener onSetTimeValidator = new TitleSpinner.OnSetValueListener() {

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
}
