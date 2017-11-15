package net.gini.android.vision.analysis;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.DocumentSubject.document;
import static net.gini.android.vision.testutils.Helpers.createDocument;
import static net.gini.android.vision.testutils.InstrumentationHelpers.getTestJpegAsset;
import static net.gini.android.vision.testutils.InstrumentationHelpers.isTablet;
import static net.gini.android.vision.testutils.InstrumentationHelpers.resetDeviceOrientation;
import static net.gini.android.vision.testutils.InstrumentationHelpers.waitForWindowUpdate;

import static org.junit.Assume.assumeTrue;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.view.Surface;
import android.view.View;

import net.gini.android.vision.R;
import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.internal.camera.photo.PhotoFactory;
import net.gini.android.vision.internal.ui.ErrorSnackbar;
import net.gini.android.vision.review.ReviewActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class AnalysisScreenTest {

    private static final long TEST_PAUSE_DURATION = 500;

    private static byte[] TEST_JPEG = null;

    @Rule
    public ActivityTestRule<AnalysisActivityTestSpy> mActivityTestRule = new ActivityTestRule<>(AnalysisActivityTestSpy.class, true, false);

    @BeforeClass
    public static void setupClass() throws IOException {
        TEST_JPEG = getTestJpegAsset();
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        TEST_JPEG = null;
    }

    @After
    public void tearDown() throws Exception {
        resetDeviceOrientation();
    }

    @Test
    public void should_showErrorSnackbar_withButton_andClickListener_whenRequested() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        final AtomicBoolean buttonClicked = new AtomicBoolean();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.showError("Test message", "Test button", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonClicked.set(true);
                    }
                });
            }
        });

        // Wait a little for the snackbar to be shown
        Thread.sleep(TEST_PAUSE_DURATION);

        Espresso.onView(ViewMatchers.withText("Test message"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText("Test button"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());

        assertThat(buttonClicked.get()).isTrue();
    }

    @Test
    public void should_showErrorSnackbar_withoutButton_whenRequested() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.showError("Test message", ErrorSnackbar.LENGTH_LONG);
            }
        });

        // Wait a little for the snackbar to be shown
        Thread.sleep(TEST_PAUSE_DURATION);

        Espresso.onView(ViewMatchers.withText("Test message"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void should_hideErrorSnackbar_whenRequested() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.showError("Test message", ErrorSnackbar.LENGTH_LONG);
            }
        });

        // Wait a little for the snackbar to be shown
        Thread.sleep(TEST_PAUSE_DURATION);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.hideError();
            }
        });

        // Wait a little for the snackbar to be hidden
        Thread.sleep(TEST_PAUSE_DURATION);

        Espresso.onView(ViewMatchers.withText("Test message"))
                .check(ViewAssertions.doesNotExist());
    }

    @Test
    public void should_notInvokeAddDataToResult_whenHomeButton_wasPressed() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        // Click home (back)
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
                .perform(ViewActions.click());

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.addDataToResultIntent).isNull();
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_notInvokeAddDataToResult_whenBackButton_wasPressed() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        // Click back
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.addDataToResultIntent).isNull();
    }

    @Test
    public void should_showErrorMessage_whenAnalysisErrorMessage_wasGiven() throws InterruptedException {
        Intent intent = getAnalysisActivityIntentWithDocument(TEST_JPEG, 0);
        intent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE, "Something happened");
        mActivityTestRule.launchActivity(intent);

        // Allow the activity to run a little for the error message to be shown
        Thread.sleep(TEST_PAUSE_DURATION);

        Espresso.onView(ViewMatchers.withText("Something happened"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void should_invokeAnalyzeDocument_whenAnalysisErrorRetryButton_wasClicked() throws InterruptedException {
        Intent intent = getAnalysisActivityIntentWithDocument(TEST_JPEG, 0);
        intent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE, "Something happened");
        AnalysisActivityTestSpy activity = mActivityTestRule.launchActivity(intent);

        // Allow the activity to run a little for the error message to be shown
        Thread.sleep(TEST_PAUSE_DURATION);

        Espresso.onView(ViewMatchers.withId(R.id.gv_button_error))
                .perform(ViewActions.click());

        assertThat(activity.analyzeDocument).isNotNull();

        assertAbout(document()).that(activity.analyzeDocument)
                .isEqualToDocument(DocumentFactory.newDocumentFromPhoto(
                        PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "phone", "camera")));
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_forcePortraitOrientation_onPhones() throws Exception {
        // Given
        assumeTrue(!isTablet());

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationLeft();
        waitForWindowUpdate(uiDevice);

        final AnalysisActivity analysisActivity = startAnalysisActivity(TEST_JPEG, 90);
        waitForWindowUpdate(uiDevice);

        // Then
        int rotation = analysisActivity.getWindowManager().getDefaultDisplay().getRotation();
        assertThat(rotation).isEqualTo(Surface.ROTATION_0);
    }

    private AnalysisActivityTestSpy startAnalysisActivity(byte[] jpeg, int orientation) {
        Intent intent = getAnalysisActivityIntent();
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(jpeg, orientation, "portrait", "phone", "camera")));
        return mActivityTestRule.launchActivity(intent);
    }

    private Intent getAnalysisActivityIntent() {
        return new Intent(InstrumentationRegistry.getTargetContext(), AnalysisActivityTestSpy.class);
    }

    private Intent getAnalysisActivityIntentWithDocument(byte[] jpeg, int orientation) {
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), AnalysisActivityTestSpy.class);
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, createDocument(jpeg, orientation, "portrait", "phone", "camera"));
        return intent;
    }
}
