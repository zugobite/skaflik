package com.zugobite.skaflik.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import com.zugobite.skaflik.R;
import com.zugobite.skaflik.adapter.PantryAdapter;
import com.zugobite.skaflik.data.AuthManager;
import com.zugobite.skaflik.data.PantryRepository;
import com.zugobite.skaflik.data.RepositoryCallback;
import com.zugobite.skaflik.data.UserPreferences;
import com.zugobite.skaflik.model.PantryItem;

import java.util.List;

/**
 * Home screen: everything currently in the pantry.
 *
 * <p>The list is driven by a Firestore snapshot listener, so an add, edit or
 * delete anywhere in the app redraws this screen without a manual refresh. The
 * listener is attached in {@link #onStart()} and removed in {@link #onStop()}
 * so it never outlives the view.</p>
 */
public class PantryListFragment extends Fragment
        implements PantryAdapter.OnItemActionListener {

    private static final String TAG = "PantryListFragment";

    private final PantryRepository pantryRepository = new PantryRepository();

    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateView;

    /** Held so the Firestore listener can be detached when the view goes away. */
    @Nullable
    private ListenerRegistration pantryListener;

    /** Guards against attaching a listener after the fragment has stopped. */
    private boolean isStarted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pantry_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_pantry);
        emptyStateView = view.findViewById(R.id.text_empty_pantry);

        adapter = new PantryAdapter(this,
                UserPreferences.isExpiryAlertsEnabled(requireContext()),
                UserPreferences.getDisplaySystem(requireContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        FloatingActionButton addButton = view.findViewById(R.id.fab_add_item);
        addButton.setOnClickListener(button -> openAddScreen());
    }

    @Override
    public void onStart() {
        super.onStart();
        isStarted = true;

        // Sign-in is asynchronous and this fragment is created before it
        // finishes on a cold start, so wait for the UID rather than assume it.
        AuthManager.runWhenSignedIn(
                () -> {
                    // The fragment may have stopped while sign-in was in flight.
                    if (isAdded() && isStarted) {
                        observePantry();
                    }
                },
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Not used; success is handled by the Runnable above.
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        Log.e(TAG, "Sign-in failed, cannot load the pantry", error);
                        if (isAdded()) {
                            showMessage(getString(R.string.error_sign_in));
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pick up a change made on the Settings tab without needing a restart.
        adapter.setHighlightExpiring(
                UserPreferences.isExpiryAlertsEnabled(requireContext()));
        adapter.setDisplaySystem(UserPreferences.getDisplaySystem(requireContext()));
    }

    @Override
    public void onStop() {
        super.onStop();
        isStarted = false;
        // Without this the listener keeps firing into a dead view hierarchy.
        if (pantryListener != null) {
            pantryListener.remove();
            pantryListener = null;
        }
    }

    /** Subscribes to the pantry and redraws the list on every change. */
    private void observePantry() {
        pantryListener = pantryRepository.observeItems(new RepositoryCallback<List<PantryItem>>() {
            @Override
            public void onSuccess(List<PantryItem> items) {
                // The callback can arrive after the view is gone.
                if (!isAdded()) {
                    return;
                }
                adapter.submitItems(items);
                showEmptyState(items.isEmpty());
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not load the pantry", error);
                if (isAdded()) {
                    showMessage(RepositoryCallback.messageFor(error,
                            getString(R.string.error_load_pantry),
                            getString(R.string.error_permission_denied)));
                }
            }
        });
    }

    /** Swaps between the list and the friendly empty message. */
    private void showEmptyState(boolean isEmpty) {
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /** Opens the Add/Edit screen with no item, so it starts in "add" mode. */
    private void openAddScreen() {
        startActivity(AddEditIngredientActivity.createIntent(requireContext(), null));
    }

    /**
     * Opens the tapped item for editing.
     *
     * <p>Only the document ID travels in the Intent; the edit screen reloads the
     * item itself. Passing an ID rather than the whole object keeps the two
     * screens from disagreeing if the data changed in between.</p>
     */
    @Override
    public void onItemClicked(@NonNull PantryItem item) {
        startActivity(AddEditIngredientActivity.createIntent(requireContext(), item.getId()));
    }

    @Override
    public void onItemLongPressed(@NonNull PantryItem item) {
        confirmDelete(item);
    }

    /**
     * Asks before deleting, because a long-press is easy to trigger by accident
     * and there is no undo.
     */
    private void confirmDelete(@NonNull PantryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_item_title)
                .setMessage(getString(R.string.delete_item_message, item.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteItem(item))
                .show();
    }

    /** Deletes the item; the snapshot listener removes the row from the list. */
    private void deleteItem(@NonNull PantryItem item) {
        pantryRepository.deleteItem(item.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    showMessage(getString(R.string.delete_item_done, item.getName()));
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "Could not delete " + item.getName(), error);
                if (isAdded()) {
                    showMessage(getString(R.string.error_delete_item));
                }
            }
        });
    }

    /** Shows a short message anchored to this fragment's view. */
    private void showMessage(@NonNull String message) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
