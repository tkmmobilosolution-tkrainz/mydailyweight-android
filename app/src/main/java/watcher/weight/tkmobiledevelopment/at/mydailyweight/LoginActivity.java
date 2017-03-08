package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

/**
 * Created by tkrainz on 03/03/2017.
 */

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth authentication = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authListener = null;
    CallbackManager mCallbackManager = null;
    private AlertDialog hintAlertDialog;
    private ProgressDialog progressDialog = null;
    private TextView hintTitleView, hintMessageView;
    private EditText emailET, passwordET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        emailET = (EditText) findViewById(R.id.loginEmail);
        passwordET = (EditText) findViewById(R.id.loginPassword);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        LoginButton facebookLoginButton = (LoginButton) findViewById(R.id.facebookButton);
        Button registerButton = (Button) findViewById(R.id.registerButton);

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(LoginActivity.this);
        final View hintAlertView = inflater.inflate(R.layout.hint_alert, null);
        hintTitleView = (TextView) hintAlertView.findViewById(R.id.hintTitleTextView);
        hintMessageView = (TextView) hintAlertView.findViewById(R.id.hintMessageTextView);
        final Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);

        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hintAlertDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog.setCancelable(false);
        hintAlertDialog = dialogHintBuilder.create();

        mCallbackManager = CallbackManager.Factory.create();
        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage("Logging in");

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                checkUserState(user);
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAction();
            }
        });

        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
            }
        });

        facebookLoginButton.setReadPermissions("email", "public_profile");
        facebookLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterActivity();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authListener != null) {
            authentication.removeAuthStateListener(authListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        authentication.addAuthStateListener(authListener);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authentication.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                }
            }
        });
    }

    private boolean passwordFormat(String password) {
        return password.length() >= 8;
    }

    private boolean emailFormat(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private void loginAction() {
        progressDialog.show();

        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        if (emailFormat(email) && passwordFormat(password)) {
            authentication.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (!task.isSuccessful()) {
                        signInFailed(task);
                    }
                }
            });
        } else if (!emailFormat(email)) {
            showHintAlertDialog("Hint", "Check your email address, wrong format");
        } else if (!passwordFormat(password)) {
            showHintAlertDialog("Hint", "Password lenght must be 8 characters or longer.");
        }
    }

    private void checkUserState(FirebaseUser user) {
        if (user != null) {
            final Gson gson = new Gson();
            final String localUser = getSharedPreferences("USER", this.MODE_PRIVATE).getString("USER", null);

            if (localUser == null || localUser.equals("")) {
                handleDatabaseUser(user);
            } else {
                setLocalUser(localUser);
            }
        }
    }

    private void handleDatabaseUser(FirebaseUser user) {
        final Gson gson = new Gson();
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("user_profile").child(user.getUid());
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User databaseUser = dataSnapshot.getValue(User.class);

                if (databaseUser != null) {
                    String dbUserJson = gson.toJson(databaseUser);
                    SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("USER", dbUserJson);
                    editor.apply();

                    progressDialog.hide();
                    showMainActivity();
                } else {
                    progressDialog.hide();
                    showUserActivity();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showHintAlertDialog("Error", "A server error occored. Try again later.");
            }
        });
    }

    private void setLocalUser(String userJson) {
        final Gson gson = new Gson();
        User savedUser = gson.fromJson(userJson, User.class);

        if (savedUser == null) {
            SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USER", userJson);
            editor.apply();
        }

        progressDialog.hide();
        showMainActivity();
    }

    private void signInFailed(Task<AuthResult> task) {
        FirebaseAuthException exception = (FirebaseAuthException) task.getException();

        if (exception.getErrorCode().equals("ERROR_USER_NOT_FOUND")) {
            showHintAlertDialog("Error", "No Account for this email address");
        } else if (exception.getErrorCode().equals("ERROR_WRONG_PASSWORD")) {
            showHintAlertDialog("Error", "Wrong Password for this email address");
        } else {
            showHintAlertDialog("Error", "Try again later!");
        }
    }

    private void showHintAlertDialog(String title, String message) {
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
        hintTitleView.setText(title);
        hintMessageView.setText(message);
        hintAlertDialog.show();
    }

    private void showUserActivity() {
        Intent intent = new Intent(getApplicationContext(), UserActivity.class);
        startActivity(intent);
    }

    private void showMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void showRegisterActivity() {
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
