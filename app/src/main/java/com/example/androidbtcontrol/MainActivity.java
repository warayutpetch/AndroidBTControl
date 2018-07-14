/*
Android Example to connect to and communicate with Bluetooth
In this exercise, the target is a Arduino Due + HC-06 (Bluetooth Module)

Ref:
- Make BlueTooth connection between Android devices
http://android-er.blogspot.com/2014/12/make-bluetooth-connection-between.html
- Bluetooth communication between Android devices
http://android-er.blogspot.com/2014/12/bluetooth-communication-between-android.html
 */
package com.example.androidbtcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.media.RingtoneManager;
import android.media.Ringtone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;
    Uri uriAlarm;
    Ringtone ringTone;
    TextView textInfo, textStatus, textTest;
    ListView listViewPairedDevice;
    LinearLayout inputPane;
    EditText inputField;
    Button btnSend;
    View leftTop, topCenter, rightTop, leftCenter, centerLeft, center, centerRight, rightCenter, leftBot, centerBot, rightBot;
    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;

    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInfo = (TextView) findViewById(R.id.info);
        textStatus = (TextView) findViewById(R.id.status);
        listViewPairedDevice = (ListView) findViewById(R.id.pairedlist);

        inputPane = (LinearLayout) findViewById(R.id.inputpane);
        inputField = (EditText) findViewById(R.id.input);
        btnSend = (Button) findViewById(R.id.send);
        btnSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myThreadConnected != null) {
                    byte[] bytesToSend = inputField.getText().toString().getBytes();
                    myThreadConnected.write(bytesToSend);
                }
            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
        textInfo.setText(stInfo);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice pairedDevice : bluetoothAdapter.getBondedDevices()) {

            Log.d("log", "\tDevice Name: " + pairedDevice.getName());
            Log.d("log", "\tDevice MAC: " + pairedDevice.getAddress());

        }
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
                Log.e("ssss", pairedDevices.toString());
            }

            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    textStatus.setText("start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (myThreadConnectBTdevice != null) {
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                setup();
            } else {
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket) {

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if (success) {
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            textStatus.setText(msgconnected);

                            setContentView(R.layout.activity_about);

                            leftTop = (View) findViewById(R.id.leftTop);
                            topCenter = (View) findViewById(R.id.topCenter);
                            rightTop = (View) findViewById(R.id.rightTop);
                            leftCenter = (View) findViewById(R.id.leftCenter);
                            centerLeft = (View) findViewById(R.id.centerLeft);
                            center = (View) findViewById(R.id.center);
                            centerRight = (View) findViewById(R.id.centerRight);
                            rightCenter = (View) findViewById(R.id.rightCenter);
                            leftBot = (View) findViewById(R.id.leftBot);
                            centerBot = (View) findViewById(R.id.centerBot);
                            rightBot = (View) findViewById(R.id.rightBot);
                            textTest = (TextView) findViewById(R.id.textView2);
                            //  leftTop,topCenter,rightTop,leftCenter,centerLeft,center,centerRight,rightCenter,leftBot,centerBot,rightBot

                            listViewPairedDevice.setVisibility(View.GONE);
                            inputPane.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Error Open Again",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

                startThreadConnected(bluetoothSocket);

            } else {
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    final String strReceived = new String(buffer, 0, bytes);
//                    final String msgReceived = String.valueOf(bytes) +
//                            " bytes received:\n"
//                            + strReceived;
                    final String msgReceived = String.valueOf(bytes) +
                            " bytes received:\n"
                            + strReceived;

                    runOnUiThread(new Runnable() {
                        ///////recive
                        boolean play = false;
                        @Override
                        public void run() {
                            //  leftTop,topCenter,rightTop,leftCenter,centerLeft,center,centerRight,rightCenter,leftBot,centerBot,rightBot

                            textTest.setText(strReceived);
                            uriAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

                            ringTone = RingtoneManager
                                    .getRingtone(getApplicationContext(), uriAlarm);
                            if (strReceived.equals("a")) {
                                leftTop.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("b")) {
                                leftTop.setBackgroundColor(Color.parseColor("#4422dd"));

                            }


                            if (strReceived.equals("m")) {
                                topCenter.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("n")) {
                                topCenter.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("g")) {
                                rightTop.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("h")) {
                                rightTop.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("c")) {
                                leftCenter.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("d")) {
                                leftCenter.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("s")) {
                                centerLeft.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("t")) {
                                centerLeft.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("q")) {
                                center.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("r")) {
                                center.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("u")) {
                                centerRight.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("v")) {
                                centerRight.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("i")) {
                                rightCenter.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("j")) {
                                rightCenter.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("e")) {
                                leftBot.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("f")) {
                                leftBot.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("o")) {
                                centerBot.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("p")) {
                                centerBot.setBackgroundColor(Color.parseColor("#4422dd"));
                            }

                            if (strReceived.equals("k")) {
                                rightBot.setBackgroundColor(Color.parseColor("#fd3a38"));
                            }

                            if (strReceived.equals("l")) {
                                rightBot.setBackgroundColor(Color.parseColor("#4422dd"));
                            }
                            if (strReceived.equals("L")) {
                               setContentView(R.layout.notification);
                                if(!ringTone.isPlaying()){
                                    ringTone.play();
                                }
                                Button buttonBack = (Button) findViewById(R.id.bt_back);

                                buttonBack.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        play = !play;
                                        ringTone.stop();

                                        setContentView(R.layout.activity_about);
                                        leftTop = (View) findViewById(R.id.leftTop);
                                        topCenter = (View) findViewById(R.id.topCenter);
                                        rightTop = (View) findViewById(R.id.rightTop);
                                        leftCenter = (View) findViewById(R.id.leftCenter);
                                        centerLeft = (View) findViewById(R.id.centerLeft);
                                        center = (View) findViewById(R.id.center);
                                        centerRight = (View) findViewById(R.id.centerRight);
                                        rightCenter = (View) findViewById(R.id.rightCenter);
                                        leftBot = (View) findViewById(R.id.leftBot);
                                        centerBot = (View) findViewById(R.id.centerBot);
                                        rightBot = (View) findViewById(R.id.rightBot);
                                        textTest = (TextView) findViewById(R.id.textView2);
                                    }
                                });
                            }
                            if (strReceived.equals("M")) {
                                setContentView(R.layout.notification);
                                if(!ringTone.isPlaying()){
                                    ringTone.play();
                                }
                                Button buttonBack = (Button) findViewById(R.id.bt_back);
                                buttonBack.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        play = !play;

                                        ringTone.stop();

                                        setContentView(R.layout.activity_about);
                                        leftTop = (View) findViewById(R.id.leftTop);
                                        topCenter = (View) findViewById(R.id.topCenter);
                                        rightTop = (View) findViewById(R.id.rightTop);
                                        leftCenter = (View) findViewById(R.id.leftCenter);
                                        centerLeft = (View) findViewById(R.id.centerLeft);
                                        center = (View) findViewById(R.id.center);
                                        centerRight = (View) findViewById(R.id.centerRight);
                                        rightCenter = (View) findViewById(R.id.rightCenter);
                                        leftBot = (View) findViewById(R.id.leftBot);
                                        centerBot = (View) findViewById(R.id.centerBot);
                                        rightBot = (View) findViewById(R.id.rightBot);
                                        textTest = (TextView) findViewById(R.id.textView2);
                                    }
                                });
                            }
                            if (strReceived.equals("R")) {
                                setContentView(R.layout.notification);

                                if(!ringTone.isPlaying()){
                                    ringTone.play();
                                }
                                play = !play;
                                Button buttonBack = (Button) findViewById(R.id.bt_back);
                                buttonBack.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        ringTone.stop();

                                        setContentView(R.layout.activity_about);
                                        leftTop = (View) findViewById(R.id.leftTop);
                                        topCenter = (View) findViewById(R.id.topCenter);
                                        rightTop = (View) findViewById(R.id.rightTop);
                                        leftCenter = (View) findViewById(R.id.leftCenter);
                                        centerLeft = (View) findViewById(R.id.centerLeft);
                                        center = (View) findViewById(R.id.center);
                                        centerRight = (View) findViewById(R.id.centerRight);
                                        rightCenter = (View) findViewById(R.id.rightCenter);
                                        leftBot = (View) findViewById(R.id.leftBot);
                                        centerBot = (View) findViewById(R.id.centerBot);
                                        rightBot = (View) findViewById(R.id.rightBot);
                                        textTest = (TextView) findViewById(R.id.textView2);
                                    }
                                });
                            }

                            //                            textStatus.setText(msgReceived);
                        }

                    });

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            textStatus.setText(msgConnectionLost);
                        }
                    });
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
