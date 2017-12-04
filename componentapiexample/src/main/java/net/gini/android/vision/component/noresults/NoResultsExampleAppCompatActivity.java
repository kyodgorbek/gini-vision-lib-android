package net.gini.android.vision.component.noresults;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.gini.android.vision.Document;
import net.gini.android.vision.component.R;
import net.gini.android.vision.noresults.NoResultsFragmentListener;

public class NoResultsExampleAppCompatActivity extends AppCompatActivity implements
        NoResultsFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";
    private NoResultsScreenHandlerAppCompat mNoResultsScreenHandler;

    public static Intent newInstance(final Document document, final Context context) {
        final Intent intent = new Intent(context, NoResultsExampleAppCompatActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, document);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_results_compat);
        mNoResultsScreenHandler = new NoResultsScreenHandlerAppCompat(this);
        mNoResultsScreenHandler.onCreate(savedInstanceState);
    }

    @Override
    public void onBackToCameraPressed() {
        mNoResultsScreenHandler.onBackToCameraPressed();
    }
}
