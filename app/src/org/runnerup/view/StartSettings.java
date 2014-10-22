package org.runnerup.view;

import android.content.SharedPreferences;

import org.runnerup.workout.Workout;

public interface StartSettings {
    Workout getWorkout(SharedPreferences pref);
    SharedPreferences getAudioPreferences(SharedPreferences pref);
    boolean isStartButtonEnabled();
    void update();
}
