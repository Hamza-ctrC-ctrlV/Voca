package com.ensapfe.voca.fragments;

import com.ensapfe.voca.R;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class HomeFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final String GEMINI_API_KEY = "AIzaSyBzMjR2340N5OyOjgFI1sEgOIRcHR0dh38";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
    private static final int RECORD_AUDIO_PERMISSION = 1001;
    private TextView sentenceTextView;
    private TextView instructionTextView;
    private Button generateButton;
    private ImageButton playButton;
    private ImageButton recordButton;
    private ProgressBar loadingProgressBar;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private boolean isTTSReady = false;
    private String currentSentence = "";
    private String userTranscription = "";
    private Handler mainHandler;
    private OkHttpClient httpClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        initializeServices();
        setupClickListeners();
        checkPermissions();
    }

    private void initializeViews(View view) {
        sentenceTextView = view.findViewById(R.id.sentence_text_view);
        instructionTextView = view.findViewById(R.id.instruction_text_view);
        generateButton = view.findViewById(R.id.generate_button);
        playButton = view.findViewById(R.id.play_button);
        recordButton = view.findViewById(R.id.record_button);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        sentenceTextView.setText("Welcome to Pronunciation Practice! 🎯\n\nTap 'Generate Sentence' to get started.");
        instructionTextView.setText("📝 Instructions:\n1. Generate a random sentence\n2. Listen to pronunciation\n3. Record yourself saying it\n4. Get your score!");
        playButton.setEnabled(false);
        recordButton.setEnabled(false);
    }

    private void initializeServices() {
        mainHandler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        textToSpeech = new TextToSpeech(getContext(), this);
        if (getContext() != null && SpeechRecognizer.isRecognitionAvailable(getContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
            if (speechRecognizer != null) {
                speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
            }
        }
    }

    private void setupClickListeners() {
        generateButton.setOnClickListener(v -> generateRandomSentence());
        playButton.setOnClickListener(v -> playCurrentSentence());
        recordButton.setOnClickListener(v -> toggleRecording());
    }

    private void checkPermissions() {
        if (getContext() != null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (getActivity() != null) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Microphone permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Microphone permission required for speech recording", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void generateRandomSentence() {
        showLoading(true);
        generateButton.setEnabled(false);

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", "Generate one random English sentence for pronunciation practice. The sentence should be:\n" +
                    "- 8-15 words long\n" +
                    "- Clear and meaningful\n" +
                    "- Good for pronunciation practice\n" +
                    "- Not too complex\n" +
                    "Examples: 'The quick brown fox jumps over the lazy dog', 'She sells seashells by the seashore'\n" +
                    "Return ONLY the sentence, nothing else.");

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("maxOutputTokens", 50);
            generationConfig.put("temperature", 0.8);
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
                        showLoading(false);
                        generateButton.setEnabled(true);
                        showFallbackSentence();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            String generatedSentence = candidates.getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text")
                                    .trim()
                                    .replaceAll("[\"']", ""); // Remove quotes

                            mainHandler.post(() -> {
                                showLoading(false);
                                generateButton.setEnabled(true);
                                displaySentence(generatedSentence);
                            });
                        } else {
                            mainHandler.post(() -> {
                                showLoading(false);
                                generateButton.setEnabled(true);
                                showFallbackSentence();
                            });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            generateButton.setEnabled(true);
                            showFallbackSentence();
                        });
                    }
                }
            });

        } catch (Exception e) {
            showLoading(false);
            generateButton.setEnabled(true);
            showFallbackSentence();
        }
    }

    private void showFallbackSentence() {
        String[] fallbackSentences = {
                "The quick brown fox jumps over the lazy dog",
                "She sells seashells by the seashore",
                "How much wood would a woodchuck chuck",
                "Peter Piper picked a peck of pickled peppers",
                "A watched pot never boils",
                "Practice makes perfect every single day"
        };

        int randomIndex = (int) (Math.random() * fallbackSentences.length);
        displaySentence(fallbackSentences[randomIndex]);
    }

    private void displaySentence(String sentence) {
        currentSentence = sentence;
        sentenceTextView.setText("🎯 Practice Sentence:\n\n\"" + sentence + "\"");
        instructionTextView.setText("🔊 Tap 'Play' to hear pronunciation\n🎙️ Tap 'Record' to practice speaking");

        playButton.setEnabled(isTTSReady);
        recordButton.setEnabled(true);
    }

    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            instructionTextView.setText("🤖 AI is generating a sentence...");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "TTS Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                isTTSReady = true;
                textToSpeech.setSpeechRate(0.8f);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            mainHandler.post(() -> {
                                if (playButton != null) {
                                    playButton.setEnabled(true);
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            mainHandler.post(() -> {
                                if (playButton != null) {
                                    playButton.setEnabled(true);
                                }
                            });
                        }
                    });
                }

                if (!currentSentence.isEmpty() && playButton != null) {
                    playButton.setEnabled(true);
                }
            }
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void playCurrentSentence() {
        if (isTTSReady && !currentSentence.isEmpty() && textToSpeech != null) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_playback");
            textToSpeech.speak(currentSentence, TextToSpeech.QUEUE_FLUSH, params, "sentence_playback");
            playButton.setEnabled(false);

            mainHandler.postDelayed(() -> {
                if (playButton != null) {
                    playButton.setEnabled(true);
                }
            }, 5000);
        }
    }

    private void toggleRecording() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        if (speechRecognizer != null && getContext() != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            isRecording = true;
            recordButton.setImageResource(R.drawable.ic_stop);
            recordButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_red_light));
            instructionTextView.setText("🎙️ Recording... Speak the sentence clearly!");

            speechRecognizer.startListening(intent);
        }
    }

    private void stopRecording() {
        if (speechRecognizer != null && isRecording && getContext() != null) {
            speechRecognizer.stopListening();
            isRecording = false;
            recordButton.setImageResource(R.drawable.ic_mic);
            recordButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_blue_bright));
            instructionTextView.setText("⏳ Processing your speech...");
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {}

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            isRecording = false;
            if (getContext() != null) {
                recordButton.setImageResource(R.drawable.ic_mic);
                recordButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_blue_bright));
            }

            String errorMessage = "Recording error. Please try again.";
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMessage = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMessage = "Client side error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMessage = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMessage = "No speech detected. Please try again.";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMessage = "Speech timeout. Please speak louder.";
                    break;
            }

            instructionTextView.setText("❌ " + errorMessage);
        }

        @Override
        public void onResults(Bundle results) {
            isRecording = false;
            if (getContext() != null) {
                recordButton.setImageResource(R.drawable.ic_mic);
                recordButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_blue_bright));
            }

            ArrayList<String> voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (voiceResults != null && !voiceResults.isEmpty()) {
                userTranscription = voiceResults.get(0);
                instructionTextView.setText("✅ Recording complete! Analyzing...");
                analyzePronunciation();
            } else {
                instructionTextView.setText("❌ No speech detected. Please try again.");
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    private void analyzePronunciation() {
        if (currentSentence.isEmpty() || userTranscription.isEmpty()) {
            return;
        }

        // Calculate similarity score
        PronunciationAnalysis analysis = calculatePronunciationScore(currentSentence, userTranscription);
        showScoreDialog(analysis);
    }

    private PronunciationAnalysis calculatePronunciationScore(String original, String spoken) {
        // Normalize text for comparison
        String originalNorm = original.toLowerCase().replaceAll("[^a-zA-Z\\s]", "").trim();
        String spokenNorm = spoken.toLowerCase().replaceAll("[^a-zA-Z\\s]", "").trim();

        String[] originalWords = originalNorm.split("\\s+");
        String[] spokenWords = spokenNorm.split("\\s+");

        // Calculate word accuracy
        Set<String> originalSet = new HashSet<>(Arrays.asList(originalWords));
        Set<String> spokenSet = new HashSet<>(Arrays.asList(spokenWords));
        Set<String> correctWords = new HashSet<>(originalSet);
        correctWords.retainAll(spokenSet);

        // Calculate similarity using Levenshtein distance
        int editDistance = levenshteinDistance(originalNorm, spokenNorm);
        int maxLength = Math.max(originalNorm.length(), spokenNorm.length());
        double similarity = maxLength > 0 ? (1.0 - (double) editDistance / maxLength) * 100 : 100;

        // Find missing/incorrect words
        List<String> missingWords = new ArrayList<>(originalSet);
        missingWords.removeAll(spokenSet);

        List<String> extraWords = new ArrayList<>(spokenSet);
        extraWords.removeAll(originalSet);

        // Calculate final score (weighted)
        double wordAccuracy = originalWords.length > 0 ? (double) correctWords.size() / originalWords.length * 100 : 100;
        int finalScore = (int) ((similarity * 0.7) + (wordAccuracy * 0.3));
        finalScore = Math.max(0, Math.min(100, finalScore));

        return new PronunciationAnalysis(finalScore, similarity, missingWords, extraWords,
                correctWords.size(), originalWords.length);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private void showScoreDialog(PronunciationAnalysis analysis) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Create custom view for dialog
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_score, null);
        if (dialogView == null) return;

        TextView scoreText = dialogView.findViewById(R.id.score_text);
        TextView accuracyText = dialogView.findViewById(R.id.accuracy_text);
        TextView mistakesText = dialogView.findViewById(R.id.mistakes_text);
        TextView improvementText = dialogView.findViewById(R.id.improvement_text);
        TextView transcriptionText = dialogView.findViewById(R.id.transcription_text);

        // Null safety checks
        if (scoreText == null || accuracyText == null || mistakesText == null ||
                improvementText == null || transcriptionText == null) {
            return;
        }

        // Set score with emoji
        String scoreEmoji = analysis.score >= 90 ? "🏆" : analysis.score >= 80 ? "🎉" :
                analysis.score >= 70 ? "👍" : analysis.score >= 60 ? "🙂" : "💪";
        scoreText.setText(scoreEmoji + " Your Score: " + analysis.score + "/100");

        // Set accuracy
        accuracyText.setText("📊 Word Accuracy: " + analysis.correctWords + "/" + analysis.totalWords +
                " (" + (analysis.totalWords > 0 ? (analysis.correctWords * 100 / analysis.totalWords) : 100) + "%)");

        // Set transcription
        transcriptionText.setText("🎤 What you said: \"" + userTranscription + "\"");

        // Set mistakes
        StringBuilder mistakes = new StringBuilder("❌ Issues Found:\n");
        if (analysis.missingWords.isEmpty() && analysis.extraWords.isEmpty()) {
            mistakes.append("• No major word errors detected! 🎯");
        } else {
            if (!analysis.missingWords.isEmpty()) {
                mistakes.append("• Missing words: ").append(String.join(", ", analysis.missingWords)).append("\n");
            }
            if (!analysis.extraWords.isEmpty()) {
                mistakes.append("• Extra words: ").append(String.join(", ", analysis.extraWords)).append("\n");
            }
        }
        mistakesText.setText(mistakes.toString());

        // Set improvement tips
        String improvements = generateImprovementTips(analysis);
        improvementText.setText("💡 Tips for Improvement:\n" + improvements);

        try {
            builder.setView(dialogView)
                    .setTitle("🎯 Pronunciation Results")
                    .setPositiveButton("Try Again", (dialog, which) -> {
                        if (instructionTextView != null) {
                            instructionTextView.setText("🔊 Tap 'Play' to hear pronunciation\n🎙️ Tap 'Record' to practice speaking");
                        }
                    })
                    .setNeutralButton("New Sentence", (dialog, which) -> generateRandomSentence())
                    .show();
        } catch (Exception e) {
            // Handle dialog creation errors
            Toast.makeText(getContext(), "Score: " + analysis.score + "/100", Toast.LENGTH_LONG).show();
        }
    }

    private String generateImprovementTips(PronunciationAnalysis analysis) {
        StringBuilder tips = new StringBuilder();

        if (analysis.score >= 90) {
            tips.append("• Excellent pronunciation! Keep practicing! 🌟\n");
            tips.append("• Try more complex sentences to challenge yourself\n");
        } else if (analysis.score >= 80) {
            tips.append("• Great job! Focus on clearer articulation\n");
            tips.append("• Practice speaking slightly slower\n");
        } else if (analysis.score >= 70) {
            tips.append("• Good effort! Work on word pronunciation\n");
            tips.append("• Listen to the audio again before recording\n");
        } else if (analysis.score >= 60) {
            tips.append("• Keep practicing! Focus on each word clearly\n");
            tips.append("• Try speaking in a quieter environment\n");
        } else {
            tips.append("• Don't give up! Practice makes perfect\n");
            tips.append("• Speak slowly and clearly\n");
            tips.append("• Make sure your microphone is working well\n");
        }

        if (!analysis.missingWords.isEmpty()) {
            tips.append("• Pay attention to these words: ").append(String.join(", ", analysis.missingWords)).append("\n");
        }

        return tips.toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Properly cleanup TTS
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        // Properly cleanup Speech Recognizer
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception e) {
                // Handle any cleanup exceptions
            }
            speechRecognizer = null;
        }

        // Cleanup HTTP client
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    // Analysis result class
    private static class PronunciationAnalysis {
        int score;
        double similarity;
        List<String> missingWords;
        List<String> extraWords;
        int correctWords;
        int totalWords;

        PronunciationAnalysis(int score, double similarity, List<String> missingWords,
                              List<String> extraWords, int correctWords, int totalWords) {
            this.score = score;
            this.similarity = similarity;
            this.missingWords = missingWords;
            this.extraWords = extraWords;
            this.correctWords = correctWords;
            this.totalWords = totalWords;
        }
    }
}