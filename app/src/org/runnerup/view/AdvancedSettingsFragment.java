package org.runnerup.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.runnerup.R;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;
import org.runnerup.workout.WorkoutSerializer;

import java.util.ArrayList;
import java.util.List;

public class AdvancedSettingsFragment extends Fragment implements StartSettingsFragment {
    TitleSpinner advancedWorkoutSpinner = null;
    WorkoutListAdapter advancedWorkoutListAdapter = null;
    TitleSpinner advancedAudioSpinner = null;
    AudioSchemeListAdapter advancedAudioListAdapter = null;
    Button advancedDownloadWorkoutButton = null;
    Workout advancedWorkout = null;
    ListView advancedStepList = null;
    WorkoutStepsAdapter advancedWorkoutStepsAdapter = new WorkoutStepsAdapter();
    StartFragment startFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startFragment = (StartFragment) getParentFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_advanced, container, false);

        advancedAudioListAdapter = new AudioSchemeListAdapter(startFragment.getDB(), inflater, false);
        advancedAudioListAdapter.reload();
        advancedAudioSpinner = (TitleSpinner) view.findViewById(R.id.advanced_audio_cue_spinner);
        advancedAudioSpinner.setAdapter(advancedAudioListAdapter);
        advancedWorkoutSpinner = (TitleSpinner) view.findViewById(R.id.advanced_workout_spinner);
        advancedWorkoutListAdapter = new WorkoutListAdapter(inflater);
        advancedWorkoutListAdapter.reload();
        advancedWorkoutSpinner.setAdapter(advancedWorkoutListAdapter);
        advancedWorkoutSpinner.setOnSetValueListener(new TitleSpinner.OnSetValueListener() {
            @Override
            public String preSetValue(String newValue)
                    throws IllegalArgumentException {
                loadAdvanced(newValue);
                return newValue;
            }

            @Override
            public int preSetValue(int newValue)
                    throws IllegalArgumentException {
                loadAdvanced(null);
                return newValue;
            }
        });
        advancedStepList = (ListView) view.findViewById(R.id.advanced_step_list);
        advancedStepList.setDividerHeight(0);
        advancedStepList.setAdapter(advancedWorkoutStepsAdapter);
        advancedDownloadWorkoutButton = (Button) view.findViewById(R.id.advanced_download_button);
        advancedDownloadWorkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ManageWorkoutsActivity.class);
                getParentFragment().startActivityForResult(intent, 113);
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Workout getWorkout(SharedPreferences pref) {
        return advancedWorkout;
    }

    @Override
    public SharedPreferences getAudioPreferences(SharedPreferences pref) {
        return WorkoutBuilder.getAudioCuePreferences(getActivity(), pref,
                getResources().getString(R.string.pref_advanced_audio));
    }

    @Override
    public boolean isStartButtonEnabled() {
        return advancedWorkout != null;
    }

    @Override
    public void update() {
        advancedWorkoutListAdapter.reload();
    }

    @Override
    public void onResume() {
        super.onResume();

        advancedAudioListAdapter.reload();
        advancedWorkoutListAdapter.reload();

        loadAdvanced(null);
    }

    private void loadAdvanced(String name) {
        Context ctx = getActivity().getApplicationContext();
        if (name == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            name = pref.getString(getResources().getString(R.string.pref_advanced_workout), "");
        }
        advancedWorkout = null;
        if ("".contentEquals(name))
            return;
        try {
            advancedWorkout = WorkoutSerializer.readFile(ctx, name);
            advancedWorkoutStepsAdapter.steps = advancedWorkout.getSteps();
            advancedWorkoutStepsAdapter.notifyDataSetChanged();
            advancedDownloadWorkoutButton.setVisibility(View.GONE);
        } catch (Exception ex) {
            ex.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Failed to load workout!!");
            builder.setMessage("" + ex.toString());
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
            return;
        }
    }


    class WorkoutStepsAdapter extends BaseAdapter {

        List<Workout.StepListEntry> steps = new ArrayList<Workout.StepListEntry>();

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
            Workout.StepListEntry entry = steps.get(position);
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

    Runnable onWorkoutChanged = new Runnable() {
        @Override
        public void run() {
            String name = advancedWorkoutSpinner.getValue().toString();
            if (advancedWorkout != null) {
                Context ctx = getActivity().getApplicationContext();
                try {
                    WorkoutSerializer.writeFile(ctx, name, advancedWorkout);
                } catch (Exception ex) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Failed to load workout!!");
                    builder.setMessage("" + ex.toString());
                    builder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                    return;
                }
            }
        }
    };
}
