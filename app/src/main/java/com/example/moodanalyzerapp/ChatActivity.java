package com.example.moodanalyzerapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private EditText messageInput;
    private DatabaseReference databaseReference;
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

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("messages");

        // Load existing messages from Firebase
        loadMessages();

        // Initialize responses map
        responses = new HashMap<>();
        initializeResponses();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageInput.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    long timestamp = System.currentTimeMillis();
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // Create and save user message
                    ChatMessage userChatMessage = new ChatMessage(userMessage, userId, timestamp);
                    messages.add(userChatMessage);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                    messageInput.setText("");

                    // Save user message to Firebase
                    saveMessageToFirebase(userChatMessage);

                    new Handler().postDelayed(() -> {
                        String botReply = getBotReply(userMessage);
                        if (!botReply.isEmpty()) {
                            ChatMessage botChatMessage = new ChatMessage(botReply, "Bot", System.currentTimeMillis());
                            messages.add(botChatMessage);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                            saveMessageToFirebase(botChatMessage); // Save bot reply to Firebase
                        }
                    }, 1000); // 1 second delay
                }
            }
        });
    }

    private void saveMessageToFirebase(ChatMessage chatMessage) {
        String messageId = databaseReference.push().getKey();
        if (messageId != null) {
            databaseReference.child(messageId).setValue(chatMessage).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadMessages() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messages.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        messages.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeResponses() {
        responses.put("hello", "Hello! How can I assist you today?");
        responses.put("hi", "Hello! How can I assist you today?");
        responses.put("how are you", "I'm just a bot, but I'm here to help you!");
        responses.put("what is your name", "I am your friendly chatbot!");
        responses.put("help", "Sure! What do you need help with?");
        responses.put("bye", "Goodbye! Have a great day!");
        responses.put("thank you", "You're welcome! If you have more questions, feel free to ask.");
    }

    private String getBotReply(String userMessage) {
        String normalizedMessage = userMessage.toLowerCase();
        for (String keyword : responses.keySet()) {
            if (normalizedMessage.contains(keyword)) {
                return responses.get(keyword);
            }
        }
        return "I'm sorry, I didn't understand that. Can you please rephrase?";
    }
}
