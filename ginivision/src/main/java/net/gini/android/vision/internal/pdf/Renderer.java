package net.gini.android.vision.internal.pdf;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import net.gini.android.vision.internal.AsyncCallback;
import net.gini.android.vision.internal.util.Size;

/**
 * @exclude
 */
public interface Renderer {
    void toBitmap(@NonNull final Size targetSize,
            @NonNull final AsyncCallback<Bitmap> asyncCallback);

    void getPageCount(@NonNull final AsyncCallback<Integer> asyncCallback);

    int getPageCount();
}
