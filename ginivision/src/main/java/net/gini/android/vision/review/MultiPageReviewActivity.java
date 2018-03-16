package net.gini.android.vision.review;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.gini.android.vision.R;
import net.gini.android.vision.document.GiniVisionDocumentError;
import net.gini.android.vision.document.ImageDocument;
import net.gini.android.vision.document.ImageMultiPageDocument;
import net.gini.android.vision.internal.AsyncCallback;
import net.gini.android.vision.internal.camera.photo.Photo;
import net.gini.android.vision.internal.camera.photo.PhotoFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiPageReviewActivity extends AppCompatActivity {

    /**
     * @exclude
     */
    public static final String EXTRA_IN_DOCUMENT = "GV_EXTRA_IN_DOCUMENT";
    private ViewPager mImagesPager;
    private ImagesPagerChangeListener mImagesPagerChangeListener;
    private ImageMultiPageDocument mMultiPageDocument;
    private TextView mPageIndicator;
    private final Map<ImageDocument, Photo> mDocumentPhotoMap = new HashMap<>();
    private RelativeLayout mRootView;
    private RecyclerView mThumbnailsRV;
    private RecyclerView.SmoothScroller mThumbnailsScroller;
    private ImageButton mButtonNext;
    private ImageButton mDeleteButton;

    public static Intent createIntent(@NonNull final Context context,
            @NonNull final ImageMultiPageDocument multiPageDocument) {
        final Intent intent = new Intent(context, MultiPageReviewActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, multiPageDocument);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gv_activity_multi_page_review);

        readExtras();

        mButtonNext = findViewById(R.id.gv_button_next);
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                finish();
            }
        });

        mImagesPager = findViewById(R.id.gv_view_pager);

        mPageIndicator = findViewById(R.id.gv_page_indicator);
        mThumbnailsRV = findViewById(R.id.gv_thumbnails_panel);

        mThumbnailsScroller = new LinearSmoothScroller(this);

        mImagesPagerChangeListener = new ImagesPagerChangeListener(this,
                new ImagesPagerChangeListener.UpdateThumbnailsListener() {
                    @Override
                    public void setCurrentThumbnail(final int position) {
                        final ThumbnailsAdapter thumbnailsAdapter =
                                (ThumbnailsAdapter) mThumbnailsRV.getAdapter();
                        thumbnailsAdapter.highlightPosition(position);
                        thumbnailsAdapter.notifyDataSetChanged();
                        mThumbnailsScroller.setTargetPosition(position);
                        mThumbnailsRV.getLayoutManager().startSmoothScroll(mThumbnailsScroller);
                    }
                });
        mImagesPager.addOnPageChangeListener(mImagesPagerChangeListener);

        mRootView = findViewById(R.id.root_view);

        final ImageButton reorderButton = findViewById(R.id.gv_button_reorder);
        reorderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final boolean isSelected = !v.isSelected();
                v.setSelected(isSelected);

                TransitionManager.beginDelayedTransition(mRootView);
                final RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) mThumbnailsRV.getLayoutParams();
                if (isSelected) {
                    layoutParams.addRule(RelativeLayout.ALIGN_TOP, 0);
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.gv_toolbar);
                } else {
                    layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.gv_toolbar);
                    layoutParams.addRule(RelativeLayout.ABOVE, 0);
                }
                mThumbnailsRV.requestLayout();
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        mThumbnailsRV.setLayoutManager(layoutManager);

        findViewById(R.id.gv_button_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final int currentItem = mImagesPager.getCurrentItem();
                final ImageDocument document =
                        mMultiPageDocument.getDocuments().get(currentItem);
                final Photo photo = mDocumentPhotoMap.get(document);
                final int rotationStep = 90;
                final int degrees = photo.getRotationForDisplay() + rotationStep;
                final ImagesPagerAdapter imagesPagerAdapter =
                        (ImagesPagerAdapter) mImagesPager.getAdapter();
                final ThumbnailsAdapter thumbnailsAdapter =
                        (ThumbnailsAdapter) mThumbnailsRV.getAdapter();
                photo.setRotationForDisplay(degrees);
                imagesPagerAdapter.rotateImageInCurrentItemBy(mImagesPager, rotationStep);
                thumbnailsAdapter.rotateHighlightedThumbnailBy(rotationStep);
            }
        });

        mDeleteButton = findViewById(R.id.gv_button_delete);

        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final int deletedItem = mImagesPager.getCurrentItem();
                final List<ImageDocument> documents = mMultiPageDocument.getDocuments();
                documents.remove(deletedItem);
                mDocumentPhotoMap.remove(deletedItem);
                final int newPosition = getNewPositionAfterDeletion(deletedItem, documents.size());
                updatePageIndicator(newPosition);
                final ImagesPagerAdapter imagesPagerAdapter =
                        (ImagesPagerAdapter) mImagesPager.getAdapter();
                final ThumbnailsAdapter thumbnailsAdapter =
                        (ThumbnailsAdapter) mThumbnailsRV.getAdapter();
                imagesPagerAdapter.notifyDataSetChanged();
                thumbnailsAdapter.removeThumbnail(deletedItem);
                mThumbnailsScroller.setTargetPosition(newPosition);
                mThumbnailsRV.getLayoutManager().startSmoothScroll(mThumbnailsScroller);
                if (documents.size() == 1) {
                    mDeleteButton.setEnabled(false);
                    mDeleteButton.setAlpha(0.2f);
                }
            }
        });

        mMultiPageDocument.loadData(this, new AsyncCallback<byte[]>() {
            @Override
            public void onSuccess(final byte[] result) {
                for (final ImageDocument imageDocument : mMultiPageDocument.getDocuments()) {
                    mDocumentPhotoMap.put(imageDocument,
                            PhotoFactory.newPhotoFromDocument(imageDocument));
                }
                showPhotos();
            }

            @Override
            public void onError(final Exception exception) {

            }
        });
    }

    private void showPhotos() {
        final ImagesPagerAdapter imagesPagerAdapter = new ImagesPagerAdapter(
                getSupportFragmentManager(), mMultiPageDocument, mDocumentPhotoMap);

        final ThumbnailChangeListener thumbnailChangeListener = new ThumbnailChangeListener() {
            @Override
            public void onThumbnailMoved() {
                final PagerAdapter adapter = mImagesPager.getAdapter();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onThumbnailSelected(final int position) {
                mImagesPagerChangeListener.skipNextThumbnailsUpdate();
                mImagesPager.setCurrentItem(position);
            }
        };

        if (mMultiPageDocument.getDocuments().size() == 1) {
            mDeleteButton.setEnabled(false);
            mDeleteButton.setAlpha(0.2f);
        }

        mImagesPager.setAdapter(imagesPagerAdapter);
        final ThumbnailsAdapter thumbnailsAdapter = new ThumbnailsAdapter(mMultiPageDocument,
                mDocumentPhotoMap, thumbnailChangeListener);
        mThumbnailsRV.setAdapter(thumbnailsAdapter);

        final ItemTouchHelper.Callback callback =
                new ThumbnailsTouchHelperCallback(thumbnailsAdapter);
        final ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mThumbnailsRV);
        thumbnailsAdapter.setItemTouchHelper(touchHelper);

        mImagesPager.setCurrentItem(0);
        updatePageIndicator(0);
        thumbnailsAdapter.highlightPosition(0);
        mThumbnailsScroller.setTargetPosition(0);
        mThumbnailsRV.getLayoutManager().startSmoothScroll(mThumbnailsScroller);
    }

    private static int getNewPositionAfterDeletion(final int deletedPosition, final int newSize) {
        final int newPosition;
        if (deletedPosition == newSize) {
            // Last item was removed, highlight the new last item
            newPosition = deletedPosition - 1;
        } else {
            // Non-last item deletion moves the right neighbour to the same position
            newPosition = deletedPosition;
        }
        return newPosition;
    }

    private void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMultiPageDocument = extras.getParcelable(EXTRA_IN_DOCUMENT);
        }
        checkRequiredExtras();
    }

    private void checkRequiredExtras() {
        if (mMultiPageDocument == null) {
            throw new IllegalStateException(
                    "MultiPageReviewActivity requires a GiniVisionMultiPageDocument. Set it as an extra using the EXTRA_IN_DOCUMENT key.");
        }
    }

    private void updatePageIndicator(final int position) {
        mPageIndicator.setText(String.format("%d von %d", position + 1,
                mMultiPageDocument.getDocuments().size()));
    }

    public interface ThumbnailsTouchHelperListener {

        void onDragFinished();

        void onItemMove(final RecyclerView.ViewHolder viewHolder, final int fromPos,
                final RecyclerView.ViewHolder target, final int toPos);
    }

    private interface ThumbnailChangeListener {

        void onThumbnailMoved();

        void onThumbnailSelected(final int position);
    }

    private static class ImagesPagerAdapter extends FragmentStatePagerAdapter {

        private final ImageMultiPageDocument mMultiPageDocument;
        private final Map<ImageDocument, Photo> mDocumentPhotoMap;

        ImagesPagerAdapter(@NonNull final FragmentManager fm,
                @NonNull final ImageMultiPageDocument multiPageDocument,
                @NonNull final Map<ImageDocument, Photo> documentPhotoMap) {
            super(fm);
            mMultiPageDocument = multiPageDocument;
            mDocumentPhotoMap = documentPhotoMap;
        }

        @Override
        public int getCount() {
            return mMultiPageDocument.getDocuments().size();
        }

        @Override
        public int getItemPosition(final Object object) {
            // Required for reloading the visible fragment
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(final int position) {
            final ImageDocument document =
                    mMultiPageDocument.getDocuments().get(position);
            final Photo photo = mDocumentPhotoMap.get(document);
            final GiniVisionDocumentError documentError =
                    mMultiPageDocument.getErrorForDocument(document);
            String errorMessage = null;
            if (documentError != null) {
                errorMessage = documentError.getMessage();
            }
            return ImageFragment.createInstance(photo, errorMessage);
        }

        void rotateImageInCurrentItemBy(@NonNull final ViewPager viewPager, final int degrees) {
            final ImageFragment fragment = (ImageFragment) instantiateItem(viewPager,
                    viewPager.getCurrentItem());
            fragment.rotateImageViewBy(degrees, true);
        }
    }

    private static class ImagesPagerChangeListener implements ViewPager.OnPageChangeListener {

        private final MultiPageReviewActivity mActivity;
        private final UpdateThumbnailsListener mUpdateThumbnailsListener;
        private int mLastPosition = -1;
        private boolean mSkipNextThumbnailsUpdate;

        private ImagesPagerChangeListener(
                final MultiPageReviewActivity activity,
                final UpdateThumbnailsListener updateThumbnailsListener) {
            mActivity = activity;
            mUpdateThumbnailsListener = updateThumbnailsListener;
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset,
                final int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(final int position) {
            if (mLastPosition != position) {
                mLastPosition = position;
                mActivity.updatePageIndicator(position);
                if (!mSkipNextThumbnailsUpdate) {
                    mUpdateThumbnailsListener.setCurrentThumbnail(position);
                }
                mSkipNextThumbnailsUpdate = false;
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
        }

        void skipNextThumbnailsUpdate() {
            mSkipNextThumbnailsUpdate = true;
        }

        interface UpdateThumbnailsListener {

            void setCurrentThumbnail(final int position);
        }
    }

    private static class Thumbnail {

        @Nullable
        final Bitmap bitmap;
        boolean highlighted;

        Thumbnail(@Nullable final Bitmap bitmap, final boolean highlighted) {
            this.bitmap = bitmap;
            this.highlighted = highlighted;
        }
    }

    private static class ThumbnailsAdapter extends
            RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder> implements
            ThumbnailsTouchHelperListener {

        private final ImageMultiPageDocument mMultiPageDocument;
        private final Map<ImageDocument, Photo> mDocumentPhotoMap;
        private final ThumbnailChangeListener mThumbnailChangeListener;
        private final List<Thumbnail> mThumbnails;
        private ItemTouchHelper mItemTouchHelper;
        private RecyclerView mRecyclerView;

        ThumbnailsAdapter(@NonNull final ImageMultiPageDocument multiPageDocument,
                @NonNull final Map<ImageDocument, Photo> documentPhotoMap,
                @NonNull final ThumbnailChangeListener thumbnailChangeListener) {
            mMultiPageDocument = multiPageDocument;
            mDocumentPhotoMap = documentPhotoMap;
            final List<ImageDocument> documents = mMultiPageDocument.getDocuments();
            mThumbnails = new ArrayList<>(documents.size());
            for (final ImageDocument document : documents) {
                final Photo photo = mDocumentPhotoMap.get(document);
                Bitmap preview = null;
                if (photo != null) {
                    preview = photo.getBitmapPreview();
                }
                mThumbnails.add(new Thumbnail(preview, false));
            }
            mThumbnailChangeListener = thumbnailChangeListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(
                final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.gv_item_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder,
                final int position) {
            final ImageView imageView = holder.thumbnailContainer.getImageView();
            final Bitmap bitmap = mThumbnails.get(position).bitmap;
            if (bitmap != null) {
                imageView.setBackgroundColor(Color.TRANSPARENT);
                imageView.setImageBitmap(bitmap);
            } else {
              imageView.setBackgroundColor(Color.BLACK);
              imageView.setImageBitmap(null);
            }
            final ImageDocument document = mMultiPageDocument.getDocuments().get(position);
            final Photo photo = mDocumentPhotoMap.get(document);
            if (photo != null) {
                holder.thumbnailContainer.rotateImageView(
                        photo.getRotationForDisplay(), false);
            }
            holder.badge.setText(String.valueOf(position + 1));
            holder.highlight.setAlpha(mThumbnails.get(position).highlighted ? 1f : 0f);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int adapterPosition = holder.getAdapterPosition();
                    highlightPosition(adapterPosition);
                    mThumbnailChangeListener.onThumbnailSelected(adapterPosition);
                    notifyDataSetChanged();
                }
            });
            holder.handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        final int adapterPosition = holder.getAdapterPosition();
                        holder.highlight.setAlpha(0.5f);
                        highlightPosition(adapterPosition);
                        mThumbnailChangeListener.onThumbnailSelected(adapterPosition);
                        if (mItemTouchHelper != null) {
                            mItemTouchHelper.startDrag(holder);
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mThumbnails.size();
        }

        @Override
        public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mRecyclerView = recyclerView;
        }

        @Override
        public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mRecyclerView = null;
        }

        @Override
        public void onDragFinished() {
            if (mRecyclerView != null) {
                if (mRecyclerView.isComputingLayout()) {
                    mRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                } else {
                    notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onItemMove(final RecyclerView.ViewHolder viewHolder, final int fromPos,
                final RecyclerView.ViewHolder target, final int toPos) {
            Collections.swap(mThumbnails, fromPos, toPos);
            Collections.swap(mMultiPageDocument.getDocuments(), fromPos, toPos);
            notifyItemMoved(fromPos, toPos);
            ((ViewHolder) viewHolder).badge.setText(String.valueOf(toPos + 1));
            ((ViewHolder) target).badge.setText(String.valueOf(fromPos + 1));
            highlightPosition(toPos);
            mThumbnailChangeListener.onThumbnailMoved();
            mThumbnailChangeListener.onThumbnailSelected(toPos);
        }

        void removeThumbnail(final int deletedPosition) {
            mThumbnails.remove(deletedPosition);
            notifyItemRemoved(deletedPosition);
            final int newPosition = getNewPositionAfterDeletion(deletedPosition,
                    mThumbnails.size());
            highlightPosition(newPosition);
            notifyItemChanged(newPosition);
            notifyDataSetChanged();
        }

        void highlightPosition(final int position) {
            for (final Thumbnail image : mThumbnails) {
                image.highlighted = false;
            }
            mThumbnails.get(position).highlighted = true;
        }

        void rotateHighlightedThumbnailBy(final int degrees) {
            int highlightedPosition = -1;
            for (int i = 0; i < mThumbnails.size(); i++) {
                if (mThumbnails.get(i).highlighted) {
                    highlightedPosition = i;
                    break;
                }
            }
            if (mRecyclerView != null) {
                final ViewHolder viewHolder =
                        (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(
                                highlightedPosition);
                if (viewHolder != null) {
                    viewHolder.thumbnailContainer.rotateImageViewBy(degrees, true);
                }
            }
        }

        void setItemTouchHelper(final ItemTouchHelper itemTouchHelper) {
            mItemTouchHelper = itemTouchHelper;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            final TextView badge;
            final View handle;
            final View highlight;
            final RotatableImageViewContainer thumbnailContainer;

            ViewHolder(final View itemView) {
                super(itemView);
                thumbnailContainer = itemView.findViewById(R.id.gv_thumbnail_container);
                badge = itemView.findViewById(R.id.gv_badge);
                highlight = itemView.findViewById(R.id.gv_highlight);
                handle = itemView.findViewById(R.id.gv_handle);
            }
        }

    }

    private static class ThumbnailsTouchHelperCallback extends ItemTouchHelper.Callback {

        private final ThumbnailsTouchHelperListener mListener;

        ThumbnailsTouchHelperCallback(
                final ThumbnailsTouchHelperListener listener) {
            mListener = listener;
        }

        @Override
        public int getMovementFlags(final RecyclerView recyclerView,
                final RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    0);
        }

        @Override
        public boolean onMove(final RecyclerView recyclerView,
                final RecyclerView.ViewHolder viewHolder,
                final RecyclerView.ViewHolder target) {
            mListener.onItemMove(viewHolder, viewHolder.getAdapterPosition(), target,
                    target.getAdapterPosition());
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) {
        }

        @Override
        public void onSelectedChanged(final RecyclerView.ViewHolder viewHolder,
                final int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                mListener.onDragFinished();
            }
        }
    }
}