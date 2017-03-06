package watcher.weight.tkmobiledevelopment.at.mydailyweight;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private AlertDialog addAlertDialog;
    private AlertDialog hintAlertDialog;

    private ArrayList<Weight> list = new ArrayList<>();
    private ListView listView;
    private WeightView weightView;
    private String today;

    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_mob_app_id));
        setAdvertisment();

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstital_ad_unit_id));
        requestNewInterstitial();

        trackInteraction("MainActivity", "start", "Screen_MainActivity");

        today = getCurrentDate();
        list = getWeightList();


        weightView = (WeightView) findViewById(R.id.weightView);
        weightView.setVisibility(list.size() > 0 ? View.VISIBLE : View.GONE);
        weightView.setWeightArrayList(list);

        LayoutInflater inflater = this.getLayoutInflater();
        listView = (ListView) findViewById(R.id.wightList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (list.size() == 0 || position == list.size()) {
                    trackInteraction("List", "add", "Click_Add_List");
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
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_weight_entered), Toast.LENGTH_LONG).show();
                } else {
                    trackInteraction("Button", "Add", "Click_Add_Button");
                    double weight = Double.parseDouble(newWeight.getText().toString());
                    Weight currentWeight = new Weight(weight, today);
                    list.add(currentWeight);
                    refreshLayout(list);
                    addAlertDialog.dismiss();
                }
            }
        });

        Button cancelButton = (Button) addAlertView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Button", "Cancel", "Click_Cancel_Button");
                addAlertDialog.dismiss();
            }
        });

        addDialogBuilder.setView(addAlertView);
        addAlertDialog = addDialogBuilder.create();
        addAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(this);
        View hintAlertView = inflater.inflate(R.layout.hint_alert, null);

        Button hintButton = (Button) hintAlertView.findViewById(R.id.hintButton);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Button", "Hint", "Click_Hint_Button");
                hintAlertDialog.dismiss();
            }
        });

        dialogHintBuilder.setView(hintAlertView);
        hintAlertDialog = dialogHintBuilder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem_Info = menu.add(0, R.id.menuid_add, 0, "").setIcon(android.R.drawable.ic_menu_edit);
        MenuItem menuItem_user = menu.add(1, R.id.menuid_user, 1, "").setIcon(android.R.drawable.ic_menu_more);
        MenuItemCompat.setShowAsAction(menuItem_Info,
                MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        MenuItemCompat.setShowAsAction(menuItem_user,
                MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuid_add) {
            trackInteraction("Menu", "Add", "Click_Add_Menu");
            showAddDialog();
        } else if (item.getItemId() == R.id.menuid_user) {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshLayout(ArrayList<Weight> list) {

        saveList(list);
        weightView.setWeightArrayList(list);
        weightView.setVisibility(list.size() != 0 ? View.VISIBLE : View.GONE);
        weightView.invalidate();
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
                    trackInteraction("Interstitial", "closed", "Interstitial_Closed");
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    switch (i) {
                        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                            trackInteraction("Interstitial", "failed", "Interstitial_Internal_Error");
                            break;
                        case AdRequest.ERROR_CODE_INVALID_REQUEST:
                            trackInteraction("Interstitial", "failed", "Interstitial_Invalid_Request");
                            break;
                        case AdRequest.ERROR_CODE_NETWORK_ERROR:
                            trackInteraction("Interstitial", "failed", "Interstitial_Network_Error");
                            break;
                        case AdRequest.ERROR_CODE_NO_FILL:
                            trackInteraction("Interstitial", "failed", "Interstitial_No_Ad");
                            break;
                        default:
                            trackInteraction("Interstitial", "failed", "Interstitial_Failed_Default");
                    }
                }

                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    trackInteraction("Interstitial", "leftApp", "Interstitial_Left");
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    trackInteraction("Interstitial", "opened", "Interstitial_Opened");
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    trackInteraction("Interstitial", "loaded", "Interstitial_Loaded");
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }
    }

    private void showAddDialog() {
        if (list.size() == 0) {
            trackInteraction("Add", "first", "Weight_Add_First");
            addAlertDialog.show();
        } else if (list.get(list.size() - 1).date.equals(today)) {
            trackInteraction("Add", "tomorrow", "Weight_Add_Tomorrow");
            hintAlertDialog.show();
        } else {
            trackInteraction("Add", "more", "Weight_Add_More");
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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {}.getType();
        ArrayList<Weight> l = gson.fromJson(json, type);
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
                trackInteraction("Banner", "closed", "Banner_Closed");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                trackInteraction("Banner", "loaded", "Banner_Loaded");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                switch (i) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        trackInteraction("Banner", "failed", "Banner_Internal_Error");
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        trackInteraction("Banner", "failed", "Banner_Invalid_Request");
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        trackInteraction("Banner", "failed", "Banner_Network_Error");
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        trackInteraction("Banner", "failed", "Banner_No_Ad");
                        break;
                    default:
                        trackInteraction("Banner", "failed", "Banner_Failed_Default");
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                trackInteraction("Banner", "opened", "Banner_Opened");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                trackInteraction("Banner", "leftApp", "Banner_Left");
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

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USER", null);
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
