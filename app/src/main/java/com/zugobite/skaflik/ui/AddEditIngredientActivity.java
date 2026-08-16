package com.zugobite.skaflik.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.data.PantryRepository;
import com.zugobite.skaflik.data.RepositoryCallback;
import com.zugobite.skaflik.data.UserPreferences;
import com.zugobite.skaflik.logic.UnitConverter;
import com.zugobite.skaflik.model.PantryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Creates and edits pantry items.
 *
 * <p>One activity serves both jobs. Launching it with no extras means "add";
 * launching it with {@link #EXTRA_ITEM_ID} means "edit that item". Reusing the
 * screen keeps the validation rules in a single place rather than duplicated
 * across two nearly identical forms.</p>
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    private static final String TAG = "AddEditIngredient";

    /** Intent extra carrying the document ID of the item being edited. */
    public static final String EXTRA_ITEM_ID = "com.zugobite.skaflik.EXTRA_ITEM_ID";

    /** Expiry dates are stored and displayed in ISO form so they sort naturally. */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /** The brief caps ingredient names at 50 characters. */
    private static final int MAX_NAME_LENGTH = 50;

    private final PantryRepository pantryRepository = new PantryRepository();

    private TextInputLayout nameInputLayout;
    private TextInputLayout quantityInputLayout;
    private TextInputEditText nameInput;
    private TextInputEditText quantityInput;
    private TextInputEditText expiryInput;
    private Spinner unitSpinner;

    /** Null in add mode, set in edit mode. */
    @Nullable
    private String editingItemId;

    /**
     * Builds the Intent that opens this screen.
     *
     * @param context  the launching context
     * @param itemId   the item to edit, or null to add a new one
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @Nullable String itemId) {
        Intent intent = new Intent(context, AddEditIngredientActivity.class);
        if (itemId != null) {
            intent.putExtra(EXTRA_ITEM_ID, itemId);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        bindViews();
        setUpUnitSpinner();
        setUpExpiryPicker();

        editingItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        boolean isEditing = editingItemId != null;

        MaterialToolbar toolbar = findViewById(R.id.toolbar_add_edit);
        toolbar.setTitle(isEditing ? R.string.title_edit_ingredient : R.string.title_add_ingredient);
        toolbar.setNavigationOnClickListener(view -> finish());

        if (isEditing) {
            loadExistingItem(editingItemId);
        }

        MaterialButton saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(view -> onSavePressed());
    }

    private void bindViews() {
        nameInputLayout = findViewById(R.id.input_layout_name);
        quantityInputLayout = findViewById(R.id.input_layout_quantity);
        nameInput = findViewById(R.id.input_name);
        quantityInput = findViewById(R.id.input_quantity);
        expiryInput = findViewById(R.id.input_expiry);
        unitSpinner = findViewById(R.id.spinner_unit);
    }

    /**
     * Fills the spinner with every unit the app can convert.
     *
     * <p>All units are always offered, whichever system the user prefers – the
     * preference only decides which one is selected first for a new item, so
     * changing it can never strand something already saved in the other
     * system.</p>
     */
    private void setUpUnitSpinner() {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, UnitConverter.ALLOWED_UNITS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        int defaultPosition = UnitConverter.ALLOWED_UNITS
                .indexOf(UserPreferences.getDefaultUnit(this));
        if (defaultPosition >= 0) {
            unitSpinner.setSelection(defaultPosition);
        }
    }

    /**
     * Opens a DatePicker when the expiry field is touched.
     *
     * <p>The field itself is not focusable, so the only way to set a date is
     * through the picker. That rules out unparseable typed input entirely.</p>
     */
    private void setUpExpiryPicker() {
        expiryInput.setOnClickListener(view -> showDatePicker());
        findViewById(R.id.input_layout_expiry).setOnClickListener(view -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Reopen on the date already chosen, rather than always on today.
        Date existing = parseDate(textOf(expiryInput));
        if (existing != null) {
            calendar.setTime(existing);
        }

        new DatePickerDialog(this,
                (picker, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth);
                    expiryInput.setText(formatDate(chosen.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /** Loads the item being edited and puts its values into the form. */
    private void loadExistingItem(@NonNull String itemId) {
        pantryRepository.getItemById(itemId, new RepositoryCallback<PantryItem>() {
            @Override
            public void onSuccess(PantryItem item) {
                if (item == null) {
                    // Deleted from another screen while this one was opening.
                    showMessage(getString(R.string.error_item_missing));
                    finish();
                    return;
                }
                populateForm(item);
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load item " + itemId, error);
                showMessage(getString(R.string.error_load_item));
                finish();
            }
        });
    }

    private void populateForm(@NonNull PantryItem item) {
        nameInput.setText(item.getName());
        quantityInput.setText(formatQuantity(item.getQuantity()));
        expiryInput.setText(item.getExpiryDate());

        int unitPosition = UnitConverter.ALLOWED_UNITS.indexOf(item.getUnit());
        if (unitPosition >= 0) {
            unitSpinner.setSelection(unitPosition);
        }
    }

    // --- Saving and validation ---

    /** Validates the form, then saves if everything passes. */
    private void onSavePressed() {
        if (!isFormValid()) {
            return;
        }

        String name = textOf(nameInput);
        double quantity = Double.parseDouble(textOf(quantityInput));
        String unit = (String) unitSpinner.getSelectedItem();
        String expiry = textOf(expiryInput);
        PantryItem item = new PantryItem(name, null, quantity, unit,
                expiry.isEmpty() ? null : expiry);

        if (editingItemId != null) {
            updateItem(editingItemId, item);
        } else {
            // Only new items can collide with something already in the pantry.
            checkForDuplicateThenInsert(item);
        }
    }

    /**
     * Checks every field and shows inline errors.
     *
     * <p>Every rule is evaluated rather than returning at the first failure, so
     * the user sees all the problems at once instead of fixing them one at a
     * time.</p>
     *
     * @return true when the form is safe to save
     */
    private boolean isFormValid() {
        boolean isValid = true;

        // Name: required, non-blank, within the length cap.
        String name = textOf(nameInput);
        if (name.isEmpty()) {
            nameInputLayout.setError(getString(R.string.error_name_required));
            isValid = false;
        } else if (name.length() > MAX_NAME_LENGTH) {
            nameInputLayout.setError(getString(R.string.error_name_too_long, MAX_NAME_LENGTH));
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // Quantity: required, numeric, greater than zero.
        String quantityText = textOf(quantityInput);
        if (quantityText.isEmpty()) {
            quantityInputLayout.setError(getString(R.string.error_quantity_required));
            isValid = false;
        } else {
            try {
                double quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    quantityInputLayout.setError(getString(R.string.error_quantity_positive));
                    isValid = false;
                } else {
                    quantityInputLayout.setError(null);
                }
            } catch (NumberFormatException notANumber) {
                // Reachable despite the numeric keyboard, e.g. a lone ".".
                quantityInputLayout.setError(getString(R.string.error_quantity_numeric));
                isValid = false;
            }
        }

        // Expiry: optional. A past date is a warning, not a blocker, because
        // people do legitimately track things that have just gone out of date.
        String expiry = textOf(expiryInput);
        if (!expiry.isEmpty() && isInThePast(expiry)) {
            showMessage(getString(R.string.warning_expiry_past));
        }

        return isValid;
    }

    /**
     * Offers to merge when the pantry already holds the same ingredient.
     *
     * <p>Matching is on the normalised name, so adding "Tomatoes" when
     * "tomato" is already there is caught. Merging only makes sense when the
     * units are comparable; otherwise the user is left to save a second row.</p>
     */
    private void checkForDuplicateThenInsert(@NonNull PantryItem item) {
        pantryRepository.findByNormalisedName(item.getName(),
                new RepositoryCallback<PantryItem>() {
                    @Override
                    public void onSuccess(PantryItem existing) {
                        if (existing != null
                                && UnitConverter.areComparable(existing.getUnit(), item.getUnit())) {
                            promptToMerge(existing, item);
                        } else {
                            insertItem(item);
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        // A failed duplicate check should not block the save.
                        Log.w(TAG, "Duplicate check failed; saving as a new item", error);
                        insertItem(item);
                    }
                });
    }

    /** Asks whether to combine the new amount into the existing row. */
    private void promptToMerge(@NonNull PantryItem existing, @NonNull PantryItem incoming) {
        // Add in the existing item's unit so the stored value stays consistent.
        double existingInBase = UnitConverter.toBaseUnit(existing.getQuantity(), existing.getUnit());
        double incomingInBase = UnitConverter.toBaseUnit(incoming.getQuantity(), incoming.getUnit());
        double perExistingUnit = UnitConverter.toBaseUnit(1, existing.getUnit());
        double mergedQuantity = (existingInBase + incomingInBase) / perExistingUnit;

        new AlertDialog.Builder(this)
                .setTitle(R.string.merge_title)
                .setMessage(getString(R.string.merge_message,
                        existing.getName(),
                        formatQuantity(mergedQuantity),
                        existing.getUnit()))
                .setNegativeButton(R.string.merge_keep_separate, (dialog, which) -> insertItem(incoming))
                .setPositiveButton(R.string.merge_combine, (dialog, which) ->
                        mergeInto(existing, mergedQuantity))
                .show();
    }

    private void mergeInto(@NonNull PantryItem existing, double mergedQuantity) {
        pantryRepository.updateQuantity(existing.getId(), mergedQuantity, existing.getUnit(),
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        finishWithMessage(getString(R.string.merge_done, existing.getName()));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        Log.e(TAG, "Could not merge quantities", error);
                        showMessage(getString(R.string.error_save_item));
                    }
                });
    }

    private void insertItem(@NonNull PantryItem item) {
        pantryRepository.insertItem(item, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String newItemId) {
                finishWithMessage(getString(R.string.save_added, item.getName()));
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not add item", error);
                showMessage(getString(R.string.error_save_item));
            }
        });
    }

    private void updateItem(@NonNull String itemId, @NonNull PantryItem item) {
        pantryRepository.updateItem(itemId, item, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                finishWithMessage(getString(R.string.save_updated, item.getName()));
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not update item " + itemId, error);
                showMessage(getString(R.string.error_save_item));
            }
        });
    }

    // --- Small helpers ---

    /** Closes the screen; the pantry list picks the change up from Firestore. */
    private void finishWithMessage(@NonNull String message) {
        Intent result = new Intent();
        result.putExtra(Intent.EXTRA_TEXT, message);
        setResult(RESULT_OK, result);
        finish();
    }

    private void showMessage(@NonNull String message) {
        Snackbar.make(findViewById(R.id.button_save), message, Snackbar.LENGTH_LONG).show();
    }

    /** Reads a field's text, never null and always trimmed. */
    @NonNull
    private String textOf(@NonNull TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Renders a quantity without a trailing ".0". */
    @NonNull
    private String formatQuantity(double quantity) {
        return quantity == Math.floor(quantity)
                ? String.valueOf((long) quantity)
                : String.format(Locale.getDefault(), "%.2f", quantity);
    }

    @NonNull
    private String formatDate(@NonNull Date date) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(date);
    }

    /** Parses an ISO date, returning null when the text is absent or malformed. */
    @Nullable
    private Date parseDate(@NonNull String isoDate) {
        if (isoDate.isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN, Locale.US).parse(isoDate);
        } catch (ParseException malformed) {
            return null;
        }
    }

    /** True when the given date is before today. */
    private boolean isInThePast(@NonNull String isoDate) {
        Date expiry = parseDate(isoDate);
        if (expiry == null) {
            return false;
        }

        // Compare against the start of today so today's date is not "past".
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        return expiry.before(startOfToday.getTime());
    }
}
