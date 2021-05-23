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
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.messenger.Adapters.MessagesAdapter;
import com.example.messenger.Models.Message;
import com.example.messenger.R;
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
        setSupportActionBar(binding.toolbar);



        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        String name = getIntent().getStringExtra("name");
        receiverUid = getIntent().getStringExtra("uid");
        String profile = getIntent().getStringExtra("image");
        senderUid = FirebaseAuth.getInstance().getUid();
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        binding.name.setText(name);
        Glide.with(ChatActivity.this).load(profile).
                placeholder(R.drawable.avatar).into(binding.profile);

        messages = new ArrayList<>();
        adapter = new MessagesAdapter(this , messages,senderRoom,receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });



        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.getValue(String.class);
                    if(!status.isEmpty()) {
                        if(status.equals("Offline")){
                            binding.status.setVisibility(View.GONE);
                        }
                        else {
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);
                        }


                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
        final Handler handler = new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                database.getReference().child("presence").child(senderUid).setValue("typing...");

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping , 1000);

            }
            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid).setValue("Online");

                }
            };
        });

//        getSupportActionBar().setTitle(name);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
    protected void onResume() {
        super.onResume();
        String currentUid = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentUid).setValue("Online");
    }
    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}