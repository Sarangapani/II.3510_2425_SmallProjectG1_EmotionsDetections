package com.example.moodanalyzerapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log; // Import Log class
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity"; // Tag for logging
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages; // List of chat messages
    private EditText messageInput;
    private DatabaseReference databaseReference; // Firebase Database reference

    // A map to hold user queries and responses
    private Map<String, String> responses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        Button sendButton = findViewById(R.id.sendButton);

        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messages);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("chats");

        // Load existing chat messages from Firebase
        loadChatMessages();

        // Initialize responses map
        responses = new HashMap<>();
        initializeResponses();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageInput.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    long timestamp = System.currentTimeMillis(); // Get current timestamp
                    ChatMessage userChatMessage = new ChatMessage(userMessage, "You", timestamp);
                    messages.add(userChatMessage); // Add user message
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                    messageInput.setText(""); // Clear the input field

                    // Save user message to Firebase
                    databaseReference.push().setValue(userChatMessage).addOnSuccessListener(aVoid -> {
                        // Log the message sent
                        Log.d(TAG, "Sent message: " + userChatMessage.getMessage());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message: " + e.getMessage());
                    });

                    // Generate a bot reply with a delay
                    new Handler().postDelayed(() -> {
                        String botReply = getBotReply(userMessage);
                        if (!botReply.isEmpty()) {
                            ChatMessage botChatMessage = new ChatMessage(botReply, "Bot", System.currentTimeMillis());
                            messages.add(botChatMessage); // Add bot reply
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            chatRecyclerView.scrollToPosition(messages.size() - 1);

                            // Save bot reply to Firebase
                            databaseReference.push().setValue(botChatMessage).addOnSuccessListener(aVoid -> {
                                // Log the bot reply
                                Log.d(TAG, "Received message: " + botChatMessage.getMessage());
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to send bot reply: " + e.getMessage());
                            });
                        }
                    }, 1000); // 1 second delay
                }
            }
        });
    }

    // Load existing chat messages from Firebase
    private void loadChatMessages() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear(); // Clear the current messages
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        messages.add(chatMessage);
                        // Log the loaded message
                        Log.d(TAG, "Loaded message: " + chatMessage.getMessage());
                    }
                }
                chatAdapter.notifyDataSetChanged(); // Notify adapter of data change
                chatRecyclerView.scrollToPosition(messages.size() - 1); // Scroll to the last message
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors
            }
        });
    }

    // Initialize responses for the bot
    private void initializeResponses() {
        responses.put("hello", "Hello! How can I assist you today?");
        responses.put("hi", "Hello! How can I assist you today?");
        responses.put("how are you", "I'm just a bot, but I'm here to help you!");
        responses.put("what is your name", "I am your friendly chatbot!");
        responses.put("help", "Sure! What do you need help with?");
        responses.put("bye", "Goodbye! Have a great day!");
        responses.put("thank you", "You're welcome! If you have more questions, feel free to ask.");
        // Add more responses as needed
    }

    // Enhanced bot reply logic
    private String getBotReply(String userMessage) {
        // Convert user message to lower case for matching
        String normalizedMessage = userMessage.toLowerCase();

        // Check for matching responses
        for (String keyword : responses.keySet()) {
            if (normalizedMessage.contains(keyword)) {
                return responses.get(keyword);
            }
        }

        // Default response if no match found
        return "I'm sorry, I didn't understand that. Can you please rephrase?";
    }
}
