package net.gini.android.vision.onboarding;

import android.support.annotation.VisibleForTesting;

import net.gini.android.vision.R;

import java.util.ArrayList;

/**
 * @exclude
 */
public enum DefaultPagesTablet {
    LIGHTING(
            new OnboardingPage(R.string.gv_onboarding_lighting, R.drawable.gv_onboarding_lighting)),
    FLAT(new OnboardingPage(R.string.gv_onboarding_flat, R.drawable.gv_onboarding_flat)),
    PARALLEL(
            new OnboardingPage(R.string.gv_onboarding_parallel, R.drawable.gv_onboarding_parallel)),
    ALIGN(new OnboardingPage(R.string.gv_onboarding_align, R.drawable.gv_onboarding_align,
            false, true));

    private final OnboardingPage mOnboardingPage;

    DefaultPagesTablet(final OnboardingPage onboardingPage) {
        mOnboardingPage = onboardingPage;
    }

    @VisibleForTesting
    OnboardingPage getPage() {
        return mOnboardingPage;
    }

    public static ArrayList<OnboardingPage> asArrayList() { // NOPMD - ArrayList required (Bundle)
        final ArrayList<OnboardingPage> arrayList = new ArrayList<>(values().length);
        for (final DefaultPagesTablet pages : values()) {
            arrayList.add(pages.getPage());
        }
        return arrayList;
    }
}
