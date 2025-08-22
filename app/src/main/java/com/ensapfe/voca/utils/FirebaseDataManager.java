package com.ensapfe.voca.utils;

import android.graphics.Bitmap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataManager {

    private static FirebaseDataManager instance;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;

    private FirebaseDataManager() {
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    public static synchronized FirebaseDataManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDataManager();
        }
        return instance;
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // PROFILE DATA MANAGEMENT
    public void saveUserProfile(String username, String email, String profileImageUrl,
                                OnDataSaveListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("username", username);
        profileData.put("email", email);
        if (profileImageUrl != null) {
            profileData.put("profileImageUrl", profileImageUrl);
        }
        profileData.put("lastUpdated", System.currentTimeMillis());

        databaseRef.child("users").child(userId).child("profile")
                .updateChildren(profileData)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // CHAT HISTORY MANAGEMENT
    public void saveChatMessage(String chatId, String message, boolean isUser,
                                OnDataSaveListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("isUser", isUser);
        messageData.put("timestamp", System.currentTimeMillis());

        String messageId = databaseRef.child("users").child(userId)
                .child("chats").child(chatId).child("messages").push().getKey();

        if (messageId != null) {
            databaseRef.child("users").child(userId)
                    .child("chats").child(chatId).child("messages").child(messageId)
                    .setValue(messageData)
                    .addOnSuccessListener(aVoid -> {
                        // Update chat metadata
                        updateChatMetadata(chatId, message, isUser);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        }
    }

    private void updateChatMetadata(String chatId, String lastMessage, boolean isUserMessage) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", lastMessage);
        chatData.put("lastTimestamp", System.currentTimeMillis());
        chatData.put("isUserLastMessage", isUserMessage);

        databaseRef.child("users").child(userId)
                .child("chats").child(chatId)
                .updateChildren(chatData);
    }

    // PRONUNCIATION SCORES MANAGEMENT
    public void savePronunciationScore(String sentence, int score, String userTranscription,
                                       List<String> mistakes, OnDataSaveListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("sentence", sentence);
        scoreData.put("score", score);
        scoreData.put("userTranscription", userTranscription);
        scoreData.put("mistakes", mistakes);
        scoreData.put("timestamp", System.currentTimeMillis());
        scoreData.put("date", java.text.DateFormat.getDateInstance().format(new java.util.Date()));

        String scoreId = databaseRef.child("users").child(userId)
                .child("pronunciationScores").push().getKey();

        if (scoreId != null) {
            databaseRef.child("users").child(userId)
                    .child("pronunciationScores").child(scoreId)
                    .setValue(scoreData)
                    .addOnSuccessListener(aVoid -> {
                        updateUserStatistics(score);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        }
    }

    // USER STATISTICS MANAGEMENT - FIXED VERSION
    private void updateUserStatistics(int newScore) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        DatabaseReference statsRef = databaseRef.child("users").child(userId)
                .child("profile").child("statistics");

        // Read current stats and update
        statsRef.get().addOnSuccessListener(snapshot -> {
            Map<String, Object> stats = new HashMap<>();

            if (snapshot.exists()) {
                // Get existing values with proper null checking
                Long totalSessionsValue = snapshot.child("totalSessions").getValue(Long.class);
                long totalSessions = totalSessionsValue != null ? totalSessionsValue : 0L;

                Long totalScoreValue = snapshot.child("totalScore").getValue(Long.class);
                long totalScore = totalScoreValue != null ? totalScoreValue : 0L;

                Long wordsLearnedValue = snapshot.child("wordsLearned").getValue(Long.class);
                long wordsLearned = wordsLearnedValue != null ? wordsLearnedValue : 0L;

                // Update values
                stats.put("totalSessions", totalSessions + 1);
                stats.put("totalScore", totalScore + newScore);
                stats.put("averageScore", (totalScore + newScore) / (totalSessions + 1));
                stats.put("wordsLearned", wordsLearned + 1);
            } else {
                // First time stats
                stats.put("totalSessions", 1);
                stats.put("totalScore", newScore);
                stats.put("averageScore", newScore);
                stats.put("wordsLearned", 1);
            }

            stats.put("lastPracticeDate", System.currentTimeMillis());
            statsRef.updateChildren(stats);
        });
    }

    // AUDIO RECORDINGS MANAGEMENT
    public void saveAudioRecording(byte[] audioData, String sentence, int score,
                                   OnDataSaveListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        // Upload audio file to Storage
        String filename = "recording_" + System.currentTimeMillis() + ".3gp";
        StorageReference audioRef = storageRef.child("users")
                .child(userId).child("recordings").child(filename);

        audioRef.putBytes(audioData)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Save metadata to Database
                        Map<String, Object> recordingData = new HashMap<>();
                        recordingData.put("audioUrl", uri.toString());
                        recordingData.put("sentence", sentence);
                        recordingData.put("score", score);
                        recordingData.put("timestamp", System.currentTimeMillis());
                        recordingData.put("filename", filename);

                        String recordingId = databaseRef.child("users").child(userId)
                                .child("audioRecordings").push().getKey();

                        if (recordingId != null) {
                            databaseRef.child("users").child(userId)
                                    .child("audioRecordings").child(recordingId)
                                    .setValue(recordingData)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                        }
                    }).addOnFailureListener(e -> listener.onFailure("Failed to get audio URL"));
                })
                .addOnFailureListener(e -> listener.onFailure("Failed to upload audio"));
    }

    // PROFILE IMAGE UPLOAD
    public void uploadProfileImage(Bitmap bitmap, OnImageUploadListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        // Compress image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        // Upload to Storage
        String filename = "profile_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("users")
                .child(userId).child("profile").child(filename);

        imageRef.putBytes(data)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    listener.onProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        listener.onSuccess(uri.toString());
                    }).addOnFailureListener(e -> listener.onFailure("Failed to get image URL"));
                })
                .addOnFailureListener(e -> listener.onFailure("Upload failed: " + e.getMessage()));
    }

    // USER SETTINGS MANAGEMENT
    public void saveUserSettings(Map<String, Object> settings, OnDataSaveListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        databaseRef.child("users").child(userId).child("settings")
                .updateChildren(settings)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // INTERFACES
    public interface OnDataSaveListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onProgress(int progress);
        void onFailure(String error);
    }

    public interface OnDataLoadListener<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }
}