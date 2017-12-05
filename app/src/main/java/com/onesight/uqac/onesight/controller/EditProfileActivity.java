package com.onesight.uqac.onesight.controller;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.onesight.uqac.onesight.R;
import com.onesight.uqac.onesight.model.Sex;
import com.onesight.uqac.onesight.model.UserInfo;
import com.onesight.uqac.onesight.view.PhotoSelectionModeFragment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.onesight.uqac.onesight.controller.BitmapHandlingHelper.getRealPathFromURI;
import static com.onesight.uqac.onesight.controller.BitmapHandlingHelper.setCroppedBitmap;

/**
 * EditProfileActivity: user can pick a photo from local storage to use it as profile photo.
 * Photo Url is saved into database, photo file is saved into storage.
 */
public class EditProfileActivity extends AppCompatActivity implements Authentication,
        PhotoSelectionModeFragment.IPicModeSelectListener {

    public static final String TAG = "EditProfileActivity";

    private static final String CAPTURE_IMAGE_FILE_PROVIDER =
            "com.onesight.uqac.onesight.fileprovider";
    private static final String FILES_PATH = "files/Pictures";

    public Uri cameraPhotoURI;
    public String cameraPhotoPath;

    /**
     * ID to identify an update photo request / permission request.
     */
    private static final int RC_PHOTO_PICKER =  2;
    private static final int RC_CAMERA = 3;

    /**
     * Permissions required to read and write to storage and use the camera.
     */
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static String[] PERMISSION_CAMERA = { Manifest.permission.CAMERA };

    //Firebase
    private FirebaseAuth mAuth;
    private StorageReference mProfilePhotosStorageReference;
    private DatabaseReference mDatabaseReference;
    private SharedPreferences mSharedPreferences;

    // UI
    private ImageView mProfilePhoto;

    private RadioGroup mSexRg;
    private RadioButton mSexMaleRb;
    private RadioButton mSexFemaleRb;
    private RadioGroup mOrientationRg;
    private RadioButton mOrientationMaleRb;
    private RadioButton mOrientationFemaleRb;
    private RadioButton mOrientationBothRb;

    /**
     * Dialog box to choose camera/gallery.
     */
    @Override
    public void onPicModeSelected(String mode)
    {
        actionProfilePic(mode);
    }

    /**
     * Depending on the user's action, checks for the right permissions.
     *
     * @param action camera or gallery action.
     */
    private void actionProfilePic(String action)
    {
        if (action.equals(getResources().getString(R.string.camera)))
        {
            Log.i(TAG, "Camera choice. Checking permission.");
            // Check if the camera permission is already available.
            int permission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA);

            if (permission != PackageManager.PERMISSION_GRANTED)
            {
                // Read storage permission has not been granted.
                requestCameraPermission();
                Log.d(TAG, "Camera: permission != PM.Permission_Granted");
            }
            else
            {
                // Storage permission is already available, show the photo picker.
                Log.i(TAG,
                        "CAMERA permission has already been granted. Displaying photo picker.");
                takePhoto();
            }
        }
        else /* pick photo */
        {
            Log.i(TAG, "Pick photos choice. Checking permission.");
            // Check if the storage permission is already available.
            int readPermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            int writePermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (readPermission != PackageManager.PERMISSION_GRANTED
                    || writePermission != PackageManager.PERMISSION_GRANTED)
            {
                // Read storage permission has not been granted.
                requestStoragePermission();
                Log.d(TAG, "Storage: permission != PM.Permission_Granted");
            }
            else
            {
                // Storage permission is already available, show the photo picker.
                Log.i(TAG,
                        "STORAGE permission has already been granted. Displaying photo picker.");
                pickPhotos();
            }
        }
    }

    /**
     * Displays the fragment to choose the image adding mode.
     */
    private void showAddProfilePicDialog()
    {
        PhotoSelectionModeFragment dialogFragment = new PhotoSelectionModeFragment();
        dialogFragment.setIPicModeSelectListener(this);
        dialogFragment.show(getFragmentManager(), "picModeSelector");
    }


    /**
     * Take photo from camera
     */
    private void takePhoto()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {

           /*File photo = null;
            try
            {
                photo = createImageFile(this);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (photo != null)
            {
                cameraPhotoURI = Uri.fromFile(photo);
                if (cameraPhotoURI == null)
                {
                    Log.d(TAG, "PHOTO URI IS NULL AFTER CREATING FILE.");
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoURI);
                startActivityForResult(intent, RC_CAMERA);
            }
            else
            {
                Log.d(TAG, "photo is NULL.");
            }*/

            File path = new File(getFilesDir(), FILES_PATH);
            if (!path.exists())
            {
                if (path.mkdirs())
                {
                    Log.d(TAG, "Folder creation succeeded.");
                }
                else
                {
                    Log.e(TAG, "Error when creating images folder");
                }
            }
            File image = new File(path, "image.jpg");
            Uri imageUri = FileProvider.getUriForFile(this,
                    CAPTURE_IMAGE_FILE_PROVIDER, image);
            cameraPhotoURI = imageUri;
            cameraPhotoPath = image.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, RC_CAMERA);

        }
        else
        {
            Log.d(TAG, "Camera not available.");
        }
    }

    /**
     * Starts photo picker.
     */
    private void pickPhotos()
    {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/png, image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select picture"),
                RC_PHOTO_PICKER);
    }

    /**
     * Requests the Camera permission.
     */
    private void requestCameraPermission()
    {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA))
        {
            Log.i(TAG,
                    "Displaying camera permission rationale to provide additional context.");
            Toast.makeText(EditProfileActivity.this, R.string.permission_camera_rationale,
                    Toast.LENGTH_LONG).show();
        }
        else
        {
            // Storage permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, PERMISSION_CAMERA,
                    RC_CAMERA);
        }
    }

    /**
     * Requests the Storage permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestStoragePermission()
    {
        Log.i(TAG, "STORAGE permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.i(TAG,
                    "Displaying storage permission rationale to provide additional context.");
            Toast.makeText(EditProfileActivity.this, R.string.permission_storage_rationale,
                    Toast.LENGTH_LONG).show();
        }
        else
        {
            // Storage permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    RC_PHOTO_PICKER);
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == RC_CAMERA)
        {
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for CAMERA permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Camera permission has been granted
                Log.i(TAG, "CAMERA permission has now been granted.");
                Toast.makeText(EditProfileActivity.this,
                        R.string.permision_available_camera, Toast.LENGTH_SHORT).show();

                // LAUNCH ACTION
                takePhoto();
            }
            else
            {
                Log.i(TAG, "CAMERA permission was NOT granted.");
                Toast.makeText(EditProfileActivity.this, R.string.permissions_not_granted,
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == RC_PHOTO_PICKER)
        {
            // Received permission result for storage permission.
            Log.i(TAG, "Received response for STORAGE permission request.");

            // Check if the permissions have been granted
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                // Storage permission has been granted
                Log.i(TAG, "STORAGE permission has now been granted.");
                Toast.makeText(EditProfileActivity.this,
                        R.string.permision_available_storage, Toast.LENGTH_SHORT).show();

                // LAUNCH ACTION
                pickPhotos();
            }
            else
            {
                Log.i(TAG, "STORAGE permission was NOT granted.");
                Toast.makeText(EditProfileActivity.this, R.string.permissions_not_granted,
                        Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                // sign out
                mAuth.signOut();
                checkUserLogIn(mAuth.getCurrentUser());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Uri selectedImageUri = null;
        String picturePath = null;

        if (requestCode == RC_CAMERA && resultCode == RESULT_OK)
        {


            selectedImageUri = cameraPhotoURI;
            picturePath = cameraPhotoPath;
            /*Bundle extras = data.getExtras();
            Bitmap imageBitmap = null;
            if (extras != null) {
                imageBitmap = (Bitmap) extras.get("data");
                selectedImageUri = setCroppedBitmapAndSave(this, imageBitmap, mProfilePhoto);
            }
            else
            {
                Log.d(TAG, "Bitmap handling failed.");
            }*/
        }

        else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK && data != null)
        {
            selectedImageUri = data.getData();
            Log.d(TAG, "COUCOU HÉHO : " + selectedImageUri);
            picturePath = getRealPathFromURI(selectedImageUri, this);
        }

        else
        {
            Log.d(TAG, "FAILED: CODES: " + requestCode + " - " + resultCode);
        }

        StorageReference photoRef;

        //Place photo
        if (selectedImageUri != null)
        {
            Log.d(TAG, "Picture path: " + picturePath);
            photoRef = mProfilePhotosStorageReference.child(mAuth.getCurrentUser().getUid()
                    /*selectedImageUri.getLastPathSegment()*/);

            setCroppedBitmap(picturePath, mProfilePhoto);

            // Save photo URI locally
            mSharedPreferences = getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                    MODE_PRIVATE);
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putString("photoURI", picturePath);
            mEditor.apply();

            // Upload file to Firebase storage
            UploadTask uploadTask;
            uploadTask = photoRef.putFile(selectedImageUri);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Log.d(TAG, "PhotoURL upload failed.");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size,
                    // content-type, and download URL.
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        Map<String, Object> result = new HashMap<>();
                        if (downloadUrl != null) {
                            result.put("photoUrl", downloadUrl.toString());
                        }

                        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                                .child("users").child(user.getUid());
                        mDatabaseReference.updateChildren(result);

                    }
                }
            });
        }
        else
        {
            Log.d(TAG, "Uri is NULL.");
        }
    }

    /**
     * If no user is logged in, got to FirstScreenActivity.
     */
    @Override
    public void checkUserLogIn(FirebaseUser user) {
        if (user == null) {
            Intent firstScreenActivityIntent =
                    new Intent(EditProfileActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivityIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        checkUserLogIn(mAuth.getCurrentUser());

        FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();
        mProfilePhotosStorageReference = mFirebaseStorage.getReference().child("profile_photos");

        //Layout
        View mLayout = findViewById(R.id.edit_profile_layout);

        mProfilePhoto = findViewById(R.id.profile_photo);
        ImageButton mPhotoPickerButton = findViewById(R.id.photoPickerButton);

        mSexRg = findViewById(R.id.sex);
        mSexMaleRb = findViewById(R.id.sex_man);
        mSexFemaleRb = findViewById(R.id.sex_woman);
        mOrientationRg = findViewById(R.id.orientation);
        mOrientationMaleRb = findViewById(R.id.searched_sex_man);
        mOrientationFemaleRb = findViewById(R.id.searched_sex_woman);
        mOrientationBothRb = findViewById(R.id.searched_sex_both);
        Button validate_btn = findViewById(R.id.validate_params);

        mSharedPreferences = getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                MODE_PRIVATE);
        String photoURI = mSharedPreferences.getString(UserInfo.USER_PHOTO.getInfo(),null);
        if (photoURI != null)
        {
            setCroppedBitmap(photoURI, mProfilePhoto);
        }

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddProfilePicDialog();
            }
        });

        mSharedPreferences = getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                MODE_PRIVATE);

        String oldUserSex = mSharedPreferences.getString(UserInfo.USER_SEX.getInfo(),
                null);
        String oldUserOrientation = mSharedPreferences.getString(
                UserInfo.USER_ORIENTATION.getInfo(), null);

        if (oldUserOrientation != null && oldUserSex != null)
        {
            if (oldUserSex.equals("MALE"))
            {
                mSexRg.check(mSexMaleRb.getId());
            }
            else if (oldUserSex.equals("FEMALE"))
            {
                mSexRg.check(mSexFemaleRb.getId());
            }

            switch (oldUserOrientation) {
                case "MALE":
                    mOrientationRg.check(mOrientationMaleRb.getId());
                    break;
                case "FEMALE":
                    mOrientationRg.check(mOrientationFemaleRb.getId());
                    break;
                case "ALL":
                    mOrientationRg.check(mOrientationBothRb.getId());
                    break;
            }
        }

        validate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null)
                {
                    // update profile parameters
                    Sex user_sex = Sex.MALE;
                    Sex user_searched_sex = Sex.MALE;
                    boolean modifiedSex = true;
                    boolean modifiedOrientation = true;

                    int sex_id = mSexRg.getCheckedRadioButtonId();
                    int searched_sex_id = mOrientationRg.getCheckedRadioButtonId();

                    if (sex_id == mSexMaleRb.getId())  { user_sex = Sex.MALE; }
                    else if (sex_id == mSexFemaleRb.getId()) { user_sex = Sex.FEMALE; }
                    else { modifiedSex = false; }

                    if (searched_sex_id == mOrientationMaleRb.getId())
                    {
                        user_searched_sex = Sex.MALE;
                    }
                    else if (searched_sex_id == mOrientationFemaleRb.getId())
                    {
                        user_searched_sex = Sex.FEMALE;
                    }
                    else if (searched_sex_id == mOrientationBothRb.getId())
                    {
                        user_searched_sex = Sex.ALL;
                    }
                    else { modifiedOrientation = false; }

                    if (modifiedSex)
                    {
                        Map<String, Object> result = new HashMap<>();
                        result.put(UserInfo.USER_SEX.getInfo(), user_sex);
                        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                                .child("users").child(user.getUid());
                        mDatabaseReference.updateChildren(result);

                        SharedPreferences mSharedPreferences =
                                getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(), MODE_PRIVATE);
                        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                        mEditor.putString(UserInfo.USER_SEX.getInfo(), user_sex.getString());
                        mEditor.apply();
                        Log.d(TAG, "modifiedSex OK " + user_sex.getString());
                    }
                    if (modifiedOrientation)
                    {
                        Map<String, Object> result = new HashMap<>();
                        result.put(UserInfo.USER_ORIENTATION.getInfo(), user_searched_sex);
                        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                                .child("users").child(user.getUid());
                        mDatabaseReference.updateChildren(result);

                        SharedPreferences mSharedPreferences =
                                getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(),
                                        MODE_PRIVATE);
                        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                        mEditor.putString(UserInfo.USER_ORIENTATION.getInfo(), user_searched_sex.getString());
                        mEditor.apply();
                        Log.d(TAG, "modifiedOrientation OK " + user_searched_sex.getString());

                    }

                } //END field validation

                Toast.makeText(EditProfileActivity.this,
                        getResources().getString(R.string.info_updated), Toast.LENGTH_SHORT).show();
                Intent homepageActivityIntent =
                        new Intent(EditProfileActivity.this,
                                HomepageActivity.class);
                startActivity(homepageActivityIntent);
            }
        });
    }
}
