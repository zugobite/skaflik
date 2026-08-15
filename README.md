# Skaflik - Smart Pantry Manager

An Android app that helps you cut food waste by tracking the ingredients you
actually have at home and suggesting recipes you can cook using **strictly only
those ingredients** - no shopping trip required.

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
activities via explicit Intents.

## Database choice: Cloud Firestore (Firebase)

The brief permits SQLite, Firebase or PostgreSQL-via-REST. This project uses
**Cloud Firestore**, for these reasons:

- **Persistence across restarts and reinstalls.** Pantry data lives server-side
  and is keyed to an anonymous Firebase Auth UID, so it survives an app restart
  - the requirement - and also survives a reinstall or a device change.
- **Offline-first out of the box.** Firestore's local cache means the app still
  reads and writes with no connectivity, then syncs when the network returns.
  Kitchens are not always well-connected.
- **Real-time listeners.** `addSnapshotListener` pushes changes straight into
  the RecyclerView adapters, so a create/update/delete is reflected immediately
  without manual refresh plumbing.
- **A realistic document model.** Recipes with a nested ingredient list map
  naturally onto documents and subcollections, avoiding the join a relational
  schema would need.

### Collections

```
users/{uid}/pantryItems/{itemId}
    name        string    // as typed by the user
    nameNorm    string    // normalised key used for matching
    quantity    number
    unit        string
    expiryDate  string?   // ISO yyyy-MM-dd, nullable
    createdAt   timestamp

recipes/{recipeId}
    name        string
    steps       array<string>
    servings    number
    prepMinutes number
    ingredients array<{ name, nameNorm, quantity, unit }>
```

Recipes are seeded once on first run (18 recipes built from overlapping common
ingredients) and are shared across all users; pantry items are scoped per user.

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK Platform 36, `minSdk` 24, `targetSdk` 36
- Gradle 8.14.3 via the bundled wrapper (`./gradlew`) - no separate install needed

## Setup

1. **Clone and open**

   ```bash
   git clone <repo-url> skaflik
   ```

   Open the `skaflik` folder in Android Studio and let the Gradle sync finish.

2. **Connect your Firebase project**

   `app/google-services.json` in this repo is a **placeholder** and will not
   connect to a live backend. Replace it with your own:

   - In the [Firebase console](https://console.firebase.google.com), create a
     project and add an Android app with package name `com.skaflik`.
   - Download the generated `google-services.json` and drop it into `app/`,
     overwriting the placeholder.
   - In the console, enable **Firestore Database** and enable **Anonymous**
     sign-in under Authentication → Sign-in method.

3. **Build and run**

   ```bash
   ./gradlew assembleDebug
   ```

   Or press Run in Android Studio with an emulator (API 24+) or a physical
   device attached.

## Project structure

```
app/src/main/java/com/skaflik/
├── MainActivity.java              // host + BottomNavigationView
├── ui/                            // fragments and child activities
├── adapter/                       // RecyclerView adapters
├── model/                         // PantryItem, Recipe, RecipeIngredient
├── data/                          // Firestore repositories + recipe seeder
└── logic/                         // IngredientMatcher, UnitConverter,
                                   // RecipeSuggestionEngine
```

Business logic lives in `logic/` - matching and unit conversion are deliberately
kept out of the activities.

## Licence

Coursework project. Not published to the Play Store.
