package net.gini.android.vision.camera.api;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.camera.photo.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * @exclude
 */
public final class Util {

    private static final String CAMERA_EXCEPTION_MESSAGE_NO_ACCESS = "Fail to connect to camera service";

    @Nullable
    public static Size getLargestFourThreeRatioSize(@NonNull List<Size> sizes) {
        Size bestFit = null;
        for (Size size : sizes) {
            if (Math.abs((float) size.width / (float) size.height - 4.f / 3.f) < 0.001 &&
                    (bestFit == null || bestFit.width * bestFit.height < size.width * size.height)) {
                bestFit = size;
            }
        }
        return bestFit != null ? new Size(bestFit.width, bestFit.height) : null;
    }

    @NonNull
    public static List<Size> convertCameraSizes(@NonNull List<Camera.Size> sizes) {
        List<Size> output = new ArrayList<>(sizes.size());
        for (Camera.Size size : sizes) {
            output.add(new Size(size.width, size.height));
        }
        return output;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    public static List<Size> convertAndroidUtilSizes(@NonNull android.util.Size[] sizes) {
        List<Size> output = new ArrayList<>(sizes.length);
        for (android.util.Size size : sizes) {
            output.add(new Size(size.getWidth(), size.getHeight()));
        }
        return output;
    }

    @NonNull
    public static GiniVisionError cameraExceptionToGiniVisionError(@NonNull Exception exception) {
        if (exception instanceof SecurityException) {
            return new GiniVisionError(GiniVisionError.ErrorCode.CAMERA_NO_ACCESS, "");
        }

        // String comparison is the only way to determine the cause of the camera exception with the old Camera API
        // Here are the possible error messages:
        // https://android.googlesource.com/platform/frameworks/base/+/marshmallow-release/core/java/android/hardware/Camera.java#415
        String message = exception.getMessage();
        message = message == null ? "" : message;
        if (message.equals(CAMERA_EXCEPTION_MESSAGE_NO_ACCESS)) {
            return new GiniVisionError(GiniVisionError.ErrorCode.CAMERA_NO_ACCESS, message);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (exception instanceof CameraAccessException) {
                CameraAccessException cameraAccessException = (CameraAccessException) exception;
                if (cameraAccessException.getReason() == CameraAccessException.CAMERA_DISABLED) {
                    return new GiniVisionError(GiniVisionError.ErrorCode.CAMERA_NO_ACCESS, message);
                }
            }
        }

        return new GiniVisionError(GiniVisionError.ErrorCode.CAMERA_UNKNOWN, message);
    }

    /**
     * <p>
     * Converts the tap's coordinates in the view to the coordinates used by the camera sensor.
     * </p>
     * <p>
     * The camera sensor's coordinates are (0,0) in the center, (-1000,-1000) in the top left and
     * (1000,1000) in the lower right:
     * <pre>
     * (-1000,-1000)-----|-----(1000,-1000)
     * |                 |                |
     * |                 |                |
     * ----------------(0,0)---------------
     * |                 |                |
     * |                 |                |
     * (-1000,1000)------|------(1000,1000)
     * </pre>
     * The sensor's coordinates are not adapted to the display orientation, that means that in our case where
     * we always show the camera preview in portrait, the coordinates are simply turned 90 degrees clockwise
     * (for most devices, for others the calculated rect is rotated):
     * <pre>
     * (-1000,1000)---|---(-1000,-1000)
     * |              |               |
     * |              |               |
     * |              |               |
     * |      A       |       B       |
     * |              |               |
     * |              |               |
     * |              |               |
     * |              |               |
     * |              |               |
     * -------------(0,0)--------------
     * |              |               |
     * |              |               |
     * |              |               |
     * |      C       |       D       |
     * |              |               |
     * |              |               |
     * |              |               |
     * |              |               |
     * |              |               |
     * (1000,1000)----|----(1000,-1000)
     * </pre>
     * </p>
     * <p>
     * For easier conversion, we divided the view area into four parts (A, B, C, D) and do the conversion for each one
     * separately.
     * </p>
     * <p>
     * Calculations are made with the assumption of a 90 degree
     * camera orientation. The real camera's orientation is normalized by subtracting 90 degrees and then the
     * calculated
     * rect is rotated by the normalized degrees.
     * </p>
     *
     * @param x             tap's X position in the view
     * @param y             tap's Y position in the view
     * @param orientation   camera's orientation, see {@link Camera.CameraInfo#orientation}
     * @param tapViewWidth  the width of the tappable view
     * @param tapViewHeight the height of the tappable view
     */
    public static Rect calculateTapAreaForCameraAPI(float x, float y, int orientation, int tapViewWidth, int tapViewHeight) {
        Rect rect = new Rect(0, 0, 0, 0);
        if (x < tapViewWidth / 2.f && y < tapViewHeight / 2.f) {
            // A: x: -1000 .. 0; y: 1000 .. 0
            rect.left = -(1000 - (int) (1000 * (y / (tapViewHeight / 2.f))));
            rect.top = 1000 - (int) (1000 * (x / (tapViewWidth / 2.f)));
        } else if (x < tapViewWidth / 2.f && y >= tapViewHeight / 2.f) {
            // C: x: 0 .. 1000; y: 1000 .. 0
            y = y - tapViewHeight / 2.f;
            rect.left = (int) (1000 * (y / (tapViewHeight / 2.f)));
            rect.top = 1000 - (int) (1000 * (x / (tapViewWidth / 2.f)));
        } else if (x >= tapViewWidth / 2.f && y < tapViewHeight / 2.f) {
            // B: x: -1000 .. 0; y: 0 .. -1000
            x = x - tapViewWidth / 2.f;
            rect.left = -(1000 - (int) (1000 * (y / (tapViewHeight / 2.f))));
            rect.top = -(int) (1000 * (x / (tapViewWidth / 2.f)));
        } else if (x >= tapViewWidth / 2.f && y >= tapViewHeight / 2.f) {
            // D: x: 0 .. 1000; y: 0 .. -1000
            x = x - tapViewWidth / 2.f;
            y = y - tapViewHeight / 2.f;
            rect.left = (int) (1000 * (y / (tapViewHeight / 2.f)));
            rect.top = -(int) (1000 * (x / (tapViewWidth / 2.f)));
        }
        // Give a size to the rect
        rect.bottom = rect.top + 5;
        rect.right = rect.left + 5;
        // Rotate the rect according to the camera's orientation
        // Tap area was calculated for a camera with a 90 degrees orientation
        // so we have to normalize the rotation taking that into account
        int rectRotation = orientation - 90;
        RectF rectF = new RectF(rect);
        Matrix matrix = new Matrix();
        matrix.setRotate(rectRotation);
        matrix.mapRect(rectF);
        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        return rect;
    }

    public static Rect calculateTapAreaForCamera2API(float x, float y, int orientation, int tapViewWidth, int tapViewHeight, Rect sensorArrayRect) {
        Rect rect = new Rect(0, 0, 0, 0);

        // Percentual position in the view
        float xPercent = x / tapViewWidth;
        // Transfer the position to the sensor coordinates
        rect.left = (int) (sensorArrayRect.width() * xPercent);

        // Percentual position in the view
        float yPercent = y / tapViewHeight;
        // Transfer the position to the sensor coordinates
        rect.top = (int) (sensorArrayRect.height() * yPercent);

        // Give a size to the rect
        rect.bottom = rect.top + 200;
        rect.right = rect.left + 200;

        RectF rectF = new RectF(rect);
        Matrix matrix = new Matrix();
        matrix.mapRect(rectF);
        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);

        return rect;
    }

    private Util() {
    }
}