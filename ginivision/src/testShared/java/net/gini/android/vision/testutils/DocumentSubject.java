package net.gini.android.vision.testutils;

import static net.gini.android.vision.testutils.Helpers.isRobolectricTest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import net.gini.android.vision.Document;
import net.gini.android.vision.document.ImageDocument;
import net.gini.android.vision.document.PdfDocument;

import java.util.Arrays;

public class DocumentSubject extends Subject<DocumentSubject, Document> {

    private DocumentSubject(final FailureStrategy failureStrategy,
            @Nullable final Document subject) {
        super(failureStrategy, subject);
        isNotNull();
    }

    public static SubjectFactory<DocumentSubject, Document> document() {
        return new SubjectFactory<DocumentSubject, Document>() {

            @Override
            public DocumentSubject getSubject(final FailureStrategy fs, final Document that) {
                return new DocumentSubject(fs, that);
            }
        };
    }

    public void isEqualToDocument(Document other) {
        Document document = getSubject();
        if (document == null) {
            fail("is equal to another Document - subject is null");
            return;
        }
        if (other == null) {
            fail("is equal to another Document - comparing to null");
            return;
        }

        if (!areEqualDocuments(document, other)) {
            fail("is equal to Document " + other);
            return;
        }

        if (document instanceof ImageDocument) {
            if (!areEqualImageDocuments((ImageDocument) document, (ImageDocument) other)) {
                fail("is equal to Document " + other);
            }
        } else if (document instanceof PdfDocument) {
            if (!areEqualPdfDocuments((PdfDocument) document, (PdfDocument) other)) {
                fail("is equal to Document " + other);
            }
        }
    }

    private boolean areEqualDocuments(final Document document, final Document other) {
        return document.getType() == document.getType()
                && document.isImported() == document.isImported()
                && document.isReviewable() == document.isReviewable()
                && document.getIntent() == document.getIntent();
    }

    private boolean areEqualImageDocuments(final ImageDocument document,
            final ImageDocument other) {
        if (!isRobolectricTest()) {
            //noinspection ConstantConditions - null check done above
            Bitmap bitmap = BitmapFactory.decodeByteArray(document.getData(), 0,
                    document.getJpeg().length);
            //noinspection ConstantConditions - null check done above
            Bitmap otherBitmap = BitmapFactory.decodeByteArray(other.getData(), 0,
                    other.getJpeg().length);
            if (!bitmap.sameAs(otherBitmap)) {
                return false;
            }
        }
        return document.getFormat() == other.getFormat()
                && document.getDeviceOrientation().equals(other.getDeviceOrientation())
                && document.getDeviceType().equals(other.getDeviceType())
                && document.getImportMethod().equals(other.getImportMethod())
                && document.getSource().equals(other.getSource())
                && document.getRotationForDisplay() == other.getRotationForDisplay();
    }

    private boolean areEqualPdfDocuments(final PdfDocument document, final PdfDocument other) {
        return Arrays.equals(document.getData(), other.getData())
                && document.getUri().compareTo(other.getUri()) == 0;
    }
}
