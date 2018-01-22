package net.gini.android.vision.review;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.Helpers.createDocument;
import static net.gini.android.vision.testutils.RobolectricHelpers.getTestJpegJavaResource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.support.annotation.NonNull;

import net.gini.android.vision.BuildConfig;
import net.gini.android.vision.Document;
import net.gini.android.vision.analysis.AnalysisActivityTestSpy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Alpar Szotyori on 22.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class ReviewScreenRoboTest {

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenDocument_wasNotGiven() {
        final Intent intent =
                new Intent(RuntimeEnvironment.application, ReviewActivityTestSpy.class);
        intent.putExtra(ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY,
                new Intent(RuntimeEnvironment.application,
                        AnalysisActivityTestSpy.class));
        Robolectric.buildActivity(ReviewActivityTestSpy.class, intent).setup();
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenAnalysisActivityClass_wasNotGiven() throws IOException {
        final Intent intent =
                new Intent(RuntimeEnvironment.application, ReviewActivityTestSpy.class);
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT,
                createDocument(getTestJpegJavaResource(), 0, "portrait", "phone", "camera"));
        Robolectric.buildActivity(ReviewActivityTestSpy.class, intent).setup();
    }

    @Test
    public void should_rotatePreview_accordingToOrientation()
            throws IOException, InterruptedException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 180);

        assertThat(
                activity.getFragment().getFragmentImpl().getImageDocument().getRotation()).isWithin(
                0.0f).of(180);
    }

    private ReviewActivityTestSpy startReviewActivity(final byte[] jpeg, final int orientation) {
        final Intent intent = getReviewActivityIntent(jpeg, orientation);
        return Robolectric.buildActivity(ReviewActivityTestSpy.class, intent).setup().get();
    }

    private Intent getReviewActivityIntent(final byte[] jpeg, final int orientation) {
        final Intent intent = new Intent(RuntimeEnvironment.application,
                ReviewActivityTestSpy.class);
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT,
                createDocument(jpeg, orientation, "portrait", "phone", "camera"));
        intent.putExtra(ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY,
                new Intent(RuntimeEnvironment.application,
                        AnalysisActivityTestSpy.class));
        return intent;
    }

    @Test
    public void should_rotatePreview_whenRotateButton_isClicked()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 90);

        activity.getFragment().getFragmentImpl().rotateImage(false);

        assertThat(
                activity.getFragment().getFragmentImpl().getImageDocument().getRotation()).isWithin(
                0.0f).of(180);
    }

    @Test
    public void should_invokeAnalyzeDocument_whenLaunched()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 90);

        final AtomicBoolean analyzeDocumentInvoked = new AtomicBoolean();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                analyzeDocumentInvoked.set(true);
            }
        });

        assertThat(analyzeDocumentInvoked.get()).isTrue();
    }

    @Test
    public void should_compressJpeg_beforeAnalyzeDocument_isInvoked()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 90);

        final AtomicReference<Document> documentToAnalyze = new AtomicReference<>();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                documentToAnalyze.set(document);
            }
        });

        assertThat(documentToAnalyze.get()).isNotNull();
        assertThat(documentToAnalyze.get().getData().length).isLessThan(
                getTestJpegJavaResource().length);
    }

    @Test
    public void should_onlyInvokeProceedToAnalysis_whenNextButton_wasClicked_ifDocument_wasModified_andNotAnalyzed()
            throws InterruptedException, IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 0);

        // Modify the document
        activity.getFragment().getFragmentImpl().rotateImage(false);

        final AtomicBoolean proceedToAnalysisInvoked = new AtomicBoolean();
        final AtomicBoolean addDataToResultInvoked = new AtomicBoolean();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onAddDataToResult(@NonNull final Intent result) {
                addDataToResultInvoked.set(true);
            }

            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                proceedToAnalysisInvoked.set(true);
            }
        });

        // Click next
        activity.getFragment().getFragmentImpl().onNextClicked();

        assertThat(proceedToAnalysisInvoked.get()).isTrue();
        assertThat(addDataToResultInvoked.get()).isFalse();
    }

    @Test
    public void should_onlyInvokeProceedToAnalysis_whenNextButton_wasClicked_ifDocument_wasModified_andAnalyzed()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 0);

        // Modify the document
        activity.getFragment().getFragmentImpl().rotateImage(false);

        final AtomicBoolean proceedToAnalysisInvoked = new AtomicBoolean();
        final AtomicBoolean addDataToResultInvoked = new AtomicBoolean();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                // Notify that document was analyzed
                activity.onDocumentAnalyzed();
            }

            @Override
            public void onAddDataToResult(@NonNull final Intent result) {
                addDataToResultInvoked.set(true);
            }

            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                proceedToAnalysisInvoked.set(true);
            }
        });

        // Click next
        activity.getFragment().getFragmentImpl().onNextClicked();

        assertThat(proceedToAnalysisInvoked.get()).isTrue();
        assertThat(addDataToResultInvoked.get()).isFalse();
    }

    @Test
    public void should_onlyInvokeProceedToAnalysis_whenNextButton_wasClicked_ifDocument_wasNotModified_andNotAnalyzed()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 0);

        final AtomicBoolean proceedToAnalysisInvoked = new AtomicBoolean();
        final AtomicBoolean addDataToResultInvoked = new AtomicBoolean();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onAddDataToResult(@NonNull final Intent result) {
                addDataToResultInvoked.set(true);
            }

            @Override
            public void onProceedToAnalysisScreen(@NonNull final Document document) {
                proceedToAnalysisInvoked.set(true);
            }
        });

        // Click next
        activity.getFragment().getFragmentImpl().onNextClicked();

        assertThat(proceedToAnalysisInvoked.get()).isTrue();
        assertThat(addDataToResultInvoked.get()).isFalse();
    }

    @Test
    public void should_invokeDocumentReviewed_andAddDataToResult_whenNextButton_wasClicked_ifDocument_wasNotModified_andWasAnalyzed()
            throws IOException {
        final ReviewActivityTestSpy activity = startReviewActivity(getTestJpegJavaResource(), 0);

        final AtomicBoolean documentReviewedCalled = new AtomicBoolean();
        final AtomicBoolean addDataToResultInvoked = new AtomicBoolean();

        activity.setListenerHook(new ReviewActivityTestSpy.ListenerHook() {
            @Override
            public void onShouldAnalyzeDocument(@NonNull final Document document) {
                // Notify that document was analyzed
                activity.onDocumentAnalyzed();
            }

            @Override
            public void onAddDataToResult(@NonNull final Intent result) {
                addDataToResultInvoked.set(true);
            }

            @Override
            public void onDocumentReviewedAndAnalyzed(@NonNull final Document document) {
                documentReviewedCalled.set(true);
            }
        });

        // Click next
        activity.getFragment().getFragmentImpl().onNextClicked();

        assertThat(documentReviewedCalled.get()).isTrue();
        assertThat(addDataToResultInvoked.get()).isTrue();
    }

    @Test
    public void should_notInvokeAnyListenerMethods_whenHomeButton_wasClicked()
            throws IOException {
        final Intent intent = getReviewActivityIntent(getTestJpegJavaResource(), 0);
        final ActivityController<ReviewActivityTestSpy> activityController =
                Robolectric.buildActivity(ReviewActivityTestSpy.class, intent).setup();
        final ReviewActivityTestSpy activity = activityController.get();

        final ReviewActivityTestSpy.ListenerHook listenerHook = mock(
                ReviewActivityTestSpy.ListenerHook.class);

        activity.setListenerHook(listenerHook);

        activityController.pause().stop().destroy();

        verify(listenerHook, never()).onDocumentReviewedAndAnalyzed(any(Document.class));
        verify(listenerHook, never()).onAddDataToResult(any(Intent.class));
        verify(listenerHook, never()).onProceedToAnalysisScreen(any(Document.class));
    }

}
