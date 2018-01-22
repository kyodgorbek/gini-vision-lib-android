package net.gini.android.vision.onboarding;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.onboarding.PageIndicatorsHelper.createPageIndicatorsInstance;

import net.gini.android.vision.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class PageChangeListenerTest {

    @Test
    public void should_updatePageIndicators_onPageChange() {
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                createPageIndicatorsInstance(2);
        final OnboardingFragmentImpl.PageChangeListener.Callback callback =
                new OnboardingFragmentImpl.PageChangeListener.Callback() {
                    @Override
                    public void onLastPage() {
                    }
                };

        final OnboardingFragmentImpl.PageChangeListener pageChangeListener =
                new OnboardingFragmentImpl.PageChangeListener(pageIndicators, 0, 2, callback);
        pageChangeListener.init();

        pageChangeListener.onPageSelected(1);

        PageIndicatorsHelper.isPageActive(pageIndicators, 1);
    }

    @Test
    public void should_setPageIndicator_toInitialCurrentPage() {
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                createPageIndicatorsInstance(2);
        final OnboardingFragmentImpl.PageChangeListener.Callback callback =
                new OnboardingFragmentImpl.PageChangeListener.Callback() {
                    @Override
                    public void onLastPage() {
                    }
                };

        final OnboardingFragmentImpl.PageChangeListener pageChangeListener =
                new OnboardingFragmentImpl.PageChangeListener(pageIndicators, 1, 2, callback);
        pageChangeListener.init();

        PageIndicatorsHelper.isPageActive(pageIndicators, 1);
    }

    @Test
    public void should_invokeCallback_whenLastPage_wasReached() {
        final OnboardingFragmentImpl.PageIndicators pageIndicators =
                createPageIndicatorsInstance(4);

        final AtomicBoolean lastPageCalled = new AtomicBoolean();
        final OnboardingFragmentImpl.PageChangeListener.Callback callback =
                new OnboardingFragmentImpl.PageChangeListener.Callback() {
                    @Override
                    public void onLastPage() {
                        lastPageCalled.set(true);
                    }
                };

        final OnboardingFragmentImpl.PageChangeListener pageChangeListener =
                new OnboardingFragmentImpl.PageChangeListener(pageIndicators, 1, 4, callback);
        pageChangeListener.init();

        pageChangeListener.onPageSelected(3);

        assertThat(lastPageCalled.get()).isTrue();
    }
}
