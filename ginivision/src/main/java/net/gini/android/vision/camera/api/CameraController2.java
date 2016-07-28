package net.gini.android.vision.camera.api;

import static net.gini.android.vision.camera.api.Util.calculateTapAreaForCamera2API;
import static net.gini.android.vision.camera.api.Util.convertAndroidUtilSizes;
import static net.gini.android.vision.camera.api.Util.getLargestFourThreeRatioSize;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import net.gini.android.vision.camera.photo.Size;
import net.gini.android.vision.util.promise.SimpleDeferred;
import net.gini.android.vision.util.promise.SimplePromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @exclude
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraController2 implements CameraInterface {

    private static final Logger LOG = LoggerFactory.getLogger(CameraController2.class);

    private final Activity mActivity;
    private final CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private Size mPreviewSize = new Size(0, 0);
    private Size mPictureSize = new Size(0, 0);

    private Integer mCameraOrientation;
    private boolean mFlashSupported;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private AtomicBoolean mFocusing = new AtomicBoolean();
    private boolean mAutoFocusRegionsSupported = false;
    private Rect mSensorArrayRect;

    private final Handler mResetFocusHandler;
    private final UIExecutor mUIExecutor;// TODO: needed?

    private Runnable mResetFocusMode = new Runnable() {
        @Override
        public void run() {
            if (mCameraDevice == null) {
                return;
            }
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                // TODO: log
            }
        }
    };

    public CameraController2(Activity activity) {
        mActivity = activity;
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        mResetFocusHandler = new Handler();
        mUIExecutor = new UIExecutor();
    }

    @NonNull
    @Override
    public SimplePromise open() {
        final SimpleDeferred deferred = new SimpleDeferred();
        getBackFacingCameraId()
                .done(new SimplePromise.DoneCallback() {
                    @Nullable
                    @Override
                    public SimplePromise onDone(@Nullable Object result) {
                        String cameraId = (String) result;
                        if (cameraId != null) {
                            return openCamera(cameraId);
                        }
                        return null;
                    }
                })
                .done(new SimplePromise.DoneCallback() {
                    @Nullable
                    @Override
                    public SimplePromise onDone(@Nullable Object result) {
                        if (result instanceof CameraDevice) {
                            mCameraDevice = (CameraDevice) result;
                            configureCamera();
                            deferred.resolve(null);
                        }
                        return null;
                    }
                })
                .fail(new SimplePromise.FailCallback() {
                    @Nullable
                    @Override
                    public SimplePromise onFailed(@Nullable Object failure) {
                        deferred.reject(failure);
                        return null;
                    }
                });
        return deferred.promise();
    }

    /**
     *
     * @return a {@link SimplePromise}{@code [done: String, fail: String|CameraAccessException]}
     */
    private SimplePromise getBackFacingCameraId() {
        SimpleDeferred deferred = new SimpleDeferred();
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    deferred.resolve(cameraId);
                    return deferred.promise();
                }
            }
            deferred.reject("No back-facing camera found");
        } catch (CameraAccessException e) {
            deferred.reject(e);
        }
        return deferred.promise();
    }

    /**
     *
     * @param cameraId
     * @return a {@link SimplePromise}{@code [done: CameraDevice, fail: String|CameraAccessException|SecurityException]}
     */
    private SimplePromise openCamera(@NonNull String cameraId) {
        if (mCameraDevice != null) {
            return SimpleDeferred.resolvedPromise(mCameraDevice);
        }
        final SimpleDeferred deferred = new SimpleDeferred();
        try {
            //noinspection MissingPermission
            mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    deferred.resolve(camera);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    deferred.reject("Cannot open camera: camera disconnected");
                    camera.close();
                    close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    deferred.reject("Cannot open camera: " + stateCallbackErrorToString(error));
                    camera.close();
                    close();
                }
            }, null);
        } catch (CameraAccessException | SecurityException e) {
            deferred.reject(e);
        }
        return deferred.promise();
    }

    @NonNull
    private String stateCallbackErrorToString(int stateCallbackError) {
        switch (stateCallbackError) {
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                return "Camera encountered a fatal error";
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                return "Camera could not be opened due to a device policy";
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                return "Camera already in use";
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                return "Camera service encountered a fatal error";
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                return "Too many open cameras";
        }
        return "";
    }

    @Override
    public void close() {
        LOG.info("Closing camera");
        if (mCameraDevice == null) {
            LOG.debug("Camera already closed");
            LOG.info("Camera closed");
            return;
        }
        mCameraDevice.close();
        mCameraDevice = null;
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mPreviewRequestBuilder != null) {
            mPreviewRequestBuilder = null;
        }
        LOG.info("Camera closed");
    }

    @NonNull
    @Override
    public SimplePromise startPreview(@NonNull SurfaceTexture surfaceTexture) {
        if (mCameraDevice == null) {
            LOG.error("Cannot start preview: camera not open");
            return SimpleDeferred.rejectedPromise("Cannot start preview: camera not open");
        }
        final SimpleDeferred deferred = new SimpleDeferred();
        Surface surface = new Surface(surfaceTexture);
        try {
            createPreviewRequestBuilder(surfaceTexture, surface);
        } catch (CameraAccessException e) {
            deferred.reject(e);
        }
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCameraDevice == null) {
                                // TODO: log
                                return;
                            }
                            mCaptureSession = session;

                            try {
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
                                deferred.resolve(null);
                            } catch (CameraAccessException e) {
                                mPreviewRequestBuilder = null;
                                deferred.reject(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            session.close();
                            if (mCaptureSession != null) {
                                mCaptureSession.close();
                                mCaptureSession = null;
                            }
                            mPreviewRequestBuilder = null;
                        }
                    }, null);
        } catch (CameraAccessException e) {
            mPreviewRequestBuilder = null;
            deferred.reject(e);
        }
        return deferred.promise();
    }

    private void createPreviewRequestBuilder(@NonNull SurfaceTexture surfaceTexture, Surface surface) throws CameraAccessException {
        if (mPreviewRequestBuilder != null) {
            return;
        }
        surfaceTexture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height);

        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(surface);
        // Auto Focus
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // Flash
        if (mFlashSupported) {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        }
        // Orientation
        mPreviewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                getJPEGOrientation(mActivity));
    }

    @Override
    public void stopPreview() {
        if (mCaptureSession == null) {
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            // TODO: log
        }
    }

    @Override
    public void enableTapToFocus(@NonNull View tapView, @Nullable final TapToFocusListener listener) {
        tapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    final float x = event.getX();
                    final float y = event.getY();
                    LOG.debug("Handling tap to focus touch at point ({}, {})", x, y);
                    if (mCameraDevice == null) {
                        LOG.error("Cannot focus on tap: camera not open");
                        return false;
                    }
                    if (mFocusing.get()) {
                        LOG.debug("Already focusing");
                        return false;
                    }
                    if (!mAutoFocusRegionsSupported) {
                        LOG.warn("Focus areas not supported");
                        return false;
                    }

                    Rect focusRect = calculateTapAreaForCamera2API(x, y, mCameraOrientation, view.getWidth(), view.getHeight(), mSensorArrayRect);
                    LOG.debug("Focus rect calculated (l:{}, t:{}, r:{}, b:{})", focusRect.left, focusRect.top, focusRect.right, focusRect.bottom);

                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_START);

                    MeteringRectangle[] meteringRectangles = new MeteringRectangle[1];
                    meteringRectangles[0] = new MeteringRectangle(focusRect, MeteringRectangle.METERING_WEIGHT_MAX);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangles);
                    LOG.debug("Focus area set");

                    try {
                        mFocusing.set(true);
                        if (listener != null) {
                            listener.onFocusing(new Point(Math.round(x), Math.round(y)));
                        }
                        LOG.info("Focusing started");
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                boolean success = false;
                                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                                if (afState != null) {
                                    success = afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED;
                                }
                                if (listener != null) {
                                    listener.onFocused(success);
                                }
                                mResetFocusHandler.removeCallbacks(mResetFocusMode);
                                mResetFocusHandler.postDelayed(mResetFocusMode, 5000);
                            }

                            @Override
                            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                                if (listener != null) {
                                    listener.onFocused(false);
                                }
                                mResetFocusHandler.removeCallbacks(mResetFocusMode);
                                mResetFocusHandler.postDelayed(mResetFocusMode, 5000);
                            }
                        }, null);
                    } catch (Exception e) {
                        mFocusing.set(false);
                        LOG.error("Could not focus", e);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void disableTapToFocus(@NonNull View tapView) {
        tapView.setOnClickListener(null);
    }

    @NonNull
    @Override
    public SimplePromise focus() {
        return null;
    }

    @NonNull
    @Override
    public SimplePromise takePicture() {
        return null;
    }

    @NonNull
    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @NonNull
    @Override
    public Size getPictureSize() {
        return mPictureSize;
    }

    private void configureCamera() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCameraDevice.getId());
        } catch (CameraAccessException e) {
            // TODO: log
        }
        if (characteristics == null) {
            // TODO: log
            return;
        }

        mCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupported = available == null ? false : available;

        Integer maxAfRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        mAutoFocusRegionsSupported = maxAfRegions != null && maxAfRegions > 0;

        mSensorArrayRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (configurationMap == null) {
            // TODO: log
            return;
        }

        android.util.Size[] previewSizes = configurationMap.getOutputSizes(SurfaceHolder.class);
        if (previewSizes != null) {
            Size previewSize = getLargestFourThreeRatioSize(convertAndroidUtilSizes(previewSizes));
            if (previewSize != null) {
                mPreviewSize = previewSize;
            } else {
                // TODO: log
            }
        } else {
            // TODO: log
        }

        android.util.Size[] pictureSizes = configurationMap.getOutputSizes(ImageFormat.JPEG);
        if (pictureSizes != null) {
            Size pictureSize = getLargestFourThreeRatioSize(convertAndroidUtilSizes(pictureSizes));
            if (pictureSize != null) {
                mPictureSize = pictureSize;
            } else {
                // TODO: log
            }
        } else {
            // TODO: log
        }
    }

    private int getJPEGOrientation(Activity activity) {
        LOG.debug("Setting camera display orientation");
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        LOG.debug("Default display rotation {}", degrees);

        int result = (mCameraOrientation + degrees) % 360;
        result = (360 - result) % 360;  // compensate the mirror
        LOG.debug("JPEG orientation is {}", result);
        return result;
    }
}
