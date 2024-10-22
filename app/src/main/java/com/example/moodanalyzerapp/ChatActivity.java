package com.example.moodanalyzerapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private EditText messageInput;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

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

        databaseReference = FirebaseDatabase.getInstance().getReference("chats");
        loadChatMessages();

        mAuth = FirebaseAuth.getInstance();
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageInput.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    long timestamp = System.currentTimeMillis();
                    ChatMessage userChatMessage = new ChatMessage(userMessage, "You", timestamp);
                    messages.add(userChatMessage);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                    messageInput.setText("");

                    databaseReference.push().setValue(userChatMessage).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Sent message: " + userChatMessage.getMessage());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message: " + e.getMessage());
                    });

                    String uid = mAuth.getCurrentUser().getUid(); // Get the unique user ID
                    String apiUrl = "http://api.brainshop.ai/get?bid=183471&key=MyTrsbEGBm0YY25h&uid=" + uid + "&msg=" + userMessage;

                    new SendMessageTask(apiUrl).execute();
                }
            }
        });
    }

    private void loadChatMessages() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        messages.add(chatMessage);
                        Log.d(TAG, "Loaded message: " + chatMessage.getMessage());
                    }
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors
            }
        });
    }

    private class SendMessageTask extends AsyncTask<Void, Void, String> {
        private String apiUrl;

        SendMessageTask(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);  // Set connection timeout to 10 seconds
                connection.setReadTimeout(10000);     // Set read timeout to 10 seconds
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return response.toString();
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Connection timed out", e);
                return "(Time out)";  // Return a timeout message
            } catch (IOException e) {
                Log.e(TAG, "Error fetching response from Brainshop", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d(TAG, "Raw response: " + result);
                if (result.startsWith("{") && result.endsWith("}")) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        String botReply = jsonObject.getString("cnt");
                        long timestamp = System.currentTimeMillis();
                        ChatMessage botChatMessage = new ChatMessage(botReply, "Bot", timestamp);
                        messages.add(botChatMessage);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                        databaseReference.push().setValue(botChatMessage);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Unexpected response format: " + result);
                    Toast.makeText(ChatActivity.this, "Unexpected response format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to get response from Brainshop");
                Toast.makeText(ChatActivity.this, "Failed to get response from Brainshop", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
