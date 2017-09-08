package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

/**
 * Created by tkrainz on 07/09/2017.
 */

public class EditProfileActivity extends AppCompatActivity {

    private User user = null;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private AlertDialog hintAlertDialog;
    private ProgressDialog progressDialog = null;
    private FirebaseAnalytics analytics = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);

        getProfile();

        final EditText name = (EditText) findViewById(R.id.editTextUserName);
        name.setText(user.getName());
        final EditText age = (EditText) findViewById(R.id.editTextAge);
        age.setText(user.getAge());

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(EditProfileActivity.this);
        final View hintAlertView = inflater.inflate(R.layout.hint_alert, null);
        final TextView hintTitleView = (TextView) hintAlertView
            .findViewById(R.id.hintTitleTextView);
        final TextView hintMessageView = (TextView) hintAlertView
            .findViewById(R.id.hintMessageTextView);
        final Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);

        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage(getString(R.string.progressbar_update_user));

        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("User", "Hint", "user_hint_button");
                hintAlertDialog.dismiss();
            }
        });

        hintTitleView.setText(getString(R.string.hint));

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();

        final EditText height = (EditText) findViewById(R.id.editTextHeight);
        height.setText(user.getHeight());
        final EditText dreamWeight = (EditText) findViewById(R.id.editTextDesiredWeight);
        dreamWeight.setText(user.getDreamWeight());

        Button updateButton = (Button) findViewById(R.id.editProfileButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = name.getText().toString();
                String userAge = age.getText().toString();
                String userHeight = height.getText().toString();
                String userDreamWeight = dreamWeight.getText().toString();

                if (!isStringAvailable(userName)) {
                    trackInteraction("User", "Hint", "user_no_username");
                    hintMessageView.setText(getString(R.string.user_no_nickname));
                    hintAlertDialog.show();
                    return;
                }

                if (!isStringAvailable(userAge)) {
                    trackInteraction("User", "Hint", "user_no_age");
                    hintMessageView.setText(getString(R.string.user_no_age));
                    hintAlertDialog.show();
                    return;
                }

                if (!isStringAvailable(userHeight)) {
                    trackInteraction("User", "Hint", "user_no_height");
                    hintMessageView.setText(getString(R.string.user_no_height));
                    hintAlertDialog.show();
                    return;
                }

                if (!isStringAvailable(userDreamWeight)) {
                    trackInteraction("User", "Hint", "user_no_dream_weight");
                    hintMessageView.setText(getString(R.string.user_no_dream_weight));
                    hintAlertDialog.show();
                    return;
                }

                progressDialog.show();

                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("name")
                    .setValue(userName);
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("email")
                    .setValue(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("age")
                    .setValue(userAge);
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("gender")
                    .setValue(user.getGender());
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("height")
                    .setValue(userHeight);
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("currentWeight").setValue(user.getCurrentWeight());
                mDatabase.child("user_profile")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("dreamWeight").setValue(userDreamWeight);

                User newUser = new User();
                newUser.setName(userName);
                newUser.setAge(userAge);
                newUser.setHeight(userHeight);
                newUser.setCurrentWeight(user.getCurrentWeight());
                newUser.setDreamWeight(userDreamWeight);
                newUser.setGender(user.getGender());

                saveUser(newUser);

                progressDialog.hide();

                trackInteraction("Edit", "Intent", "edit_profile");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                Toast.makeText(EditProfileActivity.this, getString(R.string.edit_profile_success), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isStringAvailable(String checkString) {
        return checkString != null && !checkString.isEmpty();
    }

    private void saveUser(User user) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("USER", json);
        editor.apply();
    }

    private void trackInteraction(String key, String value, String event) {
        Bundle track = new Bundle();
        track.putString(key, value);

        if (analytics != null) {
            analytics.logEvent(event, track);
        }
    }

    private void getProfile() {
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("USER", null);
        user = gson.fromJson(json, User.class);

        if (user == null) {
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_profile")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user = (User) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
