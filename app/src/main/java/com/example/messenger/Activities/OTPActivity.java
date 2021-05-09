package com.example.messenger.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.messenger.databinding.ActivityOTPBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mukesh.OnOtpCompletionListener;

import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {
    ActivityOTPBinding binding ;
    FirebaseAuth auth;
    String verificationId ;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOTPBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("sending otp ...");
        dialog.setCancelable(false);
        dialog.show();

        binding.otpView.requestFocus();
        auth = FirebaseAuth.getInstance();
        getSupportActionBar().hide();

        String phoneNumber = getIntent().getStringExtra(AuthenticationActivity.PHONE_NUMBER_KEY);
        binding.phoneLabel.setText("verify " + phoneNumber);

        //creating options for the server to send OTP to the phone
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(OTPActivity.this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                    }

                    // This callback is invoked in an invalid request for verification is made,
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Invalid request
                            Toast.makeText(OTPActivity.this,"invalid request",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    @Override
                    public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verifyId, forceResendingToken);
                        verificationId = verifyId;
                        dialog.dismiss();
                    }
                }).build();
        //initiating for the server to server to send OTP
        PhoneAuthProvider.verifyPhoneNumber(options);

        //after knowing the OTP,after filling the 6 digits completely, onOtpCompleted is called automatically
        binding.otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,otp);
                dialog.setMessage("verifying ...");
                dialog.show();
                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dialog.dismiss();
                        if(task.isSuccessful()){
                            Toast.makeText(OTPActivity.this,"Logged in",Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(OTPActivity.this, SetUserProfileActivity.class);
                            startActivity(intent);
                            finishAffinity();
                        }
                        else{
                            Toast.makeText(OTPActivity.this,"Failed",Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

    }
}