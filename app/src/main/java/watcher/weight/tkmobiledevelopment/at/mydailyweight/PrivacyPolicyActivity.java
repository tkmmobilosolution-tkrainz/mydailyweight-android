package watcher.weight.tkmobiledevelopment.at.mydailyweight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by tkrainz on 06/09/2017.
 */

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.privacy_policy);

        TextView privacyPolicyView = (TextView)findViewById(R.id.pPText);
        privacyPolicyView.setText(Html.fromHtml(getString(R.string.privacy_policy)));

        Button btn = (Button) findViewById(R.id.buttonPP);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PrivacyPolicyActivity.this, StartupActivity.class);
                startActivity(intent);

                SharedPreferences sharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(PrivacyPolicyActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean("privacy_policy_accept", true);
                editor.apply();
            }
        });

    }
}
