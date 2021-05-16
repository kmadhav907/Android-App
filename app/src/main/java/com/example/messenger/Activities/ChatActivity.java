package com.example.messenger.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.example.messenger.Adapters.MessagesAdapter;
import com.example.messenger.Models.Message;
import com.example.messenger.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom , receiverRoom;
    FirebaseDatabase database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getIntent().getStringExtra("name");
        String receiverUid = getIntent().getStringExtra("uid");
        String senderUid = FirebaseAuth.getInstance().getUid();
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        adapter = new MessagesAdapter(this , messages,senderRoom,receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);


        database = FirebaseDatabase.getInstance();
        database.getReference().child("chats").child(senderRoom).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for(DataSnapshot snapshot1: snapshot.getChildren()){
                    Message message = snapshot1.getValue(Message.class);
                    message.setMessageId(snapshot1.getKey());
                    messages.add(message);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();

                String messageText = binding.messageBox.getText().toString();
                Message message = new Message(messageText,senderUid, date.getTime());
                binding.messageBox.setText("");
                String randomKey = database.getReference().push().getKey();
                HashMap<String,Object> lastMessageObj = new HashMap<>();
                lastMessageObj.put("lastMessage",message.getMessage());
                lastMessageObj.put("lastMessageTime",new Date().getTime());

                database.getReference()
                        .child("chats")
                        .child(senderRoom)
                        .updateChildren(lastMessageObj);

                database.getReference()
                        .child("chats")
                        .child(receiverRoom)
                        .updateChildren(lastMessageObj);

                database.getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey).setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });
                    }

                });
            }
        });

        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}