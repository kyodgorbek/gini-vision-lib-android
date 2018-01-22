package net.gini.android.vision.analysis;

import static net.gini.android.vision.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.gini.android.vision.Document;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.R;
import net.gini.android.vision.document.GiniVisionDocument;
import net.gini.android.vision.document.PdfDocument;
import net.gini.android.vision.internal.AsyncCallback;
import net.gini.android.vision.internal.document.DocumentRenderer;
import net.gini.android.vision.internal.document.DocumentRendererFactory;
import net.gini.android.vision.internal.ui.ErrorSnackbar;
import net.gini.android.vision.internal.ui.FragmentImplCallback;
import net.gini.android.vision.internal.util.Size;
import net.gini.android.vision.util.UriHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Random;

class AnalysisFragmentImpl implements AnalysisFragmentInterface {

    protected static final Logger LOG = LoggerFactory.getLogger(AnalysisFragmentImpl.class);

    private static final AnalysisFragmentListener NO_OP_LISTENER = new AnalysisFragmentListener() {
        @Override
        public void onAnalyzeDocument(@NonNull final Document document) {
        }

        @Override
        public void onError(@NonNull final GiniVisionError error) {
        }
    };

    private final FragmentImplCallback mFragment;
    private int mFragmentHeight;
    private TextView mAnalysisMessageTextView;
    private ViewPropertyAnimatorCompat mHintHeadlineAnimation;
    private ViewPropertyAnimatorCompat mHintAnimation;
    private View mHintContainer;
    private ImageView mHintImageView;
    private TextView mHintTextView;
    private TextView mHintHeadlineTextView;
    private List<AnalysisHint> mHints;
    private DocumentRenderer mDocumentRenderer;
    private final GiniVisionDocument mDocument;
    private final String mDocumentAnalysisErrorMessage;
    private ImageView mImageDocument;
    private RelativeLayout mLayoutRoot;
    private AnalysisFragmentListener mListener = NO_OP_LISTENER;
    private ProgressBar mProgressActivity;
    private Runnable mHintCycleRunnable;
    private LinearLayout mPdfOverlayLayout;
    private TextView mPdfTitleTextView;
    private TextView mPdfPageCountTextView;
    private LinearLayout mAnalysisOverlay;

    private static final int HINT_ANIMATION_DURATION = 500;
    private static final int HINT_START_DELAY = 5000;
    private static final int HINT_CYCLE_INTERVAL = 4000;
    private boolean mStopped;


    AnalysisFragmentImpl(final FragmentImplCallback fragment, final Document document,
            final String documentAnalysisErrorMessage) {
        mFragment = fragment;
        mDocument = (GiniVisionDocument) document;
        mDocumentAnalysisErrorMessage = documentAnalysisErrorMessage;
    }

    @Override
    public void hideError() {
        if (mLayoutRoot == null) {
            return;
        }
        ErrorSnackbar.hideExisting(mLayoutRoot);
    }

    @Override
    public void onNoExtractionsFound() {

    }

    @Override
    public void onDocumentAnalyzed() {

    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        if (mFragment.getActivity() == null || mLayoutRoot == null) {
            return;
        }
        ErrorSnackbar.make(mFragment.getActivity(), mLayoutRoot, message, null, null,
                duration).show();
    }

    @Override
    public void showError(@NonNull final String message, @NonNull final String buttonTitle,
            @NonNull final View.OnClickListener onClickListener) {
        if (mFragment.getActivity() == null) {
            return;
        }
        ErrorSnackbar.make(mFragment.getActivity(), mLayoutRoot, message, buttonTitle,
                onClickListener, ErrorSnackbar.LENGTH_INDEFINITE).show();
    }

