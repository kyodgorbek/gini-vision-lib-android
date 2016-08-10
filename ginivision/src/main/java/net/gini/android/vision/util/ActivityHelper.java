package net.gini.android.vision.util;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * @exclude
 */
public final class ActivityHelper {

    public static void enableHomeAsUp(AppCompatActivity activity) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static boolean handleMenuItemPressedForHomeButton(AppCompatActivity activity, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            activity.finish();
            return true;
        }
        return false;
    }

    private ActivityHelper() {
    }
}
