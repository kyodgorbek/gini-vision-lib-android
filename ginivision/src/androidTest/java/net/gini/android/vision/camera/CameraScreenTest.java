package net.gini.android.vision.camera;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.OncePerInstallEventStoreHelper.clearOnboardingWasShownPreference;
import static net.gini.android.vision.testutils.InstrumentationHelpers.isTablet;
import static net.gini.android.vision.testutils.InstrumentationHelpers.prepareLooper;
import static net.gini.android.vision.testutils.InstrumentationHelpers.resetDeviceOrientation;
import static net.gini.android.vision.testutils.InstrumentationHelpers.waitForWindowUpdate;

import static org.junit.Assume.assumeTrue;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.RequiresDevice;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.view.Surface;
import android.view.View;

import net.gini.android.vision.R;
import net.gini.android.vision.analysis.AnalysisActivityTestSpy;
import net.gini.android.vision.review.ReviewActivityTestSpy;
import net.gini.android.vision.testutils.EspressoAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CameraScreenTest {

    private static final int PAUSE_DURATION = 500;

    private static final long CLOSE_CAMERA_PAUSE_DURATION = 1000;
    private static final long TAKE_PICTURE_PAUSE_DURATION = 4000;

    @Rule
    public IntentsTestRule<CameraActivity> mIntentsTestRule = new IntentsTestRule<>(
            CameraActivity.class, true, false);

    @Before
    public void setup() throws Exception {
        prepareLooper();
    }

    @After
    public void teardown() throws Exception {
        clearOnboardingWasShownPreference();
        // Wait a little for the camera to close
        Thread.sleep(CLOSE_CAMERA_PAUSE_DURATION);
        resetDeviceOrientation();
    }

    @NonNull
    private Intent getCameraActivityIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        CameraActivity.setReviewActivityExtra(intent, InstrumentationRegistry.getTargetContext(),
                ReviewActivityTestSpy.class);
        CameraActivity.setAnalysisActivityExtra(intent, InstrumentationRegistry.getTargetContext(),
                AnalysisActivityTestSpy.class);
        return intent;
    }

    @NonNull
    private CameraActivity startCameraActivityWithoutOnboarding() {
        Intent intent = getCameraActivityIntent();
        intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING_AT_FIRST_RUN, false);
        return mIntentsTestRule.launchActivity(intent);
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void a_should_showNoPermissionView_ifNoCameraPermission() {
        // Gini Vision Library does not handle runtime permissions and the no permission view is
        // shown by default
        startCameraActivityWithoutOnboarding();

        Espresso.onView(ViewMatchers.withId(R.id.gv_layout_camera_no_permission))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void b_should_showCameraPreview_afterCameraPermission_wasGranted()
            throws UiObjectNotFoundException {
        startCameraActivityWithoutOnboarding();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Open the Application Details in the Settings
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_camera_no_permission))
                .perform(ViewActions.click());

        // Open the Permissions settings
        UiObject permissionsItem = uiDevice.findObject(new UiSelector().text("Permissions"));
        permissionsItem.clickAndWaitForNewWindow();

        // Grant Camera permission
        UiObject cameraItem = uiDevice.findObject(new UiSelector().text("Camera"));
        if (!cameraItem.isChecked()) {
            cameraItem.click();
        }

        // Go back to our test app
        uiDevice.pressBack();
        uiDevice.pressBack();

        // Verifiy that the no permission view was removed
        Espresso.onView(ViewMatchers.withId(R.id.gv_layout_camera_no_permission))
                .check(ViewAssertions.matches(
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Verify that the camera preview is visible
        Espresso.onView(ViewMatchers.withId(R.id.gv_camera_preview))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @RequiresDevice
    @Test
    public void should_takeOnlyOnePicture_ifTrigger_wasPressedMultipleTimes()
            throws InterruptedException {
        startCameraActivityWithoutOnboarding();

        Espresso.onView(ViewMatchers.withId(R.id.gv_button_camera_trigger))
                .perform(ViewActions.doubleClick());

        // Give some time for the camera to take a picture
        Thread.sleep(TAKE_PICTURE_PAUSE_DURATION);

        Intents.intended(IntentMatchers.hasComponent(ReviewActivityTestSpy.class.getName()));
    }

    @RequiresDevice
    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_adaptCameraPreviewSize_toLandscapeOrientation_onTablets() throws Exception {
        // Given
        assumeTrue(isTablet());

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationNatural();
        waitForWindowUpdate(uiDevice);

        final CameraActivity cameraActivity = startCameraActivityWithoutOnboarding();
        View cameraPreview = cameraActivity.findViewById(R.id.gv_camera_preview);
        final int initialWidth = cameraPreview.getWidth();
        final int initialHeight = cameraPreview.getHeight();

        // When
        uiDevice.setOrientationRight();
        waitForWindowUpdate(uiDevice);

        // Then
        // Preview should have the reverse aspect ratio
        Espresso.onView(
                ViewMatchers.withId(R.id.gv_camera_preview)).check(
                EspressoAssertions.hasSizeRatio((float) initialHeight / initialWidth));
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_forcePortraitOrientation_onPhones() throws Exception {
        // Given
        assumeTrue(!isTablet());

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationLeft();
        waitForWindowUpdate(uiDevice);

        final CameraActivity cameraActivity = startCameraActivityWithoutOnboarding();
        waitForWindowUpdate(uiDevice);

        // Then
        int rotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
        assertThat(rotation)
                .isEqualTo(Surface.ROTATION_0);
    }
}