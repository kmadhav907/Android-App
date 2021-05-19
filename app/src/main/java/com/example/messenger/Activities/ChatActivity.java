package com.example.messenger.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.messenger.Adapters.MessagesAdapter;
import com.example.messenger.Models.Message;
import com.example.messenger.databinding.ActivityChatBinding;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom , receiverRoom;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String senderUid;
    String receiverUid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);

        String name = getIntent().getStringExtra("name");
        receiverUid = getIntent().getStringExtra("uid");
        senderUid = FirebaseAuth.getInstance().getUid();
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        messages = new ArrayList<>();
        adapter = new MessagesAdapter(this , messages,senderRoom,receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        storage = FirebaseStorage.getInstance();
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
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent , 25);
            }
        });

        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 25){
            if(data != null){
                if(data.getData() != null){
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();

                                        Date date = new Date();

                                        String messageText = binding.messageBox.getText().toString();
                                        Message message = new Message(messageText,senderUid, date.getTime());
                                        message.setMessage("photo");
                                        message.setImageUrl(filePath);
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
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}