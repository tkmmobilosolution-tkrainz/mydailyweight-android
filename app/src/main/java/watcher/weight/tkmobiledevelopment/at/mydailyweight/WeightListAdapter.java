package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Created by tkrainz on 10/02/2017.
 */

public class WeightListAdapter extends BaseAdapter {

    private ArrayList<Weight> weightArrayList;
    private ArrayList<BMI> bmiArrayList;
    private Context context;
    private double startWeight;

    public WeightListAdapter(Context context, ArrayList<Weight> weightArrayList,
        ArrayList<BMI> bmiArrayList, double startWeight) {
        this.weightArrayList = weightArrayList;
        this.context = context;
        this.bmiArrayList = bmiArrayList;
        this.startWeight = startWeight;
    }

    @Override
    public int getCount() {
        return weightArrayList.size() > 0 ? weightArrayList.size() + 1 : 1;
    }

    @Override
    public Weight getItem(int position) {
        return weightArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LayoutInflater vi = LayoutInflater.from(context);

        if (weightArrayList.size() == 0 || position == weightArrayList.size()) {

            v = vi.inflate(R.layout.list_item_placeholder, null);
            TextView placeholderTextView = (TextView) v.findViewById(R.id.placeholderTextView);
            placeholderTextView.setText(weightArrayList.size() == 0 ? context.getResources()
                .getString(R.string.add_first_weight)
                : context.getResources().getString(R.string.add_another_weight));
        } else {

            v = vi.inflate(R.layout.weight_list_item, null);
            Weight currentWeightItem = getItem(position);
            BMI bmi = bmiArrayList.get(position);

            if (currentWeightItem != null) {
                TextView dateTextView = (TextView) v.findViewById(R.id.dateTextView);
                TextView weightTextView = (TextView) v.findViewById(R.id.weightTextView);
                TextView bmiTextView = (TextView) v.findViewById(R.id.tvBmi);
                TextView difTextView = (TextView) v.findViewById(R.id.tvDiffernece);

                if (dateTextView != null) {
                    dateTextView.setText(currentWeightItem.date);
                }

                if (weightTextView != null) {
                    weightTextView.setText(currentWeightItem.weightValue + "kg");
                }

                if (bmiTextView != null) {
                    if (bmi.getWeightGroup() != 2) {
                        bmiTextView.setTextColor(Color.RED);
                    } else {
                        bmiTextView.setTextColor(Color.GREEN);
                    }

                    bmiTextView.setText("BMI: " + String.format("%.2f", bmi.getBmi()));
                }

                if (difTextView != null) {
                    double differnece = startWeight - currentWeightItem.weightValue;
                    String difString = "";
                    if (currentWeightItem.weightValue > startWeight) {
                        difString = "+" + String.format("%.2f", differnece) + "kg";
                    } else if (currentWeightItem.weightValue < startWeight) {
                        difString = "-" + String.format("%.2f", differnece) + "kg";
                    } else if (currentWeightItem.weightValue == startWeight && !currentWeightItem.date.equals(context.getString(R.string.start_weight))) {
                        difString = "keine Veränderung";
                    }
                    difTextView.setText(difString);
                }
            }
        }

        return v;
    }
}
