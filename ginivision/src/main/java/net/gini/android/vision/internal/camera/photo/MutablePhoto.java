package net.gini.android.vision.internal.camera.photo;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import net.gini.android.vision.document.ImageDocument;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * @exclude
 */
class MutablePhoto extends ImmutablePhoto implements Parcelable {

    private static final Logger LOG = LoggerFactory.getLogger(MutablePhoto.class);

    private Exif.RequiredTags mRequiredTags;
    private String mContentId = "";
    private int mRotationDelta;
    private String mDeviceOrientation;
    private String mDeviceType;
    private String mSource;
    private String mImportMethod;
    private ImageDocument mImageDocument;

    MutablePhoto(@NonNull final byte[] data, final int orientation,
            @NonNull final String deviceOrientation,
            @NonNull final String deviceType,
            @NonNull final String source,
            @NonNull final String importMethod,
            @NonNull final ImageDocument.ImageFormat format, final boolean isImported) {
        super(data, orientation, format, isImported);
        mContentId = generateUUID();
        mDeviceOrientation = deviceOrientation;
        mDeviceType = deviceType;
        mSource = source;
        mImportMethod = importMethod;
        readRequiredTags();
        updateExif(); // NOPMD
    }

    MutablePhoto(@NonNull final ImageDocument document) {
        super(document);
        mImageDocument = document;
        initFieldsFromExif();
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private void initFieldsFromExif() {
        final byte[] data = getData();
        if (data == null) {
            return;
        }

        readRequiredTags();

        ExifReader exifReader = null;
        String userComment = "";
        try {
            exifReader = ExifReader.forJpeg(data);
            userComment = exifReader.getUserComment();
        } catch (final ExifReaderException e) {
            LOG.warn("Could not read exif User Comment", e);
        }
        initContentId(userComment);
        initRotationDelta(userComment);
        initDeviceOrientation(userComment);
        initDeviceType(userComment);
        initSource(userComment);
        initImportMethod(userComment);
        initRotationForDisplay(exifReader);
    }

    private void initContentId(final String userComment) {
        mContentId = ExifReader.getValueForKeyFromUserComment(Exif.USER_COMMENT_CONTENT_ID,
                userComment);
        if (mContentId == null) {
            mContentId = generateUUID();
        }
    }

    private void initRotationDelta(final String userComment) {
        final String rotationDelta = ExifReader.getValueForKeyFromUserComment(
                Exif.USER_COMMENT_ROTATION_DELTA,
                userComment);
        if (rotationDelta != null) {
            try {
                mRotationDelta = Integer.parseInt(rotationDelta);
            } catch (final NumberFormatException e) {
                LOG.error("Could not set rotation delta from exif User Comment", e);
            }
        }
    }

    private void initDeviceOrientation(final String userComment) {
        mDeviceOrientation =
                ExifReader.getValueForKeyFromUserComment(Exif.USER_COMMENT_DEVICE_ORIENTATION,
                        userComment);
        if (mDeviceOrientation == null && mImageDocument != null) {
            mDeviceOrientation = mImageDocument.getDeviceOrientation();
        }
    }

    private void initDeviceType(final String userComment) {
        mDeviceType =
                ExifReader.getValueForKeyFromUserComment(Exif.USER_COMMENT_DEVICE_TYPE,
                        userComment);
        if (mDeviceType == null && mImageDocument != null) {
            mDeviceType = mImageDocument.getDeviceType();
        }
    }

    private void initSource(final String userComment) {
        mSource =
                ExifReader.getValueForKeyFromUserComment(Exif.USER_COMMENT_SOURCE,
                        userComment);
        if (mSource == null && mImageDocument != null) {
            mSource = mImageDocument.getSource();
        }
    }

    private void initImportMethod(final String userComment) {
        mImportMethod =
                ExifReader.getValueForKeyFromUserComment(Exif.USER_COMMENT_IMPORT_METHOD,
                        userComment);
        if (mImportMethod == null && mImageDocument != null) {
            mImportMethod = mImageDocument.getImportMethod();
        }
    }

    private void initRotationForDisplay(@Nullable final ExifReader exifReader) {
        if (mImageDocument != null && mImageDocument.isImported()
                && exifReader != null) {
            mRotationForDisplay = exifReader.getOrientationAsDegrees();
        }
    }

    @Override
    public synchronized void updateBitmapPreview() {
        super.mBitmapPreview = createPreview();
    }

    @Override
    public synchronized int getRotationForDisplay() {
        return super.mRotationForDisplay;
    }

    @Override
    public synchronized void setRotationForDisplay(final int degrees) {
        // Converts input degrees to degrees between [0,360)
        super.mRotationForDisplay = ((degrees % 360) + 360) % 360;
    }

    @Override
    public synchronized void updateRotationDeltaBy(final int degrees) {
        // Converts input degrees to degrees between [0,360)
        mRotationDelta = ((mRotationDelta + degrees % 360) + 360) % 360;
    }

    @Override
    public void setData(final byte[] data) {
        super.mData = data;
    }

    @VisibleForTesting
    synchronized int getRotationDelta() {
        return mRotationDelta;
    }

    @Override
    public String getDeviceOrientation() {
        return mDeviceOrientation;
    }

    @Override
    public String getDeviceType() {
        return mDeviceType;
    }

    @Override
    public String getSource() {
        return mSource;
    }

    @Override
    public String getImportMethod() {
        return mImportMethod;
    }

    @VisibleForTesting
    @NonNull
    synchronized String getContentId() {
        return mContentId;
    }

    private synchronized void readRequiredTags() {
        final byte[] data = getData();
        if (data == null) {
            return;
        }
        try {
            mRequiredTags = Exif.readRequiredTags(data);
        } catch (IOException | ImageReadException e) {
            LOG.error("Could not read exif tags", e);
        }
    }

    @Override
    public synchronized void updateExif() {
        final byte[] data = getData();
        if (data == null) {
            return;
        }
        try {
            boolean addMake = false;
            boolean addModel = false;

            final Exif.Builder exifBuilder = Exif.builder(data);

            if (mRequiredTags != null) {
                exifBuilder.setRequiredTags(mRequiredTags);
                addMake = mRequiredTags.make == null;
                addModel = mRequiredTags.model == null;
            }

            final Exif.UserCommentBuilder builder = Exif.userCommentBuilder();
            builder.setAddMake(addMake)
                    .setAddModel(addModel)
                    .setContentId(mContentId)
                    .setRotationDelta(mRotationDelta)
                    .setDeviceType(mDeviceType)
                    .setDeviceOrientation(mDeviceOrientation)
                    .setSource(mSource);
            if (!TextUtils.isEmpty(mImportMethod)) {
                builder.setImportMethod(mImportMethod);
            }

            final String userComment = builder.build();
            exifBuilder.setUserComment(userComment)
                    .setOrientationFromDegrees(super.mRotationForDisplay);

            final byte[] jpeg = exifBuilder.build().writeToJpeg(data);
            setData(jpeg);
        } catch (ImageReadException | ImageWriteException | IOException e) {
            LOG.error("Could not add required exif tags", e);
        }
    }

    @Override
    public synchronized PhotoEdit edit() {
        return new PhotoEdit(this);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mContentId);
        dest.writeInt(mRotationDelta);
        dest.writeString(mDeviceOrientation);
        dest.writeString(mDeviceType);
        dest.writeString(mSource);
        dest.writeString(mImportMethod);
    }

