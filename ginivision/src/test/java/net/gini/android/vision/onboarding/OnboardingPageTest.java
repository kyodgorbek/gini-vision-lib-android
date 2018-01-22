package net.gini.android.vision.onboarding;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.Helpers.doParcelingRoundTrip;

import net.gini.android.vision.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class OnboardingPageTest {

    @Test
    public void should_beParcelable() {
        //noinspection ResourceType
        final OnboardingPage toParcel = new OnboardingPage(314, 42, true);
        final OnboardingPage fromParcel = doParcelingRoundTrip(toParcel, OnboardingPage.CREATOR);

        assertThat(toParcel.getTextResId()).isEqualTo(fromParcel.getTextResId());
        assertThat(toParcel.getImageResId()).isEqualTo(fromParcel.getImageResId());
        assertThat(toParcel.isTransparent()).isEqualTo(fromParcel.isTransparent());
    }
}
