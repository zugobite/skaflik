# Smart Pantry Manager - Build Spec & Plan

> **Purpose of this file:** a self-contained brief for an LLM/coding agent to build the app.
> Scope covered here: **the Android application + GitHub version control only.**
> Deliberately excluded: video demonstration and written report requirements.

---

## 1. Context

**Module:** Mobile App Development 700 (Richfield) - individual practical assignment.
**Deliverable in scope:** a fully functional Android application written in **Java**, developed under Git/GitHub version control.

**Concept:** The app helps a user cut food waste by tracking the ingredients they actually have at home (their "pantry") and suggesting recipes they can cook using **strictly only those ingredients** - no shopping trip required.

---

## 2. Hard constraints (non-negotiable)

| #   | Constraint                                                                                               |
| --- | -------------------------------------------------------------------------------------------------------- |
| C1  | **Java only** - not Kotlin. Built in Android Studio.                                                     |
| C2  | **No Google Maps, no mapping SDK, no GPS/location services.** No "nearby stores" features.               |
| C3  | No payment processing or financial transactions.                                                         |
| C4  | No Play Store publishing.                                                                                |
| C5  | Minimum **4 distinct screens** (Activities, or Fragments in a single host Activity) + a Settings screen. |
| C6  | Data must **persist across app restarts** (not in-memory only).                                          |
| C7  | Full **CRUD** on pantry items must be demonstrable.                                                      |
| C8  | At least one **RecyclerView/ListView with a custom Adapter** bound to the database.                      |
| C9  | **Intents** used for navigation and passing data between screens.                                        |
| C10 | A working navigation element: bottom nav bar, toolbar menu, or navigation drawer.                        |
| C11 | Input validation on every data-entry form.                                                               |
| C12 | Recipes: **15–20 seeded on first run**, each with name, required ingredients, prep steps.                |
| C13 | Git history: **≥10 meaningful, incremental commits** over time - not one dump. Public repo + README.md.  |

---

## 3. The strict-matching rule (core business logic)

This is the single most heavily-weighted piece of logic. The marker tests it directly.

**Rule:** A recipe appears in _Suggested Recipes_ **if and only if every required ingredient is present in the pantry, in at least the required quantity.**

```
suggested(recipe) = for ALL ingredients i in recipe.ingredients:
                        exists p in pantry where
                            normalize(p.name) == normalize(i.name)
                            AND toBaseUnit(p.qty, p.unit) >= toBaseUnit(i.qty, i.unit)
```

**Explicit requirements:**

- 5 required ingredients, 4 present → recipe **must not** appear.
- No partial matches, no "almost there" recipes in the main list.
- Must be **robust to real-world messiness** - a naive exact-string match that breaks on `"tomato"` vs `"tomatoes"` will be **marked down**.
- Full NLP is **not** required.

**Normalisation strategy (implement in a dedicated helper class, e.g. `IngredientMatcher.java`):**

1. Trim, lowercase, collapse repeated whitespace.
2. Strip punctuation and common qualifiers (`fresh`, `chopped`, `large`, `diced`, `ground`).
3. Naive singular/plural folding: strip trailing `es` after `s/x/ch/sh` (`tomatoes → tomato`), otherwise strip trailing `s` (`eggs → egg`), with a small irregular map (`leaves → leaf`, `loaves → loaf`).
4. Optional synonym map (`spring onion ↔ scallion`, `aubergine ↔ eggplant`).

**Unit handling (`UnitConverter.java`):**

- Mass family: `mg / g / kg` → base **g**.
- Volume family: `ml / l / tsp / tbsp / cup` → base **ml** (tsp=5, tbsp=15, cup=250).
- Count family: `piece / pcs / unit / whole` → base **count**.
- Cross-family comparison (e.g. g vs ml) → treat as **not satisfied** unless names match and one side is count-based; document the decision in code comments.

**Optional stretch (bonus credit):** a separate **"Almost There"** list showing recipes missing exactly 1 ingredient. Must be **clearly separated** from the strict suggestions list - never merged into it.

**Empty-state requirement:** when zero recipes match, show a friendly message (e.g. _"No recipes match your pantry yet - add more ingredients"_), not a blank or broken screen.

---

## 4. Screens

| Screen                    | Purpose                                | Key elements                                                                                                                      |
| ------------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **Pantry List**           | Home. All current ingredients from DB. | RecyclerView + custom adapter; FAB to add; tap = edit; swipe/long-press = delete with confirm dialog; empty state.                |
| **Add / Edit Ingredient** | Create & update pantry items.          | Fields: name, quantity, unit (spinner), optional expiry date (DatePicker). Validation. Reused for add and edit via Intent extra.  |
| **Suggested Recipes**     | Runs strict-matching against pantry.   | RecyclerView of qualifying recipes only; empty state; optional "Almost There" section below a clear divider.                      |
| **Recipe Detail**         | Full recipe view.                      | Recipe name, full ingredient list (with have/need quantities), numbered preparation steps. Reached via Intent carrying recipe ID. |
| **Settings / Profile**    | Satisfies min-screens requirement.     | Toggle: expiring-soon alerts. Preference: default units (metric/imperial). Persisted via `SharedPreferences`.                     |

