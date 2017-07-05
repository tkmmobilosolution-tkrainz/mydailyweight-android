package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by tkrainz on 04/03/2017.
 */

public class UserActivity extends AppCompatActivity {

    String gender = "";
    ArrayList<String> genderList = new ArrayList<>();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private AlertDialog hintAlertDialog;
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        genderList.add(getString(R.string.gender_male));
        genderList.add(getString(R.string.gender_female));

        final EditText name = (EditText) findViewById(R.id.userNameEditText);
        final EditText age = (EditText) findViewById(R.id.userAgeEditText);

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(UserActivity.this);
        final View hintAlertView = inflater.inflate(R.layout.hint_alert, null);
        final TextView hintTitleView = (TextView) hintAlertView.findViewById(R.id.hintTitleTextView);
        final TextView hintMessageView = (TextView) hintAlertView.findViewById(R.id.hintMessageTextView);
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

        Spinner genderSpinner = (Spinner) findViewById(R.id.spinnerGender);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, R.layout.spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        genderSpinner.setAdapter(adapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    gender = "Male";
                } else if (position == 1) {
                    gender = "Female";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final EditText height = (EditText) findViewById(R.id.userHeightEditText);
        final EditText weight = (EditText) findViewById(R.id.userWeightEditText);
        final EditText dreamWeight = (EditText) findViewById(R.id.userDreamWeightEditText);
        Button updateButton = (Button) findViewById(R.id.userUpdateButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = name.getText().toString();
                String userAge = age.getText().toString();
                String userHeight = height.getText().toString();
                String userCurrentWeight = weight.getText().toString();
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

                if (!isStringAvailable(userCurrentWeight)) {
                    trackInteraction("User", "Hint", "user_no_current_weight");
                    hintMessageView.setText(getString(R.string.user_no_current_weight));
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

                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getApplicationContext());

                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("name").setValue(userName);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("email").setValue(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("age").setValue(userAge);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("gender").setValue(gender);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("height").setValue(userHeight);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("currentWeight").setValue(userCurrentWeight);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("dreamWeight").setValue(userDreamWeight);

                analytics.setUserProperty("age", userAge);
                analytics.setUserProperty("gender", gender);

                User newUser = new User();
                newUser.setName(userName);
                newUser.setAge(userAge);
                newUser.setHeight(userHeight);
                newUser.setCurrentWeight(userCurrentWeight);
                newUser.setDreamWeight(userDreamWeight);
                newUser.setGender(gender);

                saveUser(newUser);

                progressDialog.hide();

                trackInteraction("User", "Intent", "user_open_main");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // do nothing.
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
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle track = new Bundle();
        track.putString(key, value);
        analytics.logEvent(event, track);
    }
}
