package net.gini.android.vision;

import org.robolectric.RuntimeEnvironment;

public final class OncePerInstallEventStoreRoboHelper {

    private OncePerInstallEventStoreRoboHelper() {
    }

    public static void setOnboardingWasShownPreference() {
        OncePerInstallEventStore store =
                new OncePerInstallEventStore(RuntimeEnvironment.application);
        store.saveEvent(OncePerInstallEvent.SHOW_ONBOARDING);
    }
}
