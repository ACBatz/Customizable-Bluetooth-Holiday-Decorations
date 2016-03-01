package com.example.andrew.capstoneproject;

/*********************************************************
 *  Author:         Andrew C. Batzel
 *  Description:    This app is a controller for a device that controls LEDs, the device is called
 *                    Customizable Bluetooth Holiday Decorations and allows users to connect via
 *                    Bluetooth and change the colors and sequences of the LEDs in the decoration
 *  Purpose:        This app was designed for my Capstone project
 *  Class:          CS493 Senior Capsone
 *  School:         Regis University
 ********************************************************/

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    /*******************************
     ** Handler message types
     ***/
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME= 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int SCANNING = 6;
    public static final int NO_DEVICE = 7;
    public static final int DEVICE_FOUND = 8;
    public static final int DONE_SCANNING = 9;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    /*******************************
     ** Message types received from decoration server
     ***/
    public static final int OK = 1;
    public static final int BAD = 2;

    /*******************************
     ** Sequence constants for LEDs
     ** Values correspond to decoration device sequence values
     ***/
    public static final String SOLID = "2";
    public static final String ALTERNATE = "3";
    public static final String CHASE = "4";
    public static final String REACTIVE = "5";


    /*******************************
     ** Bluetooth objects and supporting objects
     ***/
    private static BluetoothAdapter mBluetoothAdapter = null;
    private static BluetoothDevice mBluetoothDevice = null;
    private static BluetoothSerialService mSerialService = null;
    private static int mState = -1;
    private static String mConnectedDeviceName = null;
    private static TextView mBluetoothStatus;

    /*******************************
     ** Fragment objects for LED controller
     ***/
    private static ScrollView scrollView = null;
    private static RelativeLayout relativeLayout = null;
    private static ProgressBar progressBar  = null;
    private static ToggleButton power = null;
    private static ImageView star = null;
    private static ImageView ornaments = null;
    private static LinearLayout colors = null;
    private static Button addColor = null;
    private static Spinner sequence = null;
    private static Button send = null;
    private static String[] sequenceTypes =  {"solid", "alternate", "chase", "reactive"};
    private static String sequenceSelected = "0";


    /*****************************
     ** Fragment objects for Bluetooth
     ***/
    private ImageButton bluetoothBtn = null;

    private static boolean toggleStarTouch;
    private static boolean toggleOrnamentTouch;
    private static int selectedColorRGB;
    private static String myColor;

    /*******************************
     ** Model that holds all user defined colors and sequences
     ***/
    private ColorSelection mColorSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Reconnect to device if app reopened and not destroyed
        setContentView(R.layout.fragment_container_portrait);
        if (savedInstanceState != null) {
            if (mSerialService != null) {
                assertBluetooth();
            }
        }

        // Bluetooth Fragment objects
        bluetoothBtn = (ImageButton) findViewById(R.id.bluetooth_btn);
        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assertBluetooth();
            }
        });
        mBluetoothStatus = (TextView) findViewById(R.id.bluetooth_status);

        // Lights Fragment object references
        scrollView = (ScrollView) findViewById(R.id.lights_scroll_view);
        relativeLayout = (RelativeLayout) findViewById(R.id.lights_layout);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        power = (ToggleButton) findViewById(R.id.togglePower);
        star = (ImageView) findViewById(R.id.star_color);
        ornaments = (ImageView) findViewById(R.id.ornament_color);
        colors = (LinearLayout) findViewById(R.id.color_picker_layout);
        addColor = (Button) findViewById(R.id.pick_color);
        sequence = (Spinner) findViewById(R.id.sequence_spinner);
        send = (Button) findViewById(R.id.send_colors);

        // Create new object for holding user selections
        mColorSelection = new ColorSelection();

        // Setup spinner for sequence options
        ArrayAdapter<String> sequenceAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, sequenceTypes);
        sequence.setAdapter(sequenceAdapter);
        sequence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    sequenceSelected = SOLID;
                } else if (position == 1) {
                    sequenceSelected = ALTERNATE;
                } else if (position == 2) {
                    sequenceSelected = CHASE;
                } else if (position == 3) {
                    sequenceSelected = REACTIVE;
                } else {
                    sequenceSelected = "0";
                }
                mColorSelection.setSequenceNum(sequenceSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Setup event listeners for buttons and imageviews
        power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupAnimation(power);
                if (power.isChecked()) { // turn off
                    mSerialService.write("0".getBytes());
                } else {                // turn on
                    mSerialService.write("1".getBytes());
                }
            }
        });

        toggleStarTouch = false;
        star.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (toggleStarTouch) { // reset color to blank
                        star.setBackground(getDrawable(R.drawable.colors_border));
                        mColorSelection.setStarColor("000000");
                        toggleStarTouch = false;
                        return true;
                    } else {                // establish a color
                        final ColorPicker cp = new ColorPicker(MainActivity.this, 255, 255, 255);
                        cp.show();
                        Button okColor = (Button) cp.findViewById(R.id.okColorButton);
                        okColor.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                star.setBackgroundColor(cp.getColor());
                                mColorSelection.setStarColor(cp.getMyColor());
                                cp.dismiss();
                                toggleStarTouch = true;
                            }
                        });
                    }
                }
                return false;
            }
        });

        toggleOrnamentTouch = false;
        ornaments.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (toggleOrnamentTouch) {
                        ornaments.setBackground(getDrawable(R.drawable.colors_border));
                        mColorSelection.setOrnamentColor("000000");
                        toggleOrnamentTouch = false;
                        return true;
                    } else {
                        final ColorPicker cp = new ColorPicker(MainActivity.this, 255, 255, 255);
                        cp.show();
                        Button okColor = (Button) cp.findViewById(R.id.okColorButton);
                        okColor.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ornaments.setBackgroundColor(cp.getColor());
                                mColorSelection.setOrnamentColor(cp.getMyColor());
                                cp.dismiss();
                                toggleOrnamentTouch = true;
                            }
                        });
                    }
                }
                return false;
            }
        });

        addColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPicker cp = new ColorPicker(MainActivity.this, 255, 255, 255);
                if (mColorSelection.getColors().size() >= 5) {  // ensures only 5 colors are selected, this is due to the limitations of the decorations device
                    Toast.makeText(getApplicationContext(), "Only 5 color selections available at this time", Toast.LENGTH_SHORT).show();
                } else {
                    cp.show();
                    Button okColor = (Button) cp.findViewById(R.id.okColorButton);
                    okColor.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            myColor = cp.getMyColor();
                            selectedColorRGB = cp.getColor();
                            cp.dismiss();
                            ImageView newColor = new ImageView(MainActivity.this);
                            newColor.setBackgroundColor(selectedColorRGB);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                            newColor.setLayoutParams(layoutParams);
                            newColor.setId(mColorSelection.getColors().size());
                            newColor.setClickable(true);
                            // set up listener to remove color when pressed
                            newColor.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                        Log.i("Motion:", "" + v.getId());
                                        colors.removeView(mColorSelection.getColors().get(v.getId()));
                                        mColorSelection.removeColor(mColorSelection.getColors().get(v.getId()));
                                        return true;
                                    }
                                    return false;
                                }
                            });
                            colors.addView(newColor);
                            mColorSelection.addColor(newColor, myColor);
                        }
                    });
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupAnimation(send);
                String colors = mColorSelection.getSequenceNum() + mColorSelection.getStarColor() + mColorSelection.getOrnamentColor();
                for (String string : mColorSelection.getTrueColors().values()) {
                    colors += string;
                }
                mSerialService.write(colors.getBytes());
            }
        });

        // Establish state if onCrease is called
        if (savedInstanceState == null) {
            mBluetoothStatus.setText("click to start ->");
            disableButtons();
        }
        else {
            mHandlerBt.obtainMessage(MESSAGE_STATE_CHANGE, mSerialService.getState()).sendToTarget();
        }
    }

    // Animates buttons to give interactive responses
    private void setupAnimation(View view) {

        Animator anim = AnimatorInflater.loadAnimator(MainActivity.this, R.animator.scale);
        anim.setTarget(view);
        anim.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.capstoneproject, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Sets up Bluetooth on device to connect to the decorations device
    public synchronized void assertBluetooth() {
        Log.i("Method:", "assertBluetooth()");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDevice = null;
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (mBluetoothAdapter == null) {    // this means the device doesn't support Bluetooth
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {   // Turns on Bluetooth is it isn't already
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        }

        if (mSerialService == null) {
            Log.i("mSerialService is", "null");
            mSerialService = new BluetoothSerialService(this, mHandlerBt);
        }
        if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
            mSerialService.start();
        }

        if (mBluetoothDevice == null) {
            if (!checkPairedDevices()) {
                Log.i("Device", "not paired");
                // register intents for BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                this.registerReceiver(mBroadcastReceiver, filter);
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                this.registerReceiver(mBroadcastReceiver, filter);
                filter = new IntentFilter((BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
                this.registerReceiver(mBroadcastReceiver, filter);

                doDiscovery();
            }
        }
    }

    // return true if the decorations device is current paired
    public boolean checkPairedDevices() {
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            Log.i("Device found:", device.getName());
            if (device.getName().contains("raspberry")) {
                mBluetoothDevice = device;
                Log.i("Decorations found @:", device.getAddress());
                mHandlerBt.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, DEVICE_FOUND, -1).sendToTarget();
                return true;
            }
        }
        return false;
    }

    // determines what happens when a intents are found
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action) && mBluetoothDevice == null) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("Device found:", device.getName());

                if (device.getName().contains("raspberry")) {
                    mBluetoothDevice = device;
                    // cannot create a bond and immediately connect to it, the pairing message informs the user that their phone is pairing with the decorations device as a distraction to allow enough time to establish pairing and connection to device
                    if (mBluetoothDevice.createBond()) {
                        pairingMessage();
                    }
                    else {
                        mBluetoothDevice = null;
                        doDiscovery();
                    }

                    Log.i("Decorations found @:", device.getAddress());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Decorations device found!", Toast.LENGTH_SHORT).show();
                }

            }
            else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    //turn on BT if necessary
                }

            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i("Discovery:", "finished");
                progressBar.setVisibility(View.GONE);
                if (mBluetoothDevice == null) {
                    Toast.makeText(getApplicationContext(), "No Decorations device found", Toast.LENGTH_SHORT).show();
                    mSerialService = null;
                    mHandlerBt.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, NO_DEVICE, -1).sendToTarget();
                }
            }
        }
    };

    // informs user of pairing between phone and decorations device
    public void pairingMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Pairing")
                .setMessage("We are pairing with your decorations now")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        assertBluetooth();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        Log.i("Method:", "onResume()");

        if (mBluetoothDevice != null) {
            Log.i("device", mBluetoothDevice.getName());
        }

        if (mSerialService != null) {
            if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
                mSerialService.start();
            }
        }
    }

    // Handler handles all of the message traffic internally(MainActivity) and BluetoothSerialService
    private static final Handler mHandlerBt = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mState = msg.arg1;
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            Log.i("Message", "connected");
                            stateConnected();
                            break;
                        case BluetoothSerialService.STATE_CONNECTING:
                            Log.i("Message", "connecting");
                            stateConnecting();
                            break;
                        case BluetoothSerialService.STATE_LISTEN:
                        case BluetoothSerialService.STATE_NONE:
                            Log.i("Message", "none");
                            stateNone();
                            break;
                        case SCANNING:
                            Log.i("Message", "scanning");
                            stateScanning();
                            break;
                        case NO_DEVICE:
                            Log.i("Message", "no device found");
                            stateNoDevice();
                            break;
                        case DEVICE_FOUND:
                            Log.i("Message", "device found");
                            stateDeviceFound();
                            break;
                        case DONE_SCANNING:
                            Log.i("Message", "done scanning");
                            stateDoneScanning();
                            break;
                        default:
                            stateNone();
                    }
                    break;
                case MESSAGE_WRITE:
                        // if for some reason something outside of the UI thread needed to write to the BluetoothSocket
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    //Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                        //Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_READ:
                    switch(msg.arg1) {
                        case 0:
                        case OK:
                            Log.i("Read:","OK");
                            enableButtons();
                            break;
                        case BAD:
                            Log.i("Read:","BAD");
                            break;
                    }
                    break;
            }
        }
    };


    //The following are the different states the phone can be in with respect to the decorations device
    private static synchronized void stateConnected() {
        progressBar.setVisibility(View.GONE);
        mBluetoothStatus.setText(R.string.status_connected);
    }

    private static synchronized void stateConnecting() {
        progressBar.setVisibility(View.VISIBLE);
        mBluetoothStatus.setText(R.string.status_connecting);
    }

    private static synchronized void connectToDevice(BluetoothDevice device) {
        progressBar.setVisibility(View.VISIBLE);
        mSerialService.connect(device);
    }

    private static synchronized void stateDeviceFound() {
        mBluetoothStatus.setText(R.string.status_found);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        connectToDevice(mBluetoothDevice);
    }

    private static synchronized void stateScanning() {
        progressBar.setVisibility(View.VISIBLE);
        mBluetoothStatus.setText(R.string.scanning);
    }

    private synchronized void doDiscovery() {
        Log.i("Method:", "doDiscovery()");
        progressBar.setVisibility(View.VISIBLE);
        mHandlerBt.obtainMessage(MESSAGE_STATE_CHANGE, SCANNING, -1).sendToTarget();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private static synchronized void stateNone() {
        mBluetoothStatus.setText(R.string.status_not_connected);
        disableButtons();
        progressBar.setVisibility(View.GONE);
    }

    private static void stateNoDevice() {
        mBluetoothStatus.setText(R.string.status_no_device);
        disableButtons();
    }

    private static void stateDoneScanning() {
        if ((mSerialService.getState() != BluetoothSerialService.STATE_CONNECTED) && (mSerialService.getState() != BluetoothSerialService.STATE_CONNECTING)) {
            if (mBluetoothDevice != null) {
                //Something went terribly wrong
                Log.i("don't","quit your day job");
            }
        }
    }

    private static void enableButtons() {
        relativeLayout.setAlpha(1.0f);
        power.setClickable(true);
        star.setClickable(true);
        ornaments.setClickable(true);
        addColor.setClickable(true);
        sequence.setClickable(true);
        send.setClickable(true);
    }

    private static void disableButtons() {
        relativeLayout.setAlpha(.25f);
        power.setClickable(false);
        star.setClickable(false);
        ornaments.setClickable(false);
        addColor.setClickable(false);
        sequence.setClickable(false);
        send.setClickable(false);
    }

    public synchronized void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        ;
        Log.i("onActivityResult", "start");

        if (resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        Log.i("Method:", "onDestroy()");
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (mSerialService != null) {
            mSerialService.stop();
        }
    }
}
