package com.onesight.uqac.onesight.controller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.model.User;
import com.onesight.uqac.onesight.model.UserInfo;
import com.onesight.uqac.onesight.view.DatePickerFragment;
import static com.onesight.uqac.onesight.controller.InputValidationHelper.*;

/**
 * FIRST PHASE OF REGISTRATION: EMAIL, PASSWORD, NAME, SURNAME AND BIRTH DATE FILLING,
 * ACCOUNT CREATION IN FIREBASE AUTHENTICATION + USER ID IN DATABASE.
 */
public class RegistrationActivity extends AppCompatActivity
implements com.onesight.uqac.onesight.view.OnCompleteListener
{
    private static final String TAG = "RegistrationActivity";

    private EditText mNameEditField;
    private EditText mSurnameEditField;
    private EditText mEmailEditField;
    private EditText mPasswordEditField;
    private Button mBirthDateBtn;
    private TextView mBirthDateTv;
    private String birthDate;

    // Firebase
    private FirebaseAuth mAuth;

    /* Get birth date from DatePicker */
    @Override
    public void onComplete(String date)
    {
        birthDate = date;
        String [] birthDate = date.split("-");
        final String text = getResources().getString(R.string.birth_date) + birthDate[2] + "/"
                + birthDate[1] + "/" + birthDate[0];
        mBirthDateTv.setText(text);
    }

    // attach to an onclick handler to show the date picker
    public void showDatePickerDialog(View v)
    {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

    /**
     * Saves user information in a bundle to pass to the next activity.
     *
     * @return Bundle for next activity.
     */
    private Bundle gatherUserInfo()
    {
        Bundle userInfo = new Bundle();
        userInfo.putString(UserInfo.USER_NAME.getInfo(), mNameEditField.getText().toString());
        userInfo.putString(UserInfo.USER_SURNAME.getInfo(), mSurnameEditField.getText().toString());
        userInfo.putString(UserInfo.USER_BIRTHDATE.getInfo(), birthDate);

        return userInfo;
    }

    /**
     * Create user's account into Firebase Authentication system.
     *
     * @param email user's provided email
     * @param password user's provided password
     */
    private void createAccount(String email, String password)
    {
        Log.d(TAG, "createAccount: " + email);

        if (!validateRegistrationForm())
        {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            Log.d(TAG, "createUserWithEmail: success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            updateActivity(user); // go to next activity
                        }
                        else
                        {
                            Log.w(TAG, "createUserWithEmail: failure", task.getException());
                            Toast.makeText(RegistrationActivity.this,
                                    getResources().getString(R.string.auth_failed),
                                    Toast.LENGTH_SHORT).show();

                            updateActivity(null); // return to homepage
                        }
                    }
                });
    }

    /**
     * If sign in succeeded, goes to next registration activity. Returns to homepage activity
     * otherwise.
     *
     * @param user Firebase user
     */
    private void updateActivity(FirebaseUser user)
    {
        if (user == null)
        {
            Intent firstScreenActivityIntent = new Intent(RegistrationActivity.this,
                    FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
        else
        {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            // create new user in Firebase database
            User newUser = new User();
            ref.child(user.getUid()).setValue(newUser);
            Intent sexActivityIntent =
                    new Intent(RegistrationActivity.this, SexActivity.class);
            sexActivityIntent.putExtras(gatherUserInfo());

            startActivity(sexActivityIntent);
        }
    }

    /**
     * Checks if the email, name, surname, password and birth date fields are filled correctly.
     *
     * @return true if everything is OK, false otherwise.
     */
    private boolean validateRegistrationForm()
    {
        boolean valid = true;

        String email = mEmailEditField.getText().toString();
        if (TextUtils.isEmpty(email) || !isValidEmail(email))
        {
            mEmailEditField.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mEmailEditField.setError(null);
        }

        String name = mNameEditField.getText().toString();
        if (TextUtils.isEmpty(name) || !isValidName(name))
        {
            mNameEditField.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mNameEditField.setError(null);
        }

        String surname = mSurnameEditField.getText().toString();
        if (TextUtils.isEmpty(surname) || !isValidName(surname))
        {
            mSurnameEditField.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mSurnameEditField.setError(null);
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


        if (birthDate == null)
        {
            mBirthDateBtn.setError(getResources().getString(R.string.required));
            valid = false;
        }
        else
        {
            mBirthDateBtn.setError(null);
        }

        return valid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // UI elements initialization
        Button validateBtn = findViewById(R.id.validate_name);
        mBirthDateBtn = findViewById(R.id.birthDate_btn);
        mBirthDateTv = findViewById(R.id.birthDate);
        mNameEditField = findViewById(R.id.name);
        mSurnameEditField = findViewById(R.id.surname);
        mEmailEditField = findViewById(R.id.email);
        mPasswordEditField = findViewById(R.id.password);

        // FIREBASE AUTH INSTANCIATION
        mAuth = FirebaseAuth.getInstance();

        // Launches date picker when button is clicked
        mBirthDateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showDatePickerDialog(view);
            }
        });

        validateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (validateRegistrationForm())
                {
                    // Add account to Firebase authentication
                    createAccount(mEmailEditField.getText().toString(),
                            mPasswordEditField.getText().toString());
                }
                else
                {
                    Toast.makeText(RegistrationActivity.this,
                            getResources().getString(R.string.please_fill), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }
}
