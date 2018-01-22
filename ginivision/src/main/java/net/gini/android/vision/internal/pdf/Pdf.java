package net.gini.android.vision.internal.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import net.gini.android.vision.document.PdfDocument;
import net.gini.android.vision.internal.AsyncCallback;
import net.gini.android.vision.internal.util.Size;

/**
 * @exclude
 */
public final class Pdf implements Parcelable {

    // Default preview size is set to be tolerably small and has a DIN A4 aspect ratio
    static final int DEFAULT_PREVIEW_HEIGHT = 1500;
    static final int DEFAULT_PREVIEW_WIDTH = 1080;

    private final Uri mUri;
    private Renderer mRenderer;

    public static Pdf fromDocument(@NonNull final PdfDocument document) {
        return new Pdf(document.getUri());
    }

    public static Pdf fromUri(@NonNull final Uri uri) {
        return new Pdf(uri);
    }

    private Pdf(@NonNull final Uri uri) {
        mUri = uri;
    }

    public void toBitmap(@NonNull final Size targetSize, @NonNull final Context context,
            @NonNull final AsyncCallback<Bitmap> asyncCallback) {
        getRenderer(context).toBitmap(targetSize, asyncCallback);
    }

    public void getPageCount(@NonNull final Context context,
            @NonNull final AsyncCallback<Integer> asyncCallback) {
        getRenderer(context).getPageCount(asyncCallback);
    }

    public int getPageCount(@NonNull final Context context) {
        return getRenderer(context).getPageCount();
    }

    private Renderer getRenderer(@NonNull final Context context) {
        if (mRenderer != null) {
            return mRenderer;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mRenderer = new RendererLollipop(mUri, context);
        } else {
            mRenderer = new RendererPreLollipop();
        }
        return mRenderer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(mUri, flags);
    }

    public static final Creator<Pdf> CREATOR = new Creator<Pdf>() {
        @Override
        public Pdf createFromParcel(final Parcel in) {
            return new Pdf(in);
        }

        @Override
        public Pdf[] newArray(final int size) {
            return new Pdf[size];
        }
    };

    private Pdf(final Parcel in) {
        mUri = in.readParcelable(Uri.class.getClassLoader());
    }
}
