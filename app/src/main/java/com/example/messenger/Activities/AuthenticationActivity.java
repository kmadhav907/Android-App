package com.example.messenger.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.messenger.databinding.ActivityAuthenticationBinding;
import com.google.firebase.auth.FirebaseAuth;

public class AuthenticationActivity extends AppCompatActivity {
    ActivityAuthenticationBinding binding;
    FirebaseAuth auth;
    final static String PHONE_NUMBER_KEY = "PHONE_NUMBER";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() != null){
            Intent intent = new Intent(AuthenticationActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        getSupportActionBar().hide();
        binding.phoneNumber.requestFocus();

        binding.phoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //passing phone number to otp activity
                Intent intent = new Intent(AuthenticationActivity.this, OTPActivity.class);
                intent.putExtra(PHONE_NUMBER_KEY , binding.phoneNumber.getText().toString());
                startActivity(intent);
            }
        });
    }
}