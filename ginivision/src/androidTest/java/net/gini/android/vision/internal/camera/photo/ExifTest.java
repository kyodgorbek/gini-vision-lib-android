package net.gini.android.vision.internal.camera.photo;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.InstrumentationHelpers.getTestJpegAsset;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExifTest {

    @Test
    public void should_handleStringTags_containingNullBytes() throws Exception {
        // Given
        // Test jpeg make and model tags contain null bytes
        final byte[] testJpeg = getTestJpegAsset("exif-string-tag-with-null-bytes.jpeg");
        final Exif.RequiredTags requiredTags = Exif.readRequiredTags(testJpeg);

        // When
        final Exif exif = Exif.builder(testJpeg).setRequiredTags(requiredTags).build();

        // Then
        final byte[] outJpeg = exif.writeToJpeg(testJpeg);
        final Exif.RequiredTags outRequiredTags = Exif.readRequiredTags(outJpeg);

        assertThat((String[]) outRequiredTags.make.getValue()).asList().contains("Lenovo");
        assertThat((String[]) outRequiredTags.model.getValue()).asList().contains(
                "Lenovo TAB 2 A10-70F");
    }
}