    public static final Creator<MutablePhoto> CREATOR = new Creator<MutablePhoto>() {
        @Override
        public MutablePhoto createFromParcel(final Parcel in) {
            return new MutablePhoto(in);
        }

        @Override
        public MutablePhoto[] newArray(final int size) {
            return new MutablePhoto[size];
        }
    };

    private MutablePhoto(final Parcel in) {
        super(in);
        mContentId = in.readString();
        mRotationDelta = in.readInt();
        mDeviceOrientation = in.readString();
        mDeviceType = in.readString();
        mSource = in.readString();
        mImportMethod = in.readString();

        readRequiredTags();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final MutablePhoto that = (MutablePhoto) o;

        if (mRotationDelta != that.mRotationDelta) {
            return false;
        }
        if (mRequiredTags != null ? !mRequiredTags.equals(that.mRequiredTags)
                : that.mRequiredTags != null) {
            return false;
        }
        if (mContentId != null ? !mContentId.equals(that.mContentId) : that.mContentId != null) {
            return false;
        }
        if (mDeviceOrientation != null ? !mDeviceOrientation.equals(that.mDeviceOrientation)
                : that.mDeviceOrientation != null) {
            return false;
        }
        if (mDeviceType != null ? !mDeviceType.equals(that.mDeviceType)
                : that.mDeviceType != null) {
            return false;
        }
        if (mSource != null ? !mSource.equals(that.mSource) : that.mSource != null) {
            return false;
        }
        if (mImportMethod != null ? !mImportMethod.equals(that.mImportMethod)
                : that.mImportMethod != null) {
            return false;
        }
        return mImageDocument != null ? mImageDocument.equals(that.mImageDocument)
                : that.mImageDocument == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mRequiredTags != null ? mRequiredTags.hashCode() : 0);
        result = 31 * result + (mContentId != null ? mContentId.hashCode() : 0);
        result = 31 * result + mRotationDelta;
        result = 31 * result + (mDeviceOrientation != null ? mDeviceOrientation.hashCode() : 0);
        result = 31 * result + (mDeviceType != null ? mDeviceType.hashCode() : 0);
        result = 31 * result + (mSource != null ? mSource.hashCode() : 0);
        result = 31 * result + (mImportMethod != null ? mImportMethod.hashCode() : 0);
        result = 31 * result + (mImageDocument != null ? mImageDocument.hashCode() : 0);
        return result;
    }
}