    @Override
    public void startScanAnimation() {
        mProgressActivity.setVisibility(View.VISIBLE);
        mAnalysisMessageTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void stopScanAnimation() {
        mProgressActivity.setVisibility(View.GONE);
        mAnalysisMessageTextView.setVisibility(View.GONE);
    }

    public void onCreate(final Bundle savedInstanceState) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        forcePortraitOrientationOnPhones(activity);
        mDocumentRenderer = DocumentRendererFactory.fromDocument(mDocument, activity);
        mHints = generateRandomHintsList();
    }

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gv_fragment_analysis, container, false);
        bindViews(view);
        return view;
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public void onDestroy() {
        mImageDocument = null; // NOPMD
        stopScanAnimation();
    }

    public void onStart() {
        mStopped = false;
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        startScanAnimation();
        LOG.debug("Loading document data");
        mDocument.loadData(activity,
                new AsyncCallback<byte[]>() {
                    @Override
                    public void onSuccess(final byte[] result) {
                        LOG.debug("Document data loaded");
                        if (mStopped) {
                            return;
                        }
                        observeViewTree();
                    }

                    @Override
                    public void onError(final Exception exception) {
                        LOG.error("Failed to load document data", exception);
                        if (mStopped) {
                            return;
                        }
                        mListener.onError(new GiniVisionError(GiniVisionError.ErrorCode.ANALYSIS,
                                "An error occurred while loading the document."));
                    }
                });
        if (mDocument.getType() == Document.Type.IMAGE) {
            showHints();
        }
    }

    private void showHints() {
        mHintCycleRunnable = new Runnable() {
            @Override
            public void run() {
                mHintAnimation = getSlideDownAnimation();
                mHintAnimation.start();
            }
        };
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(mHintHeadlineTextView.getText())) {
                    mHintHeadlineAnimation = getHintHeadlineSlideDownAnimation();
                    mHintHeadlineAnimation.start();
                }
                mHandler.post(mHintCycleRunnable);
            }
        }, HINT_START_DELAY);
    }

    private ViewPropertyAnimatorCompat getHintHeadlineSlideDownAnimation() {
        return ViewCompat.animate(mHintHeadlineTextView)
                .translationY(mFragmentHeight)
                .setDuration(HINT_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        showHeadlineText();
                        mHintHeadlineAnimation = getHintHeadlineSlideUpAnimation();
                        mHintHeadlineAnimation.start();
                    }
                });
    }

    private void showHeadlineText() {
        mHintHeadlineTextView.setText(R.string.gv_analysis_hint_headline);
    }

    private ViewPropertyAnimatorCompat getHintHeadlineSlideUpAnimation() {
        return ViewCompat.animate(mHintHeadlineTextView)
                .translationY(0)
                .setDuration(HINT_ANIMATION_DURATION);
    }

    @NonNull
    private ViewPropertyAnimatorCompat getSlideDownAnimation() {
        return ViewCompat.animate(mHintContainer)
                .translationY(mFragmentHeight)
                .setDuration(HINT_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        setNextHint();
                        mHintAnimation = getSlideUpAnimation();
                        mHintAnimation.start();
                    }
                });
    }

    @NonNull
    private ViewPropertyAnimatorCompat getSlideUpAnimation() {
        return ViewCompat.animate(mHintContainer)
                .translationY(0)
                .setDuration(HINT_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        mHandler.postDelayed(mHintCycleRunnable, HINT_CYCLE_INTERVAL);
                    }
                });
    }

    private void setNextHint() {
        final AnalysisHint nextHint = getNextHint();
        final Context context = mFragment.getActivity();
        if (context != null) {
            mHintImageView.setImageDrawable(
                    ContextCompat.getDrawable(context, nextHint.getDrawableResource()));
        }
        mHintTextView.setText(nextHint.getTextResource());
    }

    private AnalysisHint getNextHint() {
        final AnalysisHint analysisHint = mHints.remove(0);
        mHints.add(analysisHint);
        return analysisHint;
    }

    private List<AnalysisHint> generateRandomHintsList() {
        final List<AnalysisHint> list = AnalysisHint.getArray();
        Collections.shuffle(list, new Random());
        return list;
    }

    void onStop() {
        mStopped = true;
        mHandler.removeCallbacks(mHintCycleRunnable);
        if (mHintAnimation != null) {
            mHintAnimation.cancel();
            mHintContainer.clearAnimation();
            mHintAnimation.setListener(null);
        }
        if (mHintHeadlineAnimation != null) {
            mHintHeadlineAnimation.cancel();
            mHintHeadlineTextView.clearAnimation();
            mHintHeadlineAnimation.setListener(null);
        }
    }

    public void setListener(@Nullable final AnalysisFragmentListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    @VisibleForTesting
    ImageView getImageDocument() {
        return mImageDocument;
    }

    @VisibleForTesting
    ProgressBar getProgressActivity() {
        return mProgressActivity;
    }

    private void analyzeDocument() {
        if (mFragment.getActivity() == null) {
            return;
        }
        if (mDocumentAnalysisErrorMessage != null) {
            showError(mDocumentAnalysisErrorMessage,
                    mFragment.getActivity().getString(R.string.gv_document_analysis_error_retry),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            mListener.onAnalyzeDocument(mDocument);
                        }
                    });
        } else {
            mListener.onAnalyzeDocument(mDocument);
        }
    }

    private void bindViews(@NonNull final View view) {
        mLayoutRoot = view.findViewById(R.id.gv_layout_root);
        mImageDocument = view.findViewById(R.id.gv_image_picture);
        mProgressActivity = view.findViewById(R.id.gv_progress_activity);
        mHintImageView = view.findViewById(R.id.gv_analysis_hint_image);
        mHintTextView = view.findViewById(R.id.gv_analysis_hint_text);
        mHintContainer = view.findViewById(R.id.gv_analysis_hint_container);
        mAnalysisMessageTextView = view.findViewById(R.id.gv_analysis_message);
        mPdfOverlayLayout = view.findViewById(R.id.gv_pdf_info);
        mPdfTitleTextView = view.findViewById(R.id.gv_pdf_filename);
        mPdfPageCountTextView = view.findViewById(R.id.gv_pdf_page_count);
        mAnalysisOverlay = view.findViewById(R.id.gv_analysis_overlay);
        mHintHeadlineTextView = view.findViewById(R.id.gv_analysis_hint_headline);
    }

    private void observeViewTree() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        LOG.debug("Observing the view layout");
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        onViewLayoutFinished();
                        mFragmentHeight = view.getHeight();
                    }
                });
        view.requestLayout();
    }

    private void onViewLayoutFinished() {
        LOG.debug("View layout finished");
        showPdfInfoForPdfDocument();
        showDocument();
        analyzeDocument();
    }

    private void rotateDocumentImageView(final int rotationForDisplay) {
        if (rotationForDisplay == 0) {
            return;
        }
        int newWidth = mLayoutRoot.getWidth();
        int newHeight = mLayoutRoot.getHeight();
        if (rotationForDisplay == 90 || rotationForDisplay == 270) {
            newWidth = mLayoutRoot.getHeight();
            newHeight = mLayoutRoot.getWidth();
        }

        final FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mImageDocument.getLayoutParams();
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        mImageDocument.setLayoutParams(layoutParams);
        mImageDocument.setRotation(rotationForDisplay);
    }

    private void showDocument() {
        LOG.debug("Rendering the document");
        final Size previewSize = new Size(mImageDocument.getWidth(), mImageDocument.getHeight());
        mDocumentRenderer.toBitmap(previewSize, new DocumentRenderer.Callback() {
            @Override
            public void onBitmapReady(@Nullable final Bitmap bitmap, final int rotationForDisplay) {
                LOG.debug("Document rendered");
                if (mStopped) {
                    return;
                }
                rotateDocumentImageView(rotationForDisplay);
                mImageDocument.setImageBitmap(bitmap);
            }
        });
    }

    private void showPdfInfoForPdfDocument() {
        if (mDocument instanceof PdfDocument) {
            final Activity activity = mFragment.getActivity();
            if (activity == null) {
                return;
            }
            mPdfOverlayLayout.setVisibility(View.VISIBLE);
            mAnalysisOverlay.setBackgroundColor(Color.TRANSPARENT);
            mAnalysisMessageTextView.setText("");

            final PdfDocument pdfDocument = (PdfDocument) mDocument;
            final String filename = getPdfFilename(activity, pdfDocument);
            if (filename != null) {
                mPdfTitleTextView.setText(filename);
            }

            mPdfPageCountTextView.setVisibility(View.VISIBLE);
            mPdfPageCountTextView.setText("");

            mDocumentRenderer.getPageCount(new AsyncCallback<Integer>() {
                @Override
                public void onSuccess(final Integer result) {
                    if (result > 0) {
                        mPdfPageCountTextView.setVisibility(View.VISIBLE);
                        final String pageCountString = activity.getResources().getQuantityString(
                                R.plurals.gv_analysis_pdf_pages, result, result);
                        mPdfPageCountTextView.setText(pageCountString);
                    } else {
                        mPdfPageCountTextView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(final Exception exception) {
                    mPdfPageCountTextView.setVisibility(View.GONE);
                }
            });
        }
    }

    @Nullable
    private String getPdfFilename(final Activity activity, final PdfDocument pdfDocument) {
        final Uri uri = pdfDocument.getUri();
        try {
            return UriHelper.getFilenameFromUri(uri, activity);
        } catch (final IllegalStateException e) { // NOPMD
            // Ignore
        }
        return null;
    }
}
