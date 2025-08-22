package com.ensapfe.voca.fragments;

import com.ensapfe.voca.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatFragment extends Fragment {

    private static final String GEMINI_API_KEY = "AIzaSyBzMjR2340N5OyOjgFI1sEgOIRcHR0dh38";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private Handler mainHandler;
    private OkHttpClient httpClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mainHandler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();


        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_button);


        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);


        addMessage("Hi! I'm your English tutor 👨‍🏫\n\nChoose how you'd like to practice:\n• Say 'roleplay' for conversation practice\n• Ask questions about vocabulary\n• Just chat normally!", false);


        sendButton.setOnClickListener(v -> sendMessage());
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String userMessage = messageEditText.getText().toString().trim();
        if (!userMessage.isEmpty()) {
            addMessage(userMessage, true);
            messageEditText.setText("");
            getTutorResponse(userMessage);
        }
    }

    private void addMessage(String message, boolean isUser) {
        mainHandler.post(() -> {
            messageList.add(new ChatMessage(message, isUser));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }

    private void getTutorResponse(String userMessage) {
        addMessage("💭 Thinking...", false);
        final int typingIndex = messageList.size() - 1;

        try {

            StringBuilder conversationContext = new StringBuilder();
            int contextLimit = Math.min(4, messageList.size() - 1);

            for (int i = Math.max(0, messageList.size() - contextLimit - 1); i < messageList.size() - 1; i++) {
                ChatMessage msg = messageList.get(i);
                if (msg.isUser()) {
                    conversationContext.append("Student: ").append(msg.getMessage()).append("\n");
                } else if (!msg.getMessage().contains("💭") && !msg.getMessage().contains("🔄")) {
                    conversationContext.append("Tutor: ").append(msg.getMessage().replace("👨‍🏫 ", "")).append("\n");
                }
            }

            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            String prompt = "You are an English tutor in an ongoing conversation. NEVER suggest restarting or clearing conversation. Keep the flow going naturally. " +
                    "Context of recent conversation:\n" + conversationContext.toString() +
                    "\nStudent's latest message: " + userMessage +
                    "\n\nRespond as a tutor (1-3 sentences). If student wants roleplay, start immediately with a scenario. If they ask vocabulary, give simple definitions with examples.";

            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            // Short response config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("maxOutputTokens", 80);
            generationConfig.put("temperature", 0.7);
            requestBody.put("generationConfig", generationConfig);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> {
                        removeMessage(typingIndex);
                        if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                            addMessage("⏱️ Taking too long. Let me try a quick response...", false);
                            retryQuickResponse(userMessage);
                        } else {
                            addMessage("❌ Connection issue. Please check your internet and try again.", false);
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String tutorResponse = parts.getJSONObject(0).getString("text");

                            mainHandler.post(() -> {
                                removeMessage(typingIndex);
                                // Clean the response of any restart/clear suggestions
                                String cleanResponse = tutorResponse.trim()
                                        .replaceAll("(?i).*restart.*", "")
                                        .replaceAll("(?i).*clear.*", "")
                                        .replaceAll("(?i).*refresh.*", "")
                                        .trim();

                                if (cleanResponse.isEmpty()) {
                                    cleanResponse = "Let's continue! What would you like to practice?";
                                }

                                addMessage("👨‍🏫 " + cleanResponse, false);
                            });
                        } else {
                            String errorMsg = getErrorMessage(response.code());
                            mainHandler.post(() -> {
                                removeMessage(typingIndex);
                                addMessage(errorMsg, false);
                            });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            removeMessage(typingIndex);
                            addMessage("❌ Error processing response. Please try again.", false);
                        });
                    }
                }
            });

        } catch (Exception e) {
            mainHandler.post(() -> {
                removeMessage(typingIndex);
                addMessage("❌ Error sending message. Please try again.", false);
            });
        }
    }

    private void retryQuickResponse(String userMessage) {
        addMessage("🔄 Quick retry...", false);
        final int retryIndex = messageList.size() - 1;

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", "Reply in 1 sentence as English tutor: " + userMessage);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("maxOutputTokens", 30);
            generationConfig.put("temperature", 0.5);
            requestBody.put("generationConfig", generationConfig);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> {
                        removeMessage(retryIndex);
                        addMessage("❌ Both attempts failed. Please try again later.", false);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            String tutorResponse = candidates.getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            mainHandler.post(() -> {
                                removeMessage(retryIndex);
                                addMessage("✅ " + tutorResponse.trim(), false);
                            });
                        } else {
                            mainHandler.post(() -> {
                                removeMessage(retryIndex);
                                addMessage("❌ Retry failed. Please try again later.", false);
                            });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            removeMessage(retryIndex);
                            addMessage("❌ Retry error. Please try again later.", false);
                        });
                    }
                }
            });

        } catch (Exception e) {
            mainHandler.post(() -> {
                removeMessage(retryIndex);
                addMessage("❌ Retry failed. Please try again later.", false);
            });
        }
    }

    private void removeMessage(int index) {
        if (index >= 0 && index < messageList.size()) {
            messageList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }
//for debugging//
    private String getErrorMessage(int code) {
        switch (code) {
            case 400: return "❌ Bad request. Please try again.";
            case 403: return "❌ API access denied. Check your key.";
            case 404: return "❌ Service not found. Please try again.";
            case 429: return "❌ Too many requests. Wait a moment.";
            case 503: return "❌ Service busy. Please try again.";
            default: return "❌ Error " + code + ". Please try again.";
        }
    }

    public static class ChatMessage {
        private String message;
        private boolean isUser;
        private long timestamp;

        public ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() { return message; }
        public boolean isUser() { return isUser; }
        public long getTimestamp() { return timestamp; }
    }
}