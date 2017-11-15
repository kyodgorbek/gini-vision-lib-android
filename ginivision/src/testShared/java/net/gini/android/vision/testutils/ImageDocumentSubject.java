package net.gini.android.vision.testutils;

import android.support.annotation.Nullable;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import net.gini.android.vision.document.ImageDocument;
import net.gini.android.vision.internal.camera.photo.JpegByteArraySubject;

public class ImageDocumentSubject extends Subject<ImageDocumentSubject, ImageDocument> {

    private final JpegByteArraySubject mJpegByteArraySubject;

    public static SubjectFactory<ImageDocumentSubject, ImageDocument> imageDocument() {
        return new SubjectFactory<ImageDocumentSubject, ImageDocument>() {

            @Override
            public ImageDocumentSubject getSubject(final FailureStrategy fs, final ImageDocument that) {
                return new ImageDocumentSubject(fs, that);
            }
        };
    }

    private ImageDocumentSubject(final FailureStrategy failureStrategy,
            @Nullable final ImageDocument subject) {
        super(failureStrategy, subject);
        isNotNull();
        //noinspection ConstantConditions
        mJpegByteArraySubject = new JpegByteArraySubject(failureStrategy, subject.getJpeg());
    }

    public void hasSameContentIdInUserCommentAs(ImageDocument other) {
        isNotNull();
        String verb = "has same User Comment ContentId";

        if (other == null) {
            fail(verb, (Object) null);
            return;
        }

        mJpegByteArraySubject.hasSameContentIdInUserCommentAs(other.getJpeg());
    }

    public void hasRotationDeltaInUserComment(final int rotationDelta) {
        isNotNull();
        mJpegByteArraySubject.hasRotationDeltaInUserComment(rotationDelta);
    }
}
