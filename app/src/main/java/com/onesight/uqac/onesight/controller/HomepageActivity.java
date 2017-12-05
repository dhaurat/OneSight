package com.onesight.uqac.onesight.controller;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.model.User;
import com.onesight.uqac.onesight.model.UserInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.onesight.uqac.onesight.controller.BitmapHandlingHelper.*;
import static com.onesight.uqac.onesight.controller.DistanceCalculationHelper.isCloseEnough;

/**
 * HOME PAGE, WHERE USER CAN CHOOSE TO MODIFY ACCOUNT/PROFILE DETAILS, AND ACTIVATE OR NOT
 * GEOLOCATION.
 */
public class HomepageActivity extends AppCompatActivity implements Authentication {

    private final static String TAG = "HomepageActivity";

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mFirebaseUser;
    private User myUser;
    private String photoURI;

    private TextView mName;
    private ImageView mProfilePhoto;

    // Database
    private DatabaseReference mDatabaseReference;
    private StorageReference mStorageReference;
    private SharedPreferences mSharedPreferences;


    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private Boolean mRequestingLocationUpdates = false;
    private LocationCallback mLocationCallback;

    //private TextView mLatitudeText;
    //private TextView mLongitudeText;
    //private TextView mLastUpdateTimeText;
    private Button mConnectAppBtn;

    private static final int REQUEST_CHECK_SETTINGS = 518;
    private static final int REQUEST_CHECK_PERMISSIONS = 519;

