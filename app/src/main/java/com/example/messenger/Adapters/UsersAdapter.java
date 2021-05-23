package com.example.messenger.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messenger.Activities.ChatActivity;
import com.example.messenger.Models.User;
import com.example.messenger.R;
import com.example.messenger.databinding.RowConversationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UsersAdapter extends  RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    Context context;
    ArrayList<User> users;
    public  UsersAdapter(Context context, ArrayList<User> users){
    this.context = context;
    this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();

        FirebaseDatabase.getInstance().getReference()
                .child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            String lastMessage = snapshot.child("lastMessage").getValue(String.class);
                            long lastMessageTime = snapshot.child("lastMessageTime").getValue(Long.class);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                            holder.binding.msgTime.setText(dateFormat.format(new Date(lastMessageTime)));
                            holder.binding.lastMsg.setText(lastMessage);

                        }
                        else{
                            holder.binding.lastMsg.setText("tap to chat");
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });


        holder.binding.username.setText(user.getName());
        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.profile);
       holder.itemView.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent(context, ChatActivity.class);
               intent.putExtra("name", user.getName());
               intent.putExtra("image", user.getProfileImage());
               intent.putExtra("uid", user.getUid());
               context.startActivity(intent);
           }
       });
    }


    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder{
    RowConversationBinding binding;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowConversationBinding.bind(itemView);
        }
    }
}
