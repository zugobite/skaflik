# Skaflik – Smart Pantry Manager

An Android app that helps you cut food waste by tracking the ingredients you
actually have at home and suggesting recipes you can cook using **strictly only
those ingredients** – no shopping trip required.

A recipe is only suggested when **every** ingredient it requires is present in
your pantry, in at least the required quantity. There are no partial matches and
no "almost there" recipes mixed into the main list.

## Features

| Screen                | What it does                                                                     |
| --------------------- | -------------------------------------------------------------------------------- |
| Pantry List           | Every ingredient you have, in a RecyclerView. Add, edit and delete.              |
| Add / Edit Ingredient | Name, quantity, unit and optional expiry date, with inline validation.           |
| Suggested Recipes     | Only the recipes your pantry fully satisfies, plus a friendly empty state.       |
| Recipe Detail         | Full ingredient list (have vs. need) and numbered preparation steps.             |
| Settings              | Expiring-soon alerts toggle and default unit system, saved in SharedPreferences. |

Navigation between Pantry, Suggestions and Settings uses a
`BottomNavigationView`; Add/Edit and Recipe Detail are launched as child
activities via explicit Intents carrying only the relevant document ID.

### How the matching works

`RecipeSuggestionEngine` decides what appears in the suggestions list:

- **Names are normalised before comparison** (`IngredientMatcher`), so "Fresh
  Tomatoes", "tomatoes" and "tomato" are the same ingredient. Qualifier words
  such as _fresh_, _chopped_ and _large_ are dropped, plurals are folded to
  singular, and a small synonym map handles regional variants like
  _aubergine_ / _eggplant_.
- **Quantities are compared in a shared base unit** (`UnitConverter`). Mass
  converts to grams, volume to millilitres, counts stay as counts. Both metric
  and imperial units are supported, so a pantry item recorded in pounds can
  satisfy a recipe written in grams.
- **Multiple pantry rows of the same ingredient are summed**, so two part-used
  bags of flour together cover a recipe needing a full one.
- **Cross-family comparisons are treated as unsatisfied.** 500 g of milk does
  not cover 300 ml, because converting between mass and volume needs a
  per-ingredient density this app does not hold. Failing this way hides a
  cookable recipe rather than claiming a match on a guess.

Recipes missing exactly one ingredient are collected into a separate
**Almost there** list, shown below its own divider and heading. It is never
merged into the suggestions.

## Database choice: Cloud Firestore (Firebase)

The brief permits SQLite, Firebase or PostgreSQL-via-REST. This project uses
**Cloud Firestore**, for these reasons:

- **Persistence across restarts and reinstalls.** Pantry data lives server-side
  and is keyed to an anonymous Firebase Auth UID, so it survives an app restart
  – the requirement – and also survives a reinstall or a device change.
- **Offline-first out of the box.** Firestore's local cache means the app still
  reads and writes with no connectivity, then syncs when the network returns.
  Kitchens are not always well-connected.
- **Real-time listeners.** `addSnapshotListener` pushes changes straight into
  the RecyclerView adapters, so a create/update/delete is reflected immediately
  without manual refresh plumbing.
- **A realistic document model.** Recipes with a nested ingredient list map
  naturally onto documents and subcollections, avoiding the join a relational
  schema would need.

Settings are the one exception: they live in `SharedPreferences` rather than
Firestore, because they describe how this device behaves and must be readable
instantly at startup without a network round trip.

### Collections

```
users/{uid}/pantryItems/{itemId}
    name        string    // as typed by the user
    nameNorm    string    // normalised key used for matching
    quantity    number
    unit        string
    expiryDate  string?   // ISO yyyy-MM-dd, nullable
    createdAt   timestamp // set by the server

recipes/{recipeId}
    name        string
    steps       array<string>
    servings    number
    prepMinutes number
    ingredients array<{ name, nameNorm, quantity, unit }>

meta/seed
    seeded      boolean   // guards against re-seeding the recipe list
    seededAt    timestamp
    recipeCount number
```

Recipes are seeded once on first run (18 recipes built from overlapping common
ingredients) and are shared across all users; pantry items are scoped per user.
The `meta/seed` marker means the seed runs exactly once no matter how many times
the app is installed.

Field and collection names are declared in one place, `data/FirestoreContract.java`,
which doubles as the written record of the data model – Firestore has no schema
of its own.

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK Platform 36, `minSdk` 24, `targetSdk` 36
- Android Gradle plugin 8.13.2, Gradle 8.14.3 via the bundled wrapper
  (`./gradlew`) – no separate Gradle install needed

## Setup

1. **Clone and open**

   ```bash
   git clone https://github.com/zugobite/skaflik.git
   ```

   Open the `skaflik` folder in Android Studio and let the Gradle sync finish.

2. **Firebase**

   No setup required. `app/google-services.json` is committed and points at the
   project's Firebase backend, so a fresh clone builds and runs as-is. That file
   holds client identifiers, not secrets – Google expects it to ship inside the
   APK, and access is controlled by Firestore security rules rather than by
   keeping it private.

   To point the app at your *own* Firebase project instead: create a project in
   the [Firebase console](https://console.firebase.google.com), add an Android
   app with package name `com.zugobite.skaflik`, download the generated
   `google-services.json` over the committed one, then enable **Firestore
   Database** and **Anonymous** sign-in under Authentication → Sign-in method.

3. **Build and run**

   ```bash
   ./gradlew assembleDebug
   ```

   Or press Run in Android Studio with an emulator (API 24+) or a physical
   device attached.

4. **Run the unit tests**

   ```bash
   ./gradlew testDebugUnitTest
   ```

   45 tests cover the matching rules, unit conversion and the suggestion engine,
   including the awkward cases: insufficient quantities, plural and qualifier
   differences, and cross-family units.

### Security rules

`firestore.rules` holds the rules this app expects: a pantry is readable and
writable only by the UID that owns it, and recipes are read-only once the seed
marker exists. Paste the file's contents into **Firestore Database → Rules** in
the Firebase console to replace the default test-mode rules, which allow anyone
to read and write everything and expire 30 days after the database is created.

## Project structure

```
app/src/main/java/com/zugobite/skaflik/
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
│   ├── FirestoreContract.java        // collection and field names
│   ├── AuthManager.java              // anonymous sign-in
│   ├── PantryRepository.java         // full CRUD surface
│   ├── RecipeRepository.java
│   ├── RecipeSeeder.java             // the 18 starter recipes
│   ├── RepositoryCallback.java
│   └── UserPreferences.java          // SharedPreferences access
└── logic/
    ├── IngredientMatcher.java        // name normalisation
    ├── UnitConverter.java            // metric and imperial conversion
    └── RecipeSuggestionEngine.java   // strict matching + almost there
```

Business logic lives in `logic/` and data access in `data/`. No activity or
fragment talks to Firestore directly, and no matching logic sits in a screen.

## Licence

Coursework project. Not published to the Play Store.
