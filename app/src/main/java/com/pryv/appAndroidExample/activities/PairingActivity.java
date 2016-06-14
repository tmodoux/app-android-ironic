package com.pryv.appAndroidExample.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pryv.appAndroidExample.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PairingActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private ArrayList<String> devicesName;
    private HashMap<String,BluetoothDevice> devices;
    private ArrayAdapter adapter;
    private BroadcastReceiver broadcastReceiver;
    private BluetoothAdapter bluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static BluetoothSocket socket;
    private TextView discoveryState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        discoveryState = (TextView) findViewById(R.id.discoveryState);
        Button refresh = (Button) findViewById(R.id.restartDiscovery);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
            }
        });

        // Set up devices list
        ListView devicesList = (ListView) findViewById(R.id.devicesList);
        devicesName = new ArrayList<>();
        devices = new HashMap<>();
        adapter = new ArrayAdapter(this, R.layout.list_item, devicesName);
        devicesList.setAdapter(adapter);
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = ((TextView) view).getText().toString();
                new ConnectAsync(devices.get(name)).execute();
            }
        });

        // Check bluetooth availability and start discovery
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            returnWithResult("Bluetooth not supported!", RESULT_CANCELED);
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startDiscovery();
        }
    }

    private void returnWithResult(String result, int status) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(result));
        setResult(status, intent);
        finish();
    }

    private void startDiscovery() {
        discoveryState.setText("Discovery state: in progress");

        if(broadcastReceiver == null) {
            initReceiver();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(broadcastReceiver, filter);
        }
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        devicesName.clear();
        devices.clear();
        adapter.notifyDataSetChanged();
        bluetoothAdapter.startDiscovery();
    }

    private void initReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String name = device.getName() + "\n" + device.getAddress();
                    if(!devicesName.contains(name)) {
                        devicesName.add(name);
                        devices.put(name,device);
                        adapter.notifyDataSetChanged();
                    }
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    discoveryState.setText("Discovery state: finish");
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver!=null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        returnWithResult("Bluetooth discovery canceled!", RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startDiscovery();
            } else {
                returnWithResult("Bluetooth activation canceled!", resultCode);
            }
        }
    }

    private class ConnectAsync extends AsyncTask <Void, Void, Void> {
        private BluetoothDevice device;
        private ProgressDialog progressDialog;

        public ConnectAsync (BluetoothDevice device) {
            this.device = device;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(PairingActivity.this);
            progressDialog.setMessage("Trying to pair...");
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            socket = tmp;

            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket
                closeSocket();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(socket == null) {
                progressDialog.dismiss();
                Toast.makeText(PairingActivity.this,"Unable to pair with " + device.getName(),Toast.LENGTH_SHORT).show();
            } else {
                progressDialog.setMessage("Pairing successful!");
                setResult(RESULT_OK, null);
                finish();
            }
        }

        private void closeSocket() {
            try {
                socket.close();
                socket = null;
            } catch (IOException closeException) { }
        }
    }

}