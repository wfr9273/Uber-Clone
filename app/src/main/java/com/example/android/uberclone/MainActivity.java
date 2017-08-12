package com.example.android.uberclone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    String userType;
    Button startButton;
    Switch switchButton;
    TextView riderText;
    TextView driverText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        startButton = (Button) findViewById(R.id.startButton);
        switchButton = (Switch) findViewById(R.id.switchButton);
        riderText = (TextView) findViewById(R.id.riderText);
        driverText = (TextView) findViewById(R.id.driverText);
        userType = "rider";

        // If the user doesn't login
        if (ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e == null) {
                        Log.i("Login", "Successfully");
                    } else {
                        Log.i("Login", "Failed");
                    }
                }
            });
        } else {
            if (ParseUser.getCurrentUser().get("riderOrDriver") != null)
                redirect();
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.getCurrentUser().put("riderOrDriver", userType);
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        redirect();
                    }
                });
            }
        });

        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    driverText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                    riderText.setTextColor(getResources().getColor(android.R.color.black));
                    userType = "driver";
                } else {
                    riderText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                    driverText.setTextColor(getResources().getColor(android.R.color.black));
                    userType = "rider";
                }
            }
        });

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

    private void redirect() {
        Intent intent;
        if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {
            intent = new Intent(this, RiderActivity.class);
        } else {
            intent = new Intent(this, DriverActivity.class);
        }
        startActivity(intent);
    }
}
