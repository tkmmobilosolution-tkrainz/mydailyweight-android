package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by tkrainz on 03/03/2017.
 */

public class StartupActivity extends AppCompatActivity {

    private AlertDialog hintAlertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(StartupActivity.this);
        final View hintAlertView = inflater.inflate(R.layout.hint_alert, null);
        final TextView hintTitleView = (TextView) hintAlertView.findViewById(R.id.hintTitleTextView);
        final TextView hintView = (TextView) hintAlertView.findViewById(R.id.hintMessageTextView);
        final Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);
        hintButton.setText("Try again");
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Startup", "Button", "startup_tray_again");
                checkOnlineState();
                hintAlertDialog.dismiss();
            }
        });

        hintTitleView.setText("Hint");
        hintView.setText("You are not connected to the internet. Please connect to Wifi or mobile network.");

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();

        checkOnlineState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkOnlineState();
    }

    private boolean isUserLoggedIn() {
        trackInteraction("Startup", "User", "startup_check_user_logged_in");
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public boolean isOnline() {
        trackInteraction("Startup", "User", "startup_check_network_connection");
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void checkOnlineState() {
        trackInteraction("Startup", "Online", "startup_check_online");
        if (isOnline()) {
            trackInteraction("Startup", "Online", "startup_check_device_online");
            if (isUserLoggedIn()) {
                trackInteraction("Startup", "Online", "startup_open_main");
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else {
                trackInteraction("Startup", "Online", "startup_open_login");
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
        } else {
            trackInteraction("Startup", "Online", "startup_check_device_offline");
            hintAlertDialog.show();
        }
    }

    private void trackInteraction(String key, String value, String event) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle track = new Bundle();
        track.putString(key, value);
        analytics.logEvent(event, track);
    }
}
