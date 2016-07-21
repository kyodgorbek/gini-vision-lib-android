package net.gini.android.vision.review;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.ortiz.touch.TouchImageView;

import net.gini.android.vision.GiniVisionDebug;
import net.gini.android.vision.Document;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.R;
import net.gini.android.vision.camera.photo.Photo;
import net.gini.android.vision.camera.photo.PhotoEdit;
import net.gini.android.vision.ui.FragmentImplCallback;

class ReviewFragmentImpl implements ReviewFragmentInterface {

    private static final ReviewFragmentListener NO_OP_LISTENER = new ReviewFragmentListener() {
        @Override
        public void onShouldAnalyzeDocument(@NonNull Document document) {

        }

        @Override
        public void onProceedToAnalysisScreen(@NonNull Document document) {

        }

        @Override
        public void onDocumentReviewedAndAnalyzed(@NonNull Document document) {

        }

        @Override
        public void onError(@NonNull GiniVisionError error) {
        }
    };

    private FrameLayout mLayoutDocumentContainer;
    private TouchImageView mImageDocument;
    private ImageButton mButtonRotate;
    private ImageButton mButtonNext;

    private final FragmentImplCallback mFragment;
    private Photo mPhoto;
    private ReviewFragmentListener mListener = NO_OP_LISTENER;
    private boolean mPhotoWasAnalyzed = false;
    private boolean mPhotoWasModified = false;
    private int mCurrentRotation = 0;

    public ReviewFragmentImpl(@NonNull FragmentImplCallback fragment, @NonNull Document document) {
        mFragment = fragment;
        mPhoto = Photo.fromDocument(document);
    }

    public void setListener(@Nullable ReviewFragmentListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    public void onDocumentAnalyzed() {
        mPhotoWasAnalyzed = true;
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        mListener.onShouldAnalyzeDocument(Document.fromPhoto(mPhoto));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gv_fragment_review, container, false);
        bindViews(view);
        setInputHandlers();
        observeViewTree(view);
        return view;
    }

    public void onStart() {
        showDocument();
    }

    private void showDocument() {
        mImageDocument.setImageBitmap(mPhoto.getBitmapPreview());
    }

    public void onDestroy() {
        mPhoto = null;
    }

    private void bindViews(@NonNull View view) {
        mLayoutDocumentContainer = (FrameLayout) view.findViewById(R.id.gv_layout_document_container);
        mImageDocument = (TouchImageView) view.findViewById(R.id.gv_image_document);
        mButtonRotate = (ImageButton) view.findViewById(R.id.gv_button_rotate);
        mButtonNext = (ImageButton) view.findViewById(R.id.gv_button_next);
    }

    private void setInputHandlers() {
        mButtonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRotateClicked();
            }
        });
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClicked();
            }
        });
    }

    private void observeViewTree(@NonNull final View view) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onViewLayoutFinished();
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    private void onViewLayoutFinished() {
        rotateDocumentForDisplay();
    }

    private void rotateDocumentForDisplay() {
        mCurrentRotation = mPhoto.getRotationForDisplay();
        rotateImageView(mPhoto.getRotationForDisplay(), false);
    }

    private void onRotateClicked() {
        mCurrentRotation += 90;
        rotateImageView(mCurrentRotation, true);
        mPhotoWasModified = true;
    }

    private void onNextClicked() {
        if (!mPhotoWasModified) {
            if (!mPhotoWasAnalyzed) {
                proceedToAnalysisScreen();
            } else {
                // Photo was not modified and has been analyzed, client should show extraction results
                mListener.onDocumentReviewedAndAnalyzed(Document.fromPhoto(mPhoto));
                GiniVisionDebug.writePhotoToFile(mFragment.getActivity(), mPhoto, "_reviewed");
            }
        } else {
            proceedToAnalysisScreen();
        }
    }

    private void proceedToAnalysisScreen() {
        applyRotationToJpeg(new PhotoEdit.PhotoEditCallback() {
            @Override
            public void onDone(@NonNull Photo photo) {
                mListener.onProceedToAnalysisScreen(Document.fromPhoto(photo));
                GiniVisionDebug.writePhotoToFile(mFragment.getActivity(), photo, "_reviewed");
            }

            @Override
            public void onFailed() {
                mListener.onError(new GiniVisionError(GiniVisionError.ErrorCode.REVIEW, "An error occurred while applying rotation to the jpeg."));
            }
        });
    }

    private void applyRotationToJpeg(@NonNull PhotoEdit.PhotoEditCallback callback) {
        mPhoto.edit().rotate(mCurrentRotation).applyAsync(callback);
    }

    private void rotateImageView(int degrees, boolean animated) {
        if (degrees == 0) {
            return;
        }

        mImageDocument.resetZoom();

        ValueAnimator widthAnimation;
        ValueAnimator heightAnimation;
        if (degrees % 360 == 90 ||
                degrees % 360 == 270) {
            widthAnimation = ValueAnimator.ofInt(mImageDocument.getWidth(), mLayoutDocumentContainer.getHeight());
            heightAnimation = ValueAnimator.ofInt(mImageDocument.getHeight(), mLayoutDocumentContainer.getWidth());
        } else {
            widthAnimation = ValueAnimator.ofInt(mImageDocument.getWidth(), mLayoutDocumentContainer.getWidth());
            heightAnimation = ValueAnimator.ofInt(mImageDocument.getHeight(), mLayoutDocumentContainer.getHeight());
        }

        widthAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int width = (int) valueAnimator.getAnimatedValue();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mImageDocument.getLayoutParams();
                layoutParams.width = width;
                mImageDocument.requestLayout();
            }
        });
        heightAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = (int) valueAnimator.getAnimatedValue();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mImageDocument.getLayoutParams();
                layoutParams.height = height;
                mImageDocument.requestLayout();
            }
        });

        ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(mImageDocument, "rotation", degrees);

        if (!animated) {
            widthAnimation.setDuration(0);
            heightAnimation.setDuration(0);
            rotateAnimation.setDuration(0);
        }

        widthAnimation.start();
        heightAnimation.start();
        rotateAnimation.start();
    }
}