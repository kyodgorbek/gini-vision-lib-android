package net.gini.android.vision.onboarding;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

class ViewPagerAdapterCompat extends FragmentPagerAdapter {

    private final List<OnboardingPage> mPages;

    public ViewPagerAdapterCompat(@NonNull FragmentManager fm,
            @NonNull List<OnboardingPage> pages) {
        super(fm);
        mPages = pages;
    }

    @NonNull
    protected List<OnboardingPage> getPages() {
        return mPages;
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public Fragment getItem(int position) {
        return OnboardingPageFragmentCompat.createInstance(getPages().get(position));
    }
}
