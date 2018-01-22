package net.gini.android.vision.review;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.Helpers.createDocument;
import static net.gini.android.vision.testutils.ImageDocumentSubject.imageDocument;
import static net.gini.android.vision.testutils.InstrumentationHelpers.getTestJpegAsset;
import static net.gini.android.vision.testutils.InstrumentationHelpers.isTablet;
import static net.gini.android.vision.testutils.InstrumentationHelpers.prepareLooper;
import static net.gini.android.vision.testutils.InstrumentationHelpers.resetDeviceOrientation;
import static net.gini.android.vision.testutils.InstrumentationHelpers.waitForWindowUpdate;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.RequiresDevice;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.view.Surface;

import net.gini.android.vision.Document;
import net.gini.android.vision.R;
import net.gini.android.vision.analysis.AnalysisActivityTestSpy;
import net.gini.android.vision.document.ImageDocument;
import net.gini.android.vision.testutils.CurrentActivityTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@RequiresDevice
public class ReviewScreenTest {

    private static final int PAUSE_DURATION = 500;
    private static final int PAUSE_DURATION_LONG = 2_000;

    @Rule
    public CurrentActivityTestRule<ReviewActivityTestSpy> mActivityTestRule =
            new CurrentActivityTestRule<>(ReviewActivityTestSpy.class, true, false);

    private static byte[] TEST_JPEG = null;

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

    private ReviewActivityTestSpy startReviewActivity(final byte[] jpeg, final int orientation) {
        final Intent intent = getReviewActivityIntent(jpeg, orientation);
        return mActivityTestRule.launchActivity(intent);
    }

