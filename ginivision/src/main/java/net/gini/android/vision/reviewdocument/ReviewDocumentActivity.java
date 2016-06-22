package net.gini.android.vision.reviewdocument;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.R;
import net.gini.android.vision.scanner.Document;

public abstract class ReviewDocumentActivity extends AppCompatActivity implements ReviewDocumentFragmentListener, ReviewDocumentFragmentInterface {

    public static final String EXTRA_IN_DOCUMENT = "GV_EXTRA_IN_DOCUMENT";
    public static final String EXTRA_OUT_DOCUMENT = "GV_EXTRA_OUT_DOCUMENT";
    public static final String EXTRA_OUT_ERROR = "GV_EXTRA_OUT_ERROR";

    public static final int RESULT_PHOTO_WAS_REVIEWED = RESULT_FIRST_USER + 1;
    public static final int RESULT_PHOTO_WAS_REVIEWED_AND_ANALYZED = RESULT_FIRST_USER + 2;
    public static final int RESULT_ERROR = RESULT_FIRST_USER + 3;

    private ReviewDocumentFragmentCompat mFragment;
    private Document mDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gv_activity_review_document);
        if (savedInstanceState == null) {
            readExtras();
            createFragment();
            showFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMemory();
    }

    private void clearMemory() {
        mDocument = null;
    }

    private void readExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDocument = extras.getParcelable(EXTRA_IN_DOCUMENT);
        }
        checkRequiredExtras();
    }

    private void checkRequiredExtras() {
        if (mDocument == null) {
            throw new IllegalStateException("ReviewDocumentActivity requires a Document. Set it as an extra using the EXTRA_IN_DOCUMENT key.");
        }
    }

    private void createFragment() {
        mFragment = ReviewDocumentFragmentCompat.createInstance(mDocument);
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gv_fragment_review_document, mFragment)
                .commit();
    }

    // callback for subclasses for uploading the photo before it was reviewed, if the photo is not changed
    // no new upload is required
    @Override
    public abstract void onShouldAnalyzeDocument(Document document);

    @Override
    public void onProceedToAnalyzeScreen(Document document) {
        Intent result = new Intent();
        result.putExtra(EXTRA_OUT_DOCUMENT, document);
        onAddDataToResult(result);
        setResult(RESULT_PHOTO_WAS_REVIEWED, result);
        finish();
    }

    @Override
    public void onDocumentReviewedAndAnalyzed(Document document) {
        Intent result = new Intent();
        result.putExtra(EXTRA_OUT_DOCUMENT, document);
        onAddDataToResult(result);
        setResult(RESULT_PHOTO_WAS_REVIEWED_AND_ANALYZED, result);
        finish();
    }

    // Photo was already analyzed, add the extractions to the result
    public abstract void onAddDataToResult(Intent result);

    // TODO: call this, if the photo was analyzed before the review was completed, it prevents the analyze activity to
    // be started, if the photo was already analyzed and the user didn't change it
    @Override
    public void onDocumentAnalyzed() {
        mFragment.onDocumentAnalyzed();
    }

    @Override
    public void onError(GiniVisionError error) {
        Intent result = new Intent();
        result.putExtra(EXTRA_OUT_ERROR, error);
        setResult(RESULT_ERROR, result);
        finish();
    }
}
