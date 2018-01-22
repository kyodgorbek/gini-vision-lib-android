package net.gini.android.vision.onboarding;

import static com.google.common.truth.Truth.assertAbout;

import static net.gini.android.vision.onboarding.PageIndicatorImageViewSubject.pageIndicatorImageView;

import android.support.annotation.NonNull;
import android.widget.LinearLayout;

import net.gini.android.vision.R;

import org.robolectric.RuntimeEnvironment;

final class PageIndicatorsHelper {

    static void isPageActive(final OnboardingFragmentImpl.PageIndicators pageIndicators,
            final int pageNr) {
        assertAbout(pageIndicatorImageView()).that(
                pageIndicators.getPageIndicators().get(pageNr)).showsDrawable(
                R.drawable.gv_onboarding_indicator_active);
    }

    static void isPageInactive(final OnboardingFragmentImpl.PageIndicators pageIndicators,
            final int pageNr) {
        assertAbout(pageIndicatorImageView()).that(
                pageIndicators.getPageIndicators().get(pageNr)).showsDrawable(
                R.drawable.gv_onboarding_indicator_inactive);
    }

    @NonNull
    static OnboardingFragmentImpl.PageIndicators createPageIndicatorsInstance(final int nrOfPages) {
        final LinearLayout linearLayout = new LinearLayout(RuntimeEnvironment.application);
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                new OnboardingFragmentImpl.PageIndicators(
                        RuntimeEnvironment.application, nrOfPages, linearLayout);
        pageIndicators.create();
        return pageIndicators;
    }

    private PageIndicatorsHelper() {
    }
}
