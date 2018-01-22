package net.gini.android.vision.component.analysis.standard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import net.gini.android.vision.Document;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.analysis.AnalysisFragmentListener;
import net.gini.android.vision.analysis.AnalysisFragmentStandard;
import net.gini.android.vision.component.ExtractionsActivity;
import net.gini.android.vision.component.R;
import net.gini.android.vision.component.noresults.standard.NoResultsExampleActivity;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Standard Activity using the {@link AnalysisScreenHandler} to host the
 * {@link AnalysisFragmentStandard} and to start the {@link ExtractionsActivity} or the
 * {@link NoResultsExampleActivity}.
 */
public class AnalysisExampleActivity extends Activity implements
        AnalysisFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";
    public static final String EXTRA_IN_ERROR_MESSAGE = "EXTRA_IN_ERROR_MESSAGE";
    private AnalysisScreenHandler mAnalysisScreenHandler;

    @Override
    public void onAnalyzeDocument(@NonNull final Document document) {
        mAnalysisScreenHandler.onAnalyzeDocument(document);
    }

    @Override
    public void onError(@NonNull final GiniVisionError error) {
        mAnalysisScreenHandler.onError(error);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        mAnalysisScreenHandler = new AnalysisScreenHandler(this);
        mAnalysisScreenHandler.onCreate(savedInstanceState);
    }

    public static Intent newInstance(final Document document,
            final String errorMessage, final Context context) {
        final Intent intent = new Intent(context, AnalysisExampleActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, document);
        intent.putExtra(EXTRA_IN_ERROR_MESSAGE, errorMessage);
        return intent;
    }
}
