package com.example.messenger.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.messenger.Adapters.TopStatusAdaptor;
import com.example.messenger.Models.Status;
import com.example.messenger.Models.UserStatus;
import com.example.messenger.R;
import com.example.messenger.Models.User;
import com.example.messenger.Adapters.UsersAdapter;
import com.example.messenger.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    TopStatusAdaptor topStatusAdaptor;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog progressDialog;
    User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("uploading image...");
        progressDialog.setCancelable(false);

        users = new ArrayList<>();
        userStatuses = new ArrayList<>();
        usersAdapter = new UsersAdapter(this, users);

        topStatusAdaptor = new TopStatusAdaptor(this,userStatuses);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.statusList.setLayoutManager(linearLayoutManager);

        binding.recyclerView.setAdapter(usersAdapter);
        binding.statusList.setAdapter(topStatusAdaptor);

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        //get stories from database and update the stories list
        database.getReference().
                child("stories")
                .addValueEventListener(new ValueEventListener() {
                    @Override

                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            userStatuses.clear();

                            for (DataSnapshot storySnapshot : snapshot.getChildren()){

                                UserStatus userStatus = new UserStatus();
                                userStatus.setName(storySnapshot.child("name").getValue(String.class));
                                userStatus.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                                userStatus.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));
                                userStatus.setUid(storySnapshot.child("uid").getValue(String.class));

                                ArrayList<Status> perStatues = new ArrayList<>();

                                for(DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()){
                                    Status sampleStatus = statusSnapshot.getValue(Status.class);
                                    perStatues.add(sampleStatus);
                                }
                                userStatus.setStatuses(perStatues);
                                userStatuses.add(userStatus);

                            }
                            topStatusAdaptor.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        binding.bottomNavigationView2.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent,75);
                        break;
                }
                return false;
            }
        });

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    User user = snapshot1.getValue(User.class);
                    if(!user.getUid().equals(FirebaseAuth.getInstance().getUid()))
                        users.add(user);
                }
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    //creating status and uploading to database
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            if(data.getData() != null){
                progressDialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");
                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(currentUser.getName());
                                    userStatus.setProfileImage(currentUser.getProfileImage());
                                    userStatus.setUid(currentUser.getUid());
                                    userStatus.setLastUpdated(date.getTime());
                                    HashMap<String,Object> obj= new HashMap<>();
                                    obj.put("name",userStatus.getName());
                                    obj.put("uid",userStatus.getUid());
                                    obj.put("profileImage",userStatus.getProfileImage());
                                    obj.put("lastUpdated",userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl,userStatus.getLastUpdated());

                                    database.getReference()
                                            .child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(obj);

                                    database.getReference().
                                            child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    progressDialog.dismiss();
                                }
                            });
                        }
                    }
                });
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
        String currentUid = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentUid).setValue("Offline");
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.search:
                Toast.makeText(this,"Search Clicked", Toast.LENGTH_LONG).show();
                break;
            case R.id.settings:
                Toast.makeText(this,"Settings Clicked", Toast.LENGTH_LONG).show();
                break;
            case R.id.invite:
                Toast.makeText(this, "Invite Clicked", Toast.LENGTH_LONG).show();
                break;
            case R.id.groups:
                Toast.makeText(this, "Groups Clicked", Toast.LENGTH_LONG).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}