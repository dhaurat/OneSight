package com.onesight.uqac.onesight.controller;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.model.User;
import com.onesight.uqac.onesight.model.UserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class FirstScreenActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    LoginButton loginButton;
    CallbackManager callbackManager;
    private static final String TAG = "FacebookLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        setContentView(R.layout.activity_first_screen);


        // Buttons
        Button mConnectBtn = findViewById(R.id.connect);
        //Button mFBConnectBtn = findViewById(R.id.connect_fb);

        //Facebook button
        callbackManager = CallbackManager.Factory.create();
        loginButton = findViewById(R.id.fb_login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_birthday", "user_friends"));
        Button mRegisterBtn = findViewById(R.id.register);


        // AUTH
        // FIREBASE INITIALIZATION
        mAuth = FirebaseAuth.getInstance();

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registrationActivityIntent =
                        new Intent(FirstScreenActivity.this,
                                RegistrationActivity.class);
                startActivity(registrationActivityIntent);
            }
        });

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailSignInActivityIntent =
                        new Intent(FirstScreenActivity.this,
                                EmailSignInActivity.class);
                startActivity(emailSignInActivityIntent);
            }
        });


        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // [START_EXCLUDE]
                // [END_EXCLUDE]
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                // [START_EXCLUDE]
                // [END_EXCLUDE]
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null).
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent homepageActivityIntent =
                    new Intent(FirstScreenActivity.this, HomepageActivity.class);
            startActivity(homepageActivityIntent);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateActivity(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(FirstScreenActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateActivity(null);
                        }

                        // ...
                    }
                });
    }

    private void updateActivity(FirebaseUser user) {
        if (user == null) {
            Intent firstScreenActivityIntent =
                    new Intent(FirstScreenActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            // create new user in Firebase database
            User newUser = new User();
            ref.child(user.getUid()).setValue(newUser);
            getFbInfo();
        }
    }

    private void getFbInfo() {

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        // Application code

                        try {

                            Log.d(TAG, object.toString());

                            Bundle user_info = new Bundle();

                            user_info.putString(UserInfo.USER_NAME.getInfo(), object.getString("last_name"));
                            user_info.putString(UserInfo.USER_SURNAME.getInfo(), object.getString("first_name"));
                            user_info.putString(UserInfo.USER_BIRTHDATE.getInfo(), object.getString("birthday"));


                            Intent sexActivityIntent =
                                    new Intent(FirstScreenActivity.this, SexActivity.class);
                            sexActivityIntent.putExtras(user_info);
                            startActivity(sexActivityIntent);
                        }

                        catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name, last_name, birthday");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}