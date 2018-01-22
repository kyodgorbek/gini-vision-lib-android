package net.gini.android.vision.camera;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import static net.gini.android.vision.camera.Util.cameraExceptionToGiniVisionError;
import static net.gini.android.vision.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.vision.internal.util.AndroidHelper.isMarshmallowOrLater;
import static net.gini.android.vision.internal.util.ContextHelper.getClientApplicationId;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import net.gini.android.vision.Document;
import net.gini.android.vision.DocumentImportEnabledFileTypes;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.GiniVisionFeatureConfiguration;
import net.gini.android.vision.R;
import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.document.GiniVisionDocument;
import net.gini.android.vision.document.QRCodeDocument;
import net.gini.android.vision.internal.camera.api.CameraController;
import net.gini.android.vision.internal.camera.api.CameraException;
import net.gini.android.vision.internal.camera.api.CameraInterface;
import net.gini.android.vision.internal.camera.api.UIExecutor;
import net.gini.android.vision.internal.camera.photo.Photo;
import net.gini.android.vision.internal.camera.view.CameraPreviewSurface;
import net.gini.android.vision.internal.fileimport.FileChooserActivity;
import net.gini.android.vision.internal.permission.PermissionRequestListener;
import net.gini.android.vision.internal.qrcode.PaymentQRCodeData;
import net.gini.android.vision.internal.qrcode.PaymentQRCodeReader;
import net.gini.android.vision.internal.qrcode.QRCodeDetectorTask;
import net.gini.android.vision.internal.qrcode.QRCodeDetectorTaskGoogleVision;
import net.gini.android.vision.internal.ui.ErrorSnackbar;
import net.gini.android.vision.internal.ui.ViewStubSafeInflater;
import net.gini.android.vision.internal.util.DeviceHelper;
import net.gini.android.vision.internal.util.FileImportValidator;
import net.gini.android.vision.internal.util.Size;
import net.gini.android.vision.util.IntentHelper;
import net.gini.android.vision.util.UriHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import jersey.repackaged.jsr166e.CompletableFuture;

class CameraFragmentImpl implements CameraFragmentInterface, PaymentQRCodeReader.Listener {

    private static final String GV_SHARED_PREFS = "GV_SHARED_PREFS";
    @VisibleForTesting
    static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final long HIDE_QRCODE_DETECTED_POPUP_DELAY_MS = 10000;
    private static final long DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS = 200;
    private static final Logger LOG = LoggerFactory.getLogger(CameraFragmentImpl.class);

    private static final CameraFragmentListener NO_OP_LISTENER = new CameraFragmentListener() {
        @Override
        public void onDocumentAvailable(@NonNull final Document document) {
        }

        @Override
        public void onQRCodeAvailable(@NonNull final QRCodeDocument qrCodeDocument) {

        }

        @Override
        public void onCheckImportedDocument(@NonNull final Document document,
                @NonNull final DocumentCheckResultCallback callback) {
            callback.documentAccepted();
        }

        @Override
        public void onError(@NonNull final GiniVisionError error) {
        }
    };

    private static final int REQ_CODE_CHOOSE_FILE = 1;
    private static final String SHOW_HINT_POP_UP = "SHOW_HINT_POP_UP";

    private final CameraFragmentImplCallback mFragment;
    private final GiniVisionFeatureConfiguration mGiniVisionFeatureConfiguration;
    private HideQRCodeDetectedRunnable mHideQRCodeDetectedPopupRunnable;

    private View mImageCorners;
    private boolean mInterfaceHidden;
    private CameraFragmentListener mListener = NO_OP_LISTENER;
    private final UIExecutor mUIExecutor = new UIExecutor();
    private CameraInterface mCameraController;
    private PaymentQRCodeReader mPaymentQRCodeReader;

    private RelativeLayout mLayoutRoot;
    private CameraPreviewSurface mCameraPreview;
    private ImageView mCameraFocusIndicator;
    private ImageButton mButtonCameraTrigger;
    private LinearLayout mLayoutNoPermission;
    private ImageButton mButtonImportDocument;
    private View mQRCodeDetectedPopupContainer;
    private PaymentQRCodeData mPaymentQRCodeData;
    private View mUploadHintCloseButton;
    private View mUploadHintContainer;
    private View mUploadHintContainerArrow;
    private View mCameraPreviewShade;
    private View mActivityIndicatorBackground;
    private ProgressBar mActivityIndicator;
    private ViewPropertyAnimatorCompat mUploadHintContainerArrowAnimation;
    private ViewPropertyAnimatorCompat mCameraPreviewShadeAnimation;
    private ViewPropertyAnimatorCompat mUploadHintContainerAnimation;
    private ViewPropertyAnimatorCompat mQRCodeDetectedPopupAnimation;

