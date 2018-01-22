package net.gini.android.vision.util;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.vision.testutils.InstrumentationHelpers.copyAssetToStorage;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.FileProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

/**
 * Created by Alpar Szotyori on 28.11.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

@RunWith(AndroidJUnit4.class)
public class UriHelperTest {

    private static final String TEST_FILE = "invoice.jpg";
    private static File sFileProviderDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpFileProvider();
    }

    private static void setUpFileProvider() throws IOException {
        sFileProviderDir = getFileProviderDir();
        if (!sFileProviderDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            sFileProviderDir.mkdirs();
        }
        copyAssetToStorage(TEST_FILE, sFileProviderDir.getPath());
    }

    @NonNull
    private static File getFileProviderDir() {
        final Context context = InstrumentationRegistry.getTargetContext();
        return new File(context.getFilesDir(), "file-provider");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        tearDownFileProvider();
    }

    private static void tearDownFileProvider() throws IOException {
        final File testFile = new File(sFileProviderDir, TEST_FILE);
        //noinspection ResultOfMethodCallIgnored
        testFile.delete();
    }

    @Test
    public void should_getFileSize_forContentUri() {
        // Given
        final Uri contentUri = getTestFileContentUri();
        final int expectedSize = getTestFileSize();
        // When
        final int size = UriHelper.getFileSizeFromUri(contentUri,
                InstrumentationRegistry.getTargetContext());
        // Then
        assertThat(size).isEqualTo(expectedSize);
    }

    private int getTestFileSize() {
        return (int) new File(sFileProviderDir, TEST_FILE).length();
    }

    private Uri getTestFileContentUri() {
        final File file = new File(sFileProviderDir, TEST_FILE);
        return FileProvider.getUriForFile(InstrumentationRegistry.getTargetContext(),
                "net.gini.android.vision.test.fileprovider", file);
    }

    @Test
    public void should_getFileSize_forFileUri() {
        // Given
        final Uri fileUri = getTestFileFileUri();
        final int expectedSize = getTestFileSize();
        // When
        final int size = UriHelper.getFileSizeFromUri(fileUri,
                InstrumentationRegistry.getTargetContext());
        // Then
        assertThat(size).isEqualTo(expectedSize);
    }

    private Uri getTestFileFileUri() {
        final File file = new File(sFileProviderDir, TEST_FILE);
        return Uri.parse(file.getPath());
    }

    @Test
    public void should_getFilename_forContentUri() {
        // Given
        final Uri contentUri = getTestFileContentUri();
        // When
        final String filename = UriHelper.getFilenameFromUri(contentUri,
                InstrumentationRegistry.getTargetContext());
        // Then
        assertThat(filename).isEqualTo(TEST_FILE);
    }

    @Test
    public void should_getFilename_forFileUri() {
        // Given
        final Uri fileUri = getTestFileFileUri();
        // When
        final String filename = UriHelper.getFilenameFromUri(fileUri,
                InstrumentationRegistry.getTargetContext());
        // Then
        assertThat(filename).isEqualTo(TEST_FILE);
    }

}