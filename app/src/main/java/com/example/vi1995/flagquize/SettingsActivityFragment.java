package com.example.vi1995.flagquize;

import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.os.Bundle;

public class SettingsActivityFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preferences);
    }
}
