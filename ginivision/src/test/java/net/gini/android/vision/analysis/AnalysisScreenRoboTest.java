package net.gini.android.vision.analysis;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.DocumentSubject.document;
import static net.gini.android.vision.testutils.Helpers.createDocument;
import static net.gini.android.vision.testutils.RobolectricHelpers.getTestJpegJavaResource;

import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;

import net.gini.android.vision.BuildConfig;
import net.gini.android.vision.Document;
import net.gini.android.vision.review.ReviewActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by aszotyori on 14.11.17.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        manifest = "AndroidManifest.xml")
public class AnalysisScreenRoboTest {

    @Test
    public void should_invokeAnalyzeDocument_whenLaunched()
            throws IOException {
        final Document document = createDocument(getTestJpegJavaResource(), 0, "portrait", "phone",
                        "camera");
        AnalysisActivityTestSpy activity = startAnalysisActivity(document);

        assertThat(activity.analyzeDocument).isNotNull();
        assertAbout(document()).that(activity.analyzeDocument).isEqualToDocument(document);
    }

    private AnalysisActivityTestSpy startAnalysisActivity(Document document) {
        Intent intent = getAnalysisActivityIntent();
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, document);
        return Robolectric.buildActivity(AnalysisActivityTestSpy.class, intent).setup().get();
    }

    private Intent getAnalysisActivityIntent() {
        return new Intent(RuntimeEnvironment.application, AnalysisActivityTestSpy.class);
    }

    @Test
    public void should_rotatePreview_accordingToOrientation() throws IOException {
        AnalysisActivityTestSpy activity = startAnalysisActivity(180);

        assertThat(
                activity.getFragment().getFragmentImpl().getImageDocument().getRotation()).isWithin(
                0.0f).of(180);
    }

    private AnalysisActivityTestSpy startAnalysisActivity(int orientation) throws IOException {
        return startAnalysisActivity(getTestJpegJavaResource(), orientation);
    }

    private AnalysisActivityTestSpy startAnalysisActivity(byte[] jpeg, int orientation) {
        return startAnalysisActivity(createDocument(jpeg, orientation, "portrait", "phone", "camera"));
    }

    @Test
    public void should_startIndeterminateProgressAnimation_ifRequested() throws IOException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(0);

        activity.startScanAnimation();

        ProgressBar progressBar = activity.getFragment().getFragmentImpl().getProgressActivity();
        assertThat(progressBar.isIndeterminate()).isTrue();
        assertThat(progressBar.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void should_stopIndeterminateProgressAnimation_ifRequested() throws IOException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(0);

        activity.startScanAnimation();

        activity.stopScanAnimation();

        ProgressBar progressBar = activity.getFragment().getFragmentImpl().getProgressActivity();
        assertThat(progressBar.isIndeterminate()).isTrue();
        assertThat(progressBar.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void should_invokeAddDataToResult_andFinish_whenDocumentAnalyzed_hasBeenCalled() throws IOException {
        AnalysisActivityTestSpy activity = startAnalysisActivity(0);

        activity.onDocumentAnalyzed();

        assertThat(activity.addDataToResultIntent).isNotNull();
        assertThat(activity.finishWasCalled).isTrue();
    }

    @Test
    public void should_notInvokeAddDataToResult_whenFinished_withoutDocumentAnalyzed_beingCalled() throws IOException {
        AnalysisActivityTestSpy activity = startAnalysisActivity(0);

        activity.finish();

        assertThat(activity.addDataToResultIntent).isNull();
        assertThat(activity.finishWasCalled).isTrue();
    }

    @Test
    public void should_notInvokeAnalyzeDocument_whenAnalysisErrorMessage_wasGiven()
            throws IOException {
        Intent intent = getAnalysisActivityIntentWithDocument(getTestJpegJavaResource(), 0);
        intent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE,
                "Something happened");
        AnalysisActivityTestSpy activity = Robolectric.buildActivity(
                AnalysisActivityTestSpy.class, intent)
                .setup()
                .get();

        assertThat(activity.analyzeDocument).isNull();
    }

    private Intent getAnalysisActivityIntentWithDocument(byte[] jpeg, int orientation) {
        Intent intent = new Intent();
        intent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT,
                createDocument(jpeg, orientation, "portrait", "phone", "camera"));
        return intent;
    }
}
