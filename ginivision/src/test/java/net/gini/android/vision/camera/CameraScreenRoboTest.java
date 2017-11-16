package net.gini.android.vision.camera;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.OncePerInstallEventStoreRoboHelper.setOnboardingWasShownPreference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import net.gini.android.vision.BuildConfig;
import net.gini.android.vision.R;
import net.gini.android.vision.analysis.AnalysisActivityTestSpy;
import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.internal.camera.photo.PhotoFactory;
import net.gini.android.vision.onboarding.OnboardingActivity;
import net.gini.android.vision.onboarding.OnboardingPage;
import net.gini.android.vision.review.ReviewActivity;
import net.gini.android.vision.review.ReviewActivityTestSpy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

/**
 * Created by aszotyori on 16.11.17.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class CameraScreenRoboTest {

    @Test
    public void
    should_finishIfEnabledByClient_whenReceivingActivityResult_withResultCodeCancelled_fromReviewActivity() {
        final Intent intentAllowBackButtonToClose = getCameraActivityIntent();
        intentAllowBackButtonToClose.putExtra(
                CameraActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, true);

        final CameraActivity cameraActivity = new CameraActivity();
        cameraActivity.setIntent(intentAllowBackButtonToClose);
        cameraActivity.readExtras();

        final CameraActivity cameraActivitySpy = Mockito.spy(cameraActivity);

        cameraActivitySpy.onActivityResult(CameraActivity.REVIEW_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(cameraActivitySpy).finish();
    }

    @Test
    public void
    should_notFinish_whenReceivingActivityResult_withResultCodeCancelled_fromReviewActivity() {
        final CameraActivity cameraActivitySpy = Mockito.spy(new CameraActivity());

        cameraActivitySpy.onActivityResult(CameraActivity.REVIEW_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(cameraActivitySpy, never()).finish();
    }

    @Test
    public void should_notShowOnboarding_onFirstLaunch_ifDisabled() {
        startCameraActivityWithoutOnboarding();

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @NonNull
    private CameraActivity startCameraActivityWithoutOnboarding() {
        Intent intent = getCameraActivityIntent();
        intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING_AT_FIRST_RUN, false);
        return startCameraActivity(intent);
    }

    @Test
    public void should_passAnalysisActivityIntent_toReviewActivity() throws InterruptedException {
        final CameraActivity cameraActivity = startCameraActivityWithoutOnboarding();

        // Fake taking of a picture, which will cause the ReviewActivity to be launched
        cameraActivity.onDocumentAvailable(DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(new byte[]{}, 0, "portrait", "phone", "camera")));

        Intent expectedIntent = new Intent(cameraActivity, ReviewActivityTestSpy.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getComponent()).isEqualTo(expectedIntent.getComponent());
        final Intent analysisActivityIntent =
                actual.getParcelableExtra(ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY);
        assertThat(analysisActivityIntent).isNotNull();
        assertThat(analysisActivityIntent.getComponent()).isNotNull();
        assertThat(analysisActivityIntent.getComponent().getClassName())
                .isEqualTo(AnalysisActivityTestSpy.class.getName());
    }

    @Test
    public void should_passBackButtonClosesLibraryExtra_toReviewActivity()
            throws InterruptedException {
        final CameraActivity cameraActivity = startCameraActivityWithBackButtonShouldCloseLibrary();

        final CameraActivity cameraActivitySpy = Mockito.spy(cameraActivity);
        // Prevent really starting the ReviewActivity
        doNothing().when(cameraActivitySpy).startActivityForResult(any(Intent.class), anyInt());
        // Fake taking of a picture, which will cause the ReviewActivity to be launched
        cameraActivitySpy.onDocumentAvailable(DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(new byte[]{}, 0, "portrait", "phone", "camera")));

        // Check that the extra was passed on to the ReviewActivity
        verify(cameraActivitySpy).startActivityForResult(argThat(
                intentWithExtraBackButtonShouldCloseLibrary()), anyInt());
    }

    @NonNull
    private CameraActivity startCameraActivityWithBackButtonShouldCloseLibrary() {
        final Intent intentAllowBackButtonToClose = getCameraActivityIntent();
        intentAllowBackButtonToClose.putExtra(
                CameraActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, true);
        return startCameraActivity(intentAllowBackButtonToClose);
    }

    @NonNull
    private ArgumentMatcher<Intent> intentWithExtraBackButtonShouldCloseLibrary() {
        return new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(final Intent intent) {
                //noinspection UnnecessaryLocalVariable
                final boolean shouldCloseLibrary = intent.getBooleanExtra(
                        ReviewActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, false);
                return shouldCloseLibrary;
            }

            @Override
            public String toString() {
                return "Intent { EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY=true }";
            }
        };
    }

    @Test
    public void should_passCustomOnboardingPages_toOnboardingActivity()
            throws Exception {
        ArrayList<OnboardingPage> onboardingPages = new ArrayList<>(1);
        onboardingPages.add(
                new OnboardingPage(R.string.gv_onboarding_align, R.drawable.gv_onboarding_align));

        Intent intent = getCameraActivityIntent();
        intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING_AT_FIRST_RUN, false);
        intent.putExtra(CameraActivity.EXTRA_IN_ONBOARDING_PAGES, onboardingPages);
        CameraActivity cameraActivity = startCameraActivity(intent);
        cameraActivity.startOnboardingActivity();

        Intent expectedIntent = new Intent(cameraActivity, OnboardingActivity.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getComponent()).isEqualTo(expectedIntent.getComponent());
        assertThat(actual.getSerializableExtra(OnboardingActivity.EXTRA_ONBOARDING_PAGES))
                .isEqualTo(onboardingPages);
    }

    @Test
    public void should_showOnboarding_ifRequested_andWasAlreadyShownOnFirstLaunch() {
        setOnboardingWasShownPreference();

        Intent intent = getCameraActivityIntent();
        intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING, true);
        CameraActivity cameraActivity =
                startCameraActivity(intent);

        Intent expectedIntent = new Intent(cameraActivity, OnboardingActivity.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getComponent()).isEqualTo(expectedIntent.getComponent());
    }

    @Test
    public void should_showOnboarding_onFirstLaunch_ifNotDisabled() {
        Intent intent = getCameraActivityIntent();
        CameraActivity cameraActivity =
                startCameraActivity(intent);

        Intent expectedIntent = new Intent(cameraActivity, OnboardingActivity.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getComponent()).isEqualTo(expectedIntent.getComponent());
    }

    private CameraActivity startCameraActivity(final Intent intent) {
        return Robolectric.buildActivity(CameraActivity.class, intent).setup().get();
    }

    @NonNull
    private Intent getCameraActivityIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        CameraActivity.setReviewActivityExtra(intent, RuntimeEnvironment.application,
                ReviewActivityTestSpy.class);
        CameraActivity.setAnalysisActivityExtra(intent, RuntimeEnvironment.application,
                AnalysisActivityTestSpy.class);
        return intent;
    }

    @Test
    public void should_showReviewScreen_afterPictureWasTaken() throws InterruptedException {
        final CameraActivity cameraActivity = startCameraActivityWithoutOnboarding();

        // Fake taking of a picture, which will cause the ReviewActivity to be launched
        cameraActivity.onDocumentAvailable(DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(new byte[]{}, 0, "portrait", "phone", "camera")));

        Intent expectedIntent = new Intent(cameraActivity, ReviewActivityTestSpy.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(actual.getComponent()).isEqualTo(expectedIntent.getComponent());
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenAnalysisActivityClass_wasNotGiven() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        CameraActivity.setReviewActivityExtra(intent, RuntimeEnvironment.application,
                ReviewActivityTestSpy.class);

        Robolectric.buildActivity(CameraActivity.class, intent).setup();
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenReviewActivityClass_wasNotGiven() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        CameraActivity.setAnalysisActivityExtra(intent, RuntimeEnvironment.application,
                AnalysisActivityTestSpy.class);

        Robolectric.buildActivity(CameraActivity.class, intent).setup();
    }

}
