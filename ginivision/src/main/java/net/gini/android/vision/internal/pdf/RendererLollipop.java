package net.gini.android.vision.internal.pdf;

import static net.gini.android.vision.internal.pdf.Pdf.DEFAULT_PREVIEW_HEIGHT;
import static net.gini.android.vision.internal.pdf.Pdf.DEFAULT_PREVIEW_WIDTH;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import net.gini.android.vision.internal.AsyncCallback;
import net.gini.android.vision.internal.util.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class is not thread safe due to the underlying {@link PdfRenderer}.
 *
 * @exclude
 */
@RequiresApi(21)
class RendererLollipop implements Renderer {

    private static final Logger LOG = LoggerFactory.getLogger(RendererLollipop.class);

    private final Uri mUri;
    private final Context mContext;

    RendererLollipop(@NonNull final Uri uri, @NonNull final Context context) {
        mUri = uri;
        mContext = context;
    }

    @Nullable
    private synchronized Bitmap toBitmap(@NonNull final Size targetSize) {
        Bitmap bitmap = null;
        final PdfRenderer pdfRenderer = getPdfRenderer();
        if (pdfRenderer == null) {
            return null;
        }
        if (pdfRenderer.getPageCount() > 0) {
            final PdfRenderer.Page page = pdfRenderer.openPage(0);
            final Size optimalSize = calculateOptimalRenderingSize(page, targetSize);
            bitmap = createWhiteBitmap(optimalSize);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
        }
        pdfRenderer.close();
        return bitmap;
    }

    @Nullable
    private PdfRenderer getPdfRenderer() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        ParcelFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = contentResolver.openFileDescriptor(mUri, "r");
        } catch (final FileNotFoundException e) {
            LOG.error("Pdf not found", e);
        }
        if (fileDescriptor == null) {
            return null;
        }
        try {
            return new PdfRenderer(fileDescriptor);
        } catch (final IOException e) {
            LOG.error("Could not read pdf", e);
        }
        return null;
    }

    @Override
    public void toBitmap(@NonNull final Size targetSize,
            @NonNull final AsyncCallback<Bitmap> asyncCallback) {
        final RenderAsyncTask asyncTask = new RenderAsyncTask(this,
                targetSize,
                new AsyncCallback<Bitmap>() {
                    @Override
                    public void onSuccess(final Bitmap result) {
                        asyncCallback.onSuccess(result);
                    }

                    @Override
                    public void onError(final Exception exception) {
                        asyncCallback.onError(exception);
                    }
                });
        asyncTask.execute();
    }

    @Override
    public void getPageCount(@NonNull final AsyncCallback<Integer> asyncCallback) {
        final PageCountAsyncTask asyncTask = new PageCountAsyncTask(this,
                new AsyncCallback<Integer>() {
                    @Override
                    public void onSuccess(final Integer result) {
                        asyncCallback.onSuccess(result);
                    }

                    @Override
                    public void onError(final Exception exception) {
                        asyncCallback.onError(exception);
                    }
                });
        asyncTask.execute();
    }

    @Override
    public synchronized int getPageCount() {
        final PdfRenderer pdfRenderer = getPdfRenderer();
        if (pdfRenderer == null) {
            return 0;
        }
        final int pageCount = pdfRenderer.getPageCount();
        pdfRenderer.close();
        return pageCount;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    private static Size calculateOptimalRenderingSize(@NonNull final PdfRenderer.Page page,
            @NonNull final Size previewSize) {
        final Size newPreviewSize = getDefaultPreviewSizeIfEmpty(previewSize);
        final float pageRatio = (float) page.getWidth() / (float) page.getHeight();
        final float previewRatio = (float) newPreviewSize.width / (float) newPreviewSize.height;
        if (pageRatio < previewRatio) {
            // The PDF page is taller than wide, or at least more so than the preview => fit the
            // height of the pdf page
            // to the preview and resize the width according to the pdf page's aspect ratio
            final int height = newPreviewSize.height;
            final int width = (int) ((float) height * pageRatio);
            return new Size(width, height);
        } else {
            // The PDF page is wider than tall, or at least more so than the preview => fit the
            // width of the pdf page
            // to the preview and resize the height according to the pdf page's aspect ratio
            final int width = newPreviewSize.width;
            final int height = (int) ((float) width / pageRatio);
            return new Size(width, height);
        }
    }

    @NonNull
    private static Bitmap createWhiteBitmap(@NonNull final Size renderingSize) {
        final Bitmap bitmap = Bitmap.createBitmap(renderingSize.width, renderingSize.height,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        return bitmap;
    }

    @NonNull
    private static Size getDefaultPreviewSizeIfEmpty(@NonNull final Size size) {
        if (size.width == 0 || size.height == 0) {
            return new Size(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);
        }
        return size;
    }

    private static class RenderAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private final RendererLollipop mRendererLollipop;
        private final Size mTargetSize;
        private final AsyncCallback<Bitmap> mCallback;

        private RenderAsyncTask(final RendererLollipop rendererLollipop,
                final Size targetSize,
                final AsyncCallback<Bitmap> callback) {
            mRendererLollipop = rendererLollipop;
            mTargetSize = targetSize;
            mCallback = callback;
        }

        @Override
        protected Bitmap doInBackground(final Void... voids) {
            return mRendererLollipop.toBitmap(mTargetSize);
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mCallback.onSuccess(bitmap);
        }
    }

    private static class PageCountAsyncTask extends AsyncTask<Void, Void, Integer> {

        private final RendererLollipop mRendererLollipop;
        private final AsyncCallback<Integer> mCallback;

        private PageCountAsyncTask(final RendererLollipop rendererLollipop,
                final AsyncCallback<Integer> callback) {
            mRendererLollipop = rendererLollipop;
            mCallback = callback;
        }

        @Override
        protected Integer doInBackground(final Void... voids) {
            return mRendererLollipop.getPageCount();
        }

        @Override
        protected void onPostExecute(final Integer pageCount) {
            super.onPostExecute(pageCount);
            mCallback.onSuccess(pageCount);
        }
    }
}
