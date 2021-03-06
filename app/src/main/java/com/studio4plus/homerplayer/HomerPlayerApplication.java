package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.flurry.android.FlurryAgent;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.ui.HomeActivity;
import com.studio4plus.homerplayer.util.MediaScannerUtil;
import com.studio4plus.homerplayer.util.VersionUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";
    private static final String DEMO_SAMPLES_URL =
            "https://homer-player.firebaseapp.com/samples.zip";
    private static final String FLURRY_API_KEY_ASSET = "api_keys/flurry";

    private ApplicationComponent component;
    private MediaStoreUpdateObserver mediaStoreUpdateObserver;

    @Inject public GlobalSettings globalSettings;
    @Inject public AnalyticsTracker analyticsTracker;  // Force creation of the tracker early.

    @Override
    public void onCreate() {
        super.onCreate();

        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());

        String flurryKey = getFlurryKey(getAssets());
        if (flurryKey != null && VersionUtil.isOfficialVersion()) {
            new FlurryAgent.Builder()
                    .withLogEnabled(true)
                    .build(this, flurryKey);
        }

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this, Uri.parse(DEMO_SAMPLES_URL)))
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();
        component.inject(this);

        mediaStoreUpdateObserver = new MediaStoreUpdateObserver(new Handler(getMainLooper()));
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreUpdateObserver);

        HomeActivity.setEnabled(this, globalSettings.isAnyKioskModeEnabled());

        createAudioBooksDirectory(component.getAudioBookManager().getDefaultAudioBooksDirectory());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        getContentResolver().unregisterContentObserver(mediaStoreUpdateObserver);
        mediaStoreUpdateObserver = null;
    }

    public static ApplicationComponent getComponent(Context context) {
        return ((HomerPlayerApplication) context.getApplicationContext()).component;
    }

    private void createAudioBooksDirectory(File path) {
        if (!path.exists()) {
            if (path.mkdirs()) {
                // The MediaScanner doesn't work so well with directories (registers them as regular
                // files) so make it scan a dummy.
                final File dummyFile = new File(path, ".ignore");
                try {
                    if (dummyFile.createNewFile()) {
                        MediaScannerUtil.scanAndDeleteFile(this, dummyFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // Just ignore.
                }
            }
        }
    }

  private @Nullable String getFlurryKey(AssetManager assets) {
      try {
          InputStream inputStream = assets.open(FLURRY_API_KEY_ASSET);
          try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
              String key = reader.readLine();
              inputStream.close();
              return key;
          } catch(IOException e) {
              inputStream.close();
              return null;
          }
      } catch (IOException e) {
          return null;
      }
  }
}
