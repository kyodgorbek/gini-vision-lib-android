package net.gini.android.vision.onboarding;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.onboarding.PageIndicatorsHelper.createPageIndicatorsInstance;
import static net.gini.android.vision.onboarding.PageIndicatorsHelper.isPageActive;
import static net.gini.android.vision.onboarding.PageIndicatorsHelper.isPageInactive;

import net.gini.android.vision.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class PageIndicatorsTest {

    @Test
    public void should_createPageIndicatorImageViews() {
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                createPageIndicatorsInstance(2);

        assertThat(pageIndicators.getPageIndicators().size()).isEqualTo(2);
    }

    @Test
    public void should_setActiveRequiredPageIndicator() {
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                createPageIndicatorsInstance(2);
        pageIndicators.setActive(0);

        isPageActive(pageIndicators, 0);
        isPageInactive(pageIndicators, 1);

        pageIndicators.setActive(1);

        isPageInactive(pageIndicators, 0);
        isPageActive(pageIndicators, 1);
    }

}
