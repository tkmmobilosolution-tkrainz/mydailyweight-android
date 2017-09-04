package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Created by tkrainz on 04/09/2017.
 */

public class BmiListAdapter extends BaseAdapter {

    private ArrayList<BMI> bmiArrayList;
    private Context context;

    public BmiListAdapter(Context context, ArrayList<BMI> bmiArrayList) {
        this.bmiArrayList = bmiArrayList;
        this.context = context;
    }
    @Override
    public int getCount() {
        return bmiArrayList.size();
    }

    @Override
    public BMI getItem(int position) {
        return bmiArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LayoutInflater vi = LayoutInflater.from(context);

        if (!bmiArrayList.isEmpty()) {

            v = vi.inflate(R.layout.bmi_list_item, null);
            BMI currentWeightItem = getItem(position);

            if (currentWeightItem != null) {
                TextView dateTextView = (TextView) v.findViewById(R.id.tvDate);
                TextView weightTextView = (TextView) v.findViewById(R.id.tvWeight);
                TextView bmiTextView = (TextView) v.findViewById(R.id.tvBMI);
                TextView weightGroupTextView = (TextView) v.findViewById(R.id.tvWeightGroup);

                if (dateTextView != null) {
                    dateTextView.setText(context.getResources().getString(R.string.date) + " " + currentWeightItem.date);
                }

                if (weightTextView != null) {
                    weightTextView.setText(context.getResources().getString(R.string.weight) + " " + currentWeightItem.weight);
                }

                if (bmiTextView != null) {
                    bmiTextView.setText("BMI: " + " " + currentWeightItem.bmi);
                }

                if (weightGroupTextView != null) {
                    weightGroupTextView.setText("Du hast" + " " + currentWeightItem.weightGroup);
                }
            }
        }

        return v;
    }
}
