package com.onesight.uqac.onesight.controller;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
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
import com.onesight.uqac.onesight.model.UserInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.onesight.uqac.onesight.controller.BitmapHandlingHelper.setCroppedBitmap;

public class MatchActivity extends AppCompatActivity {

    private final String TAG = "MatchActivity";

    // Database
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabaseReference;

    private ImageView matchPicture;

    private ValueEventListener listener;

    private void setActivatedToFalse()
    {
        // Save activated boolean on Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users")
                .child(mFirebaseUser.getUid());

        Map<String, Object> result = new HashMap<>();
        result.put(UserInfo.ACTIVATED.getInfo(), "false");
        mDatabaseReference.updateChildren(result);
        Log.d(TAG, "Activated boolean to FALSE stored in Firebase");

        // Save activated boolean locally
        SharedPreferences mSharedPreferences =
                getSharedPreferences(UserInfo.SHARED_PREFERENCES_ID.getInfo(), MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putBoolean(UserInfo.ACTIVATED.getInfo(), false);
        mEditor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        // FIREBASE INITIALIZATION
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // New meeting?
        sendNotification(getResources().getString(R.string.new_meeting),
                getResources().getString(R.string.interested));

        final String matchId = getIntent().getExtras().getString("MATCH_ID");
        String matchSurname = getIntent().getExtras().getString("MATCH_SURNAME");

        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(matchId);

        // Match proposition
        matchPicture = findViewById(R.id.match_picture);
        ImageView acceptBtn = findViewById(R.id.accept_btn);
        ImageView refuseBtn = findViewById(R.id.refuse_btn);
        TextView matchNameView = findViewById(R.id.match_name);

        matchNameView.setText(matchSurname);

        FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();
        StorageReference mStorageReference = mFirebaseStorage.getReference().child("profile_photos")
                .child(matchId);

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
                                    matchPicture);
                        }
                    }
            );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.d("DEBUG", "ONDATACHANGE");
                for (DataSnapshot child : dataSnapshot.getChildren()) {

                    Log.d("DEBUG", "CHILD" + child.getKey() +matchId);
                    if (child.getKey().equals(UserInfo.MATCH_ID.getInfo()))
                    {
                        String matchMatchId = child.getValue().toString();
                        String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        Log.d("DEBUG", "MATCH " +matchMatchId +" "+myId);
                        if (matchMatchId.equals(myId))
                        {
                            Log.d("DEBUG", "YOU WIN !!");
                            ImageView goodMatchImageVIew = findViewById(R.id.good_match);
                            goodMatchImageVIew.setVisibility(View.VISIBLE);

                            // It's a match!
                            sendNotification(getResources().getString(R.string.itsamatch),
                                    getResources().getString(R.string.match));
                            setActivatedToFalse();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
                Log.d("ERROR", "onCancelled error");
            }
        };

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ImageView waitingMatchImageVIew = findViewById(R.id.waiting_match);
                waitingMatchImageVIew.setVisibility(View.VISIBLE);

                mDatabaseReference.addValueEventListener(listener);

                Map<String, Object> result = new HashMap<>();
                if (matchId != null) {
                    result.put(UserInfo.MATCH_ID.getInfo(), matchId);
                }

                mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                mDatabaseReference.updateChildren(result);


            }
        });

        refuseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatabaseReference.removeEventListener(listener);

                Intent homepageActivityIntent =
                        new Intent(MatchActivity.this,
                                HomepageActivity.class);
                startActivity(homepageActivityIntent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        mDatabaseReference.removeEventListener(listener);
        Intent homepageActivityIntent =
                new Intent(MatchActivity.this,
                        HomepageActivity.class);
        startActivity(homepageActivityIntent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_HOME)) {
            mDatabaseReference.removeEventListener(listener);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void sendNotification (String title, String message)
    {
        // The id of the channel.
        String CHANNEL_ID = "my_channel_01";
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                        .setLights(Color.RED, 3000, 3000);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent();

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HomepageActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(10, mBuilder.build());
    }
}
