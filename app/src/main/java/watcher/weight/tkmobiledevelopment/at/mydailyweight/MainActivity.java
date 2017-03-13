package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private AlertDialog addAlertDialog;
    private AlertDialog hintAlertDialog;
    private AlertDialog logoutAlertDialog;
    private AlertDialog downloadAlertDialog;

    private TextView hintTitleView, hintMessageView;

    private TextView hintLogoutMessage;
    private Button logoutButton;
    private Button infoButton;

    private ArrayList<Weight> list = new ArrayList<>();
    private ArrayList<Weight> dbList = new ArrayList<>();
    private ListView listView;
    private WeightView weightView;
    private String today;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private InterstitialAd mInterstitialAd;
    private RewardedVideoAd rewardedAd;

    private ProgressDialog progressDialog = null;

    private boolean rewardedFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_mob_app_id));
        setAdvertisment();

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstital_ad_unit_id));
        requestNewInterstitial();

        rewardedAd = MobileAds.getRewardedVideoAdInstance(this);
        loadRewardedVideo();
        rewardedAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {

            }

            @Override
            public void onRewardedVideoAdOpened() {

            }

            @Override
            public void onRewardedVideoStarted() {

            }

            @Override
            public void onRewardedVideoAdClosed() {
                if (!rewardedFinished) {
                    trackInteraction("Main", "Rewarded", "main_sync_canceled");
                    loadRewardedVideo();
                    Toast.makeText(MainActivity.this, getString(R.string.main_sync_failed), Toast.LENGTH_SHORT).show();
                }

                rewardedFinished = false;
                Log.e("Rewarded error", "");
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {
                rewardedFinished = true;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                if (prefs.getBoolean("logoutsync", false)) {
                    trackInteraction("Main", "Rewarded", "main_sync_logout_success");
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("logoutsync", false);
                    editor.apply();

                    syncDatabase();
                    logout();
                } else {
                    trackInteraction("Main", "Rewarded", "main_sync_success");
                    syncDatabase();
                }
            }

            @Override
            public void onRewardedVideoAdLeftApplication() {

            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {
                trackInteraction("Main", "Rewarded", "main_rewarded_error_" + i);
                Log.e("Rewarded error", "" + i);
            }
        });

        progressDialog = new ProgressDialog(this, R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

        today = getCurrentDate();
        list = getWeightList();

        infoButton = (Button) findViewById(R.id.infoButton);
        infoButton.setVisibility(list.size() > 0 ? View.VISIBLE : View.GONE);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_progress_button");
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final Gson gson = new Gson();
                User savedUser = gson.fromJson(pref.getString("USER", null), User.class);
                double differenceSinceBegin = Double.parseDouble(savedUser.getCurrentWeight()) - list.get(list.size() - 1).weightValue;
                double differenceToDream = Double.parseDouble(savedUser.getDreamWeight()) - list.get(list.size() - 1).weightValue;
                String hintText = getString(R.string.main_diff_sinde_begin) + Math.abs(differenceSinceBegin) + " kg" + "\n\n" +
                        getString(R.string.main_till_end) + Math.abs(differenceToDream) + " kg";

                showHintAlertDialog("Weight Information", hintText);
            }
        });

        weightView = (WeightView) findViewById(R.id.weightView);
        weightView.setVisibility(list.size() > 0 ? View.VISIBLE : View.GONE);
        weightView.setWeightArrayList(list);

        LayoutInflater inflater = this.getLayoutInflater();
        listView = (ListView) findViewById(R.id.wightList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (list.size() == 0 || position == list.size()) {
                    trackInteraction("Main", "List", "main_list_add");
                    showAddDialog();
                }
            }
        });
        listView.setDivider(new ColorDrawable(list.size() > 0 ? Color.WHITE : Color.TRANSPARENT));
        listView.setAdapter(new WeightListAdapter(this, list));

        AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(this);
        View addAlertView = inflater.inflate(R.layout.add_alert, null);

        final EditText newWeight = (EditText) addAlertView.findViewById(R.id.editText);

        Button addButton = (Button) addAlertView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newWeight.getText().toString().equals("")) {
                    trackInteraction("Main", "Button", "main_add_hint_button_failed");
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_weight_entered), Toast.LENGTH_LONG).show();
                } else {
                    progressDialog.setMessage(getString(R.string.progressbar_refresh));
                    progressDialog.show();
                    trackInteraction("Main", "Button", "main_add_hint_button_success");
                    double weight = Double.parseDouble(newWeight.getText().toString());
                    Weight currentWeight = new Weight(weight, today);
                    list.add(currentWeight);
                    refreshLayout(list);

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putBoolean("able_sync", true);
                    editor.apply();

                    addAlertDialog.dismiss();
                    progressDialog.hide();
                }
            }
        });

        Button cancelButton = (Button) addAlertView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_add_hint_button_cancel");
                addAlertDialog.dismiss();
            }
        });

        addDialogBuilder.setView(addAlertView);
        addAlertDialog = addDialogBuilder.create();
        addAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(this);
        View hintAlertView = inflater.inflate(R.layout.hint_alert, null);

        hintTitleView = (TextView) hintAlertView.findViewById(R.id.hintTitleTextView);
        hintMessageView = (TextView) hintAlertView.findViewById(R.id.hintMessageTextView);

        Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_hint_button");
                hintAlertDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();

        final AlertDialog.Builder logoutHintBuilder = new AlertDialog.Builder(this);
        View logoutHintView = inflater.inflate(R.layout.logout_hint, null);
        hintLogoutMessage = (TextView) logoutHintView.findViewById(R.id.logoutHintMessage);
        Button cancelLogoutButton = (Button) logoutHintView.findViewById(R.id.cancelHintButton);
        cancelLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_logout_hint_cancel_button");
                logoutAlertDialog.dismiss();
            }
        });

        logoutButton = (Button) logoutHintView.findViewById(R.id.logoutHintButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                if (sharedPrefs.getBoolean("able_sync", false)) {
                    trackInteraction("Main", "Button", "main_logout_hint_logout_sync_button");
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putBoolean("logoutsync", true);
                    editor.apply();
                    rewardedAd.show();
                } else {
                    trackInteraction("Main", "Button", "main_logout_hint_logout_button");
                    logout();
                }
            }
        });

        logoutHintBuilder.setView(logoutHintView);
        logoutAlertDialog = logoutHintBuilder.create();

        final AlertDialog.Builder downloadHintBuilder = new AlertDialog.Builder(this);
        View downloadHintView = inflater.inflate(R.layout.logout_hint, null);
        TextView downloadMessageView = (TextView) downloadHintView.findViewById(R.id.logoutHintMessage);
        downloadMessageView.setText(getString(R.string.main_download_loose_data));
        Button cancelDownloadButton = (Button) downloadHintView.findViewById(R.id.cancelHintButton);
        cancelDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_download_hint_cancel_button");
                downloadAlertDialog.dismiss();
            }
        });

        Button downloadButton = (Button) downloadHintView.findViewById(R.id.logoutHintButton);
        downloadButton.setText(getString(R.string.main_download_anyway));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_download_hint_overwrite_button");
                list = dbList;
                downloadAlertDialog.hide();
                refreshLayout(list);
            }
        });

        downloadHintBuilder.setView(downloadHintView);
        downloadAlertDialog = downloadHintBuilder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAdd:
                trackInteraction("Main", "Menu", "main_menu_add");
                showAddDialog();
                return true;
            case R.id.sync:
                trackInteraction("Main", "Menu", "main_menu_sync");
                progressDialog.setMessage(getString(R.string.progressbar_sync));
                progressDialog.show();
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                if (prefs.getBoolean("able_sync", false)) {
                    trackInteraction("Main", "Sync", "main_sync_start");
                    rewardedAd.show();
                } else {
                    trackInteraction("Main", "Sync", "main_sync_nothing_to_sync");
                    showHintAlertDialog(getString(R.string.hint), getString(R.string.main_nothing_sync));
                }
                progressDialog.hide();
                return true;
            case R.id.download:
                trackInteraction("Main", "Menu", "main_menu_download");
                progressDialog.setMessage(getString(R.string.progressbar_loading));
                progressDialog.show();
                final DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {};
                        dbList = dataSnapshot.getValue(arrayList);

                        if (dbList != null) {

                            if (list.size() > dbList.size()) {
                                trackInteraction("Main", "Download", "main_download_overwrite");
                                downloadAlertDialog.show();
                            } else if (dbList.size() == list.size()){
                                trackInteraction("Main", "Download", "main_download_up_to_date");
                                showHintAlertDialog(getString(R.string.hint), getString(R.string.main_nothing_download));
                            } else  {
                                trackInteraction("Main", "Download", "main_download_success");
                                list = dbList;
                                refreshLayout(list);
                            }
                        }  else {
                            trackInteraction("Main", "Download", "main_download_no_data_available");
                            showHintAlertDialog(getString(R.string.hint), getString(R.string.main_nothing_download));
                        }

                        progressDialog.hide();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //showHintAlertDialog("Error", "A server error occored. Try again later.");
                        trackInteraction("Main", "Download", "main_download_error");
                        progressDialog.hide();
                    }
                });
                return true;
            case R.id.logout:
                trackInteraction("Main", "Menu", "main_menu_logout");
                if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("able_sync", false)) {
                    trackInteraction("Main", "Logout", "main_logout_sync");
                    logoutButton.setText(getString(R.string.logout));
                    hintLogoutMessage.setText(getString(R.string.main_logout_sync));
                } else {
                    trackInteraction("Main", "Logout", "main_logout");
                    logoutButton.setText(getString(R.string.logout));
                    hintLogoutMessage.setText(getString(R.string.main_logout_no_sync));
                }

                logoutAlertDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshLayout(ArrayList<Weight> list) {
        trackInteraction("Main", "List", "main_list_refresh");
        saveList(list);
        weightView.setWeightArrayList(list);
        weightView.setVisibility(list.size() != 0 ? View.VISIBLE : View.GONE);
        weightView.invalidate();
        infoButton.setVisibility(list.size() != 0 ? View.VISIBLE : View.GONE);
        listView.setDivider(new ColorDrawable(Color.WHITE));
        listView.setDividerHeight(1);
        listView.setAdapter(new WeightListAdapter(getApplicationContext(), list));
        listView.setSelection(list.size() - 1);
        listView.invalidateViews();

        if (list.size() % 5 == 0) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    trackInteraction("Main", "Interstitial", "Interstitial_Closed");
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    switch (i) {
                        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                            trackInteraction("Main", "Interstitial", "Interstitial_Internal_Error");
                            break;
                        case AdRequest.ERROR_CODE_INVALID_REQUEST:
                            trackInteraction("Main", "Interstitial", "Interstitial_Invalid_Request");
                            break;
                        case AdRequest.ERROR_CODE_NETWORK_ERROR:
                            trackInteraction("Main", "Interstitial", "Interstitial_Network_Error");
                            break;
                        case AdRequest.ERROR_CODE_NO_FILL:
                            trackInteraction("Main", "Interstitial", "Interstitial_No_Ad");
                            break;
                        default:
                            trackInteraction("Main", "Interstitial", "Interstitial_Failed_Default");
                    }
                }

                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    trackInteraction("Main", "Interstitial", "Interstitial_Left");
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    trackInteraction("Main", "Interstitial", "Interstitial_Opened");
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    trackInteraction("Main", "Interstitial", "Interstitial_Loaded");
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }
    }

    private void showAddDialog() {
        if (list.size() == 0) {
            trackInteraction("Main", "Add", "main_weight_first");
            addAlertDialog.show();
        } else if (list.get(list.size() - 1).date.equals(today)) {
            trackInteraction("Main", "Add", "main_weight_tomorrow");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.add_hint));
        } else {
            trackInteraction("Main", "Add", "main_weight_more");
            addAlertDialog.show();
        }
    }

    private void saveList(ArrayList<Weight> list) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(list);

        editor.putString("list", json);
        editor.apply();
    }

    private ArrayList<Weight> getWeightList() {
        trackInteraction("Main", "List", "main_list_get");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {}.getType();
        ArrayList<Weight> l = gson.fromJson(json, type);

        if (l == null ) {
            trackInteraction("Main", "List", "main_list_download");
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {};
                    dbList = dataSnapshot.getValue(arrayList);

                    if (dbList != null) {
                        trackInteraction("Main", "List", "main_list_db_list");
                        refreshLayout(dbList);
                    } else {
                        trackInteraction("Main", "List", "main_list_empty");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    trackInteraction("Main", "List", "main_list_default_error");
                    progressDialog.hide();
                }
            });
        }

        return l == null ? new ArrayList<Weight>() : l;
    }

    private String getCurrentDate() {
        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy");
        return df.format(c.getTime());
    }

    private void setAdvertisment() {

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                trackInteraction("Main", "Banner", "Banner_Closed");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                trackInteraction("Main", "Banner", "Banner_Loaded");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                switch (i) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        trackInteraction("Main", "Banner", "Banner_Internal_Error");
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        trackInteraction("Main", "Banner", "Banner_Invalid_Request");
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        trackInteraction("Main", "Banner", "Banner_Network_Error");
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        trackInteraction("Main", "Banner", "Banner_No_Ad");
                        break;
                    default:
                        trackInteraction("Main", "Banner", "Banner_Failed_Default");
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                trackInteraction("Main", "Banner", "Banner_Opened");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                trackInteraction("Main", "Banner", "Banner_Left");
            }
        });
    }

    private void trackInteraction(String key, String value, String event) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle track = new Bundle();
        track.putString(key, value);
        analytics.logEvent(event, track);

    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    private void loadRewardedVideo() {
        rewardedAd.loadAd(getString(R.string.ad_video), new AdRequest.Builder().build());
    }

    private void logout() {
        trackInteraction("Main", "Logout", "main_logout_success");
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("USER");
        editor.remove("list");
        editor.remove("able_sync");
        editor.remove("logoutsync");
        editor.apply();

        trackInteraction("Main", "Intent", "main_open_login");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void syncDatabase() {
        progressDialog.setMessage(getString(R.string.progressbar_sync));
        progressDialog.show();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDatabase.child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(list);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("able_sync", false);
        editor.apply();
        progressDialog.hide();
    }

    private void showHintAlertDialog(String title, String message) {
        trackInteraction("Main", "Hint", "main_show_hint");
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
        hintTitleView.setText(title);
        hintMessageView.setText(message);
        hintAlertDialog.show();
    }
}