    private ViewStubSafeInflater mViewStubInflater;

    private CompletableFuture<SurfaceHolder> mSurfaceCreatedFuture = new CompletableFuture<>();
    private boolean mIsTakingPicture;

    private boolean mImportDocumentButtonEnabled;

    CameraFragmentImpl(@NonNull final CameraFragmentImplCallback fragment) {
        this(fragment, GiniVisionFeatureConfiguration.buildNewConfiguration().build());
    }

    CameraFragmentImpl(@NonNull final CameraFragmentImplCallback fragment,
            @NonNull final GiniVisionFeatureConfiguration giniVisionFeatureConfiguration) {
        mFragment = fragment;
        mGiniVisionFeatureConfiguration = giniVisionFeatureConfiguration;
    }

    @Override
    public void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        if (mUploadHintContainer.getVisibility() == View.VISIBLE
                || mInterfaceHidden
                || mActivityIndicator.getVisibility() == View.VISIBLE) {
            hideQRCodeDetectedPopup(null);
            mPaymentQRCodeData = null; // NOPMD
            return;
        }

        final View view = mFragment.getView();
        if (view == null) {
            return;
        }

        if (mPaymentQRCodeData == null
                || mQRCodeDetectedPopupContainer.getVisibility() == View.GONE) {
            showQRCodeDetectedPopup(0);
            view.removeCallbacks(mHideQRCodeDetectedPopupRunnable);
            view.postDelayed(mHideQRCodeDetectedPopupRunnable,
                    getHideQRCodeDetectedPopupDelayMs());
        } else {
            if (mPaymentQRCodeData.equals(paymentQRCodeData)) {
                view.removeCallbacks(mHideQRCodeDetectedPopupRunnable);
                view.postDelayed(mHideQRCodeDetectedPopupRunnable,
                        getHideQRCodeDetectedPopupDelayMs());
            } else {
                view.removeCallbacks(mHideQRCodeDetectedPopupRunnable);
                hideQRCodeDetectedPopup(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        showQRCodeDetectedPopup(
                                getDifferentQRCodeDetectedPopupDelayMs());
                    }
                });
            }
        }
        mPaymentQRCodeData = paymentQRCodeData;
    }

    @VisibleForTesting
    long getHideQRCodeDetectedPopupDelayMs() {
        return HIDE_QRCODE_DETECTED_POPUP_DELAY_MS;
    }

    @VisibleForTesting
    long getDifferentQRCodeDetectedPopupDelayMs() {
        return DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS;
    }

    private class HideQRCodeDetectedRunnable implements Runnable {

        @Override
        public void run() {
            hideQRCodeDetectedPopup(null);
            mPaymentQRCodeData = null; // NOPMD
        }
    }

    @VisibleForTesting
    void showQRCodeDetectedPopup(final long startDelay) {
        if (mQRCodeDetectedPopupContainer.getAlpha() != 0) {
            return;
        }
        clearQRCodeDetectedPopUpAnimation();
        mQRCodeDetectedPopupContainer.setVisibility(View.VISIBLE);
        mQRCodeDetectedPopupAnimation = ViewCompat.animate(mQRCodeDetectedPopupContainer)
                .alpha(1.0f)
                .setStartDelay(startDelay)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mQRCodeDetectedPopupAnimation.start();
    }

    private void hideQRCodeDetectedPopup(
            @Nullable final ViewPropertyAnimatorListener animatorListener) {
        if (mQRCodeDetectedPopupContainer.getAlpha() != 1) {
            if (animatorListener != null) {
                animatorListener.onAnimationEnd(mQRCodeDetectedPopupContainer);
            }
            return;
        }
        clearQRCodeDetectedPopUpAnimation();
        mQRCodeDetectedPopupAnimation = ViewCompat.animate(mQRCodeDetectedPopupContainer)
                .alpha(0.0f)
                .setDuration(DEFAULT_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        mQRCodeDetectedPopupContainer.setVisibility(View.GONE);
                        if (animatorListener != null) {
                            animatorListener.onAnimationEnd(view);
                        }
                    }
                });
        mQRCodeDetectedPopupAnimation.start();
    }

    private void clearQRCodeDetectedPopUpAnimation() {
        if (mQRCodeDetectedPopupAnimation != null) {
            mQRCodeDetectedPopupAnimation.cancel();
            mQRCodeDetectedPopupContainer.clearAnimation();
            mQRCodeDetectedPopupAnimation.setListener(null);
        }
        final View view = mFragment.getView();
        if (view != null) {
            view.removeCallbacks(mHideQRCodeDetectedPopupRunnable);
        }
    }

    void setListener(final CameraFragmentListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    public void onCreate(final Bundle savedInstanceState) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        forcePortraitOrientationOnPhones(activity);
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gv_fragment_camera, container, false);
        bindViews(view);
        setInputHandlers();
        setSurfaceViewCallback();
        return view;
    }

    public void onStart() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        initViews();
        initCameraController(activity);
        if (mGiniVisionFeatureConfiguration.isQRCodeScanningEnabled()) {
            mHideQRCodeDetectedPopupRunnable = new HideQRCodeDetectedRunnable();
            initQRCodeReader(activity);
        }

        final CompletableFuture<Void> openCameraCompletable = openCamera();
        final CompletableFuture<SurfaceHolder> surfaceCreationCompletable = handleSurfaceCreation();

        CompletableFuture.allOf(openCameraCompletable, surfaceCreationCompletable)
                .handle(new CompletableFuture.BiFun<Void, Throwable, Object>() {
                    @Override
                    public Object apply(final Void aVoid, final Throwable throwable) {
                        if (throwable != null) {
                            // Exceptions were handled before
                            return null;
                        }
                        try {
                            final SurfaceHolder surfaceHolder = surfaceCreationCompletable.get();
                            if (surfaceHolder != null) {
                                final Size previewSize =
                                        mCameraController.getPreviewSizeForDisplay();
                                mCameraPreview.setPreviewSize(previewSize);
                                startPreview(surfaceHolder);
                                enableTapToFocus();
                                showUploadHintPopUpOnFirstExecution();
                            } else {
                                handleError(GiniVisionError.ErrorCode.CAMERA_NO_PREVIEW,
                                        "Cannot start preview: no SurfaceHolder received for SurfaceView",
                                        null);
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            handleError(GiniVisionError.ErrorCode.CAMERA_NO_PREVIEW,
                                    "Cannot start preview", e);
                        }
                        return null;
                    }
                });
    }

    private void initQRCodeReader(final Activity activity) {
        if (mPaymentQRCodeReader != null) {
            return;
        }
        final QRCodeDetectorTaskGoogleVision qrCodeDetectorTask =
                new QRCodeDetectorTaskGoogleVision(activity);
        qrCodeDetectorTask.checkAvailability(new QRCodeDetectorTask.Callback() {
            @Override
            public void onResult(final boolean isAvailable) {
                if (isAvailable) {
                    mPaymentQRCodeReader = PaymentQRCodeReader.newInstance(qrCodeDetectorTask);
                    mPaymentQRCodeReader.setListener(CameraFragmentImpl.this);
                } else {
                    LOG.warn(
                            "QRCode detector dependencies are not yet available. QRCode detection is disabled.");
                }
            }

            @Override
            public void onInterrupted() {
                LOG.debug(
                        "Checking whether the QRCode detector task is operational was interrupted.");
            }
        });
    }

    @VisibleForTesting
    PaymentQRCodeReader getPaymentQRCodeReader() {
        return mPaymentQRCodeReader;
    }


    private void showUploadHintPopUpOnFirstExecution() {
        if (shouldShowHintPopUp()) {
            showUploadHintPopUp();
        }
    }

    @VisibleForTesting
    void showUploadHintPopUp() {
        mButtonCameraTrigger.setEnabled(false);
        mUploadHintContainer.setVisibility(View.VISIBLE);
        mUploadHintContainerArrow.setVisibility(View.VISIBLE);
        mCameraPreviewShade.setVisibility(View.VISIBLE);
        mCameraPreviewShade.setClickable(true);
        clearUploadHintPopUpAnimations();
        mUploadHintContainerAnimation = ViewCompat.animate(
                mUploadHintContainer)
                .alpha(1)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mUploadHintContainerAnimation.start();
        mUploadHintContainerArrowAnimation = ViewCompat.animate(
                mUploadHintContainerArrow)
                .alpha(1)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mUploadHintContainerArrowAnimation.start();
        mCameraPreviewShadeAnimation = ViewCompat.animate(
                mCameraPreviewShade)
                .alpha(1)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mCameraPreviewShadeAnimation.start();
    }

    private void clearUploadHintPopUpAnimations() {
        if (mUploadHintContainerAnimation != null) {
            mUploadHintContainerAnimation.cancel();
            mUploadHintContainer.clearAnimation();
            mUploadHintContainerAnimation.setListener(null);
        }
        if (mUploadHintContainerArrowAnimation != null) {
            mUploadHintContainerArrowAnimation.cancel();
            mUploadHintContainerArrow.clearAnimation();
            mUploadHintContainerArrowAnimation.setListener(null);
        }
        if (mCameraPreviewShadeAnimation != null) {
            mCameraPreviewShadeAnimation.cancel();
            mCameraPreviewShade.clearAnimation();
            mCameraPreviewShadeAnimation.setListener(null);
        }
    }

    private boolean shouldShowHintPopUp() {
        if (!isDocumentImportEnabled()) {
            return false;
        }
        final Context context = mFragment.getActivity();
        if (context != null) {
            final SharedPreferences gvSharedPrefs = context.getSharedPreferences(GV_SHARED_PREFS,
                    Context.MODE_PRIVATE);
            return gvSharedPrefs.getBoolean(SHOW_HINT_POP_UP, true);
        }
        return false;
    }

    private void startPreview(final SurfaceHolder holder) {
        mCameraController.startPreview(holder)
                .handle(new CompletableFuture.BiFun<Void, Throwable, Void>() {
                    @Override
                    public Void apply(final Void aVoid, final Throwable throwable) {
                        if (throwable != null) {
                            handleError(GiniVisionError.ErrorCode.CAMERA_NO_PREVIEW,
                                    "Cannot start preview", throwable);
                        }
                        return null;
                    }
                });
    }

    private void enableTapToFocus() {
        mCameraController.enableTapToFocus(mCameraPreview,
                new CameraInterface.TapToFocusListener() {
                    @Override
                    public void onFocusing(final Point point) {
                        showFocusIndicator(point);
                    }

                    @Override
                    public void onFocused(final boolean success) {
                        hideFocusIndicator();
                    }
                });
    }

    private void showFocusIndicator(final Point point) {
        final int top = Math.round((mLayoutRoot.getHeight() - mCameraPreview.getHeight()) / 2.0f);
        final int left = Math.round((mLayoutRoot.getWidth() - mCameraPreview.getWidth()) / 2.0f);
        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mCameraFocusIndicator.getLayoutParams();
        layoutParams.leftMargin = (int) Math.round(
                left + point.x - (mCameraFocusIndicator.getWidth() / 2.0));
        layoutParams.topMargin = (int) Math.round(
                top + point.y - (mCameraFocusIndicator.getHeight() / 2.0));
        mCameraFocusIndicator.setLayoutParams(layoutParams);
        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(1.0f);
    }

    private void hideFocusIndicator() {
        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(0.0f);
    }

    private CompletableFuture<Void> openCamera() {
        LOG.info("Opening camera");
        return mCameraController.open()
                .handle(new CompletableFuture.BiFun<Void, Throwable, Void>() {
                    @Override
                    public Void apply(final Void aVoid, final Throwable throwable) {
                        if (throwable != null) {
                            if (throwable instanceof CameraException) {
                                handleError(GiniVisionError.ErrorCode.CAMERA_OPEN_FAILED,
                                        "Failed to open camera", throwable);
                            } else if (throwable instanceof Exception) {
                                handleCameraException((Exception) throwable);
                            } else {
                                handleError(GiniVisionError.ErrorCode.CAMERA_OPEN_FAILED,
                                        "Failed to open camera", throwable);
                            }
                            throw new CameraException(throwable);
                        } else {
                            LOG.info("Camera opened");
                            hideNoPermissionView();
                        }
                        return null;
                    }
                });
    }

    private CompletableFuture<SurfaceHolder> handleSurfaceCreation() {
        return mSurfaceCreatedFuture.handle(
                new CompletableFuture.BiFun<SurfaceHolder, Throwable, SurfaceHolder>() {
                    @Override
                    public SurfaceHolder apply(final SurfaceHolder surfaceHolder,
                            final Throwable throwable) {
                        if (throwable != null) {
                            handleError(GiniVisionError.ErrorCode.CAMERA_NO_PREVIEW,
                                    "Cannot start preview", throwable);
                            throw new CameraException(throwable);
                        }
                        return surfaceHolder;
                    }
                });
    }

    private void handleCameraException(@NonNull final Exception e) {
        LOG.error("Failed to open camera", e);
        final GiniVisionError error = cameraExceptionToGiniVisionError(e);
        if (error.getErrorCode() == GiniVisionError.ErrorCode.CAMERA_NO_ACCESS) {
            showNoPermissionView();
        } else {
            mListener.onError(cameraExceptionToGiniVisionError(e));
        }
    }

    void onStop() {
        closeCamera();
        clearUploadHintPopUpAnimations();
        clearQRCodeDetectedPopUpAnimation();
    }

    private void closeCamera() {
        LOG.info("Closing camera");
        if (mPaymentQRCodeReader != null) {
            mPaymentQRCodeReader.release();
            mPaymentQRCodeReader = null; // NOPMD
        }
        mCameraController.disableTapToFocus(mCameraPreview);
        mCameraController.setPreviewCallback(null);
        mCameraController.stopPreview();
        mCameraController.close();
        LOG.info("Camera closed");
    }

    private void bindViews(final View view) {
        mLayoutRoot = view.findViewById(R.id.gv_root);
        mCameraPreview = view.findViewById(R.id.gv_camera_preview);
        mImageCorners = view.findViewById(R.id.gv_image_corners);
        mCameraFocusIndicator = view.findViewById(R.id.gv_camera_focus_indicator);
        mButtonCameraTrigger = view.findViewById(R.id.gv_button_camera_trigger);
        final ViewStub stubNoPermission = view.findViewById(R.id.gv_stub_camera_no_permission);
        mViewStubInflater = new ViewStubSafeInflater(stubNoPermission);
        mButtonImportDocument = view.findViewById(R.id.gv_button_import_document);
        mUploadHintContainer = view.findViewById(R.id.gv_document_import_hint_container);
        mUploadHintContainerArrow = view.findViewById(R.id.gv_document_import_hint_container_arrow);
        mUploadHintCloseButton = view.findViewById(R.id.gv_document_import_hint_close_button);
        mCameraPreviewShade = view.findViewById(R.id.gv_camera_preview_shade);
        mActivityIndicatorBackground =
                view.findViewById(R.id.gv_activity_indicator_background);
        mActivityIndicator = view.findViewById(R.id.gv_activity_indicator);
        mQRCodeDetectedPopupContainer = view.findViewById(
                R.id.gv_qrcode_detected_popup_container);
    }

    private void initViews() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (isDocumentImportEnabled() && FileChooserActivity.canChooseFiles(activity)) {
            mImportDocumentButtonEnabled = true;
            mButtonImportDocument.setVisibility(View.VISIBLE);
            showImportDocumentButtonAnimated();
        }
    }

    private boolean isDocumentImportEnabled() {
        return mGiniVisionFeatureConfiguration.getDocumentImportEnabledFileTypes()
                != DocumentImportEnabledFileTypes.NONE;
    }

    private void setInputHandlers() {
        mButtonCameraTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                LOG.info("Taking picture");
                if (!mCameraController.isPreviewRunning()) {
                    LOG.info("Will not take picture: preview must be running");
                    return;
                }
                if (mIsTakingPicture) {
                    LOG.info("Already taking a picture");
                    return;
                }
                mIsTakingPicture = true;
                mCameraController.takePicture()
                        .handle(new CompletableFuture.BiFun<Photo, Throwable, Void>() {
                            @Override
                            public Void apply(final Photo photo, final Throwable throwable) {
                                mUIExecutor.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mIsTakingPicture = false;
                                        callListener(photo, throwable);
                                    }
                                });
                                return null;
                            }
                        });
            }
        });
        mButtonImportDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                closeUploadHintPopUp();
                LOG.info("Requesting read storage permission");
                requestStoragePermission(new PermissionRequestListener() {
                    @Override
                    public void permissionGranted() {
                        LOG.info("Read storage permission granted");
                        showFileChooser();
                    }

                    @Override
                    public void permissionDenied() {
                        LOG.info("Read storage permission denied");
                        showStoragePermissionDeniedDialog();
                    }

                    @Override
                    public void shouldShowRequestPermissionRationale(
                            @NonNull final RationaleResponse response) {
                        LOG.info("Show read storage permission rationale");
                        showStoragePermissionRationale(response);
                    }
                });
            }
        });
        mUploadHintCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                closeUploadHintPopUp();
            }
        });
        mQRCodeDetectedPopupContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                hideQRCodeDetectedPopup(null);
                if (mPaymentQRCodeData != null) {
                    final QRCodeDocument qrCodeDocument = QRCodeDocument.fromPaymentQRCodeData(
                            mPaymentQRCodeData);
                    mListener.onQRCodeAvailable(qrCodeDocument);
                    mPaymentQRCodeData = null; // NOPMD
                }
            }
        });
    }

    private void closeUploadHintPopUp() {
        hideUploadHintPopUp(new ViewPropertyAnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final View view) {
                final Context context = view.getContext();
                savePopUpShown(context);
            }
        });
    }

    private void hideUploadHintPopUp(@Nullable final ViewPropertyAnimatorListenerAdapter
            animatorListener) {
        mButtonCameraTrigger.setEnabled(true);
        clearUploadHintPopUpAnimations();
        mUploadHintContainerAnimation = ViewCompat.animate(mUploadHintContainer)
                .alpha(0)
                .setDuration(DEFAULT_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final View view) {
                        mUploadHintContainerArrow.setVisibility(View.GONE);
                        mUploadHintContainer.setVisibility(View.GONE);
                        mCameraPreviewShade.setVisibility(View.GONE);
                        mCameraPreviewShade.setClickable(false);
                        if (animatorListener != null) {
                            animatorListener.onAnimationEnd(view);
                        }
                    }
                });
        mUploadHintContainerAnimation.start();
        mUploadHintContainerArrowAnimation = ViewCompat.animate(mUploadHintContainerArrow)
                .alpha(0)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mUploadHintContainerArrowAnimation.start();
        mCameraPreviewShadeAnimation = ViewCompat.animate(mCameraPreviewShade)
                .alpha(0)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mCameraPreviewShadeAnimation.start();
    }

    private void savePopUpShown(final Context context) {
        final SharedPreferences gvSharedPrefs = context.getSharedPreferences(GV_SHARED_PREFS,
                Context.MODE_PRIVATE);
        gvSharedPrefs.edit().putBoolean(SHOW_HINT_POP_UP, false).apply();
    }

    private void showStoragePermissionRationale(
            @NonNull final PermissionRequestListener.RationaleResponse response) {
        mFragment.showAlertDialog(R.string.gv_storage_permission_rationale,
                R.string.gv_storage_permission_rationale_positive_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface,
                            final int i) {
                        LOG.info("Requesting storage permission from rationale");
                        response.requestPermission();
                    }
                }, R.string.gv_storage_permission_denied_negative_button);
    }

    private void showStoragePermissionDeniedDialog() {
        mFragment.showAlertDialog(R.string.gv_storage_permission_denied,
                R.string.gv_storage_permission_denied_positive_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            final DialogInterface dialogInterface,
                            final int i) {
                        LOG.info("Open app details in Settings app");
                        showAppDetailsSettingsScreen();
                    }
                }, R.string.gv_storage_permission_rationale_negative_button);
    }

    private void showFileChooser() {
        LOG.info("Importing document");
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final Intent fileChooserIntent = FileChooserActivity.createIntent(activity);
        fileChooserIntent.putExtra(FileChooserActivity.EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES,
                mGiniVisionFeatureConfiguration.getDocumentImportEnabledFileTypes());
        mFragment.startActivityForResult(fileChooserIntent, REQ_CODE_CHOOSE_FILE);
    }

    private void showAppDetailsSettingsScreen() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        final Uri uri = Uri.fromParts("package",
                activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    private void requestStoragePermission(@NonNull final PermissionRequestListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFragment.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, listener);
        } else {
            listener.permissionGranted();
        }
    }

    boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQ_CODE_CHOOSE_FILE) {
            if (resultCode == RESULT_OK) {
                importDocumentFromIntent(data);
            } else if (resultCode != RESULT_CANCELED) {
                final String message;
                if (resultCode == FileChooserActivity.RESULT_ERROR) {
                    final GiniVisionError error = data.getParcelableExtra(
                            FileChooserActivity.EXTRA_OUT_ERROR);
                    message = "Document import failed: " + error.getMessage();
                } else {
                    message = "Document import failed: unknown result code " + resultCode;
                }
                LOG.error(message);
                showInvalidFileError(null);
            }
            return true;
        }
        return false;
    }

    private void importDocumentFromIntent(final Intent data) {
        final Activity activity = mFragment
                .getActivity();
        if (activity == null) {
            return;
        }
        final Uri uri = IntentHelper.getUri(data);
        if (uri == null) {
            LOG.error("Document import failed: Intent has no Uri");
            showInvalidFileError(null);
            return;
        }
        if (!UriHelper.isUriInputStreamAvailable(uri, activity)) {
            LOG.error("Document import failed: InputStream not available for the Uri");
            showInvalidFileError(null);
            return;
        }
        final FileImportValidator fileImportValidator = new FileImportValidator(activity);
        if (fileImportValidator.matchesCriteria(data, uri)) {
            createDocumentAndCallListener(data, activity);
        } else {
            showInvalidFileError(fileImportValidator.getError());
        }
    }

    private void createDocumentAndCallListener(final Intent data, final Activity activity) {
        try {
            showActivityIndicatorAndDisableInteraction();
            final GiniVisionDocument document = DocumentFactory.newDocumentFromIntent(data,
                    activity,
                    DeviceHelper.getDeviceOrientation(activity),
                    DeviceHelper.getDeviceType(activity),
                    "picker");
            LOG.info("Document imported: {}", document);
            LOG.debug("Requesting document check from client");
            mListener.onCheckImportedDocument(document,
                    new CameraFragmentListener.DocumentCheckResultCallback() {
                        @Override
                        public void documentAccepted() {
                            LOG.debug("Client accepted the document");
                            hideActivityIndicatorAndEnableInteraction();
                            mListener.onDocumentAvailable(document);
                        }

                        @Override
                        public void documentRejected(@NonNull final String messageForUser) {
                            LOG.debug("Client rejected the document: {}", messageForUser);
                            hideActivityIndicatorAndEnableInteraction();
                            showInvalidFileAlert(messageForUser);
                        }
                    });

        } catch (final IllegalArgumentException e) {
            LOG.error("Failed to import selected document", e);
            hideActivityIndicatorAndEnableInteraction();
            showInvalidFileError(null);
        }
    }

    @Override
    public void showActivityIndicatorAndDisableInteraction() {
        if (mActivityIndicator == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.VISIBLE);
        mActivityIndicatorBackground.setClickable(true);
        mActivityIndicator.setVisibility(View.VISIBLE);
        disableInteraction();
    }

    @Override
    public void hideActivityIndicatorAndEnableInteraction() {
        if (mActivityIndicator == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.INVISIBLE);
        mActivityIndicatorBackground.setClickable(false);
        mActivityIndicator.setVisibility(View.INVISIBLE);
        enableInteraction();
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        if (mFragment.getActivity() == null || mLayoutRoot == null) {
            return;
        }
        ErrorSnackbar.make(mFragment.getActivity(), mLayoutRoot, message, null, null,
                duration).show();
    }

    private void enableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(true);
        mButtonImportDocument.setEnabled(true);
        mButtonCameraTrigger.setEnabled(true);
    }

    private void disableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(false);
        mButtonImportDocument.setEnabled(false);
        mButtonCameraTrigger.setEnabled(false);
    }

    private void showInvalidFileError(@Nullable final FileImportValidator.Error error) {
        LOG.error("Invalid document {}", error != null ? error.toString() : "");
        final Activity activity = mFragment
                .getActivity();
        if (activity == null) {
            return;
        }
        int messageRes = R.string.gv_document_import_invalid_document;
        if (error != null) {
            messageRes = error.getTextResource();
        }
        showInvalidFileAlert(activity.getString(messageRes));
    }

    private void showInvalidFileAlert(final String message) {
        mFragment.showAlertDialog(message,
                R.string.gv_document_import_pick_another_document,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            final DialogInterface dialogInterface,
                            final int i) {
                        showFileChooser();
                    }
                }, R.string.gv_document_import_close_error);
    }

    @UiThread
    private void callListener(final Photo photo, final Throwable throwable) {
        if (throwable != null) {
            handleError(GiniVisionError.ErrorCode.CAMERA_SHOT_FAILED, "Failed to take picture",
                    throwable);
            mCameraController.startPreview();
        } else {
            if (photo != null) {
                LOG.info("Picture taken");
                mListener.onDocumentAvailable(DocumentFactory.newDocumentFromPhoto(photo));
            } else {
                handleError(GiniVisionError.ErrorCode.CAMERA_SHOT_FAILED,
                        "Failed to take picture: no picture from the camera", null);
                mCameraController.startPreview();
            }
        }
    }

    private void setSurfaceViewCallback() {
        mCameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(final SurfaceHolder holder) {
                LOG.debug("Surface created");
                mSurfaceCreatedFuture.complete(holder);
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, final int format,
                    final int width, final int height) {
                LOG.debug("Surface changed");
            }

            @Override
            public void surfaceDestroyed(final SurfaceHolder holder) {
                LOG.debug("Surface destroyed");
                mSurfaceCreatedFuture = new CompletableFuture<>();
            }
        });
    }

    @Deprecated
    @Override
    public void showDocumentCornerGuides() {
        if (isNoPermissionViewVisible()) {
            return;
        }
        showDocumentCornerGuidesAnimated();
    }

    private void showDocumentCornerGuidesAnimated() {
        mImageCorners.animate().alpha(1.0f);
    }

    @Deprecated
    @Override
    public void hideDocumentCornerGuides() {
        if (isNoPermissionViewVisible()) {
            return;
        }
        hideDocumentCornerGuidesAnimated();
    }

    private void hideDocumentCornerGuidesAnimated() {
        mImageCorners.animate().alpha(0.0f);
    }

    @Deprecated
    @Override
    public void showCameraTriggerButton() {
        if (isNoPermissionViewVisible()) {
            return;
        }
        showCameraTriggerButtonAnimated();
    }

    private void showCameraTriggerButtonAnimated() {
        mButtonCameraTrigger.animate().alpha(1.0f);
        mButtonCameraTrigger.setEnabled(true);
    }

    @Deprecated
    @Override
    public void hideCameraTriggerButton() {
        if (isNoPermissionViewVisible()) {
            return;
        }
        hideCameraTriggerButtonAnimated();
    }

    private void hideCameraTriggerButtonAnimated() {
        mButtonCameraTrigger.animate().alpha(0.0f);
        mButtonCameraTrigger.setEnabled(false);
    }

    @Override
    public void showInterface() {
        if (!mInterfaceHidden || isNoPermissionViewVisible()) {
            return;
        }
        mInterfaceHidden = false;
        showInterfaceAnimated();
    }

    private void showInterfaceAnimated() {
        showCameraTriggerButtonAnimated();
        showDocumentCornerGuidesAnimated();
        if (mImportDocumentButtonEnabled) {
            showUploadHintPopUpOnFirstExecution();
            showImportDocumentButtonAnimated();
        }
    }

    private void showImportDocumentButtonAnimated() {
        mButtonImportDocument.animate().alpha(1.0f);
        mButtonImportDocument.setEnabled(true);
    }

    @Override
    public void hideInterface() {
        if (mInterfaceHidden || isNoPermissionViewVisible()) {
            return;
        }
        mInterfaceHidden = true;
        hideInterfaceAnimated();
    }

    private void hideInterfaceAnimated() {
        hideCameraTriggerButtonAnimated();
        hideDocumentCornerGuidesAnimated();
        if (mImportDocumentButtonEnabled) {
            hideUploadHintPopUp(null);
            hideImportDocumentButtonAnimated();
        }
    }

    private void hideImportDocumentButtonAnimated() {
        mButtonImportDocument.animate().alpha(0.0f);
        mButtonImportDocument.setEnabled(false);
    }

    private void showNoPermissionView() {
        hideCameraPreviewAnimated();
        hideInterfaceAnimated();
        inflateNoPermissionStub();
        setUpNoPermissionButton();
        if (mLayoutNoPermission != null) {
            mLayoutNoPermission.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNoPermissionViewVisible() {
        return mLayoutNoPermission != null
                && mLayoutNoPermission.getVisibility() == View.VISIBLE;
    }

    private void inflateNoPermissionStub() {
        if (mLayoutNoPermission == null) {
            LOG.debug("Inflating no permission view");
            mLayoutNoPermission = (LinearLayout) mViewStubInflater.inflate();
        }
    }

    private void hideNoPermissionView() {
        showCameraPreviewAnimated();
        showInterfaceAnimated();
        if (mLayoutNoPermission != null) {
            mLayoutNoPermission.setVisibility(View.GONE);
        }
    }

    private void setUpNoPermissionButton() {
        if (isMarshmallowOrLater()) {
            handleNoPermissionButtonClick();
        } else {
            hideNoPermissionButton();
        }
    }

    private void hideCameraPreviewAnimated() {
        mCameraPreview.animate().alpha(0.0f);
        mCameraPreview.setEnabled(false);
    }

    private void showCameraPreviewAnimated() {
        mCameraPreview.animate().alpha(1.0f);
        mCameraPreview.setEnabled(true);
    }

    private void handleNoPermissionButtonClick() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        final Button button = view.findViewById(R.id.gv_button_camera_no_permission);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startApplicationDetailsSettings();
            }
        });
    }

    private void hideNoPermissionButton() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        final Button button = (Button) view.findViewById(R.id.gv_button_camera_no_permission);
        button.setVisibility(View.GONE);
    }

    private void startApplicationDetailsSettings() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        LOG.debug("Starting Application Details");
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        final Uri uri = Uri.fromParts("package", getClientApplicationId(activity), null);
        intent.setData(uri);
        mFragment.startActivity(intent);
    }

    private void initCameraController(final Activity activity) {
        if (mCameraController == null) {
            LOG.debug("CameraController created");
            mCameraController = createCameraController(activity);
        }
        if (mGiniVisionFeatureConfiguration.isQRCodeScanningEnabled()) {
            final int rotation = mCameraController.getCameraRotation();
            mCameraController.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(final byte[] data, final Camera camera) {
                    if (mPaymentQRCodeReader == null) {
                        return;
                    }
                    mPaymentQRCodeReader.readFromImage(data, mCameraController.getPreviewSize(),
                            rotation);
                }
            });
        }
    }

    @NonNull
    protected CameraInterface createCameraController(final Activity activity) {
        return new CameraController(activity);
    }

    private void handleError(final GiniVisionError.ErrorCode errorCode,
            @NonNull final String message,
            @Nullable final Throwable throwable) {
        String errorMessage = message;
        if (throwable != null) {
            LOG.error(message, throwable);
            // Add error info to the message to help clients, if they don't have logging enabled
            errorMessage = errorMessage + ": " + throwable.getMessage();
        }
        handleError(errorCode, errorMessage);
    }

    private void handleError(final GiniVisionError.ErrorCode errorCode,
            @NonNull final String message) {
        handleError(new GiniVisionError(errorCode, message));
    }

    private void handleError(@NonNull final GiniVisionError error) {
        LOG.error(error.getMessage());
        mListener.onError(error);
    }
}
