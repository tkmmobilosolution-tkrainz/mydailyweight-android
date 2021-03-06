package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
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

public class MainFragment extends Fragment {

    private AlertDialog addAlertDialog;
    private AlertDialog hintAlertDialog;
    private AlertDialog downloadAlertDialog;
    private AlertDialog timeAlertDialog;

    private TextView hintTitleView, hintMessageView;

    private Button infoButton;

    private ArrayList<Weight> list = new ArrayList<>();
    private ArrayList<Weight> dbList = new ArrayList<>();
    private WeightView weightView;
    private String today;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private InterstitialAd mInterstitialAd;
    private RewardedVideoAd rewardedAd;

    private ProgressDialog progressDialog = null;

    private FirebaseAnalytics analytics = null;

    private boolean rewardedFinished = false;

    private User user;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        analytics = FirebaseAnalytics.getInstance(getActivity());

        getProfile();

        View view = inflater.inflate(R.layout.activity_main, container, false);

        MobileAds.initialize(getActivity(), getString(R.string.ad_mob_app_id));
        setAdvertisment(view);

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstital_ad_unit_id));
        requestNewInterstitial();

        rewardedAd = MobileAds.getRewardedVideoAdInstance(getActivity());
        loadRewardedVideo();
        rewardedAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {

            }

            @Override
            public void onRewardedVideoAdOpened() {
                trackInteraction("Main", "Rewarded", "main_sync_opened");
            }

            @Override
            public void onRewardedVideoStarted() {

            }

            @Override
            public void onRewardedVideoAdClosed() {
                if (!rewardedFinished) {
                    trackInteraction("Main", "Rewarded", "main_sync_canceled");
                    loadRewardedVideo();
                    Toast.makeText(getActivity(), getString(R.string.main_sync_failed),
                        Toast.LENGTH_SHORT).show();
                }

                rewardedFinished = false;
                Log.e("Rewarded error", "");
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {
                rewardedFinished = true;

                trackInteraction("Main", "Rewarded", "main_sync_success");
                syncDatabase();
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

        progressDialog = new ProgressDialog(getActivity(), R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

        today = getCurrentDate();
        list = getWeightList();

        if (list.isEmpty() || !list.get(0).date.equals(getString(R.string.start_weight))) {
            list.add(0, new Weight(Double.parseDouble(user.getCurrentWeight()), getString(R.string.start_weight)));
        }

        String infoButtonString = list.isEmpty() ? getString(R.string.main_button_first_weight) : getString(R.string.main_sync_button_title);
        infoButton = (Button) view.findViewById(R.id.infoButton);
        infoButton.setText(infoButtonString);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (list.isEmpty()) {
                    trackInteraction("Main", "Button", "main_button_first_weight");
                    showAddDialog();
                } else {
                    trackInteraction("Main", "Button", "main_progress_button");
                    startSync();
                }
            }
        });

        weightView = (WeightView) view.findViewById(R.id.weightView);
        weightView.setVisibility(list.size() > 0 ? View.VISIBLE : View.GONE);
        weightView.setWeightArrayList(list);

        AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(getActivity());
        View addAlertView = inflater.inflate(R.layout.add_alert, null);

        final EditText newWeight = (EditText) addAlertView.findViewById(R.id.editText);

        Button addButton = (Button) addAlertView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newWeight.getText().toString().equals("")) {
                    trackInteraction("Main", "Button", "main_add_hint_button_failed");
                    Toast.makeText(getActivity(),
                        getResources().getString(R.string.no_weight_entered), Toast.LENGTH_LONG)
                        .show();
                } else {
                    progressDialog.setMessage(getString(R.string.progressbar_refresh));
                    progressDialog.show();
                    trackInteraction("Main", "Button", "main_add_hint_button_success");
                    double weight = Double.parseDouble(newWeight.getText().toString());
                    Weight currentWeight = new Weight(weight, today);
                    list.add(currentWeight);
                    refreshLayout(list);

                    SharedPreferences sharedPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
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
        addAlertDialog.getWindow()
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        AlertDialog.Builder dialogHintBuilder = new AlertDialog.Builder(getActivity());
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

        final AlertDialog.Builder downloadHintBuilder = new AlertDialog.Builder(getActivity());
        View downloadHintView = inflater.inflate(R.layout.logout_hint, null);
        TextView downloadMessageView = (TextView) downloadHintView
            .findViewById(R.id.logoutHintMessage);
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

        final AlertDialog.Builder timeHintBuilder = new AlertDialog.Builder(getActivity());
        View timeHintView = inflater.inflate(R.layout.time_picker_alert, null);

        int hour = PreferenceManager.getDefaultSharedPreferences(getActivity())
            .getInt("timeHour", -1);
        int minute = PreferenceManager.getDefaultSharedPreferences(getActivity())
            .getInt("timeMinute", -1);

        final TimePicker timePicker = (TimePicker) timeHintView.findViewById(R.id.timePicker);

        if (hour != -1 && minute != -1) {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        Button timeCancelButton = (Button) timeHintView.findViewById(R.id.tPCancel);
        timeCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_cancel_time");
                timeAlertDialog.dismiss();
            }
        });

        Button timeAddButton = (Button) timeHintView.findViewById(R.id.tPAdd);
        timeAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "main_set_time");
                int hour = timePicker.getCurrentHour();
                int minute = timePicker.getCurrentMinute();

                setupAlarmManager(hour, minute);

                SharedPreferences sharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt("timeHour", hour);
                editor.putInt("timeMinute", minute);
                editor.commit();
                editor.apply();

                timeAlertDialog.hide();
            }
        });

        timeHintBuilder.setView(timeHintView);
        timeAlertDialog = timeHintBuilder.create();

        Button buttonAddWeight = (Button) view.findViewById(R.id.addWeightButton);
        buttonAddWeight.setText(getString(R.string.menu_add));
        buttonAddWeight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("Main", "Button", "add_weight");
                showAddDialog();
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuTimePicker:
                trackInteraction("Main", "Menu", "main_time_picker");
                timeAlertDialog.show();
                return true;
            case R.id.download:
                trackInteraction("Main", "Menu", "main_menu_download");
                progressDialog.setMessage(getString(R.string.progressbar_loading));
                progressDialog.show();
                final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                    .child("user_weights")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {
                        };
                        dbList = dataSnapshot.getValue(arrayList);

                        if (dbList != null) {

                            if (list.size() > dbList.size()) {
                                trackInteraction("Main", "Download", "main_download_overwrite");
                                downloadAlertDialog.show();
                            } else if (dbList.size() == list.size()) {
                                trackInteraction("Main", "Download", "main_download_up_to_date");
                                showHintAlertDialog(getString(R.string.hint),
                                    getString(R.string.main_nothing_download));
                            } else {
                                trackInteraction("Main", "Download", "main_download_success");
                                list = dbList;
                                refreshLayout(list);
                            }
                        } else {
                            trackInteraction("Main", "Download", "main_download_no_data_available");
                            showHintAlertDialog(getString(R.string.hint),
                                getString(R.string.main_nothing_download));
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

        if (list.size() % 5 == 0) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    trackInteraction("Main", "Interstitial", "main_interstitial_Closed");
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    switch (i) {
                        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                            trackInteraction("Main", "Interstitial",
                                "main_interstitial_Internal_Error");
                            break;
                        case AdRequest.ERROR_CODE_INVALID_REQUEST:
                            trackInteraction("Main", "Interstitial",
                                "Interstitial_Invalid_Request");
                            break;
                        case AdRequest.ERROR_CODE_NETWORK_ERROR:
                            trackInteraction("Main", "Interstitial",
                                "main_interstitial_Network_Error");
                            break;
                        case AdRequest.ERROR_CODE_NO_FILL:
                            trackInteraction("Main", "Interstitial", "main_interstitial_No_Ad");
                            break;
                        default:
                            trackInteraction("Main", "Interstitial",
                                "main_interstitial_Failed_Default");
                    }
                }

                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    trackInteraction("Main", "Interstitial", "main_interstitial_Left");
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    trackInteraction("Main", "Interstitial", "main_interstitial_Opened");
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    trackInteraction("Main", "Interstitial", "main_interstitial_Loaded");
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
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(list);

        editor.putString("list", json);
        editor.apply();
    }

    private ArrayList<Weight> getWeightList() {
        trackInteraction("Main", "List", "main_list_get");
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {
        }.getType();
        ArrayList<Weight> l = gson.fromJson(json, type);

        if (l == null) {
            trackInteraction("Main", "List", "main_list_download");
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {
                    };
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

    private void setAdvertisment(View view) {

        AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                trackInteraction("Main", "Banner", "main_banner_Closed");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                trackInteraction("Main", "Banner", "main_banner_Loaded");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                switch (i) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        trackInteraction("Main", "Banner", "main_banner_Internal_Error");
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        trackInteraction("Main", "Banner", "main_banner_Invalid_Request");
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        trackInteraction("Main", "Banner", "main_banner_Network_Error");
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        trackInteraction("Main", "Banner", "main_banner_No_Ad");
                        break;
                    default:
                        trackInteraction("Main", "Banner", "main_banner_Failed_Default");
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                trackInteraction("Main", "Banner", "main_banner_Opened");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                trackInteraction("Main", "Banner", "main_banner_Left");
            }
        });
    }

    private void trackInteraction(String key, String value, String event) {
        Bundle track = new Bundle();
        track.putString(key, value);

        if (analytics != null) {
            analytics.logEvent(event, track);
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    private void loadRewardedVideo() {
        rewardedAd.loadAd(getString(R.string.ad_video), new AdRequest.Builder().build());
    }

    private void syncDatabase() {
        progressDialog.setMessage(getString(R.string.progressbar_sync));
        progressDialog.show();
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        mDatabase.child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .setValue(list);
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

    private void showProgress() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Gson gson = new Gson();
        User savedUser = gson.fromJson(pref.getString("USER", null), User.class);
        double differenceSinceBegin = Double.parseDouble(savedUser.getCurrentWeight()) - list
            .get(list.size() - 1).weightValue;
        double differenceToDream =
            Double.parseDouble(savedUser.getDreamWeight()) - list.get(list.size() - 1).weightValue;
        String hintText = getString(R.string.main_diff_sinde_begin) + String
            .format("%.2f", Math.abs(differenceSinceBegin)) + " kg" + "\n\n" +
            getString(R.string.main_till_end) + String.format("%.2f", Math.abs(differenceToDream))
            + " kg";

        showHintAlertDialog(getString(R.string.main_weight_information), hintText);
    }

    private void startSync() {
        progressDialog.setMessage(getString(R.string.progressbar_sync));
        progressDialog.show();
        final SharedPreferences prefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());

        if (prefs.getBoolean("able_sync", false)) {
            trackInteraction("Main", "Sync", "main_sync_start");
            rewardedAd.show();
        } else {
            trackInteraction("Main", "Sync", "main_sync_nothing_to_sync");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.main_nothing_sync));
        }
        progressDialog.hide();
    }

    private void setupAlarmManager(int hour, int minute) {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        PendingIntent pi = PendingIntent
            .getBroadcast(getActivity(), 0, new Intent(getActivity(), AlarmReceiver.class), 0);

        AlarmManager manager = (AlarmManager) getActivity()
            .getSystemService(getActivity().ALARM_SERVICE);
        manager
            .setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY,
                pi);
    }

    private void getProfile() {
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("USER", null);
        user = gson.fromJson(json, User.class);

        if (user == null) {
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_profile")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user = (User) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
