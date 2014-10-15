package org.runnerup.view;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.runnerup.R;
import org.runnerup.util.Constants;
import org.runnerup.util.Formatter;
import org.runnerup.util.SafeParse;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.workout.Workout;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class ManualSettingsFragment extends Fragment implements StartSettingsFragment {
    boolean manualSetValue = false;
    TitleSpinner manualSport = null;
    TitleSpinner manualDate = null;
    TitleSpinner manualTime = null;
    TitleSpinner manualDistance = null;
    TitleSpinner manualDuration = null;
    TitleSpinner manualPace = null;
    EditText manualNotes = null;
    StartFragment startFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startFragment = (StartFragment) getParentFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_manual, container, false);

        manualSport = (TitleSpinner) view.findViewById(R.id.manual_sport);
        manualDate = (TitleSpinner) view.findViewById(R.id.manual_date);
        manualDate.setOnSetValueListener(onSetValueManual);
        manualTime = (TitleSpinner) view.findViewById(R.id.manual_time);
        manualTime.setOnSetValueListener(onSetValueManual);
        manualDistance = (TitleSpinner) view.findViewById(R.id.manual_distance);
        manualDistance.setOnSetValueListener(onSetManualDistance);
        manualDuration = (TitleSpinner) view.findViewById(R.id.manual_duration);
        manualDuration.setOnSetValueListener(onSetManualDuration);
        manualPace = (TitleSpinner) view.findViewById(R.id.manual_pace);
        manualPace.setVisibility(View.GONE);
        manualNotes = (EditText) view.findViewById(R.id.manual_notes);

        return view;
    }

    @Override
    public Workout getWorkout(SharedPreferences pref) {
        return null;
    }

    @Override
    public SharedPreferences getAudioPreferences(SharedPreferences pref) {
        return null;
    }

    @Override
    public boolean isStartButtonEnabled() {
        return false;
    }

    @Override
    public void update() {

    }

    TitleSpinner.OnSetValueListener onSetValueManual = new TitleSpinner.OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            manualSetValue = true;
            startFragment.startButton.setEnabled(true);
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            manualSetValue = true;
            startFragment.startButton.setEnabled(true);
            return newValue;
        }
    };

    void setManualPace(String distance, String duration) {
        System.err.println("distance: >" + distance + "< duration: >" + duration + "<");
        double dist = SafeParse.parseDouble(distance, 0); // convert to meters
        long seconds = SafeParse.parseSeconds(duration, 0);
        if (dist == 0 || seconds == 0) {
            manualPace.setVisibility(View.GONE);
            return;
        }
        double pace = seconds / dist;
        manualPace.setValue(startFragment.formatter.formatPace(Formatter.TXT_SHORT, pace));
        manualPace.setVisibility(View.VISIBLE);
        return;
    }

    TitleSpinner.OnSetValueListener onSetManualDistance = new TitleSpinner.OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            setManualPace(newValue, manualDuration.getValue().toString());
            startFragment.startButton.setEnabled(true);
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            startFragment.startButton.setEnabled(true);
            return newValue;
        }

    };

    TitleSpinner.OnSetValueListener onSetManualDuration = new TitleSpinner.OnSetValueListener() {

        @Override
        public String preSetValue(String newValue)
                throws IllegalArgumentException {
            setManualPace(manualDistance.getValue().toString(), newValue);
            startFragment.startButton.setEnabled(true);
            return newValue;
        }

        @Override
        public int preSetValue(int newValue) throws IllegalArgumentException {
            startFragment.startButton.setEnabled(true);
            return newValue;
        }
    };

    public void save() {
        ContentValues save = new ContentValues();
        int sport = manualSport.getValueInt();
        CharSequence date = manualDate.getValue();
        CharSequence time = manualTime.getValue();
        CharSequence distance = manualDistance.getValue();
        CharSequence duration = manualDuration.getValue();
        String notes = manualNotes.getText().toString().trim();
        long start_time = 0;

        if (notes.length() > 0) {
            save.put(Constants.DB.ACTIVITY.COMMENT, notes);
        }
        double dist = 0;
        if (distance.length() > 0) {
            dist = Double.parseDouble(distance.toString()); // convert to
            // meters
            save.put(Constants.DB.ACTIVITY.DISTANCE, dist);
        }
        long secs = 0;
        if (duration.length() > 0) {
            secs = SafeParse.parseSeconds(duration.toString(), 0);
            save.put(Constants.DB.ACTIVITY.TIME, secs);
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
        save.put(Constants.DB.ACTIVITY.START_TIME, start_time);

        save.put(Constants.DB.ACTIVITY.SPORT, sport);
        long id = startFragment.getDB().insert(Constants.DB.ACTIVITY.TABLE, null, save);

        ContentValues lap = new ContentValues();
        lap.put(Constants.DB.LAP.ACTIVITY, id);
        lap.put(Constants.DB.LAP.LAP, 0);
        lap.put(Constants.DB.LAP.INTENSITY, Constants.DB.INTENSITY.ACTIVE);
        lap.put(Constants.DB.LAP.TIME, secs);
        lap.put(Constants.DB.LAP.DISTANCE, dist);
        startFragment.getDB().insert(Constants.DB.LAP.TABLE, null, lap);

        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra("mode", "save");
        intent.putExtra("ID", id);
        startFragment.startActivityForResult(intent, 0);
    }
}
