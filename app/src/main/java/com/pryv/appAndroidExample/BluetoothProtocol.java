package com.pryv.appAndroidExample;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by Thieb on 09.05.2016.
 */
public class BluetoothProtocol extends Thread {

    private boolean fullProtocol = false;
    private final static byte[] START_BYTE = {0x21};
    private final static byte[] STOP_BYTE = {0x20};
    private final static byte DATA_START_BYTE = 0x50;
    private boolean dataReadingPhase = false;
    private ArrayList<Integer> data;

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler handler;

    public BluetoothProtocol(BluetoothSocket socket, Handler messageHandler) {

        data = new ArrayList<>();

        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        handler = messageHandler;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);

                if(fullProtocol) {
                    if(dataReadingPhase) {
                        if(data.size()<2) {
                            data.add(bytes);
                            // Send the obtained bytes to the UI activity
                            notifyUi(bytes);
                        } else {
                            dataReadingPhase = false;
                        }
                    } else if (bytes == DATA_START_BYTE) {
                        dataReadingPhase = true;
                        data.clear();
                    }
                } else {
                    notifyUi(bytes);
                }


            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {}
    }

    private void notifyUi(int content) {
        Bundle b = new Bundle();
        b.putInt("content", content);
        Message msg = new Message();
        msg.setData(b);
        handler.sendMessage(msg);
    }

    public void startCommand() {
        write(START_BYTE);
    }

    public void stopCommand() {
        write(STOP_BYTE);
    }
}