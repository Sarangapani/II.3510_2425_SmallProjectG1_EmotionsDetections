package com.example.moodanalyzerapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore; // Reference to Firestore
    private Interpreter tflite;
    private ImageView imageView;
    private TextView suggestionTextView; // Declare TextView for suggestions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Initialize Firestore
        imageView = findViewById(R.id.imageView);
        suggestionTextView = findViewById(R.id.suggestionTextView); // Initialize TextView

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile("fer_model.tflite"));
        } catch (IOException e) {
            Log.e("MainActivity", "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        Button captureButton = findViewById(R.id.captureButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button chatButton = findViewById(R.id.chatButton);

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        captureButton.setOnClickListener(v -> openCamera());
        logoutButton.setOnClickListener(v -> logoutUser());
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            detectEmotion(imageBitmap);
        }
    }

    private void detectEmotion(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
        float[][][][] input = new float[1][48][48][1];

        for (int i = 0; i < 48; i++) {
            for (int j = 0; j < 48; j++) {
                int pixel = resizedBitmap.getPixel(j, i);
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                input[0][i][j][0] = (0.299f * r + 0.587f * g + 0.114f * b);
            }
        }

        float[][] output = new float[1][7];
        tflite.run(input, output);

        int predictedIndex = argMax(output[0]);
        String emotion = mapIndexToEmotion(predictedIndex);

        suggestActivity(emotion);  // Display suggestion in TextView
        storeDecision(emotion);     // Store decision in Firestore
        imageView.setImageBitmap(bitmap);
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private String mapIndexToEmotion(int index) {
        switch (index) {
            case 0: return "Angry";
            case 1: return "Disgust";
            case 2: return "Fear";
            case 3: return "Happy";
            case 4: return "Sad";
            case 5: return "Surprise";
            case 6: return "Neutral";
            default: return "Unknown";
        }
    }

    private void suggestActivity(String emotion) {
        String suggestion;
        switch (emotion) {
            case "Happy":
                suggestion = "Keep smiling! Listen to your favorite music.";
                break;
            case "Sad":
                suggestion = "Consider talking to a friend or going for a walk.";
                break;
            case "Angry":
                suggestion = "Try some deep breathing exercises.";
                break;
            case "Surprise":
                suggestion = "Take a moment to enjoy the unexpected.";
                break;
            default:
                suggestion = "How about meditating or doing something relaxing?";
        }

        suggestionTextView.setText(suggestion);
        suggestionTextView.setVisibility(View.VISIBLE);
    }

    private void storeDecision(String emotion) {
        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(timestamp));

        Map<String, Object> analysisData = new HashMap<>();
        analysisData.put("mood", emotion);
        analysisData.put("timestamp", formattedDate);

        // Store the data in Firestore under moodAnalysis -> userId
        firestore.collection("moodAnalysis")
                .document(userId)
                .set(analysisData) // Using set to overwrite or create the document
                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Analysis saved successfully!"))
                .addOnFailureListener(e -> Log.e("MainActivity", "Error saving analysis: " + e.getMessage()));
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
