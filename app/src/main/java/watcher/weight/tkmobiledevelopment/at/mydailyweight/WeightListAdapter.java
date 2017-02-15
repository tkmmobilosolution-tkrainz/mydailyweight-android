package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.Context;
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
    private Context context;

    public WeightListAdapter(Context context, ArrayList<Weight> weightArrayList) {
        this.weightArrayList = weightArrayList;
        this.context = context;
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
            placeholderTextView.setText(weightArrayList.size() == 0 ? context.getResources().getString(R.string.add_first_weight) : context.getResources().getString(R.string.add_another_weight));
        } else {

            v = vi.inflate(R.layout.weight_list_item, null);
            Weight currentWeightItem = getItem(position);

            if (currentWeightItem != null) {
                TextView dateTextView = (TextView) v.findViewById(R.id.dateTextView);
                TextView weightTextView = (TextView) v.findViewById(R.id.weightTextView);

                if (dateTextView != null) {
                    dateTextView.setText(context.getResources().getString(R.string.date) + " " + currentWeightItem.date);
                }

                if (weightTextView != null) {
                    weightTextView.setText(context.getResources().getString(R.string.weight) + " " + currentWeightItem.weightValue);
                }
            }
        }

        return v;
    }
}
