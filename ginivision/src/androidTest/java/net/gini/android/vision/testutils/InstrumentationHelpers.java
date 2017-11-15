package net.gini.android.vision.testutils;

import static net.gini.android.vision.internal.util.StreamHelper.inputStreamToByteArray;

import android.app.Instrumentation;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;

import net.gini.android.vision.internal.util.ContextHelper;
import net.gini.android.vision.test.BuildConfig;

import java.io.IOException;
import java.io.InputStream;

public class InstrumentationHelpers {

    public static void prepareLooper() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    public static byte[] getTestJpegAsset() throws IOException {
        return getTestJpegAsset("invoice.jpg");
    }

    public static byte[] getTestJpegAsset(String filename) throws IOException {
        AssetManager assetManager = InstrumentationRegistry.getTargetContext().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(filename);
            return inputStreamToByteArray(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static boolean isTablet() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        return ContextHelper.isTablet(instrumentation.getTargetContext());
    }

    public static void resetDeviceOrientation() throws RemoteException {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            uiDevice.setOrientationNatural();
            waitForWindowUpdate(uiDevice);
            uiDevice.unfreezeRotation();
        }
    }

    public static void waitForWindowUpdate(@NonNull final UiDevice uiDevice) {
        uiDevice.waitForWindowUpdate(BuildConfig.APPLICATION_ID, 5000);
    }

}
