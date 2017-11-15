package net.gini.android.vision.internal.camera.photo;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.Helpers.doParcelingRoundTrip;
import static net.gini.android.vision.testutils.InstrumentationHelpers.getTestJpegAsset;
import static net.gini.android.vision.testutils.PhotoSubject.photo;

import android.support.test.runner.AndroidJUnit4;

import net.gini.android.vision.document.DocumentFactory;
import net.gini.android.vision.document.ImageDocument;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class PhotoTest {

    private static byte[] TEST_JPEG = null;

    @BeforeClass
    public static void setupClass() throws IOException {
        TEST_JPEG = getTestJpegAsset();
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        TEST_JPEG = null;
    }

    @Test
    public void should_supportParceling() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        MutablePhoto photoFromParcel = doParcelingRoundTrip(photo, MutablePhoto.CREATOR);
        // Then
        assertThat(photoFromParcel).isEqualTo(photo);
    }

    @Test
    public void should_keepUserComment_whenCreating_fromDocument() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        MutablePhoto fromDocument =
                (MutablePhoto) PhotoFactory.newPhotoFromDocument(
                        (ImageDocument) DocumentFactory.newDocumentFromPhoto(photo));
        // Then
        assertAbout(photo()).that(photo).hasSameUserCommentAs(fromDocument);
    }

    @Test
    public void should_setContentIdFromUserComment_whenCreating_fromDocument() {
        // Given
        MutablePhoto photo = (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        MutablePhoto fromDocument =
                (MutablePhoto) PhotoFactory.newPhotoFromDocument(
                        (ImageDocument) DocumentFactory.newDocumentFromPhoto(photo));
        // Then
        assertThat(photo.getContentId()).isEqualTo(fromDocument.getContentId());
    }

    @Test
    public void should_setRotationDeltafromUserComment_whenCreating_fromDocument() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        photo.edit().rotateTo(90).apply();
        MutablePhoto fromDocument =
                (MutablePhoto) PhotoFactory.newPhotoFromDocument(
                        (ImageDocument) DocumentFactory.newDocumentFromPhoto(photo));
        // Then
        assertThat(photo.getRotationDelta()).isEqualTo(fromDocument.getRotationDelta());
    }

    @Test
    public void should_generateUUID_forContentId_whenCreated() {
        // When
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // Then
        assertThat(UUID.fromString(photo.getContentId())).isNotNull();
    }

    @Test
    public void should_generate_uniqueContentIds_forEachInstance() {
        // Given
        MutablePhoto photo1 =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        MutablePhoto photo2 =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // Then
        assertThat(photo1.getContentId()).isNotEqualTo(photo2.getContentId());
    }

    @Test
    public void should_addContentId_toExifUserComment() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // Then
        assertAbout(photo()).that(photo).hasContentIdInUserComment(photo.getContentId());
    }

    @Test
    public void should_keepContentId_afterRotation() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        String contentId = photo.getContentId();
        // When
        photo.edit().rotateTo(90).apply();
        // Then
        assertAbout(photo()).that(photo).hasContentIdInUserComment(contentId);
    }

    @Test
    public void should_keepContentId_afterCompression() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        String contentId = photo.getContentId();
        // When
        photo.edit().compressBy(10).apply();
        // Then
        assertAbout(photo()).that(photo).hasContentIdInUserComment(contentId);
    }

    @Test
    public void should_initRotationDelta_whenCreated() {
        // When
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // Then
        assertThat(photo.getRotationDelta()).isEqualTo(0);
    }

    @Test
    public void should_initRotationDelta_whenCreated_withNonZeroOrientation() {
        // When
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 90, "portrait", "photo", "camera");
        // Then
        assertThat(photo.getRotationDelta()).isEqualTo(0);
    }

    @Test
    public void should_addRotationDelta_toExifUserComment() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(0);
    }

    @Test
    public void should_updateRotationDelta_afterCWRotation() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        photo.edit().rotateTo(90).apply();
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(90);
    }

    @Test
    public void should_updateRotationDelta_afterCCWRotation() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        photo.edit().rotateTo(-90).apply();
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(270);
    }

    @Test
    public void should_normalizeRotationDelta_forCWRotation() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        photo.edit().rotateTo(450).apply();
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(90);
    }

    @Test
    public void should_normalizeRotationDelta_forCCWRotation() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "photo", "camera");
        // When
        photo.edit().rotateTo(-270).apply();
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(90);
    }

    @Test
    public void should_keepRotationDelta_afterCompression() {
        // Given
        MutablePhoto photo =
                (MutablePhoto) PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 90, "portrait", "photo", "camera");
        // When
        photo.edit().compressBy(50).apply();
        // Then
        assertAbout(photo()).that(photo).hasRotationDeltaInUserComment(0);
    }
}