package net.gini.android.vision.help;

import static net.gini.android.vision.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import net.gini.android.vision.GiniVisionFeatureConfiguration;
import net.gini.android.vision.R;
import net.gini.android.vision.analysis.AnalysisActivity;
import net.gini.android.vision.camera.CameraActivity;
import net.gini.android.vision.noresults.NoResultsActivity;
import net.gini.android.vision.review.ReviewActivity;

/**
 * <h3>Screen API and Component API</h3>
 *
 * <p>
 *     On the Help Screen users can get information about how to best use the Gini Vision Library.
 * </p>
 * <p>
 *     This Activity can be used for both Screen and Component APIs.
 * </p>
 *
 * <p>
 *     For the Component API you need to pass in the following extra:
 * <ul>
 *     <li>{@link HelpActivity#EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION} - Must contain a {@link GiniVisionFeatureConfiguration} instance</li>
 * </ul>
 * </p>
 *
 * <h3>Customizing the Help Screen</h3>
 *
 * <p>
 *     Customizing the look of the Help Screen is done via overriding of app resources.
 * </p>
 * <p>
 *     The following items are customizable:
 * <ul>
 *     <li>
 *         <b>Background color:</b> via the color resource named {@code gv_help_activity_background}
 *     </li>
 *     <li>
 *         <b>Help list item background color:</b> via the color resource name {@code gv_help_item_background}
 *     </li>
 *     <li>
 *         <b>Help list item text style:</b> via overriding the style named {@code
 *         GiniVisionTheme.Help.Item.TextStyle}
 *     </li>
 *     <li>
 *         <b>Help list item labels:</b> via overriding the string resources found in the {@link HelpItem} enum
 *     </li>
 * </ul>
 * </p>
 *
 * <p>
 *     <b>Important:</b> All overriden styles must have their respective {@code Root.} prefixed style as their parent. Ex.: the parent of {@code GiniVisionTheme.Onboarding.Message.TextStyle} must be {@code Root.GiniVisionTheme.Onboarding.Message.TextStyle}.
 * </p>
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * <p>
 * Customizing the Action Bar is done via overriding of app resources and each one - except the
 * title string resource - is global to all Activities ({@link CameraActivity}, {@link
 * NoResultsActivity}, {@link HelpActivity}, {@link ReviewActivity}, {@link AnalysisActivity}).
 * </p>
 * <p>
 * The following items are customizable:
 * <ul>
 * <li>
 * <b>Background color:</b> via the color resource named {@code gv_action_bar} (highly recommended
 * for Android 5+: customize the status bar color via {@code gv_status_bar})
 * </li>
 * <li>
 * <b>Title:</b> via the string resource name {@code gv_title_help}
 * </li>
 * <li>
 * <b>Title color:</b> via the color resource named {@code gv_action_bar_title}
 * </li>
 * </ul>
 * </p>
 */
public class HelpActivity extends AppCompatActivity {

    /**
     * <p>
     *     Mandatory extra which must contain a {@link GiniVisionFeatureConfiguration} instance.
     * </p>
     */
    public static final String EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION =
            "GV_EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION";

    private static final int PHOTO_TIPS_REQUEST = 1;
    private GiniVisionFeatureConfiguration mGiniVisionFeatureConfiguration;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_TIPS_REQUEST
                && resultCode == PhotoTipsActivity.RESULT_SHOW_CAMERA_SCREEN) {
            finish();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gv_activity_help);
        readExtras();
        setUpHelpItems();
        forcePortraitOrientationOnPhones(this);
    }

    private void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGiniVisionFeatureConfiguration = extras.getParcelable(
                    EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION);
            if (mGiniVisionFeatureConfiguration == null) {
                throw new IllegalStateException(
                        "HelpActivity requires a GiniVisionFeatureConfiguration instance. "
                                + "Pass it in as an extra with the name HelpActivity.EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION.");
            }
        }
    }

    private void setUpHelpItems() {
        final RecyclerView recyclerView = findViewById(R.id.gv_help_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new HelpItemsAdapter(mGiniVisionFeatureConfiguration,
                new HelpItemsAdapter.HelpItemSelectedListener() {
                    @Override
                    public void onItemSelected(@NonNull final HelpItem helpItem) {
                        launchHelpScreen(helpItem);
                    }
                }));
    }

    private void launchHelpScreen(final HelpItem helpItem) {
        switch (helpItem) {
            case PHOTO_TIPS:
                launchPhotoTips();
                break;
            case FILE_IMPORT_GUIDE:
                launchFileImport();
                break;
            case SUPPORTED_FORMATS:
                launchSupportedFormats();
                break;
            default:
                throw new IllegalStateException("Unknown HelpItem: " + helpItem);
        }
    }

    private void launchPhotoTips() {
        final Intent intent = new Intent(this, PhotoTipsActivity.class);
        startActivityForResult(intent, PHOTO_TIPS_REQUEST);
    }

    private void launchFileImport() {
        final Intent intent = new Intent(this, FileImportActivity.class);
        startActivity(intent);
    }

    private void launchSupportedFormats() {
        final Intent intent = new Intent(this, SupportedFormatsActivity.class);
        intent.putExtra(SupportedFormatsActivity.EXTRA_IN_GINI_VISION_FEATURE_CONFIGURATION,
                mGiniVisionFeatureConfiguration);
        startActivity(intent);
    }
}
