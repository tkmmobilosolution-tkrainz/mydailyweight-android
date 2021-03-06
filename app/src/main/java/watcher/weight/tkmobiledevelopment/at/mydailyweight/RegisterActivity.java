package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Intent;
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
    private AlertDialog hintAlertDialog;
    private ProgressDialog progressDialog = null;
    private TextView hintTitleView, hintMessageView;
    private EditText emailET, passwordET, passwordMatchET;
    private FirebaseAnalytics analytics = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        analytics = FirebaseAnalytics.getInstance(this);

        emailET = (EditText) findViewById(R.id.registerEmail);
        passwordET = (EditText) findViewById(R.id.registerPassword);
        passwordMatchET = (EditText) findViewById(R.id.registerAcceptPassword);

        Button registerButton = (Button) findViewById(R.id.registrationButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Register", "Hint", "register_clicked_register");
                createUserAction();
            }
        });

        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage(getString(R.string.progressbar_register_user));

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(
            RegisterActivity.this);
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
        return password.equals(matchPassword);
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
            showHintAlertDialog(getString(R.string.hint),
                getString(R.string.password_worng_format));
            return;
        }

        if (!passwordMatch(password, matchPassword)) {
            trackInteraction("Register", "Hint", "register_password_not_match");
            showHintAlertDialog(getString(R.string.hint),
                getString(R.string.register_passwords_not_match));
            return;
        }

        if (emailFormat(email) && passwordFormat(password) && passwordMatch(password,
            matchPassword)) {
            authentication.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
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
            showHintAlertDialog(getString(R.string.error),
                getString(R.string.register_email_already_use));
        } else {
            trackInteraction("Register", "Error", "register_default_error");
            showHintAlertDialog(getString(R.string.error),
                getString(R.string.error_try_again_later));
        }
    }

    private void trackInteraction(String key, String value, String event) {
        Bundle track = new Bundle();
        track.putString(key, value);

        if (analytics != null) {
            analytics.logEvent(event, track);
        }
    }
}