**Navigation:** BottomNavigationView with three destinations - _Pantry_, _Suggestions_, _Settings_. Add/Edit and Recipe Detail launched as child Activities via explicit Intents.

**Validation rules (Add/Edit form):**

- Name: required, non-blank, ≤ 50 chars.
- Quantity: required, numeric, > 0.
- Unit: must be selected from the allowed list.
- Expiry date: optional; if set, must not be in the past (warn, don't block).
- Duplicate ingredient name → offer to merge quantities instead of creating a second row.
- Show errors inline via `TextInputLayout.setError()`; block save until valid.

---

## 5. Database

**Choose ONE** (all three permitted by the brief): SQLite, Firebase, or PostgreSQL-via-REST.

**Recommended: SQLite via `SQLiteOpenHelper`.**
Justification to carry into code comments and the README: fully offline (matches an app about what's in _your_ kitchen), zero backend/auth setup, no network failure modes during marking, directly aligned with the module's persistent-data chapter, and trivially demonstrable persistence across app restarts.

### Schema

```sql
CREATE TABLE pantry_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    name_norm   TEXT    NOT NULL,          -- normalized key for matching
    quantity    REAL    NOT NULL,
    unit        TEXT    NOT NULL,
    expiry_date TEXT,                      -- ISO yyyy-MM-dd, nullable
    created_at  INTEGER NOT NULL
);

CREATE TABLE recipes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    steps       TEXT    NOT NULL,          -- newline-delimited or JSON array
    servings    INTEGER DEFAULT 2,
    prep_minutes INTEGER
);

CREATE TABLE recipe_ingredients (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id   INTEGER NOT NULL,
    name        TEXT    NOT NULL,
    name_norm   TEXT    NOT NULL,
    quantity    REAL    NOT NULL,
    unit        TEXT    NOT NULL,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
);

CREATE INDEX idx_pantry_norm ON pantry_items(name_norm);
CREATE INDEX idx_ri_recipe   ON recipe_ingredients(recipe_id);
```

**Seeding:** in `onCreate()` of the helper, insert 15–20 recipes with their ingredient rows. Keep the seed data in a separate `RecipeSeeder.java` so `DatabaseHelper` stays readable. Choose recipes with **overlapping, common ingredients** (eggs, onion, rice, pasta, tomato, cheese, flour, milk, oil, garlic) so a small pantry can realistically unlock several - this makes the strict-matching demo work convincingly.

**CRUD surface (`PantryDao` / `DatabaseHelper` methods):**
`insertItem`, `getAllItems`, `getItemById`, `updateItem`, `deleteItem`, `getAllRecipes`, `getRecipeById`, `getIngredientsForRecipe`.

---

## 6. Suggested project structure

```
app/src/main/java/com/<you>/smartpantry/
├── MainActivity.java                 // host + BottomNavigationView
├── ui/
│   ├── PantryListFragment.java
│   ├── AddEditIngredientActivity.java
│   ├── SuggestedRecipesFragment.java
│   ├── RecipeDetailActivity.java
│   └── SettingsFragment.java
├── adapter/
│   ├── PantryAdapter.java
│   ├── RecipeAdapter.java
│   └── IngredientLineAdapter.java
├── model/
│   ├── PantryItem.java
│   ├── Recipe.java
│   └── RecipeIngredient.java
├── data/
│   ├── DatabaseHelper.java
│   ├── PantryDao.java
│   ├── RecipeDao.java
│   └── RecipeSeeder.java
└── logic/
    ├── IngredientMatcher.java        // normalisation + strict match
    ├── UnitConverter.java
    └── RecipeSuggestionEngine.java   // returns strict list + almostThere list
```

**Code-quality expectations (25 marks - the largest single band):** clean, readable, **commented** Java; correct use of Activities, Layouts, Intents and Adapters; no business logic buried in Activities - matching lives in `logic/`; consistent naming; no unused imports or dead code.

---

## 7. Build plan (phased, mapped to commits)

Each phase = at least one commit. Spread commits over multiple sittings - **all-at-once history immediately before the deadline is explicitly flagged for investigation.**

| Phase | Work                                                                                | Example commit message                                        |
| ----- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| 0     | Create public GitHub repo, `.gitignore` for Android, initial README skeleton.       | `Initial commit: Android project scaffold and gitignore`      |
| 1     | New Android Studio project (Java, minSdk 24), package structure, app theme/colours. | `Set up base project structure and app theme`                 |
| 2     | Model classes: `PantryItem`, `Recipe`, `RecipeIngredient`.                          | `Add data model classes for pantry and recipes`               |
| 3     | `DatabaseHelper` + table creation.                                                  | `Implement SQLiteOpenHelper with pantry and recipe tables`    |
| 4     | `PantryDao` CRUD methods.                                                           | `Implement DatabaseHelper CRUD methods for pantry items`      |
| 5     | Pantry List screen + `PantryAdapter` + empty state.                                 | `Add PantryListFragment with RecyclerView adapter`            |
| 6     | Add/Edit Ingredient screen, launched via Intent, with validation.                   | `Add AddEditIngredientActivity with input validation`         |
| 7     | Delete with confirmation dialog; wire update flow end-to-end.                       | `Wire up edit and delete flows for pantry items`              |
| 8     | `RecipeSeeder` with 15–20 recipes; seed on first run.                               | `Seed database with 18 starter recipes on first run`          |
| 9     | `UnitConverter` + `IngredientMatcher` normalisation.                                | `Add ingredient normalisation and unit conversion helpers`    |
| 10    | `RecipeSuggestionEngine` - strict matching.                                         | `Implement strict-matching recipe suggestion logic`           |
| 11    | Suggested Recipes screen + empty state message.                                     | `Add SuggestedRecipesFragment with no-match empty state`      |
| 12    | Recipe Detail screen via Intent extra (recipe ID).                                  | `Add RecipeDetailActivity showing ingredients and steps`      |
| 13    | Bottom navigation wiring across the three main destinations.                        | `Add bottom navigation between pantry, suggestions, settings` |
| 14    | Settings screen with SharedPreferences.                                             | `Add settings screen with units and expiry alert preferences` |
| 15    | _(Optional bonus)_ "Almost There" section, visually separated.                      | `Add separate Almost There list for recipes missing one item` |
| 16    | UI polish: spacing, contrast, icons, consistent styling.                            | `Polish layouts and improve list item spacing`                |
| 17    | Bug-fix pass, null-safety, rotation handling.                                       | `Fix null pointer when pantry list is empty`                  |
| 18    | Finalise README (app description, DB choice + why, setup/run steps).                | `Complete README with setup instructions and DB rationale`    |

---

## 8. README.md requirements (repo root)

Must contain:

1. App name and description.
2. **Which database option was chosen and why.**
3. Clear setup/run instructions (Android Studio version, minSdk/targetSdk, how to build and run on emulator/device, any Gradle sync notes).

---

## 9. Acceptance checklist

Functional:

- [ ] App launches without crashing; recipes seeded on first run.
- [ ] Create a pantry item → appears in list immediately.
- [ ] Read: list renders all items from DB via custom adapter.
- [ ] Update: edit an item → change reflected in list and DB.
- [ ] Delete: remove an item → gone from list and DB, with confirmation.
- [ ] Kill and reopen app → all pantry data still present.
- [ ] Suggested Recipes shows **only** fully-satisfiable recipes.
- [ ] Removing one required ingredient makes that recipe **disappear** from suggestions.
- [ ] Adding the missing ingredient makes the recipe **appear**.
- [ ] `"tomatoes"` in pantry satisfies a recipe requiring `"tomato"` (and vice versa).
- [ ] Insufficient quantity (need 500g, have 200g) → recipe excluded.
- [ ] Zero matches → friendly empty-state message, not a blank screen.
- [ ] Recipe Detail shows full ingredient list and steps.
- [ ] Settings toggles persist across restarts.
- [ ] Invalid form input blocked with visible error messages.
- [ ] Navigation element works between all main screens.

Compliance:

- [ ] Zero Kotlin files.
- [ ] Zero references to Maps/location/GPS permissions in `AndroidManifest.xml`.
- [ ] ≥5 screens present.
- [ ] ≥10 meaningful commits, spread over time, descriptive messages.
- [ ] Repo public (or marker added as collaborator) with README.md present.

---

## 10. Marks relevant to this scope

| Criterion                                                                                        | Marks  |
| ------------------------------------------------------------------------------------------------ | ------ |
| App functionality & requirements (all screens work, CRUD correct, stable, no crashes)            | 20     |
| Database implementation (correct, persists between sessions, sensible data model)                | 15     |
| Code quality & Java fundamentals (clean, commented, correct Activities/Layouts/Intents/Adapters) | 25     |
| UI/UX design (usable, consistent, appropriate for mobile)                                        | 10     |
| GitHub version control (regular meaningful commits, full push history, README)                   | 10     |
| **Subtotal in scope**                                                                            | **80** |

_(The remaining 20 marks sit with the video demonstration and written report, which are out of scope for this file.)_

---

## 11. Integrity note

The submission requires that you can **explain and defend any part of the code** if questioned. Build it so you understand it - keep the matching logic simple enough to narrate line by line.
