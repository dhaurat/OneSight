package com.onesight.uqac.onesight.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.view.DeleteAccountAlertFragment;

import java.util.HashMap;
import java.util.Map;

import static com.onesight.uqac.onesight.controller.InputValidationHelper.isValidName;

import com.onesight.uqac.onesight.model.UserInfo;

public class EditAccountActivity extends AppCompatActivity implements  Authentication,
        DeleteAccountAlertFragment.IDeleteAccountAlertListener {

    private final String TAG = "EditAccountActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;

    //private EditText mEmailEditField;
    //private EditText mPasswordEditField;
    private EditText mNameEditField;
    private EditText mSurnameEditField;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.sign_out_menu:
            {
                // sign out
                mAuth.signOut();
                checkUserLogIn(mAuth.getCurrentUser());
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * If no user is logged in, got to FirstScreenActivity.
     */
    @Override
    public void checkUserLogIn(FirebaseUser user)
    {
        if (user == null)
        {
            Intent firstScreenActivityIntent =
                    new Intent(EditAccountActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
    }

    /**
     * The user is asked his/her password before performing any changes on his/her account.
     */
    /*
    private void showPasswordPromptDialog()
    {
        ReAuthenticationFragment dialogFragment = new ReAuthenticationFragment();
        dialogFragment.setIReAuthenticationListener(this);
        dialogFragment.show(getFragmentManager(), "passwordPrompt");
    }
    */

    /**
     * Deletes user.
     */
    @Override
    public void onAccept()
    {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null)
        {
            user.delete().addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if (task.isSuccessful())
                    {
                        SharedPreferences settings =
                                getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                        MODE_PRIVATE);
                        SharedPreferences.Editor e = settings.edit();
                        e.clear();
                        e.apply();
                        Log.d(TAG, "User account deleted.");
                        Intent homePageActivityIntent =
                                new Intent(EditAccountActivity.this,
                                        HomepageActivity.class);
                        startActivity(homePageActivityIntent);
                    }
                    else
                    {
                        Log.d(TAG, "Delete failed.", task.getException());
                    }
                }
            });
        }

    }

    public void showDeleteAccountDialog()
    {
        DeleteAccountAlertFragment newFragment = new DeleteAccountAlertFragment();
        newFragment.setIDeleteAccountAlertListener(this);
        newFragment.show(getFragmentManager(), "deleteAccount");
    }

    /*
    @Override
    public void onPasswordEntered(String userInput)
    {
        FirebaseUser user = mAuth.getCurrentUser();

        if (userInput == null)
        {
            checkUserLogIn(null);
        }
        else if (user != null && user.getEmail() != null)
        {

            // Gets auth credentials from the user for re-authentication.
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), userInput);

            // Prompts the user to re-provide his/her sign-in credentials
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                Log.d(TAG, "User re-authenticated.");
                            }
                            else
                            {
                                Toast.makeText(EditAccountActivity.this,
                                        getResources().getString(R.string.wrong_credentials),
                                        Toast.LENGTH_LONG).show();
                                checkUserLogIn(null);
                                Log.d(TAG, "Re-authentication failed", task.getException());
                            }
                        }
                    });
        }
    }*/

    /**
     * Checks if the email, name, surname, password and birth date fields are filled correctly.
     *
     * @return true if everything is OK, false otherwise.
     */
    private boolean validateAccountForm()
    {
        boolean valid = true;

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null)
        {
            /*String email = mEmailEditField.getText().toString();
            if (!TextUtils.isEmpty(email))
            {
                if (isValidEmail(email))
                {
                    user.updateEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if (task.isSuccessful())
                                    {
                                        Log.d(TAG, "User email address updated.");
                                    }
                                    else {
                                        Log.d(TAG, "Email update failed.",
                                                task.getException());
                                    }
                                }
                            });
                }
                else
                {
                    mEmailEditField.setError(getResources().getString(R.string.required));
                    valid = false;
                }
            }*/

            String name = mNameEditField.getText().toString();
            if (!TextUtils.isEmpty(name))
            {
                if (isValidName(name))
                {
                    // Save in database
                    Map<String, Object> result = new HashMap<>();
                    result.put(UserInfo.USER_NAME.getInfo(), mNameEditField.getText().toString());
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                            .child("users").child(user.getUid());
                    mDatabaseReference.updateChildren(result);

                    // Save locally
                    SharedPreferences mSharedPreferences =
                            getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                    MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                    mEditor.putString(UserInfo.USER_NAME.getInfo(), name);
                    mEditor.apply();

                    Log.d(TAG, "NAME UPDATED: " + name);
                }
                else
                {
                    mNameEditField.setError(getResources().getString(R.string.required));
                    valid = false;
                }
            }

            String surname = mSurnameEditField.getText().toString();
            if (!TextUtils.isEmpty(surname))
            {
                if (isValidName(surname))
                {
                    Map<String, Object> result = new HashMap<>();
                    result.put(UserInfo.USER_SURNAME.getInfo(),
                            mSurnameEditField.getText().toString());
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                            .child("users").child(user.getUid());
                    mDatabaseReference.updateChildren(result);

                    SharedPreferences mSharedPreferences =
                            getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                    MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                    mEditor.putString(UserInfo.USER_SURNAME.getInfo(), surname);
                    mEditor.apply();

                    Log.d(TAG, "SURNAME UPDATED: " + surname);

                }
                else
                {
                    mSurnameEditField.setError(getResources().getString(R.string.required));
                    valid = false;
                }
            }
            /*
            String password = mPasswordEditField.getText().toString();
            if (!TextUtils.isEmpty(password))
            {
                if (isValidPassword(password))
                {
                    user.updatePassword(password)
                            .addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "User password updated.");
                                    }
                                    else {
                                        Log.d(TAG, "Password update failed.",
                                                task.getException());
                                    }
                                }
                            });
                }
                else {
                    mPasswordEditField.setError(getResources().getString(R.string.required));
                    valid = false;
                }
            }*/
        } //END field validation

        return valid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        // Toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // FIREBASE INITIALIZATION
        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // UI
        //mEmailEditField = findViewById(R.id.edit_email);
        //mPasswordEditField = findViewById(R.id.edit_password);
        mNameEditField = findViewById(R.id.edit_name);
        mSurnameEditField = findViewById(R.id.edit_surname);
        Button deleteAccountBtn = findViewById(R.id.suppress_account);


        deleteAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null)
                {
                    showDeleteAccountDialog();
                }
            }
        });

        Button validateBtn = findViewById(R.id.validate_params);

        validateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (validateAccountForm())
                {
                    Toast.makeText(EditAccountActivity.this,
                            getResources().getString(R.string.info_updated), Toast.LENGTH_SHORT)
                            .show();
                    Intent homepageActivityIntent =
                            new Intent(EditAccountActivity.this,
                                    HomepageActivity.class);
                    startActivity(homepageActivityIntent);
                }
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        checkUserLogIn(mAuth.getCurrentUser());
        //showPasswordPromptDialog();
    }
}
