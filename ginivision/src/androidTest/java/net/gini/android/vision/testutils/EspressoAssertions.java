package net.gini.android.vision.testutils;

import android.support.test.espresso.ViewAssertion;

public class EspressoAssertions {

    public static ViewAssertion hasSizeRatio(final float sizeRatio) {
        return new ViewSizeRatioAssertion(sizeRatio);
    }
}
