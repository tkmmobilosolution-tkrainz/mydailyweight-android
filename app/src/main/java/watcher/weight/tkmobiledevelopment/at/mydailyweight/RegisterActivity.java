package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by tkrainz on 03/03/2017.
 */

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth authentication = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authListener = null;
    private AlertDialog hintAlertDialog, hintNetworkAlert;
    private ProgressDialog progressDialog = null;
    private TextView hintTitleView, hintMessageView;
    private EditText emailET, passwordET, passwordMatchET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        emailET = (EditText) findViewById(R.id.registerEmail);
        passwordET = (EditText) findViewById(R.id.registerPassword);
        passwordMatchET = (EditText) findViewById(R.id.registerAcceptPassword);

        Button registerButton = (Button) findViewById(R.id.registrationButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Register", "Hint", "register_clicked_register");
                if (isOnline()) {
                    createUserAction();
                } else {
                    trackInteraction("Register", "Network", "register_network_offline");
                    hintNetworkAlert.show();
                }
            }
        });

        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage(getString(R.string.progressbar_register_user));

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(RegisterActivity.this);
        final View hintAlertView = inflater.inflate(R.layout.hint_alert, null);
        hintTitleView = (TextView) hintAlertView.findViewById(R.id.hintTitleTextView);
        hintMessageView = (TextView) hintAlertView.findViewById(R.id.hintMessageTextView);
        final Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Register", "Hint", "register_hint_button");
                hintAlertDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    progressDialog.hide();
                    trackInteraction("Register", "Intent", "register_open_user");
                    Intent intent = new Intent(getApplicationContext(), UserActivity.class);
                    startActivity(intent);
                }
            }
        };

        final AlertDialog.Builder dialogNetworkBuilder = new AlertDialog.Builder(RegisterActivity.this);
        final View hintNetworkView = inflater.inflate(R.layout.hint_alert, null);
        final TextView networkTitleView = (TextView) hintNetworkView.findViewById(R.id.hintTitleTextView);
        final TextView networkView = (TextView) hintNetworkView.findViewById(R.id.hintMessageTextView);
        final Button networkButton = (Button) hintNetworkView.findViewById(R.id.hintButton);
        networkButton.setText(getString(R.string.try_again));
        networkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Register", "Button", "register_tray_again");
                hintAlertDialog.dismiss();
                if (!isOnline()) {
                    trackInteraction("Register", "Network", "register_always_network_offline");
                    hintNetworkAlert.show();
                }
            }
        });

        networkTitleView.setText(getString(R.string.hint));
        networkView.setText(getString(R.string.no_connection));

        dialogHintBuilder.setView(hintNetworkView);
        hintNetworkAlert = dialogNetworkBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        authentication.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            authentication.removeAuthStateListener(authListener);
        }
    }

    private boolean passwordFormat(String password) {
        return password.length() >= 8;
    }

    private boolean passwordMatch(String password, String matchPassword) {
        return password.endsWith(matchPassword);
    }

    private boolean emailFormat(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private void showHintAlertDialog(String title, String message) {
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
        hintTitleView.setText(title);
        hintMessageView.setText(message);
        hintAlertDialog.show();
    }

    private void createUserAction() {
        progressDialog.show();
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        String matchPassword = passwordMatchET.getText().toString();

        if (!emailFormat(email)) {
            trackInteraction("Register", "Hint", "register_email_password");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.email_worng_format));
            return;
        }

        if (!passwordFormat(password)) {
            trackInteraction("Register", "Hint", "register_password_format");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.password_worng_format));
            return;
        }

        if (!passwordMatch(password, matchPassword)) {
            trackInteraction("Register", "Hint", "register_password_not_match");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.register_passwords_not_match));
            return;
        }

        if (emailFormat(email) && passwordFormat(password) && passwordMatch(password, matchPassword)) {
            authentication.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        createUserFailed(task);
                    } else {
                        trackInteraction("Register", "Sign up", "register_user_registered");
                    }
                }
            });
        }
    }

    private void createUserFailed(Task<AuthResult> task) {
        FirebaseAuthException exception = (FirebaseAuthException) task.getException();
        if (exception.getErrorCode().equals("ERROR_EMAIL_ALREADY_IN_USE")) {
            trackInteraction("Register", "Error", "register_account_exists");
            showHintAlertDialog(getString(R.string.error), getString(R.string.register_email_already_use));
        } else {
            trackInteraction("Register", "Error", "register_default_error");
            showHintAlertDialog(getString(R.string.error), getString(R.string.error_try_again_later));
        }
    }

    private void trackInteraction(String key, String value, String event) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle track = new Bundle();
        track.putString(key, value);
        analytics.logEvent(event, track);
    }

    public boolean isOnline() {
        trackInteraction("Register", "User", "register_check_network_connection");
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}