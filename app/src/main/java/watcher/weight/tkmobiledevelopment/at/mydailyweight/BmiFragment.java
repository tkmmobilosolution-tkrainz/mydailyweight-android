package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
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
import java.util.ArrayList;

/**
 * Created by tkrainz on 04/09/2017.
 */

public class BmiFragment extends Fragment {

    private User profile = null;

    private ArrayList<Weight> list = new ArrayList<>();
    private ArrayList<Weight> dbList = new ArrayList<>();
    private ArrayList<BMI> bmiList = new ArrayList<>();

    private ListView listView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        getProfile();

        View view = inflater.inflate(R.layout.bmi_fragment, container, false);

        getProfile();
        list = getWeightList();


        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                calculateBMI(list.get(i));
            }
        }

        if (!bmiList.isEmpty()) {
            listView = (ListView) view.findViewById(R.id.bmiList);
            listView.setDivider(new ColorDrawable(getResources().getColor(R.color.primaryYellow)));
            listView.setDividerHeight(1);
            listView.setAdapter(new BmiListAdapter(getActivity(), bmiList));
        }
        return view;
    }

    private void getProfile() {
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("USER", null);
        profile = gson.fromJson(json, User.class);

        if (profile == null) {
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_profile")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    profile = (User) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void calculateBMI(Weight currentWeight) {
        int age = Integer.parseInt(profile.getAge());
        String gender = profile.getGender();
        boolean male = gender.equals("Male");
        double height = Double.parseDouble(profile.getHeight()) / 100;
        double bmiHeight = height * height;

        double bmi = currentWeight.weightValue / bmiHeight;
        int ageGroup = ageGroup(age);
        int bmiGroup = male ? bmiGroupMale(ageGroup, bmi) : bmiGroupFemale(ageGroup, bmi);

        String weightGroup = weightGroup(bmiGroup);

        BMI currentBmi = new BMI();
        currentBmi.setDate(currentWeight.date);
        currentBmi.setBmi(bmi);
        currentBmi.setWeight(currentWeight.weightValue);
        currentBmi.setWeightGroup(bmiGroup);

        bmiList.add(currentBmi);
    }

    private int ageGroup(int age) {
        if (age <=24) {
            return 1;
        } else if (age > 24 && age <= 34) {
            return 2;
        } else if (age > 34 && age <= 44) {
            return 3;
        } else if (age > 44 && age <= 54) {
            return 4;
        } else if (age > 54 && age <= 64) {
            return 5;
        } else if (age > 64) {
            return 6;
        }

        return 0;
    }

    private int bmiGroupMale(int ageGroup, double bmi) {
        switch (ageGroup) {
            case 1:
                if (bmi >= 19 && bmi <= 24) {
                    return 2;
                } else if (bmi < 19) {
                    return 1;
                } else {
                    return 3;
                }
            case 2:
                if (bmi >= 20 && bmi <= 25) {
                    return 2;
                } else if (bmi < 20) {
                    return 1;
                } else {
                    return 3;
                }
            case 3:
                if (bmi >= 21 && bmi <= 26) {
                    return 2;
                } else if (bmi < 21) {
                    return 1;
                } else {
                    return 3;
                }
            case 4:
                if (bmi >= 22 && bmi <= 27) {
                    return 2;
                } else if (bmi < 22) {
                    return 1;
                } else {
                    return 3;
                }
            case 5:
                if (bmi >= 23 && bmi <= 28) {
                    return 2;
                } else if (bmi < 23) {
                    return 1;
                } else {
                    return 3;
                }
            case 6:
                if (bmi >= 24 && bmi <= 29) {
                    return 2;
                } else if (bmi < 24) {
                    return 1;
                } else {
                    return 3;
                }
            default:
                return 0;
        }
    }

    private int bmiGroupFemale(int ageGroup, double bmi) {
        switch (ageGroup) {
            case 1:
                if (bmi >= 18 && bmi <= 23) {
                    return 2;
                } else if (bmi < 18) {
                    return 1;
                } else {
                    return 3;
                }
            case 2:
                if (bmi >= 19 && bmi <= 24) {
                    return 2;
                } else if (bmi < 19) {
                    return 1;
                } else {
                    return 3;
                }
            case 3:
                if (bmi >= 20 && bmi <= 25) {
                    return 2;
                } else if (bmi < 20) {
                    return 1;
                } else {
                    return 3;
                }
            case 4:
                if (bmi >= 21 && bmi <= 26) {
                    return 2;
                } else if (bmi < 21) {
                    return 1;
                } else {
                    return 3;
                }
            case 5:
                if (bmi >= 22 && bmi <= 27) {
                    return 2;
                } else if (bmi < 22) {
                    return 1;
                } else {
                    return 3;
                }
            case 6:
                if (bmi >= 23 && bmi <= 28) {
                    return 2;
                } else if (bmi < 23) {
                    return 1;
                } else {
                    return 3;
                }
            default:
                return 0;
        }
    }

    private String weightGroup(int bmiGroup) {
        switch (bmiGroup) {
            case 1:
                return "Untergewicht";
            case 2:
                return "Normalgewicht";
            case 3:
                return "Übergewicht";
        }

        return "";
    }

    private ArrayList<Weight> getWeightList() {
        //trackInteraction("List", "List", "list_list_get");
        SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("list", null);
        Type type = new TypeToken<ArrayList<Weight>>() {
        }.getType();
        ArrayList<Weight> l = gson.fromJson(json, type);

        if (l == null) {
            //trackInteraction("List", "List", "list_list_download");
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference()
                .child("user_weights").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<ArrayList<Weight>> arrayList = new GenericTypeIndicator<ArrayList<Weight>>() {
                    };
                    dbList = dataSnapshot.getValue(arrayList);

                    if (dbList != null) {
                        //trackInteraction("List", "List", "list_list_db_list");
                        refreshWeightList(dbList);
                    } else {
                        //trackInteraction("List", "List", "list_list_empty");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //trackInteraction("List", "List", "list_list_default_error");
                    //progressDialog.hide();
                }
            });
        }

        if (!list.isEmpty()) {
            l = list;
        }

        return l == null ? new ArrayList<Weight>() : l;
    }

    private void refreshWeightList(ArrayList<Weight> dbList) {
        list = dbList;
    }
}
