package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by tkrainz on 06/09/2017.
 */

public class PrivacyPolicyMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.privacy_policy_menu, null);

        TextView tv = (TextView) view.findViewById(R.id.policyText);
        tv.setText(Html.fromHtml(getString(R.string.privacy_policy)));

        return view;
    }
}
