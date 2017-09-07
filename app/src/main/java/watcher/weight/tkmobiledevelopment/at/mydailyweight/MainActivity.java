package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAnalytics analytics = null;
    private AlertDialog deleteDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        analytics = FirebaseAnalytics.getInstance(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();

        navigationView.getMenu().getItem(0).setChecked(true);

        LayoutInflater inflater = this.getLayoutInflater();
        AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(this);
        View hintAlertView = inflater.inflate(R.layout.logout_hint, null);

        TextView deleteTitle = (TextView) hintAlertView.findViewById(R.id.logoutHintTitle);
        deleteTitle.setText(getString(R.string.hint));

        TextView deleteText = (TextView) hintAlertView.findViewById(R.id.logoutHintMessage);
        deleteText.setText(getString(R.string.delete_hint_text));

        Button deleteButton = (Button) hintAlertView.findViewById(R.id.logoutHintButton);
        deleteButton.setText(getString(R.string.delete_button));
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Button", "Hint", "delete_user_clicked");
                deleteUser();
            }
        });

        Button cancelButton = (Button) hintAlertView.findViewById(R.id.cancelHintButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Button", "Hint", "delete_user_cancel");
                deleteDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        deleteDialog = dialogHintBuilder.create();

        trackInteraction("NavDrawer", "Start", "nav_drawer_start_graph");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentManager fm = getSupportFragmentManager();

        if (id == R.id.nav_weight) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_weight_graph_clicked");
            fm.beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();
        } else if (id == R.id.nav_list) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_weight_list_clicked");
            fm.beginTransaction().replace(R.id.content_frame, new ListFragment()).commit();
        } else if (id == R.id.nav_logout) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_logout_clicked");
            syncDatabase();
        } else if (id == R.id.privacy_policy) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_privacy_polic");
            fm.beginTransaction().replace(R.id.content_frame, new PrivacyPolicyMenuFragment())
                .commit();
        } else if (id == R.id.edit_profile) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_edit_account");
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        /**} else if (id == R.id.delete_account) {
            trackInteraction("NavDrawer", "Item", "nav_drawer_delete_profile");
            deleteDialog.show(); **/
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        trackInteraction("NavDrawer", "Logout", "activity_logout");
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("USER");
        editor.remove("list");
        editor.remove("able_sync");
        editor.remove("logoutsync");
        editor.apply();

        trackInteraction("NavDrawer", "Intent", "logout_open_login");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void syncDatabase() {
        trackInteraction("NavDrawer", "Sync", "sync_nav_drawer");
        ProgressDialog progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setMessage(getString(R.string.progressbar_sync));
        progressDialog.show();
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(this);

        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {
        }.getType();
        ArrayList<Weight> list = gson.fromJson(json, type);

        FirebaseDatabase.getInstance().getReference().child("user_weights")
            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .setValue(list);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("able_sync", false);
        editor.apply();

        logout();

        progressDialog.hide();
    }

    private void deleteUser() {
        final FirebaseAuth firebaseUser = FirebaseAuth.getInstance();

        DatabaseReference userProfile = FirebaseDatabase.getInstance().getReference()
            .child("user_profile")
            .child(firebaseUser.getCurrentUser().getUid());
        userProfile.removeValue();

        DatabaseReference userWeights = FirebaseDatabase.getInstance().getReference()
            .child("user_weights")
            .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userWeights.removeValue();

        SharedPreferences prefs = PreferenceManager
            .getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("USER");
        editor.remove("list");
        editor.remove("able_sync");
        editor.remove("logoutsync");
        editor.apply();

        trackInteraction("MainActivity", "Account", "delete_account");
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.putExtra("delete", true);
        startActivity(intent);
    }

    private void trackInteraction(String key, String value, String event) {
        Bundle track = new Bundle();
        track.putString(key, value);

        if (analytics != null) {
            analytics.logEvent(event, track);
        }
    }
}
