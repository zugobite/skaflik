package com.zugobite.skaflik.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.model.PantryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Binds the user's pantry to the RecyclerView on the Pantry List screen.
 *
 * <p>The adapter holds its own copy of the list and is refreshed through
 * {@link #submitItems(List)} whenever the Firestore listener reports a change.
 * It knows nothing about where the data came from – the fragment supplies items
 * and handles the taps.</p>
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /** Expiry dates are stored in ISO form. */
    private static final String EXPIRY_DATE_PATTERN = "yyyy-MM-dd";

    /** How close an expiry date has to be before the row is flagged. */
    private static final long EXPIRING_SOON_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);

    /** How the fragment hears about taps on a row. */
    public interface OnItemActionListener {

        /** A row was tapped, which opens it for editing. */
        void onItemClicked(@NonNull PantryItem item);

        /** A row was long-pressed, which offers to delete it. */
        void onItemLongPressed(@NonNull PantryItem item);
    }

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    /** Mirrors the Settings toggle; when off, expiry dates are shown plainly. */
    private boolean highlightExpiring;

    public PantryAdapter(@NonNull OnItemActionListener listener, boolean highlightExpiring) {
        this.listener = listener;
        this.highlightExpiring = highlightExpiring;
    }

    /**
     * Updates whether expiring items are highlighted.
     *
     * <p>Called when the fragment resumes, so a change made in Settings shows
     * up without restarting the app.</p>
     */
    public void setHighlightExpiring(boolean highlightExpiring) {
        if (this.highlightExpiring != highlightExpiring) {
            this.highlightExpiring = highlightExpiring;
            notifyDataSetChanged();
        }
    }

    /**
     * Replaces everything on screen with a fresh list.
     *
     * <p>{@code notifyDataSetChanged} is a blunt instrument, but the pantry is
     * a short list that arrives whole from a snapshot listener, so the cost is
     * negligible and the code stays easy to follow.</p>
     *
     * @param newItems the current pantry contents
     */
    public void submitItems(@NonNull List<PantryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** The item at a given adapter position, used for swipe-to-delete. */
    @NonNull
    public PantryItem getItemAt(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds the views for one pantry row so they are only looked up once. */
    class PantryViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView quantityView;
        private final TextView expiryView;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_item_name);
            quantityView = itemView.findViewById(R.id.text_item_quantity);
            expiryView = itemView.findViewById(R.id.text_item_expiry);
        }

        /** Fills this row's views from one pantry item. */
        void bind(@NonNull PantryItem item) {
            nameView.setText(item.getName());
            quantityView.setText(formatQuantity(item));

            if (item.hasExpiryDate()) {
                expiryView.setVisibility(View.VISIBLE);
                expiryView.setText(itemView.getContext()
                        .getString(R.string.pantry_expires_on, item.getExpiryDate()));
                applyExpiryHighlight(item);
            } else {
                // Recycled rows may still show a previous item's expiry line.
                expiryView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(view -> listener.onItemClicked(item));
            itemView.setOnLongClickListener(view -> {
                listener.onItemLongPressed(item);
                return true;
            });
        }

        /**
         * Colours the expiry line when the date is near or past, provided the
         * user has left the alerts setting on.
         */
        private void applyExpiryHighlight(@NonNull PantryItem item) {
            boolean expiringSoon = highlightExpiring && isExpiringSoon(item.getExpiryDate());
            expiryView.setTextColor(expiringSoon
                    ? ContextCompat.getColor(itemView.getContext(), R.color.skaflik_terracotta)
                    : MaterialColors.getColor(expiryView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        }

        /** True when the date is within the next week, or already past. */
        private boolean isExpiringSoon(@Nullable String isoDate) {
            if (isoDate == null || isoDate.isEmpty()) {
                return false;
            }
            try {
                Date expiry = new SimpleDateFormat(EXPIRY_DATE_PATTERN, Locale.US).parse(isoDate);
                if (expiry == null) {
                    return false;
                }
                long millisUntilExpiry = expiry.getTime() - System.currentTimeMillis();
                return millisUntilExpiry <= EXPIRING_SOON_WINDOW_MILLIS;
            } catch (ParseException malformed) {
                // A date we cannot read is not worth warning about.
                return false;
            }
        }

        /**
         * Renders the amount without a pointless decimal place, so 4 pieces
         * reads "4 piece" rather than "4.0 piece".
         */
        private String formatQuantity(@NonNull PantryItem item) {
            double quantity = item.getQuantity();
            String amount = quantity == Math.floor(quantity)
                    ? String.valueOf((long) quantity)
                    : String.format(Locale.getDefault(), "%.2f", quantity);
            return amount + " " + item.getUnit();
        }
    }
}
