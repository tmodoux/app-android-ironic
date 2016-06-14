package com.pryv.appAndroidExample.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.pryv.appAndroidExample.AndroidConnection;
import com.pryv.appAndroidExample.BluetoothProtocol;
import com.pryv.appAndroidExample.Credentials;
import com.pryv.appAndroidExample.R;
import com.pryv.model.Stream;
import java.util.Calendar;

public class CommunicationActivity extends AppCompatActivity {

    private BluetoothProtocol bluetoothConnection;
    private Handler messageHandler;
    private Stream voltammetryStream;
    private Stream amperometryStream;

    // Plot
    private double baseTime;
    private LineGraphSeries<DataPoint> serie1;

    private final static int MAX_DATA_POINTS = 200;

    public void setupGraph() {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        serie1 = new LineGraphSeries<>();
        graph.addSeries(serie1);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(MAX_DATA_POINTS);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(20);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        baseTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        setHandlers();
        setupGraph();

        // Initiate the connection to Pryv, providing handler which will update UI
        Credentials credentials = new Credentials(this);
        AndroidConnection.sharedInstance().setConnection(credentials.getUsername(), credentials.getToken());
        voltammetryStream = AndroidConnection.sharedInstance().saveStream("voltammetry", "voltammetry");
        amperometryStream = AndroidConnection.sharedInstance().saveStream("amperometry", "amperometry");

        bluetoothConnection = new BluetoothProtocol(PairingActivity.socket, messageHandler);
        bluetoothConnection.start();
    }

    private void setHandlers() {
        messageHandler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle b = msg.getData();

                int measure = b.getInt("content");

                AndroidConnection.sharedInstance().saveEvent(amperometryStream.getId(), "electric-current/a", "" + measure);
                AndroidConnection.sharedInstance().saveEvent(voltammetryStream.getId(), "electromotive-force/v", "" + measure);
                AndroidConnection.sharedInstance().saveEvent(voltammetryStream.getId(), "electric-current/a", "" + (measure*Math.random()));

                double currentTime = Calendar.getInstance().getTimeInMillis();
                serie1.appendData(new DataPoint(currentTime - baseTime, measure), true, MAX_DATA_POINTS);
            }
        };
    }

    public void sendStartCommand(View v) {
        bluetoothConnection.startCommand();
    }

    public void sendStopCommand(View v) {
        bluetoothConnection.stopCommand();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothConnection.cancel();
    }

    @Override
    public void onBackPressed() {
        sendStopCommand(null);
        finish();
    }

}