package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.google.firebase.analytics.FirebaseAnalytics;
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
    private boolean facebookLoginFlag = false;
    private FirebaseAnalytics analytics = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        analytics = FirebaseAnalytics.getInstance(this);

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
                trackInteraction("Login", "Hint", "login_hint_button");
                hintAlertDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();

        mCallbackManager = CallbackManager.Factory.create();
        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage(getString(R.string.progressbar_sign_in));

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
                trackInteraction("Login", "Button", "login_clicked_sign_in");
                loginAction();
            }
        });

        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Login", "Button", "login_clicked_facebook");
                progressDialog.show();
                facebookLoginFlag = true;
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
                trackInteraction("Login", "Button", "login_clicked_register");
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
        if (progressDialog.isShowing() && !facebookLoginFlag) {
            progressDialog.hide();
        }
        authentication.addAuthStateListener(authListener);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authentication.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        trackInteraction("Login", "Facebook", "login_facebook_sign_in_success");
                    } else {
                        trackInteraction("Login", "Facebook", "login_facebook_sign_in_failed");
                        facebookLoginFlag = false;
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
            authentication.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {
                            signInFailed(task);
                        } else {
                            trackInteraction("Login", "Sign in", "login_sign_in_success");
                        }
                    }
                });
        } else if (!emailFormat(email)) {
            trackInteraction("Login", "Hint", "login_email_format");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.email_worng_format));
        } else if (!passwordFormat(password)) {
            trackInteraction("Login", "Hint", "login_password_format");
            showHintAlertDialog(getString(R.string.hint),
                getString(R.string.password_worng_format));
        }
    }

    private void checkUserState(FirebaseUser user) {
        if (user != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String localUser = prefs.getString("USER", null);

            if (localUser == null || localUser.equals("")) {
                handleDatabaseUser(user);
            } else {
                setLocalUser(localUser);
            }

            facebookLoginFlag = false;
        }
    }

    private void handleDatabaseUser(FirebaseUser user) {
        final Gson gson = new Gson();
        final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
            .child("user_profile").child(user.getUid());
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User databaseUser = dataSnapshot.getValue(User.class);

                if (databaseUser != null) {
                    String dbUserJson = gson.toJson(databaseUser);
                    SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
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
                showHintAlertDialog(getString(R.string.error),
                    getString(R.string.error_try_again_later));
            }
        });
    }

    private void setLocalUser(String userJson) {
        final Gson gson = new Gson();
        User savedUser = gson.fromJson(userJson, User.class);

        if (savedUser == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
            trackInteraction("Login", "Error", "login_no_account");
            showHintAlertDialog(getString(R.string.error),
                getString(R.string.login_no_account_error));
        } else if (exception.getErrorCode().equals("ERROR_WRONG_PASSWORD")) {
            trackInteraction("Login", "Error", "login_wrong_password");
            showHintAlertDialog(getString(R.string.error),
                getString(R.string.login_wrong_password));
        } else {
            trackInteraction("Login", "Error", "login_default_error");
            showHintAlertDialog(getString(R.string.error),
                getString(R.string.error_try_again_later));
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
        trackInteraction("Login", "Intent", "login_open_user");
        Intent intent = new Intent(getApplicationContext(), UserActivity.class);
        startActivity(intent);
    }

    private void showMainActivity() {
        trackInteraction("Login", "Intent", "login_open_main");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void showRegisterActivity() {
        trackInteraction("Login", "Intent", "login_open_register");
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(intent);
    }

    private void trackInteraction(String key, String value, String event) {
        Bundle track = new Bundle();
        track.putString(key, value);

        if (analytics != null) {
            analytics.logEvent(event, track);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
