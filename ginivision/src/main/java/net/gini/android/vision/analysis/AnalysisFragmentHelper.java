package net.gini.android.vision.analysis;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.gini.android.vision.Document;
import net.gini.android.vision.internal.ui.FragmentImplCallback;

final class AnalysisFragmentHelper {

    private static final String ARGS_DOCUMENT = "GV_ARGS_DOCUMENT";
    private static final String ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE =
            "GV_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE";

    public static Bundle createArguments(@NonNull final Document document,
            @Nullable final String documentAnalysisErrorMessage) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_DOCUMENT, document);
        if (documentAnalysisErrorMessage != null) {
            arguments.putString(ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE, documentAnalysisErrorMessage);
        }
        return arguments;
    }

    static AnalysisFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Bundle arguments) {
        final Document document = arguments.getParcelable(ARGS_DOCUMENT);
        if (document != null) {
            final String analysisErrorMessage = arguments.getString(
                    ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE);
            return new AnalysisFragmentImpl(fragment, document, analysisErrorMessage);
        } else {
            throw new IllegalStateException(
                    "AnalysisFragmentCompat and AnalysisFragmentStandard require a Document. Use the createInstance() method of these classes for instantiating.");
        }
    }

    public static void setListener(@NonNull final AnalysisFragmentImpl fragmentImpl,
            @NonNull final Context context) {
        if (context instanceof AnalysisFragmentListener) {
            fragmentImpl.setListener((AnalysisFragmentListener) context);
        } else {
            throw new IllegalStateException(
                    "Hosting activity must implement AnalysisFragmentListener.");
        }
    }

    private AnalysisFragmentHelper() {
    }
}