    /**
     * Create toolbar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Overflow menu to sign out.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                // sign out
                mFirebaseAuth.signOut();
                checkUserLogIn(mFirebaseAuth.getCurrentUser());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * If no user is logged in, got to FirstScreenActivity.
     */
    @Override
    public void checkUserLogIn(FirebaseUser user) {
        if (user == null) {
            Intent firstScreenActivityIntent =
                    new Intent(HomepageActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
            Log.d(TAG, "User is signed out");
        }
        else {
            Log.d(TAG, "User is signed in: " + user.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mName = findViewById(R.id.username);
        mProfilePhoto = findViewById(R.id.profile_photo);

        // FIREBASE INITIALIZATION
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").
                child(mFirebaseUser.getUid());

        mAuthStateListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                checkUserLogIn(mFirebaseUser);
            }
        };

        /////////////////////////////////////////////////
        /// GETTING AND DISPLAYING USER DATA
        /////////////////////////////////////////////////

        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                myUser = dataSnapshot.getValue(User.class);

                /* CATCH USER INFO FROM FIREBASE DATABASE */
                if (myUser != null)
                {
                    System.out.println("MY USER IS NOT NULL");

                    String username = myUser.getSurname() + " " + myUser.getName();
                    mName.setText(username);

                    FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();
                    mStorageReference = mFirebaseStorage.getReference().child("profile_photos")
                    .child(mFirebaseUser.getUid());

                    final File localFile;
                    try
                    {
                        localFile = File.createTempFile("images", "jpg");
                        mStorageReference.getFile(localFile).addOnSuccessListener(
                                new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                                {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot
                                                                  taskSnapshot)
                                    {
                                        // Local temp file has been created
                                        setCroppedBitmap(localFile.getAbsolutePath(),
                                                mProfilePhoto);
                                    }
                                }
                        );
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    // Update user info in device from Firebase
                    mSharedPreferences =
                            getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                    MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                    mEditor.putString(UserInfo.USER_SURNAME.getInfo(), myUser.getSurname());
                    mEditor.putString(UserInfo.USER_NAME.getInfo(), myUser.getName());
                    mEditor.putString(UserInfo.USER_BIRTHDATE.getInfo(), myUser.getBirthDate());
                    mEditor.putString(UserInfo.USER_SEX.getInfo(), myUser.getSex().toString());
                    mEditor.putString(UserInfo.USER_ORIENTATION.getInfo(),
                            myUser.getSearchedSex().toString());
                    mEditor.apply();
                }
                else
                {
                    System.out.println("MY USER IS NULL");


                    photoURI = mSharedPreferences.getString(UserInfo.USER_PHOTO.getInfo(),
                            null);
                    String username = mSharedPreferences.getString(UserInfo.USER_NAME.getInfo(),
                            null) + " " +
                            mSharedPreferences.getString(UserInfo.USER_SURNAME.getInfo(),
                                    null);
                    mName.setText(username);

                    if (photoURI != null)
                    {
                        setCroppedBitmap(photoURI, mProfilePhoto);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.e(TAG, "onCancelled databaseError");
            }
        });

        /////////////////////////////////////////////////
        /// GO TO OTHER ACTIVITIES
        /////////////////////////////////////////////////

        Button mAccountParams = findViewById(R.id.account_params);
        Button mProfileParams = findViewById(R.id.profile_params);

        mAccountParams.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent editAccountActivityIntent = new Intent(HomepageActivity.this,
                        EditAccountActivity.class);
                startActivity(editAccountActivityIntent);
            }
        });

        mProfileParams.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent editProfileActivityIntent = new Intent(HomepageActivity.this,
                        EditProfileActivity.class);
                startActivity(editProfileActivityIntent);
            }
        });

        /////////////////////////////////////////////////
        /// GEOLOCATION
        /////////////////////////////////////////////////

        checkDeviceSettings();
        updateValuesFromBundle(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mConnectAppBtn = findViewById(R.id.connect_app);
        if (!mRequestingLocationUpdates) {
            mConnectAppBtn.setText(R.string.start_geoloc);
        }
        else {
            mConnectAppBtn.setText(R.string.stop_geoloc);
        }
        mConnectAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRequestingLocationUpdates) {
                    mConnectAppBtn.setText(R.string.stop_geoloc);
                    startLocationUpdates();
                }
                else {
                    mConnectAppBtn.setText(R.string.start_geoloc);
                    stopLocationUpdates();
                }
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {

                    if (mFirebaseUser != null)
                    {
                        /*
                        final String FIREBASE_FUNCTION_BASE_URL
                                = "https://us-central1-uqac-onesight.cloudfunctions.net/" +
                                "addCoordinates?user=" + user.getUid() +
                                "&lat=" + mLatitudeText.getText().toString() +
                                "&long=" + mLongitudeText.getText().toString();

                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                                FIREBASE_FUNCTION_BASE_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        // Display the first 500 characters of the response string.
                                        Log.d(TAG, "Response is: "+ response.substring(0,500));
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // Code 500 whereas it works...
                            }
                        });

                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);
                        */

                        // 1. Update user's location in RealTime Database.
                        Map<String, Object> result = new HashMap<>();
                        result.put("lat", String.valueOf(location.getLatitude()));
                        result.put("long", String.valueOf(location.getLongitude()));
                        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                                .child("users").child(mFirebaseUser.getUid());
                        mDatabaseReference.updateChildren(result);
                        final double myLat = Double.parseDouble(String.valueOf(location.getLatitude()));
                        final double myLon = Double.parseDouble(String.valueOf(location.getLongitude()));

                        // 2. Receive other users' locations.
                        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                                .child("users");

                        mDatabaseReference.addValueEventListener(new ValueEventListener()
                        {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot)
                                {
                                    System.out.println("DOING");
                                    for (DataSnapshot child : dataSnapshot.getChildren())
                                    {
                                        if (!child.getKey().equals(mFirebaseUser.getUid()))
                                        {
                                            double lat = 0, lon = 0, distance = 50000;
                                            String name = "INCONNU", surname = "INCONNU",
                                                    sex = "INCONNU", orientation = "INCONNU";
                                            String activatedStr = "false";
                                            for (DataSnapshot grandChild : child.getChildren())
                                            {
                                                switch (grandChild.getKey())
                                                {
                                                    case "lat":
                                                        lat = Double.parseDouble(grandChild.getValue().toString());
                                                        break;
                                                    case "long":
                                                        lon = Double.parseDouble(grandChild.getValue().toString());
                                                        break;
                                                    case "name":
                                                        name = grandChild.getValue().toString();
                                                        break;
                                                    case "surname":
                                                        surname = grandChild.getValue().toString();
                                                        break;
                                                    case "sex":
                                                        sex = grandChild.getValue().toString();
                                                        break;
                                                    case "orientation":
                                                        orientation = grandChild.getValue().toString();
                                                        break;
                                                    case "activated":
                                                        activatedStr = grandChild.getValue().toString();

                                                }
                                            }
                                            distance = Math.min(distance, DistanceCalculationHelper.getDistance(myLat, myLon, lat, lon));
                                            Log.d("DEBUG", "ICI" + distance);
                                            if (isCloseEnough(distance) && activatedStr.equals("true"))
                                            {
                                                SharedPreferences mSharedPreferences =
                                                        getSharedPreferences(
                                                                UserInfo
                                                                        .SHARED_PREFERENCES_ID
                                                                        .getInfo(), MODE_PRIVATE);

                                                String userSex = mSharedPreferences.getString(
                                                        UserInfo.USER_SEX.getInfo(), "");
                                                String userOrientation = mSharedPreferences
                                                        .getString(UserInfo.USER_ORIENTATION
                                                                .getInfo(), "");

                                                Boolean activated = mSharedPreferences.getBoolean(
                                                        UserInfo.ACTIVATED.getInfo(),
                                                        false);

                                                if (activated
                                                        && (userOrientation.equals("ALL") || userOrientation.equals(sex))
                                                        && (orientation.equals("ALL") || orientation.equals(userSex)))
                                                {
                                                    stopLocationUpdates();
                                                    Log.d("DEBUG", "isActivated");

                                                    mDatabaseReference.removeEventListener(this);

                                                    // Start MatchActivity
                                                    Intent startMatchActivityIntent = new Intent(
                                                            HomepageActivity.this,
                                                            MatchActivity.class);
                                                    startMatchActivityIntent.putExtra(
                                                            "MATCH_ID", child.getKey());
                                                    startMatchActivityIntent.putExtra(
                                                            "MATCH_NAME", name);
                                                    startMatchActivityIntent.putExtra(
                                                            "MATCH_SURNAME", surname);
                                                    startActivity(startMatchActivityIntent);
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError firebaseError)
                                {
                                    Log.d(TAG, "onCancelled error");
                                }
                            });

                        /*
                        FirebaseDatabase.getInstance().getReference().child("users/").child(user.getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            System.out.println(snapshot.getKey());
                                            System.out.println(snapshot.getValue());
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                                */

                    }
                }
            }
        };
    } // [END ON_CREATE]

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        /*if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }


    private void stopLocationUpdates()
    {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mRequestingLocationUpdates = false;
    }

    private void startLocationUpdates()
    {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_CHECK_PERMISSIONS);
        }
        else
        {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
            mRequestingLocationUpdates = true;

            // Save activated boolean on Firebase
                mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users")
                        .child(mFirebaseUser.getUid());


                Map<String, Object> result = new HashMap<>();
                result.put(UserInfo.ACTIVATED.getInfo(),
                        mRequestingLocationUpdates.toString());
                mDatabaseReference.updateChildren(result);
                Log.d(TAG, "Activated boolean to TRUE stored in Firebase");

            // Save activated boolean locally
            SharedPreferences mSharedPreferences =
                    getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(), MODE_PRIVATE);
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean(UserInfo.ACTIVATED.getInfo(), mRequestingLocationUpdates);
            mEditor.apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CHECK_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CHECK_PERMISSIONS);
            }
            else
            {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback,
                        null /* Looper */);
                mRequestingLocationUpdates = true;

                // Save activated boolean on Firebase
                mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users")
                        .child(mFirebaseUser.getUid());


                Map<String, Object> result = new HashMap<>();
                result.put(UserInfo.ACTIVATED.getInfo(),
                        mRequestingLocationUpdates.toString());
                mDatabaseReference.updateChildren(result);
                Log.d(TAG, "Activated boolean to TRUE stored in Firebase");

                // Save activated boolean locally
                SharedPreferences mSharedPreferences =
                        getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(), MODE_PRIVATE);
                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.putBoolean(UserInfo.ACTIVATED.getInfo(), mRequestingLocationUpdates);
                mEditor.apply();
            }
        }
    }

    protected void createLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(9000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    protected void checkDeviceSettings() {
        createLocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(HomepageActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings.
                        AlertDialog.Builder builder = new AlertDialog.Builder(HomepageActivity.this);
                        builder.setMessage(R.string.settings_change_unavailable)
                                .setTitle(R.string.warning);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("REQUESTING_LOCATION_UPDATES_KEY",
                mRequestingLocationUpdates);
        //savedInstanceState.putParcelable("LOCATION_KEY", mCurrentLocation);
        //savedInstanceState.putString("LAST_UPDATED_TIME_STRING_KEY", mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains("REQUESTING_LOCATION_UPDATES_KEY")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        "REQUESTING_LOCATION_UPDATES_KEY");
            }
        }
    }
}
