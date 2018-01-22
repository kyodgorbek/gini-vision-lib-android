package net.gini.android.vision.camera;

import android.support.annotation.NonNull;

import net.gini.android.vision.Document;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.document.QRCodeDocument;

/**
 * <p>
 * Interface used by {@link CameraFragmentStandard} and {@link CameraFragmentCompat} to dispatch events to the hosting Activity.
 * </p>
 */
public interface CameraFragmentListener {
    /**
     * <p>
     * Called when the user has taken an image with the camera or has imported a document that passed the Gini Vision Library's validation and any custom checks that were implemented.
     * </p>
     *
     * @param document the image taken by the camera or the validated imported document
     */
    void onDocumentAvailable(@NonNull Document document);

    /**
     * <p>
     *     Called when the user clicked the QR Code detected popup.
     *     You should upload the {@link QRCodeDocument}'s data to the Gini API to get the extractions,
     *     close the Gini Vision Library and continue to your app's transfer form.
     * </p>
     * <p>
     *      See {@link QRCodeDocument} for supported formats.
     * </p>
     *
     * @param qrCodeDocument contains payment data from a QR Code
     */
    void onQRCodeAvailable(@NonNull QRCodeDocument qrCodeDocument);

    /**
     * <p>
     *     This method is invoked for imported documents to allow custom validations.
     * </p>
     * <p>
     *     Invoke one of the {@link DocumentCheckResultCallback} methods on the main thread to inform the Gini Vision Library about the result.
     * </p>
     * <p>
     *     <b>Note:</b> The Gini Vision Library will wait until one of the {@link DocumentCheckResultCallback} methods are invoked.
     * </p>
     * @param document a {@link Document} created from the file the user picked
     * @param callback use this callback to inform the Gini Vision Library about the result of the custom checks
     */
    void onCheckImportedDocument(@NonNull Document document,
            @NonNull DocumentCheckResultCallback callback);

    /**
     * <p>
     * Called when an error occurred.
     * </p>
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniVisionError error);

    /**
     * <p>
     *     Callback to inform the Gini Vision Library about the outcome of the custom imported document checks.
     * </p>
     */
    interface DocumentCheckResultCallback {
        /**
         * <p>
         *     Call if the document was accepted and should be analysed.
         * </p>
         * <p>
         *     <b>Note:</b> Always call this method on the main thread.
         * </p>
         */
        void documentAccepted();

        /**
         * <p>
         *     Call if the document doesn't conform to your expectations and pass in a message to be shown to the user.
         * </p>
         * <p>
         *     <b>Note:</b> Always call this method on the main thread.
         * </p>
         *
         * @param messageForUser a message informing the user why the selected file was rejected
         */
        void documentRejected(@NonNull String messageForUser);
    }
}
