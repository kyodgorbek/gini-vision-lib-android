package net.gini.android.vision.testutils;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import net.gini.android.vision.Document;
import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.internal.camera.photo.PhotoFactory;

public class Helpers {

    public static <T extends Parcelable, C extends Parcelable.Creator<T>> T doParcelingRoundTrip(
            T payload, C creator) {
        Parcel parcel = Parcel.obtain();
        payload.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }

    public static Document createDocument(byte[] jpeg, int orientation, String deviceOrientation,
            String deviceType, String source) {
        return DocumentFactory.newDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(jpeg, orientation, deviceOrientation, deviceType, source));
    }

    public static boolean isRobolectricTest() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

}
