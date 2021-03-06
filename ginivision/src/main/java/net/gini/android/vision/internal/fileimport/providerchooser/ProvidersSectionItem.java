package net.gini.android.vision.internal.fileimport.providerchooser;

import android.support.annotation.NonNull;

/**
 * @exclude
 */
public class ProvidersSectionItem extends ProvidersItem {

    private final String mSectionTitle;

    public ProvidersSectionItem(@NonNull final String sectionTitle) {
        super(FileProviderItemType.SECTION);
        mSectionTitle = sectionTitle;
    }

    @NonNull
    String getSectionTitle() {
        return mSectionTitle;
    }
}
