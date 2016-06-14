package com.pryv.appAndroidExample.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pryv.appAndroidExample.Credentials;
import com.pryv.appAndroidExample.R;

/**
 * Main screen allowing to create note events to your Pryv
 * and retrieve all of them as a list of events
 */
public class MainActivity extends AppCompatActivity {

    private final static int BLUETOOTH_PAIRING_REQUEST = 1;
    private static final int LOGIN_REQUEST = 2;

    private TextView progressView;
    private Button login;

    private Credentials credentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressView = (TextView) findViewById(R.id.progress);

        login = (Button) findViewById(R.id.login);

        credentials = new Credentials(this);
        if(credentials.hasCredentials()) {
            setLogoutView();
        } else {
            setLoginView();
        }
    }

    public void discoverDevices(View v) {
        if(credentials.hasCredentials()) {
            Intent intent = new Intent(MainActivity.this, PairingActivity.class);
            startActivityForResult(intent, BLUETOOTH_PAIRING_REQUEST);
        }
        else {
            startLogin();
        }
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST);
    }

    private void setLoginView() {
        progressView.setText("Hello guest!");
        login.setText("Login");
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });
    }

    private void setLogoutView() {
        progressView.setText("Hello " + credentials.getUsername() + "!");
        login.setText("Logout");
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                credentials.resetCredentials();
                setLoginView();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLUETOOTH_PAIRING_REQUEST) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(MainActivity.this,CommunicationActivity.class);
                startActivity(intent);
            } else {
                if(data!=null) {
                    Toast.makeText(this,data.getDataString(),Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
            setLogoutView();
        }
    }

}