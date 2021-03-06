package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public abstract class BaseActivity
        extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject public SharedPreferences sharedPreferences;
    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HomerPlayerApplication.getComponent(this).inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateOrientation();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateOrientation();
    }

    protected abstract String getScreenName();

    private void updateOrientation() {
        //noinspection ResourceType
        setRequestedOrientation(globalSettings.getScreenOrientation());
    }
}
