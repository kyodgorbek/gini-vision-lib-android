package net.gini.android.vision.testutils;

import android.support.annotation.Nullable;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import net.gini.android.vision.Document;

import java.util.Arrays;

public class DocumentSubject extends Subject<DocumentSubject, Document> {

    public static SubjectFactory<DocumentSubject, Document> document() {
        return new SubjectFactory<DocumentSubject, Document>() {

            @Override
            public DocumentSubject getSubject(final FailureStrategy fs, final Document that) {
                return new DocumentSubject(fs, that);
            }
        };
    }

    private DocumentSubject(final FailureStrategy failureStrategy,
            @Nullable final Document subject) {
        super(failureStrategy, subject);
        isNotNull();
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

        if (!Arrays.equals(document.getData(), other.getData())) {
            fail("is equal to Document " + other + " - contain different bitmaps");
        } else if (document.getRotationForDisplay() != other.getRotationForDisplay()) {
            fail("is equal to Document " + other + " - have different rotation");
        }
    }
}
