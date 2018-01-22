package net.gini.android.vision.help;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.gini.android.vision.GiniVisionFeatureConfiguration;
import net.gini.android.vision.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @exclude
 */

class HelpItemsAdapter extends Adapter<HelpItemsAdapter.HelpItemsViewHolder> {

    private final HelpItemSelectedListener mItemSelectedListener;
    private final List<HelpItem> mItems;

    HelpItemsAdapter(@NonNull final GiniVisionFeatureConfiguration giniVisionFeatureConfiguration,
            @NonNull final HelpItemSelectedListener itemSelectedListener) {
        mItemSelectedListener = itemSelectedListener;
        mItems = setUpItems(giniVisionFeatureConfiguration);
    }

    @NonNull
    private List<HelpItem> setUpItems(
            @NonNull final GiniVisionFeatureConfiguration giniVisionFeatureConfiguration) {
        final ArrayList<HelpItem> items = new ArrayList<>();
        items.add(HelpItem.PHOTO_TIPS);
        if (giniVisionFeatureConfiguration.isFileImportEnabled()) {
            items.add(HelpItem.FILE_IMPORT_GUIDE);
        }
        items.add(HelpItem.SUPPORTED_FORMATS);
        return items;
    }

    @Override
    public HelpItemsViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gv_item_help,
                parent, false);
        return new HelpItemsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final HelpItemsViewHolder holder, final int position) {
        final HelpItem helpItem = mItems.get(position);
        holder.title.setText(helpItem.title);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final int actualPosition = holder.getAdapterPosition();
                mItemSelectedListener.onItemSelected(mItems.get(actualPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class HelpItemsViewHolder extends RecyclerView.ViewHolder {

        final TextView title;

        HelpItemsViewHolder(final View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.gv_help_item_title);
        }
    }

    interface HelpItemSelectedListener {
        void onItemSelected(@NonNull final HelpItem helpItem);
    }
}
