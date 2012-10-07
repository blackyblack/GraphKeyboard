package com.sam.graphkeyboard;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GraphKeyboardSettingsActivity extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);		
        addPreferencesFromResource(R.xml.preferences);
    }
}