    private Intent getReviewActivityIntent(final byte[] jpeg, final int orientation) {
        final Intent intent = new Intent(InstrumentationRegistry.getTargetContext(),
                ReviewActivityTestSpy.class);
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT,
                createDocument(jpeg, orientation, "portrait", "phone", "camera"));
        intent.putExtra(ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY,
                new Intent(InstrumentationRegistry.getTargetContext(),
                        AnalysisActivityTestSpy.class));
        return intent;
    }

    @Test
    public void should_invokeDocumentWasRotated_whenRotateButton_wasClicked()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 0);

        final ReviewActivityTestSpy.ListenerHook listenerHook = mock(
                ReviewActivityTestSpy.ListenerHook.class);

        activity.setListenerHook(listenerHook);

        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click());

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(listenerHook).onDocumentWasRotated(any(Document.class), eq(0), eq(90));
    }

    @Test
    public void should_notFinish_whenReceivingActivityResult_withResultCodeCancelled_fromAnalysisActivity() {
        prepareLooper();

        final ReviewActivity reviewActivitySpy = Mockito.spy(new ReviewActivityTestSpy());

        reviewActivitySpy.onActivityResult(ReviewActivity.ANALYSE_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(reviewActivitySpy, never()).finish();
    }

    @Test
    public void should_finishIfEnabledByClient_whenReceivingActivityResult_withResultCodeCancelled_fromAnalysisActivity() {
        prepareLooper();

        final Intent intentAllowBackButtonToClose = getReviewActivityIntent(TEST_JPEG, 0);
        intentAllowBackButtonToClose.putExtra(
                ReviewActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, true);

        final ReviewActivity reviewActivity = new ReviewActivityTestSpy();
        reviewActivity.setIntent(intentAllowBackButtonToClose);
        reviewActivity.readExtras();

        final ReviewActivity reviewActivitySpy = Mockito.spy(reviewActivity);

        reviewActivitySpy.onActivityResult(ReviewActivity.ANALYSE_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(reviewActivitySpy).finish();
    }

    @Test
    public void should_returnDocuments_withSameContentId_inAnalyzeDocument_andProceedToAnalysis()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 90);

        final AtomicReference<Document> documentToAnalyze = new AtomicReference<>();
        final AtomicReference<Document> documentToProceedWith = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                documentToAnalyze.set(document);
            }

            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                documentToProceedWith.set(document);
            }
        });

        // Modify the document
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click());

        // Click next
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(PAUSE_DURATION);

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToAnalyze.get()).hasSameContentIdInUserCommentAs(
                (ImageDocument) documentToProceedWith.get());
    }

    @Test
    public void should_returnDocument_withZeroRotationDelta_inAnalyzeDocument()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 180);

        final AtomicReference<Document> documentToAnalyze = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                documentToAnalyze.set(document);
            }
        });

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(PAUSE_DURATION);

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToAnalyze.get()).hasRotationDeltaInUserComment(0);
    }

    @Test
    public void should_returnDocument_withNonZeroRotationDelta_inProceedToAnalysis_ifDocumentWasRotated()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 270);

        final AtomicReference<Document> documentToProceedWith = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                documentToProceedWith.set(document);
            }
        });

        // Modify the document
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click());

        // Click next
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(PAUSE_DURATION);

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToProceedWith.get()).hasRotationDeltaInUserComment(90);
    }

    @Test
    public void should_returnDocument_withCumulatedRotationDelta_inProceedToAnalysis_ifDocumentWasRotatedMultipleTimes()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 180);

        final AtomicReference<Document> documentToProceedWith = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                documentToProceedWith.set(document);
            }
        });

        // Modify the document
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click());
        // Modify the document
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click());

        // Click next
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(PAUSE_DURATION);

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToProceedWith.get()).hasRotationDeltaInUserComment(180);
    }

    @Test
    public void should_returnDocument_withNormalizedRotationDelta_inProceedToAnalysis_ifDocumentWasRotatedBeyond360Degrees()
            throws InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(TEST_JPEG, 90);

        final AtomicReference<Document> documentToProceedWith = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                documentToProceedWith.set(document);
            }
        });

        // Rotate the document 5 times
        for (int i = 0; i < 5; i++) {
            Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                    .perform(ViewActions.click());
        }

        // Click next
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_next))
                .perform(ViewActions.click());

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(PAUSE_DURATION);

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToProceedWith.get()).hasRotationDeltaInUserComment(90);
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_keepAppliedRotation_betweenOrientationChange() throws Exception {
        // Given
        assumeTrue(isTablet());

        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationNatural();
        waitForWindowUpdate(uiDevice);

        startReviewActivity(TEST_JPEG, 90);

        // Rotate the document
        Espresso.onView(ViewMatchers.withId(R.id.gv_button_rotate))
                .perform(ViewActions.click())
                .perform(ViewActions.click());

        // Give some time for the rotation animation to finish
        Thread.sleep(PAUSE_DURATION);

        // When
        uiDevice.setOrientationRight();
        waitForWindowUpdate(uiDevice);


        // Then
        final AtomicReference<Document> documentToAnalyzeAfterOrientationChange =
                new AtomicReference<>();

        final ReviewActivityTestSpy activity = mActivityTestRule.getCurrentActivity();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                documentToAnalyzeAfterOrientationChange.set(document);
            }
        });

        assertAbout(imageDocument()).that(
                (ImageDocument) documentToAnalyzeAfterOrientationChange.get()).hasRotationDeltaInUserComment(
                180);
        assertThat(documentToAnalyzeAfterOrientationChange.get().getRotationForDisplay()).isEqualTo(
                270);
        assertThat(
                activity.getFragment().getFragmentImpl().getImageDocument().getRotation()).isWithin(
                0.0f).of(270);
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_forcePortraitOrientation_onPhones() throws Exception {
        // Given
        assumeTrue(!isTablet());

        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationLeft();
        waitForWindowUpdate(uiDevice);

        final ReviewActivity reviewActivity = startReviewActivity(TEST_JPEG, 90);
        waitForWindowUpdate(uiDevice);

        // Then
        final int rotation = reviewActivity.getWindowManager().getDefaultDisplay().getRotation();
        assertThat(rotation)
                .isEqualTo(Surface.ROTATION_0);
    }
}
