package com.zugobite.skaflik.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.adapter.RecipeAdapter;
import com.zugobite.skaflik.data.AuthManager;
import com.zugobite.skaflik.data.PantryRepository;
import com.zugobite.skaflik.data.RecipeRepository;
import com.zugobite.skaflik.data.RepositoryCallback;
import com.zugobite.skaflik.logic.RecipeSuggestionEngine;
import com.zugobite.skaflik.model.PantryItem;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the recipes the pantry can currently cover.
 *
 * <p>The screen loads the pantry and the recipe list, hands both to
 * {@link RecipeSuggestionEngine}, and renders what comes back. No matching
 * logic lives here – this class only decides what to put on screen.</p>
 *
 * <p>Data is reloaded in {@link #onResume()} rather than watched live, because
 * the pantry usually changes on a different screen and the user expects this
 * one to be current when they come back to it.</p>
 */
public class SuggestedRecipesFragment extends Fragment
        implements RecipeAdapter.OnRecipeClickListener {

    private static final String TAG = "SuggestedRecipes";

    private final PantryRepository pantryRepository = new PantryRepository();
    private final RecipeRepository recipeRepository = new RecipeRepository();

    private RecipeAdapter suggestedAdapter;
    private RecipeAdapter almostThereAdapter;

    private RecyclerView suggestedRecycler;
    private RecyclerView almostThereRecycler;
    private TextView suggestedHeading;
    private TextView emptyStateView;
    private TextView almostThereHeading;
    private View almostThereDivider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggested_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        suggestedRecycler = view.findViewById(R.id.recycler_suggested);
        almostThereRecycler = view.findViewById(R.id.recycler_almost_there);
        suggestedHeading = view.findViewById(R.id.text_suggested_heading);
        emptyStateView = view.findViewById(R.id.text_empty_suggestions);
        almostThereHeading = view.findViewById(R.id.text_almost_there_heading);
        almostThereDivider = view.findViewById(R.id.divider_almost_there);

        suggestedAdapter = new RecipeAdapter(this);
        suggestedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestedRecycler.setAdapter(suggestedAdapter);

        almostThereAdapter = new RecipeAdapter(this);
        almostThereRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        almostThereRecycler.setAdapter(almostThereAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Both reads need a UID, and sign-in may still be in flight.
        AuthManager.runWhenSignedIn(
                () -> {
                    if (isAdded()) {
                        loadSuggestions();
                    }
                },
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Not used; success is handled by the Runnable above.
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        Log.e(TAG, "Sign-in failed, cannot load suggestions", error);
                        if (isAdded()) {
                            showMessage(getString(R.string.error_sign_in));
                        }
                    }
                });
    }

    /**
     * Loads the pantry, then the recipes, then runs the match.
     *
     * <p>The two reads are chained rather than run side by side: matching needs
     * both, and chaining keeps the callback handling simple enough to follow.</p>
     */
    private void loadSuggestions() {
        pantryRepository.getAllItems(new RepositoryCallback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> pantryItems) {
                if (isAdded()) {
                    loadRecipesAndMatch(pantryItems);
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load the pantry", error);
                if (isAdded()) {
                    showMessage(getString(R.string.error_load_pantry));
                }
            }
        });
    }

    private void loadRecipesAndMatch(@NonNull List<PantryItem> pantryItems) {
        recipeRepository.getAllRecipes(new RepositoryCallback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                if (isAdded()) {
                    showResult(RecipeSuggestionEngine.suggestRecipes(pantryItems, recipes),
                            pantryItems);
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load recipes", error);
                if (isAdded()) {
                    showMessage(getString(R.string.error_load_recipes));
                }
            }
        });
    }

    /** Renders the two lists, hiding whichever sections have nothing to show. */
    private void showResult(@NonNull RecipeSuggestionEngine.SuggestionResult result,
                            @NonNull List<PantryItem> pantryItems) {
        List<Recipe> suggested = result.getSuggested();
        suggestedAdapter.submitRecipes(suggested);

        // With no matches, the heading would sit above nothing, so hide it and
        // show the empty-state message in its place.
        boolean hasMatches = !suggested.isEmpty();
        suggestedHeading.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
        suggestedRecycler.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(hasMatches ? View.GONE : View.VISIBLE);

        showAlmostThere(result.getAlmostThere(), pantryItems);
    }

    /**
     * Renders the near misses under their own divider and heading.
     *
     * <p>Kept visually separate from the suggestions above: these are recipes
     * the user <em>cannot</em> cook yet, and merging them into the main list
     * would break the whole point of the strict rule.</p>
     */
    private void showAlmostThere(@NonNull List<Recipe> almostThere,
                                 @NonNull List<PantryItem> pantryItems) {
        boolean hasNearMisses = !almostThere.isEmpty();

        almostThereDivider.setVisibility(hasNearMisses ? View.VISIBLE : View.GONE);
        almostThereHeading.setVisibility(hasNearMisses ? View.VISIBLE : View.GONE);
        almostThereRecycler.setVisibility(hasNearMisses ? View.VISIBLE : View.GONE);

        if (!hasNearMisses) {
            return;
        }

        // Name the one missing ingredient on each card, so the list is useful
        // rather than just tantalising.
        Map<String, String> missingByRecipeId = new HashMap<>();
        for (Recipe recipe : almostThere) {
            List<RecipeIngredient> missing =
                    RecipeSuggestionEngine.findMissingIngredients(recipe, pantryItems);
            if (!missing.isEmpty()) {
                missingByRecipeId.put(recipe.getId(), missing.get(0).getName());
            }
        }
        almostThereAdapter.submitRecipes(almostThere, missingByRecipeId);
    }

    /**
     * Opens the tapped recipe.
     *
     * <p>Only the recipe ID travels in the Intent; the detail screen loads the
     * recipe itself.</p>
     */
    @Override
    public void onRecipeClicked(@NonNull Recipe recipe) {
        if (recipe.getId() == null) {
            showMessage(getString(R.string.error_recipe_missing));
            return;
        }
        startActivity(RecipeDetailActivity.createIntent(requireContext(), recipe.getId()));
    }

    private void showMessage(@NonNull String message) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
