package com.ensapfe.voca.fragments;

import com.ensapfe.voca.R;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.ensapfe.voca.activities.LoginActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import com.ensapfe.voca.utils.FirebaseDataManager;
import java.util.Map;


public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;
    private static final int STORAGE_PERMISSION_REQUEST = 1003;

    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private Button editProfileButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private ProgressBar loadingProgressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private StorageReference storageRef;
    private FirebaseDataManager dataManager;
    private String currentUsername = "";
    private String currentEmail = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeFirebase();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeViews(View view) {
        profileImageView = view.findViewById(R.id.profile_image_view);
        usernameTextView = view.findViewById(R.id.username_text_view);
        emailTextView = view.findViewById(R.id.email_text_view);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        logoutButton = view.findViewById(R.id.logout_button);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        profileImageView.setImageResource(R.drawable.person);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        dataManager = FirebaseDataManager.getInstance();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(userId)
                    .child("profile");
            storageRef = FirebaseStorage.getInstance().getReference()
                    .child("users")
                    .child(userId)
                    .child("profile");
        }
    }

    private void setupClickListeners() {
        // Prevent double-clicks with debouncing
        profileImageView.setOnClickListener(v -> {
            v.setEnabled(false); // Disable temporarily
            showImagePickerDialog();

            // Re-enable after delay
            new android.os.Handler().postDelayed(() -> {
                if (v != null) {
                    v.setEnabled(true);
                }
            }, 1000);
        });

        editProfileButton.setOnClickListener(v -> {
            v.setEnabled(false);
            showEditProfileDialog();
            new android.os.Handler().postDelayed(() -> {
                if (v != null) v.setEnabled(true);
            }, 500);
        });

        changePasswordButton.setOnClickListener(v -> {
            v.setEnabled(false);
            showChangePasswordDialog();
            new android.os.Handler().postDelayed(() -> {
                if (v != null) v.setEnabled(true);
            }, 500);
        });

        logoutButton.setOnClickListener(v -> {
            v.setEnabled(false);
            showLogoutConfirmation();
            new android.os.Handler().postDelayed(() -> {
                if (v != null) v.setEnabled(true);
            }, 500);
        });
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        showLoading(true);

        currentEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        emailTextView.setText("📧 " + currentEmail);

        if (userRef != null) {
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    showLoading(false);

                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        if (username != null && !username.isEmpty()) {
                            currentUsername = username;
                            usernameTextView.setText("👤 " + username);
                        } else {
                            String displayName = currentUser.getDisplayName();
                            currentUsername = displayName != null ? displayName : "User";
                            usernameTextView.setText("👤 " + currentUsername);
                        }

                        String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            loadProfileImage(imageUrl);
                        }
                    } else {
                        currentUsername = currentUser.getDisplayName() != null ?
                                currentUser.getDisplayName() : "User";
                        usernameTextView.setText("👤 " + currentUsername);
                        saveInitialProfile();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveInitialProfile() {
        if (userRef != null) {
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("username", currentUsername);
            profileData.put("email", currentEmail);
            profileData.put("createdAt", System.currentTimeMillis());
            profileData.put("lastLogin", System.currentTimeMillis());
            profileData.put("appVersion", "1.0.0");
            profileData.put("settings", createDefaultSettings());
            profileData.put("statistics", createDefaultStatistics());

            userRef.setValue(profileData)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("ProfileFragment", "Initial profile created successfully");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ProfileFragment", "Failed to create initial profile", e);
                    });
        }
    }

    private Map<String, Object> createDefaultSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("notificationsEnabled", true);
        settings.put("soundEnabled", true);
        settings.put("darkMode", false);
        settings.put("language", "en");
        settings.put("difficultyLevel", "beginner");
        return settings;
    }

    private Map<String, Object> createDefaultStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPracticeTime", 0);
        stats.put("wordsLearned", 0);
        stats.put("averageScore", 0);
        stats.put("streakDays", 0);
        stats.put("totalSessions", 0);
        stats.put("lastPracticeDate", 0);
        return stats;
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        profileImageView.setImageResource(R.drawable.person);

        // Load image in background thread
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (bitmap != null) {
                            profileImageView.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("ProfileFragment", "Failed to load profile image", e);
            }
        }).start();
    }

    private void showImagePickerDialog() {
        if (getContext() == null) return;

        String[] options = {"Camera", "Gallery", "Remove Photo"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Profile Picture")
                .setItems(options, (dialog, which) -> {
                    dialog.dismiss();
                    new android.os.Handler().postDelayed(() -> {
                        switch (which) {
                            case 0:
                                checkCameraPermission();
                                break;
                            case 1:
                                checkStoragePermission();
                                break;
                            case 2:
                                removeProfilePhoto();
                                break;
                        }
                    }, 100);
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void removeProfilePhoto() {
        profileImageView.setImageResource(R.drawable.person);

        if (storageRef != null && userRef != null) {
            storageRef.delete().addOnCompleteListener(task -> {
                userRef.child("profileImageUrl").removeValue();
                Toast.makeText(getContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = null;

            try {
                if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                    Uri imageUri = data.getData();
                    if (getContext() != null) {
                        bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                    }
                } else if (requestCode == CAMERA_REQUEST && data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        bitmap = (Bitmap) extras.get("data");
                    }
                }

                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap);
                    uploadImageToFirebase(bitmap);
                } else {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                android.util.Log.e("ProfileFragment", "Error processing image", e);
            }
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        if (bitmap == null || getContext() == null) {
            Toast.makeText(getContext(), "Unable to upload image", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        dataManager.uploadProfileImage(bitmap, new FirebaseDataManager.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                dataManager.saveUserProfile(currentUsername, currentEmail, imageUrl,
                        new FirebaseDataManager.OnDataSaveListener() {
                            @Override
                            public void onSuccess() {
                                showLoading(false);
                                Toast.makeText(getContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();

                                if (currentUser != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setPhotoUri(Uri.parse(imageUrl))
                                            .build();
                                    currentUser.updateProfile(profileUpdates);
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                showLoading(false);
                                Toast.makeText(getContext(), "Failed to save: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onProgress(int progress) {
                android.util.Log.d("ProfileFragment", "Upload progress: " + progress + "%");
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void updateProfileImage(String imageUrl, Bitmap bitmap) {
        // Update UI
        profileImageView.setImageBitmap(bitmap);

        // Update Firebase Database
        if (userRef != null) {
            userRef.child("profileImageUrl").setValue(imageUrl)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        EditText usernameEditText = dialogView.findViewById(R.id.username_edit_text);
        EditText emailEditText = dialogView.findViewById(R.id.email_edit_text);

        usernameEditText.setText(currentUsername);
        emailEditText.setText(currentEmail);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        AlertDialog dialog = builder.setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set null initially
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String newUsername = usernameEditText.getText().toString().trim();
                String newEmail = emailEditText.getText().toString().trim();

                if (validateProfileInput(newUsername, newEmail)) {
                    updateProfile(newUsername, newEmail, dialog);
                }
            });
        });

        dialog.show();
    }

    private boolean validateProfileInput(String username, String email) {
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateProfile(String newUsername, String newEmail, AlertDialog dialog) {
        showLoading(true);

        if (userRef != null) {
            userRef.child("username").setValue(newUsername);
        }

        // Update display name in Firebase Auth
        if (currentUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build();

            currentUser.updateProfile(profileUpdates);
        }

        // Update email if changed
        if (!newEmail.equals(currentEmail) && currentUser != null) {
            currentUser.updateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            currentUsername = newUsername;
                            currentEmail = newEmail;
                            usernameTextView.setText("👤 " + newUsername);
                            emailTextView.setText("📧 " + newEmail);

                            // Update email in database
                            if (userRef != null) {
                                userRef.child("email").setValue(newEmail);
                            }

                            Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update email: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Only username changed
            showLoading(false);
            currentUsername = newUsername;
            usernameTextView.setText("👤 " + newUsername);
            Toast.makeText(getContext(), "Username updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

        EditText currentPasswordEditText = dialogView.findViewById(R.id.current_password_edit_text);
        EditText newPasswordEditText = dialogView.findViewById(R.id.new_password_edit_text);
        EditText confirmPasswordEditText = dialogView.findViewById(R.id.confirm_password_edit_text);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        AlertDialog dialog = builder.setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button changeButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            changeButton.setOnClickListener(v -> {
                String currentPassword = currentPasswordEditText.getText().toString();
                String newPassword = newPasswordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();

                if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }

    private boolean validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(getContext(), "Please enter current password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            Toast.makeText(getContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        showLoading(true);

        // Re-authenticate user before changing password
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update password
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    showLoading(false);
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to change password: " +
                                                updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        mAuth.signOut();

        // Navigate back to login screen
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void showLoading(boolean show) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case CAMERA_REQUEST:
                    openCamera();
                    break;
                case STORAGE_PERMISSION_REQUEST:
                    openGallery();
                    break;
            }
        } else {
            Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}