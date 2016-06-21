package net.gini.android.vision.reviewphoto;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.R;
import net.gini.android.vision.scanner.photo.Photo;

public abstract class ReviewPhotoActivity extends AppCompatActivity implements ReviewPhotoFragmentListener {

    public static final String EXTRA_DOCUMENT = "GV_EXTRA_DOCUMENT";
    public static final String EXTRA_ERROR = "GV_EXTRA_ERROR";

    private ReviewPhotoFragmentCompat mFragment;
    private Photo mPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gv_activity_review_photo);
        bindViews();
        if (savedInstanceState == null) {
            readExtras();
            createFragment();
            showFragment();
        }
    }

    private void bindViews() {
        mFragment = (ReviewPhotoFragmentCompat) getSupportFragmentManager().findFragmentById(R.id.gv_fragment_review_photo);
    }

    private void readExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mPhoto = extras.getParcelable(EXTRA_DOCUMENT);
        }
        checkRequiredExtras();
    }

    private void checkRequiredExtras() {
        if (mPhoto == null) {
            throw new IllegalStateException("ReviewPhotoActivity requires a Photo. Set it as an extra using the EXTRA_PHOTO key.");
        }
    }

    private void createFragment() {
        mFragment = ReviewPhotoFragmentCompat.createInstance(mPhoto);
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gv_fragment_review_photo, mFragment)
                .commit();
    }

    // callback for subclasses for uploading the photo before it was reviewed, if the photo is not changed
    // no new upload is required
    @Override
    public abstract void onShouldAnalyzePhoto(Photo photo);

    @Override
    public void onProceedToAnalyzePhotoScreen(Photo photo) {
        // TODO: start analyze screen
        Toast.makeText(this, "Should proceed to Analyze Screen", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPhotoReviewedAndAnalyzed(Photo photo) {
        Intent result = new Intent();
        result.putExtra(EXTRA_DOCUMENT, photo);
        onAddDataToResult(result);
        setResult(RESULT_OK, result);
        finish();
    }

    // Photo was already analyzed, add the extractions to the result
    public abstract void onAddDataToResult(Intent result);

    // TODO: call this, if the photo was analyzed before the review was completed, it prevents the analyze activity to
    // be started, if the photo was already analyzed and the user didn't change it
    protected void onPhotoAnalyzed() {
        mFragment.onPhotoAnalyzed();
    }

    @Override
    public void onError(GiniVisionError error) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ERROR, error);
        setResult(RESULT_CANCELED, result);
        finish();
    }
}
