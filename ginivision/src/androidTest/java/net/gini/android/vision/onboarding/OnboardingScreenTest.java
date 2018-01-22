package net.gini.android.vision.onboarding;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.InstrumentationHelpers.isTablet;
import static net.gini.android.vision.testutils.InstrumentationHelpers.resetDeviceOrientation;
import static net.gini.android.vision.testutils.InstrumentationHelpers.waitForWindowUpdate;

import static org.junit.Assume.assumeTrue;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.view.Surface;

import net.gini.android.vision.R;
import net.gini.android.vision.testutils.EspressoMatchers;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class OnboardingScreenTest {

    private static final long TEST_PAUSE_DURATION = 500;

    @Rule
    public ActivityTestRule<OnboardingActivity> mActivityTestRule = new ActivityTestRule<>(
            OnboardingActivity.class, true, false);


    @After
    public void tearDown() throws Exception {
        resetDeviceOrientation();
    }

    @Test
    public void should_goToNextPage_whenNextButton_isClicked() {
        startOnboardingActivity();

        // Check that we are on the first page
        Espresso.onView(ViewMatchers.withText(getDefaultPageAtIndex(0).getTextResId()))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Check that we are on the second page
        Espresso.onView(ViewMatchers.withText(getDefaultPageAtIndex(1).getTextResId()))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void should_goToNextPage_whenSwiped() {
        startOnboardingActivity();

        // Check that we are on the first page
        Espresso.onView(ViewMatchers.withText(getDefaultPageAtIndex(0).getTextResId()))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.gv_onboarding_viewpager))
                .perform(ViewActions.swipeLeft());

        // Check that we are on the second page
        Espresso.onView(ViewMatchers.withText(getDefaultPageAtIndex(1).getTextResId()))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void should_finish_whenNextButton_isClicked_onLastPage() throws InterruptedException {
        final OnboardingActivity activity = startOnboardingActivity();

        // Go to the last page by clicking the next button
        final ViewInteraction viewInteraction = Espresso.onView(
                ViewMatchers.withId(R.id.gv_button_next));
        final int nrOfPages = getDefaultPages().length;
        for (int i = 0; i < nrOfPages; i++) {
            viewInteraction.perform(ViewActions.click());
        }

        // Give some time for paging animation to finish
        Thread.sleep(TEST_PAUSE_DURATION);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void should_finish_whenSwiped_onLastPage() throws InterruptedException {
        final OnboardingActivity activity = startOnboardingActivity();

        // Go to the last page by swiping
        final ViewInteraction viewInteraction = Espresso.onView(
                ViewMatchers.withId(R.id.gv_onboarding_viewpager));
        final int nrOfPages = getDefaultPages().length;
        for (int i = 0; i < nrOfPages; i++) {
            viewInteraction.perform(ViewActions.swipeLeft());
        }

        // Wait a little for the animation to finish
        Thread.sleep(TEST_PAUSE_DURATION);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void should_showCustomPages_whenSet() throws InterruptedException {
        final ArrayList<OnboardingPage> customPages = new ArrayList<>(1);
        customPages.add(new OnboardingPage(R.string.gv_title_camera, R.drawable.gv_camera_trigger));
        customPages.add(
                new OnboardingPage(R.string.gv_title_review, R.drawable.gv_review_button_rotate));

        final Intent intent = getOnboardingActivityIntent();
        intent.putExtra(OnboardingActivity.EXTRA_ONBOARDING_PAGES, customPages);
        final OnboardingActivity activity = startOnboardingActivity(intent);

        // Give some time for the activity to start
        Thread.sleep(TEST_PAUSE_DURATION);

        // Verify the first page
        Espresso.onView(ViewMatchers.withText(R.string.gv_title_camera))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // Go to the second page
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Give some time for paging animation to finish
        Thread.sleep(TEST_PAUSE_DURATION);

        // Verify the second page
        Espresso.onView(ViewMatchers.withText(R.string.gv_title_review))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // Click the next button again to finish
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Give some time for paging animation to finish
        Thread.sleep(TEST_PAUSE_DURATION);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void should_showEmptyLastPage_byDefault() {
        startOnboardingActivity();

        // ViewPager should contain the default pages and an empty last page
        Espresso.onView(ViewMatchers.withId(R.id.gv_onboarding_viewpager))
                .check(ViewAssertions.matches(EspressoMatchers.hasPageCount(
                        getDefaultPages().length + 1)));
    }

    @Test
    public void should_notShowEmptyLastPage_ifRequested() {
        final OnboardingActivity onboardingActivity = startOnboardingActivity();
        final OnboardingFragmentCompat onboardingFragment =
                OnboardingFragmentCompat.createInstanceWithoutEmptyLastPage();
        onboardingActivity.showFragment(onboardingFragment);

        // ViewPager should contain the default pages only
        Espresso.onView(ViewMatchers.withId(R.id.gv_onboarding_viewpager))
                .check(ViewAssertions.matches(
                        EspressoMatchers.hasPageCount(getDefaultPages().length)));
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_forcePortraitOrientation_onPhones() throws Exception {
        // Given
        assumeTrue(!isTablet());

        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationLeft();
        waitForWindowUpdate(uiDevice);

        final OnboardingActivity onboardingActivity = startOnboardingActivity();
        waitForWindowUpdate(uiDevice);

        // Then
        final int rotation = onboardingActivity.getWindowManager().getDefaultDisplay().getRotation();
        assertThat(rotation)
                .isEqualTo(Surface.ROTATION_0);
    }

    private OnboardingPage getDefaultPageAtIndex(final int index) {
        if (isTablet()) {
            return DefaultPagesTablet.values()[index].getPage();
        } else {
            return DefaultPagesPhone.values()[index].getPage();
        }
    }

    private Enum[] getDefaultPages() {
        return isTablet() ? DefaultPagesTablet.values() : DefaultPagesPhone.values();
    }

    private OnboardingActivity startOnboardingActivity() {
        return startOnboardingActivity(null);
    }

    private OnboardingActivity startOnboardingActivity(@Nullable Intent intent) {
        if (intent == null) {
            intent = getOnboardingActivityIntent();
        }
        return mActivityTestRule.launchActivity(intent);
    }

    @NonNull
    private Intent getOnboardingActivityIntent() {
        return new Intent(InstrumentationRegistry.getTargetContext(), OnboardingActivity.class);
    }

}
