package net.gini.android.vision.camera.api;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.View;

import net.gini.android.vision.camera.photo.Size;
import net.gini.android.vision.util.promise.SimplePromise;

/**
 * <p>
 *     An interface which defines an API for the camera used with the Gini Vision Library.
 * </p>
 * <p>
 *     We use this interface with the deprecated Camera API and the new Camera2 API to publish a common API for the required
 *     camera features.
 * </p>
 * @exclude
 */
public interface CameraInterface {
    /**
     * <p>
     *     Opens the first back-facing camera.
     * </p>
     * @return a {@link SimplePromise}{@code [done: null, fail: String|Exception]}
     */
    @NonNull
    SimplePromise open();

    /**
     * <p>
     *     Closes the camera.
     * </p>
     */
    void close();

    /**
     * <p>
     *     Query the camera's state.
     * </p>
     * @return {@code true}, if the camera is open
     */
    boolean isOpen();

    /**
     * <p>
     *     Starts the preview using the given {@link SurfaceHolder}.
     * </p>
     * <p>
     *     <b>Note</b>: the {@link android.view.SurfaceView} must have been created when starting the preview.
     * </p>
     * @param surfaceTexture the {@link SurfaceTexture} for the camera preview {@link android.view.TextureView}
     * @return a {@link SimplePromise}{@code [done: null, fail: String|Exception]}
     */
    @NonNull
    SimplePromise startPreview(@NonNull SurfaceTexture surfaceTexture);

    /**
     * Stops the camera preview.
     */
    void stopPreview();

    /**
     * <p>
     *     Query the preview's state.
     * </p>
     * @return {@code true}, if the preview is running
     */
    boolean isPreviewRunning();

    /**
     * <p>
     *     Enables tap-to-focus using the given view by adding touch handling to it and transforming the touch point coordinates
     *     to the camera sensor's coordinate system.
     * </p>
     * <p>
     *     <b>Note</b>: the view should have the same size as the camera preview and be above it. You could also set the
     *     camera preview {@link android.view.SurfaceView} directly as the tap view..
     * </p>
     * @param tapView the view used to handle taps
     */
    void enableTapToFocus(@NonNull View tapView, @Nullable TapToFocusListener listener);

    /**
     * Disables tap-to-focus.
     * @param tapView the view set with {@link CameraInterface#enableTapToFocus(View, TapToFocusListener)} to handle taps
     */
    void disableTapToFocus(@NonNull View tapView);

    /**
     * <p>
     *     Start a focus run.
     * </p>
     * @return a {@link SimplePromise}{@code [done: boolean, fail: null]}
     */
    @NonNull
    SimplePromise focus();

    /**
     * <p>
     *     Take a picture with the camera.
     * </p>
     * @return a {@link SimplePromise}{@code [done: Photo, fail: String]}
     */
    @NonNull
    SimplePromise takePicture();

    /**
     * <p>
     *     The selected preview size for the camera. It is the largest preview size which has an aspect ratio of 4:3.
     * </p>
     * @return preview size
     */
    @NonNull
    Size getPreviewSize();

    /**
     *<p>
     *     The selected picture size for the camera. It is the largest picture size which has an aspect ratio of 4:3.
     *</p>
     * @return picture size
     */
    @NonNull
    Size getPictureSize();

    /**
     * <p>
     *     Listener for handling tap-to-focus events.
     * </p>
     */
    interface TapToFocusListener {
        /**
         * <p>
         *     Called when the focusing starts on the tapped position.
         * </p>
         * @param point the coordinates of the tap in the tap view's coordinate system
         */
        void onFocusing(Point point);

        /**
         * <p>
         *     Called after the focusing has finished.
         * </p>
         * @param success {@code true} if focusing succeeded, otherwise {@code false}
         */
        void onFocused(boolean success);
    }

    enum CameraError {
        CAMERA_NO_ACCESS,
        CAMERA_OPEN_FAILED,
        CAMERA_NO_PREVIEW,
        CAMERA_SHOT_FAILED,
        CAMERA_UNKNOWN
    }
}
