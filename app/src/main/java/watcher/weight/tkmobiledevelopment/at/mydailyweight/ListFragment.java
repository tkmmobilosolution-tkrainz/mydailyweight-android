package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

/**
 * Created by tkrainz on 06/07/2017.
 */

public class ListFragment extends Fragment {

    private AlertDialog addAlertDialog;
    private AlertDialog hintAlertDialog;
    private AlertDialog downloadAlertDialog;
    private AlertDialog timeAlertDialog;

    private TextView hintTitleView, hintMessageView;

    private Button infoButton;

    private ArrayList<Weight> list = new ArrayList<>();
    private ArrayList<Weight> dbList = new ArrayList<>();
    private ListView listView;
    private String today;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private InterstitialAd mInterstitialAd;
    private RewardedVideoAd rewardedAd;

    private ProgressDialog progressDialog = null;

    private FirebaseAnalytics analytics = null;

    private boolean rewardedFinished = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        analytics = FirebaseAnalytics.getInstance(getActivity());

        View view = inflater.inflate(R.layout.list_fragment, container, false);

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
                trackInteraction("List", "Rewarded", "list_sync_opened");
            }

            @Override
            public void onRewardedVideoStarted() {

            }

            @Override
            public void onRewardedVideoAdClosed() {
                if (!rewardedFinished) {
                    trackInteraction("List", "Rewarded", "list_sync_canceled");
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

                trackInteraction("List", "Rewarded", "list_sync_success");
                syncDatabase();
            }

            @Override
            public void onRewardedVideoAdLeftApplication() {

            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {
                trackInteraction("List", "Rewarded", "list_rewarded_error_" + i);
                Log.e("Rewarded error", "" + i);
            }
        });

        progressDialog = new ProgressDialog(getActivity(), R.style.SpinnerTheme);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);

        today = getCurrentDate();
        list = getWeightList();

        infoButton = (Button) view.findViewById(R.id.infoButton);
        infoButton.setText(getString(R.string.main_sync_button_title));
        infoButton.setVisibility(!list.isEmpty() ? View.VISIBLE : View.GONE);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("List", "Button", "list_progress_button");
                startSync();
            }
        });

        listView = (ListView) view.findViewById(R.id.fragment_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (list.isEmpty() || position == list.size()) {
                    trackInteraction("List", "List", "list_list_add");
                    showAddDialog();
                }
            }
        });
        listView.setDivider(new ColorDrawable(list.size() > 0 ? Color.WHITE : Color.TRANSPARENT));
        listView.setAdapter(new WeightListAdapter(getActivity(), list));

        AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(getActivity());
        View addAlertView = inflater.inflate(R.layout.add_alert, null);

        final EditText newWeight = (EditText) addAlertView.findViewById(R.id.editText);

        Button addButton = (Button) addAlertView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newWeight.getText().toString().equals("")) {
                    trackInteraction("List", "Button", "list_add_hint_button_failed");
                    Toast.makeText(getActivity(),
                        getResources().getString(R.string.no_weight_entered), Toast.LENGTH_LONG)
                        .show();
                } else {
                    progressDialog.setMessage(getString(R.string.progressbar_refresh));
                    progressDialog.show();
                    trackInteraction("List", "Button", "list_add_hint_button_success");
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
                trackInteraction("List", "Button", "list_add_hint_button_cancel");
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
                trackInteraction("List", "Button", "list_hint_button");
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
                trackInteraction("List", "Button", "list_download_hint_cancel_button");
                downloadAlertDialog.dismiss();
            }
        });

        Button downloadButton = (Button) downloadHintView.findViewById(R.id.logoutHintButton);
        downloadButton.setText(getString(R.string.main_download_anyway));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("List", "Button", "list_download_hint_overwrite_button");
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
                trackInteraction("List", "Button", "list_cancel_time");
                timeAlertDialog.dismiss();
            }
        });

        Button timeAddButton = (Button) timeHintView.findViewById(R.id.tPAdd);
        timeAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackInteraction("List", "Button", "list_set_time");
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

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAdd:
                trackInteraction("List", "Menu", "list_menu_add");
                showAddDialog();
                return true;
            case R.id.menuProgress:
                trackInteraction("List", "Menu", "list_progress");
                showProgress();
                return true;
            case R.id.menuTimePicker:
                trackInteraction("List", "Menu", "list_time_picker");
                timeAlertDialog.show();
                return true;
            case R.id.download:
                trackInteraction("List", "Menu", "list_menu_download");
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
                                trackInteraction("List", "Download", "list_download_overwrite");
                                downloadAlertDialog.show();
                            } else if (dbList.size() == list.size()) {
                                trackInteraction("List", "Download", "list_download_up_to_date");
                                showHintAlertDialog(getString(R.string.hint),
                                    getString(R.string.main_nothing_download));
                            } else {
                                trackInteraction("List", "Download", "list_download_success");
                                list = dbList;
                                refreshLayout(list);
                            }
                        } else {
                            trackInteraction("List", "Download", "list_download_no_data_available");
                            showHintAlertDialog(getString(R.string.hint),
                                getString(R.string.main_nothing_download));
                        }

                        progressDialog.hide();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //showHintAlertDialog("Error", "A server error occored. Try again later.");
                        trackInteraction("List", "Download", "list_download_error");
                        progressDialog.hide();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshLayout(ArrayList<Weight> list) {
        trackInteraction("List", "List", "list_list_refresh");
        saveList(list);
        infoButton.setVisibility(list.size() != 0 ? View.VISIBLE : View.GONE);
        listView.setDivider(new ColorDrawable(Color.WHITE));
        listView.setDividerHeight(1);
        listView.setAdapter(new WeightListAdapter(getActivity(), list));
        listView.setSelection(list.size() - 1);
        listView.invalidateViews();

        if (list.size() % 5 == 0) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    trackInteraction("List", "Interstitial", "list_interstitial_Closed");
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    switch (i) {
                        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                            trackInteraction("List", "Interstitial",
                                "list_interstitial_Internal_Error");
                            break;
                        case AdRequest.ERROR_CODE_INVALID_REQUEST:
                            trackInteraction("List", "Interstitial",
                                "list_interstitial_Invalid_Request");
                            break;
                        case AdRequest.ERROR_CODE_NETWORK_ERROR:
                            trackInteraction("List", "Interstitial",
                                "list_interstitial_Network_Error");
                            break;
                        case AdRequest.ERROR_CODE_NO_FILL:
                            trackInteraction("List", "Interstitial", "list_interstitial_No_Ad");
                            break;
                        default:
                            trackInteraction("List", "Interstitial",
                                "list_interstitial_Failed_Default");
                    }
                }

                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    trackInteraction("List", "Interstitial", "list_interstitial_Left");
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    trackInteraction("List", "Interstitial", "list_interstitial_Opened");
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    trackInteraction("List", "Interstitial", "list_interstitial_Loaded");
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }
    }

    private void showAddDialog() {
        if (list.size() == 0) {
            trackInteraction("List", "Add", "list_weight_first");
            addAlertDialog.show();
        } else if (list.get(list.size() - 1).date.equals(today)) {
            trackInteraction("List", "Add", "list_weight_tomorrow");
            showHintAlertDialog(getString(R.string.hint), getString(R.string.add_hint));
        } else {
            trackInteraction("List", "Add", "list_weight_more");
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
        trackInteraction("List", "List", "list_list_get");
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {
        }.getType();
        ArrayList<Weight> l = gson.fromJson(json, type);

        if (l == null) {
            trackInteraction("List", "List", "list_list_download");
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {
                    };
                    dbList = dataSnapshot.getValue(arrayList);

                    if (dbList != null) {
                        trackInteraction("List", "List", "list_list_db_list");
                        refreshLayout(dbList);
                    } else {
                        trackInteraction("List", "List", "list_list_empty");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    trackInteraction("List", "List", "list_list_default_error");
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
                trackInteraction("List", "Banner", "list_banner_Closed");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                trackInteraction("List", "Banner", "list_banner_Loaded");
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                switch (i) {
                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                        trackInteraction("List", "Banner", "list_banner_Internal_Error");
                        break;
                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
                        trackInteraction("List", "Banner", "list-banner_Invalid_Request");
                        break;
                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
                        trackInteraction("List", "Banner", "list-banner_Network_Error");
                        break;
                    case AdRequest.ERROR_CODE_NO_FILL:
                        trackInteraction("List", "Banner", "list-banner_No_Ad");
                        break;
                    default:
                        trackInteraction("List", "Banner", "list-banner_Failed_Default");
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                trackInteraction("List", "Banner", "list-banner_Opened");
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                trackInteraction("List", "Banner", "list-banner_Left");
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
        trackInteraction("List", "Hint", "list_show_hint");
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
            trackInteraction("List", "Sync", "list_sync_start");
            rewardedAd.show();
        } else {
            trackInteraction("List", "Sync", "list_sync_nothing_to_sync");
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
}
