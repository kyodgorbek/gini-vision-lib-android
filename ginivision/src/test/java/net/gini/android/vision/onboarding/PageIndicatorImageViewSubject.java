package net.gini.android.vision.onboarding;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import java.io.ByteArrayOutputStream;

class PageIndicatorImageViewSubject extends
        Subject<PageIndicatorImageViewSubject, ImageView> {

    static SubjectFactory<PageIndicatorImageViewSubject, ImageView> pageIndicatorImageView() {
        return new SubjectFactory<PageIndicatorImageViewSubject, ImageView>() {
            @Override
            public PageIndicatorImageViewSubject getSubject(
                    final FailureStrategy fs, final ImageView that) {
                return new PageIndicatorImageViewSubject(fs, that);
            }
        };
    }

    private PageIndicatorImageViewSubject(final FailureStrategy failureStrategy,
            @Nullable final ImageView subject) {
        super(failureStrategy, subject);
    }

    void showsDrawable(@DrawableRes final int drawableResId) {
        final ImageView imageView = getSubject();

        final BitmapDrawable expectedDrawable =
                (BitmapDrawable) imageView.getResources().getDrawable(
                        drawableResId);
        if (expectedDrawable == null || expectedDrawable.getBitmap() == null) {
            fail("shows drawable with id " + drawableResId + " - no such drawable");
        }
        // NullPointerException warning is not relevant, fail() above will prevent it
        //noinspection ConstantConditions
        final Bitmap expectedBitmap = expectedDrawable.getBitmap();

        final BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        final Bitmap bitmap = bitmapDrawable.getBitmap();

        final ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, jpegOutputStream);

        final ByteArrayOutputStream expectedJpegOutputStream = new ByteArrayOutputStream();
        expectedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, expectedJpegOutputStream);

        final byte[] bytes = jpegOutputStream.toByteArray();
        final byte[] expectedBytes = expectedJpegOutputStream.toByteArray();

        if (bytes.length != expectedBytes.length) {
            fail("shows drawable with id " + drawableResId);
            return;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != expectedBytes[i]) {
                fail("shows drawable with id " + drawableResId);
                return;
            }
        }
    }
}
