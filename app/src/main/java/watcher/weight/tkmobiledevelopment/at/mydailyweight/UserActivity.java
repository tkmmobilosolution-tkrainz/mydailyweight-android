package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by tkrainz on 04/03/2017.
 */

public class UserActivity extends Activity {

    String gender = "";
    ArrayList<String> genderList = new ArrayList<>();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        genderList.add("Male");
        genderList.add("Female");

        final EditText name = (EditText) findViewById(R.id.userNameEditText);
        final EditText age = (EditText) findViewById(R.id.userAgeEditText);

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
                    name.setText("No username entered.");
                    return;
                }

                if (!isStringAvailable(userAge)) {
                    name.setText("No username entered.");
                    return;
                }

                if (!isStringAvailable(userHeight)) {
                    name.setText("No username entered.");
                    return;
                }

                if (!isStringAvailable(userCurrentWeight)) {
                    name.setText("No username entered.");
                    return;
                }

                if (!isStringAvailable(userDreamWeight)) {
                    name.setText("No username entered.");
                    return;
                }

                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("name").setValue(userName);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("email").setValue(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("age").setValue(userAge);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("gender").setValue(gender);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("height").setValue(userHeight);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("currentWeight").setValue(userCurrentWeight);
                mDatabase.child("user_profile").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("dreamWeight").setValue(userDreamWeight);

                User newUser = new User();
                newUser.setName(userName);
                newUser.setAge(userAge);
                newUser.setHeight(userHeight);
                newUser.setCurrentWeight(userCurrentWeight);
                newUser.setDreamWeight(userDreamWeight);
                newUser.setGender(gender);

                saveUser(newUser);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean isStringAvailable(String checkString) {
        return checkString != null && !checkString.isEmpty();
    }

    private void saveUser(User user) {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        editor.putString("USER", json);
        editor.apply();
    }
}
