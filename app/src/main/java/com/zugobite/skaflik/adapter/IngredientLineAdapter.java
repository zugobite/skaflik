package com.zugobite.skaflik.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.logic.UnitConverter;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lists a recipe's ingredients on the Recipe Detail screen, marking each one
 * as held or short.
 *
 * <p>Each row carries its own status so the adapter never has to work out what
 * the pantry contains – the activity resolves that with the suggestion engine
 * and passes the answer in.</p>
 */
public class IngredientLineAdapter
        extends RecyclerView.Adapter<IngredientLineAdapter.IngredientViewHolder> {

    /** One ingredient plus whether the pantry covers it. */
    public static class IngredientLine {

        private final RecipeIngredient ingredient;
        private final boolean satisfied;

        /** How much the pantry holds, in the ingredient's own unit. */
        private final double availableQuantity;

        public IngredientLine(@NonNull RecipeIngredient ingredient, boolean satisfied,
                              double availableQuantity) {
            this.ingredient = ingredient;
            this.satisfied = satisfied;
            this.availableQuantity = availableQuantity;
        }

        @NonNull
        public RecipeIngredient getIngredient() {
            return ingredient;
        }

        public boolean isSatisfied() {
            return satisfied;
        }

        public double getAvailableQuantity() {
            return availableQuantity;
        }
    }

    private final List<IngredientLine> lines = new ArrayList<>();

    /**
     * The system amounts are printed in. Metric until the activity reads the
     * user's preference, which it does before any row is bound.
     */
    private UnitConverter.System displaySystem = UnitConverter.System.METRIC;

    /** Replaces the displayed ingredient lines. */
    public void submitLines(@NonNull List<IngredientLine> newLines) {
        lines.clear();
        lines.addAll(newLines);
        notifyDataSetChanged();
    }

    /**
     * Sets the unit system amounts are shown in.
     *
     * <p>Rebinds, so a user who changes the setting and comes back sees the
     * rows already on screen redrawn rather than the old units.</p>
     */
    public void setDisplaySystem(@NonNull UnitConverter.System system) {
        if (system == displaySystem) {
            return;
        }
        displaySystem = system;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_line, parent, false);
        return new IngredientViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        holder.bind(lines.get(position), displaySystem);
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    /** Holds the views for one ingredient row. */
    static class IngredientViewHolder extends RecyclerView.ViewHolder {

        private final ImageView statusIcon;
        private final TextView nameView;
        private final TextView amountView;
        private final TextView shortfallView;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIcon = itemView.findViewById(R.id.image_have_status);
            nameView = itemView.findViewById(R.id.text_ingredient_name);
            amountView = itemView.findViewById(R.id.text_ingredient_amount);
            shortfallView = itemView.findViewById(R.id.text_ingredient_shortfall);
        }

        void bind(@NonNull IngredientLine line, @NonNull UnitConverter.System displaySystem) {
            RecipeIngredient ingredient = line.getIngredient();

            nameView.setText(ingredient.getName());
            amountView.setText(formatAmount(
                    ingredient.getQuantity(), ingredient.getUnit(), displaySystem));
            statusIcon.setImageResource(
                    line.isSatisfied() ? R.drawable.ic_check : R.drawable.ic_missing);

            if (line.isSatisfied()) {
                // Recycled rows may still show a previous ingredient's shortfall.
                shortfallView.setVisibility(View.GONE);
            } else {
                shortfallView.setVisibility(View.VISIBLE);
                shortfallView.setText(line.getAvailableQuantity() > 0
                        ? itemView.getContext().getString(R.string.detail_you_have,
                                formatAmount(line.getAvailableQuantity(), ingredient.getUnit(),
                                        displaySystem))
                        : itemView.getContext().getString(R.string.detail_none_in_pantry));
            }
        }

        /**
         * Renders an amount in the user's unit system, without a pointless
         * trailing ".0".
         */
        private String formatAmount(double quantity, String unit,
                                    @NonNull UnitConverter.System displaySystem) {
            UnitConverter.DisplayQuantity shown =
                    UnitConverter.convertForDisplay(quantity, unit, displaySystem);
            double amountValue = shown.getQuantity();
            String amount = amountValue == Math.floor(amountValue)
                    ? String.valueOf((long) amountValue)
                    : String.format(Locale.getDefault(), "%.2f", amountValue);
            return amount + " " + shown.getUnit();
        }
    }
}
