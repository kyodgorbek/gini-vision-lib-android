package net.gini.android.vision.component.analysis.compat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import net.gini.android.vision.Document;
import net.gini.android.vision.GiniVisionError;
import net.gini.android.vision.analysis.AnalysisFragmentCompat;
import net.gini.android.vision.analysis.AnalysisFragmentListener;
import net.gini.android.vision.component.ExtractionsActivity;
import net.gini.android.vision.component.R;
import net.gini.android.vision.component.noresults.compat.NoResultsExampleAppCompatActivity;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * AppCompatActivity using the {@link AnalysisScreenHandlerAppCompat} to host the
 * {@link AnalysisFragmentCompat} and to start the {@link ExtractionsActivity} or the
 * {@link NoResultsExampleAppCompatActivity}.
 */
public class AnalysisExampleAppCompatActivity extends AppCompatActivity implements
        AnalysisFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";
    public static final String EXTRA_IN_ERROR_MESSAGE = "EXTRA_IN_ERROR_MESSAGE";
    private AnalysisScreenHandlerAppCompat mAnalysisScreenHandler;

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
        setContentView(R.layout.activity_analysis_compat);
        mAnalysisScreenHandler = new AnalysisScreenHandlerAppCompat(this);
        mAnalysisScreenHandler.onCreate(savedInstanceState);
    }

    public static Intent newInstance(final Document document,
            final String errorMessage, final Context context) {
        final Intent intent = new Intent(context, AnalysisExampleAppCompatActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, document);
        intent.putExtra(EXTRA_IN_ERROR_MESSAGE, errorMessage);
        return intent;
    }
}
