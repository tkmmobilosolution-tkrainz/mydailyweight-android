package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by tkrainz on 03/03/2017.
 */

public class RegisterActivity extends Activity {

    FirebaseAuth authentication = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        final TextView wrongEmail = (TextView) findViewById(R.id.registerWrongEmail);
        final EditText emailET = (EditText) findViewById(R.id.registerEmail);
        final TextView passwordInfo = (TextView) findViewById(R.id.passwordInfo);
        final EditText passwordET = (EditText) findViewById(R.id.registerPassword);
        final TextView wrongPassword = (TextView) findViewById(R.id.registerWrongPassword);
        final EditText passwordMatchET = (EditText) findViewById(R.id.registerAcceptPassword);
        Button registerButton = (Button) findViewById(R.id.registrationButton);

        wrongEmail.setVisibility(View.GONE);
        passwordInfo.setVisibility(View.GONE);
        wrongPassword.setVisibility(View.GONE);

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent intent = new Intent(getApplicationContext(), UserActivity.class);
                    startActivity(intent);
                }
            }
        };


        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                String matchPassword = passwordMatchET.getText().toString();

                if (!emailFormat(email)) {
                    wrongEmail.setVisibility(View.VISIBLE);
                }

                if (!passwordFormat(password)) {
                    passwordInfo.setVisibility(View.VISIBLE);
                }

                if (!passwordMatch(password, matchPassword)) {
                    wrongPassword.setVisibility(View.VISIBLE);
                }

                if (emailFormat(email) && passwordFormat(password) && passwordMatch(password, matchPassword)) {
                    // TODO: register

                    authentication.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                //TODO error handling
                            }
                        }
                    });
                }
            }
        });
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
}
