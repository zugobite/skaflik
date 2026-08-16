package com.zugobite.skaflik.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.adapter.IngredientLineAdapter;
import com.zugobite.skaflik.data.PantryRepository;
import com.zugobite.skaflik.data.RecipeRepository;
import com.zugobite.skaflik.data.RepositoryCallback;
import com.zugobite.skaflik.logic.RecipeSuggestionEngine;
import com.zugobite.skaflik.model.PantryItem;
import com.zugobite.skaflik.model.Recipe;
import com.zugobite.skaflik.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * The full view of one recipe: its ingredients, marked against the pantry, and
 * its preparation steps.
 *
 * <p>Reached from the suggestions list by an explicit Intent carrying only the
 * recipe's document ID. This screen then loads the recipe itself, which keeps
 * the two screens from disagreeing and avoids making {@link Recipe}
 * serialisable purely to move it between activities.</p>
 */
public class RecipeDetailActivity extends AppCompatActivity {

    private static final String TAG = "RecipeDetail";

    /** Intent extra carrying the recipe's Firestore document ID. */
    public static final String EXTRA_RECIPE_ID = "com.zugobite.skaflik.EXTRA_RECIPE_ID";

    private final RecipeRepository recipeRepository = new RecipeRepository();
    private final PantryRepository pantryRepository = new PantryRepository();

    private final IngredientLineAdapter ingredientAdapter = new IngredientLineAdapter();

    private MaterialToolbar toolbar;
    private TextView nameView;
    private TextView metaView;
    private TextView statusView;
    private LinearLayout stepsContainer;

    /** Builds the Intent that opens this screen for a given recipe. */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String recipeId) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE_ID, recipeId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        toolbar = findViewById(R.id.toolbar_recipe_detail);
        nameView = findViewById(R.id.text_detail_name);
        metaView = findViewById(R.id.text_detail_meta);
        statusView = findViewById(R.id.text_detail_status);
        stepsContainer = findViewById(R.id.container_steps);

        toolbar.setNavigationOnClickListener(view -> finish());

        RecyclerView ingredientRecycler = findViewById(R.id.recycler_ingredients);
        ingredientRecycler.setLayoutManager(new LinearLayoutManager(this));
        ingredientRecycler.setAdapter(ingredientAdapter);

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId == null) {
            // Launched without an ID, which should not happen; fail visibly.
            Log.e(TAG, "Opened with no recipe ID");
            showMessage(getString(R.string.error_recipe_missing));
            finish();
            return;
        }

        loadRecipe(recipeId);
    }

    /** Loads the recipe, then the pantry it will be measured against. */
    private void loadRecipe(@NonNull String recipeId) {
        recipeRepository.getRecipeById(recipeId, new RepositoryCallback<Recipe>() {
            @Override
            public void onSuccess(Recipe recipe) {
                if (recipe == null) {
                    showMessage(getString(R.string.error_recipe_missing));
                    finish();
                    return;
                }
                showRecipe(recipe);
                loadPantryAndMark(recipe);
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load recipe " + recipeId, error);
                showMessage(getString(R.string.error_load_recipes));
                finish();
            }
        });
    }

    /** Renders everything that does not depend on the pantry. */
    private void showRecipe(@NonNull Recipe recipe) {
        toolbar.setTitle(recipe.getName());
        nameView.setText(recipe.getName());
        metaView.setText(getString(R.string.detail_meta,
                recipe.getServings(), recipe.getPrepMinutes()));
        showSteps(recipe.getSteps());
    }

    /**
     * Adds the preparation steps as a numbered list.
     *
     * <p>Built at runtime rather than with a second RecyclerView: the steps are
     * a short static list inside an already-scrolling screen, so a nested
     * scrolling container would be more machinery than the job needs.</p>
     */
    private void showSteps(@Nullable List<String> steps) {
        stepsContainer.removeAllViews();

        if (steps == null || steps.isEmpty()) {
            TextView noSteps = new TextView(this);
            noSteps.setText(R.string.detail_no_steps);
            stepsContainer.addView(noSteps);
            return;
        }

        for (int index = 0; index < steps.size(); index++) {
            stepsContainer.addView(buildStepRow(index + 1, steps.get(index)));
        }
    }

    /** One "1. Do the thing" line. */
    @NonNull
    private LinearLayout buildStepRow(int stepNumber, @NonNull String stepText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView number = new TextView(this);
        number.setText(getString(R.string.detail_step_number, stepNumber));
        number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        number.setGravity(Gravity.START);
        number.setWidth(dp(28));

        TextView text = new TextView(this);
        text.setText(stepText);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        text.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(number);
        row.addView(text);
        return row;
    }

    /** Loads the pantry and marks each ingredient as held or short. */
    private void loadPantryAndMark(@NonNull Recipe recipe) {
        pantryRepository.getAllItems(new RepositoryCallback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> pantryItems) {
                showIngredients(recipe, pantryItems);
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load the pantry", error);
                // The recipe is still worth reading, so show the ingredients
                // with nothing marked as held rather than showing nothing.
                showIngredients(recipe, new ArrayList<>());
                showMessage(getString(R.string.error_load_pantry));
            }
        });
    }

    /** Builds the ingredient rows and the overall can-cook status line. */
    private void showIngredients(@NonNull Recipe recipe, @NonNull List<PantryItem> pantryItems) {
        List<RecipeIngredient> ingredients = recipe.getIngredients();
        List<IngredientLineAdapter.IngredientLine> lines = new ArrayList<>();

        if (ingredients != null) {
            List<RecipeIngredient> missing =
                    RecipeSuggestionEngine.findMissingIngredients(recipe, pantryItems);

            for (RecipeIngredient ingredient : ingredients) {
                // findMissingIngredients returns the very objects held in this
                // recipe, so an identity check is enough and RecipeIngredient
                // needs no equals() written purely for this comparison.
                boolean satisfied = !containsSame(missing, ingredient);
                double available = satisfied
                        ? ingredient.getQuantity()
                        : RecipeSuggestionEngine.availableQuantityFor(ingredient, pantryItems);
                lines.add(new IngredientLineAdapter.IngredientLine(
                        ingredient, satisfied, available));
            }
        }

        ingredientAdapter.submitLines(lines);

        boolean canCook = RecipeSuggestionEngine.canCook(recipe, pantryItems);
        statusView.setText(canCook
                ? R.string.detail_can_cook
                : R.string.detail_cannot_cook);
    }

    /** True when the list holds this exact ingredient object. */
    private boolean containsSame(@NonNull List<RecipeIngredient> ingredients,
                                 @NonNull RecipeIngredient target) {
        for (RecipeIngredient candidate : ingredients) {
            if (candidate == target) {
                return true;
            }
        }
        return false;
    }

    private void showMessage(@NonNull String message) {
        Snackbar.make(findViewById(R.id.container_steps), message, Snackbar.LENGTH_LONG).show();
    }

    /** Converts dp to pixels for the views built in code. */
    private int dp(int dpValue) {
        return Math.round(dpValue * getResources().getDisplayMetrics().density);
    }
}
