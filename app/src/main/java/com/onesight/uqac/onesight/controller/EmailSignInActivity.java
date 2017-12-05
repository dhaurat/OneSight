package com.onesight.uqac.onesight.controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.onesight.uqac.onesight.R;

public class EmailSignInActivity extends AppCompatActivity {

    private static final String TAG = "EmailPassword";

    private EditText mEmailEditField;
    private EditText mPasswordEditField;

    private FirebaseAuth mAuth;

    private void updateActivity(FirebaseUser user) {
        if (user == null) {
            Intent firstScreenActivityIntent =
                    new Intent(EmailSignInActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
        else {
            Intent homepageActivityIntent =
                    new Intent(EmailSignInActivity.this, HomepageActivity.class);
            startActivity(homepageActivityIntent);
        }
    }

    /**
     * Sign in user to Firebase authentication.
     *
     * @param email user's valid email.
     * @param password user's valid password.
     */
    private void signIn(String email, String password)
    {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail: success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateActivity(user);
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail: failure", task.getException());
                            Toast.makeText(EmailSignInActivity.this,
                                    getResources().getString(R.string.auth_failed),
                                    Toast.LENGTH_SHORT).show();
                            updateActivity(null);
                        }

                    }
                });
    }

    private boolean validateForm()
    {
        boolean valid = true;

        String email = mEmailEditField.getText().toString();
        if (TextUtils.isEmpty(email))
        {
            mEmailEditField.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mEmailEditField.setError(null);
        }

        String password = mPasswordEditField.getText().toString();
        if (TextUtils.isEmpty(password))
        {
            mPasswordEditField.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mPasswordEditField.setError(null);
        }

        return valid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mEmailEditField = findViewById(R.id.email);
        mPasswordEditField = findViewById(R.id.password);

        // FIREBASE INITIALIZATION
        mAuth = FirebaseAuth.getInstance();

        Button validateBtn = findViewById(R.id.validate);

        validateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateForm())
                {
                    signIn(mEmailEditField.getText().toString(),
                            mPasswordEditField.getText().toString());
                }
                else
                {
                    Toast.makeText(EmailSignInActivity.this,
                            getResources().getString(R.string.please_fill),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
