package net.gini.android.vision.testutils;

import static net.gini.android.vision.internal.util.StreamHelper.inputStreamToByteArray;

import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by aszotyori on 15.11.17.
 */

public class RobolectricHelpers {

    public static byte[] getTestJpegJavaResource() throws IOException {
        return getTestJpegJavaResource("/invoice.jpg");
    }

    public static byte[] getTestJpegJavaResource(String resourcePath) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = RuntimeEnvironment.application.getClass().getResourceAsStream(resourcePath);
            return inputStreamToByteArray(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
