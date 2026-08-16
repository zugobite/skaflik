package com.zugobite.skaflik.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds recipes to a RecyclerView on the Suggested Recipes screen.
 *
 * <p>Used for both lists on that screen. The strict-matches list leaves the
 * "missing" line hidden; the almost-there list supplies the one ingredient
 * that is short, which the row then shows.</p>
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    /** How the fragment hears about a tapped recipe. */
    public interface OnRecipeClickListener {
        void onRecipeClicked(@NonNull Recipe recipe);
    }

    private final List<Recipe> recipes = new ArrayList<>();

    /** Recipe ID to the ingredient it is short of; empty for strict matches. */
    private final Map<String, String> missingByRecipeId = new HashMap<>();

    private final OnRecipeClickListener listener;

    public RecipeAdapter(@NonNull OnRecipeClickListener listener) {
        this.listener = listener;
    }

    /** Shows recipes with no missing-ingredient line. */
    public void submitRecipes(@NonNull List<Recipe> newRecipes) {
        submitRecipes(newRecipes, new HashMap<>());
    }

    /**
     * Shows recipes, optionally annotating what each one is missing.
     *
     * @param newRecipes the recipes to display
     * @param missing    recipe ID to missing ingredient name; may be empty
     */
    public void submitRecipes(@NonNull List<Recipe> newRecipes,
                              @NonNull Map<String, String> missing) {
        recipes.clear();
        recipes.addAll(newRecipes);
        missingByRecipeId.clear();
        missingByRecipeId.putAll(missing);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(recipes.get(position));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    /** Holds the views for one recipe card. */
    class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView metaView;
        private final TextView missingView;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_recipe_name);
            metaView = itemView.findViewById(R.id.text_recipe_meta);
            missingView = itemView.findViewById(R.id.text_recipe_missing);
        }

        void bind(@NonNull Recipe recipe) {
            nameView.setText(recipe.getName());

            List<RecipeIngredient> ingredients = recipe.getIngredients();
            int ingredientCount = ingredients == null ? 0 : ingredients.size();
            metaView.setText(itemView.getContext().getString(R.string.recipe_meta,
                    recipe.getServings(), recipe.getPrepMinutes(), ingredientCount));

            String missing = missingByRecipeId.get(recipe.getId());
            if (missing != null && !missing.isEmpty()) {
                missingView.setVisibility(View.VISIBLE);
                missingView.setText(itemView.getContext()
                        .getString(R.string.recipe_missing, missing));
            } else {
                // Recycled rows may still be showing a previous card's line.
                missingView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(view -> listener.onRecipeClicked(recipe));
        }
    }
}
