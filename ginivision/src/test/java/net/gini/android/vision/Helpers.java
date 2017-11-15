package net.gini.android.vision;

import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.internal.camera.photo.PhotoFactory;

import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by aszotyori on 14.11.17.
 */

public class Helpers {

    public static Document createDocument(byte[] jpeg, int orientation, String deviceOrientation,
            String deviceType, String source) {
        return DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(jpeg, orientation, deviceOrientation, deviceType, source));
    }

    public static byte[] getTestJpeg() throws IOException {
        return getTestJpeg("/invoice.jpg");
    }

    public static byte[] getTestJpeg(String resourcePath) throws IOException {
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

    private static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes;
        //noinspection TryFinallyCanBeTryWithResources - only for minSdkVersion 19 and above
        try {
            byte[] buffer = new byte[8192];
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }
            bytes = outputStream.toByteArray();
        } finally {
            outputStream.close();
        }
        return bytes;
    }

}
