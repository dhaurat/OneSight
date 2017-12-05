package com.onesight.uqac.onesight.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.model.Sex;
import com.onesight.uqac.onesight.model.User;
import com.onesight.uqac.onesight.model.UserInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * SECOND PHASE OF USER REGISTRATION: GET USER'S SEXUAL PREFERENCES.
 */
public class SexActivity extends AppCompatActivity {

    // Buttons
    private RadioGroup mSexRg;
    private RadioButton mSexMaleRb;
    private RadioButton mSexFemaleRb;
    private RadioGroup mSearchedSexRg;
    private RadioButton mSearchedSexMaleRb;
    private RadioButton mSearchedSexFemaleRb;
    private RadioButton mSearchedSexBothRb;

    private Sex userSex;
    private Sex userSearchedSex;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabaseReference;

    /**
     * Checks if the form has been correctly filled and updates the sex and searched sex members
     * according to the user's choices.
     *
     * @return true if the form has been correctly filled.
     */
    boolean getAndValidatePreferencesForm()
    {
        boolean correct = true;

        int sexId = mSexRg.getCheckedRadioButtonId();
        int searchedSexId = mSearchedSexRg.getCheckedRadioButtonId();

        /* User's gender */
        if (sexId == mSexMaleRb.getId())
        {
            userSex = Sex.MALE;
        }

        else if (sexId == mSexFemaleRb.getId())
        {
            userSex = Sex.FEMALE;
        }

        else
        {
            correct = false;
        }

        /* User's sexual preferences */
        if (searchedSexId == mSearchedSexMaleRb.getId())
        {
            userSearchedSex = Sex.MALE;
        }

        else if (searchedSexId == mSearchedSexFemaleRb.getId())
        {
            userSearchedSex = Sex.FEMALE;
        }

        else if (searchedSexId == mSearchedSexBothRb.getId())
        {
            userSearchedSex = Sex.ALL;
        }

        else
        {
            correct = false;
        }

        return correct;
    }

    void updateActivity(FirebaseUser user)
    {
        if (user != null)
        {
            Intent homepageActivityIntent = new Intent(SexActivity.this,
                            HomepageActivity.class);
            startActivity(homepageActivityIntent);
        }
        else
        {
            Intent firstScreenActivityIntent = new Intent(SexActivity.this,
                            FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sex);

        // FIREBASE INITIALIZATION
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mDatabase.getReference();

        // Get user info from last activity
        final Bundle userInformation = getIntent().getExtras();
        if (userInformation == null)
        {
            Toast.makeText(SexActivity.this,
                    getResources().getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
            Intent firstScreenActivityIntent = new Intent(SexActivity.this,
                    FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
        else
        {
            mSexRg = findViewById(R.id.sex);
            mSexMaleRb = findViewById(R.id.sex_man);
            mSexFemaleRb = findViewById(R.id.sex_woman);
            mSearchedSexRg = findViewById(R.id.orientation);
            mSearchedSexMaleRb = findViewById(R.id.searched_sex_man);
            mSearchedSexFemaleRb = findViewById(R.id.searched_sex_woman);
            mSearchedSexBothRb = findViewById(R.id.searched_sex_both);
            Button validateBtn = findViewById(R.id.validate_registration);

            validateBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (getAndValidatePreferencesForm())
                    {
                        // SAVE USER PROFILE IN DEVICE MEMORY
                        SharedPreferences mSharedPreferences =
                                getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                        MODE_PRIVATE);
                        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                        mEditor.putString(UserInfo.USER_SURNAME.getInfo(),
                                userInformation.getString(UserInfo.USER_SURNAME.getInfo()));
                        mEditor.putString(UserInfo.USER_NAME.getInfo(),
                                userInformation.getString(UserInfo.USER_NAME.getInfo()));
                        mEditor.putString(UserInfo.USER_BIRTHDATE.getInfo(),
                                userInformation.getString(UserInfo.USER_BIRTHDATE.getInfo()));
                        mEditor.putString(UserInfo.USER_SEX.getInfo(), userSex.getString());
                        mEditor.putString(UserInfo.USER_ORIENTATION.getInfo(),
                                userSearchedSex.getString());
                        mEditor.apply();

                        // SAVE USER PROFILE IN FIREBASE DATABASE
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null)
                        {
                            User user = new User(
                                    userInformation.getString(UserInfo.USER_SURNAME.getInfo()),
                                    userInformation.getString(UserInfo.USER_NAME.getInfo()),
                                    userInformation.getString(UserInfo.USER_BIRTHDATE.getInfo()),
                                    userSex,
                                    userSearchedSex);

                            // Update process
                            String key = currentUser.getUid();
                            Map<String, Object> userValues = user.toMap();
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/users/" + key, userValues);

                            mUsersDatabaseReference.updateChildren(childUpdates);

                            Toast.makeText(SexActivity.this,
                                    getResources().getString(R.string.new_user)
                                            + user.getName() + " " + user.getSurname() + ", "
                                            + user.getSex().toString() + ", "
                                            + user.getSearchedSex().toString() + " ",
                                    Toast.LENGTH_LONG).show();

                            // Update activity
                            updateActivity(currentUser);
                        }
                        else
                        {
                            updateActivity(null);
                        }
                    }
                    else
                    {
                        Toast.makeText(SexActivity.this,
                                getResources().getString(R.string.please_fill),
                                Toast.LENGTH_SHORT).show();
                    }
                }

            });
        }
    }
}