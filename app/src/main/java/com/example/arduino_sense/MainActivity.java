package com.example.arduino_sense;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements BLEControllerListener {
    private TextView logView;
    private Button connectButton;
    private Button disconnectButton;
    private Button openControlRoom;
    private BLEController bleController;
    public String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_layout);
        this.bleController = BLEController.getInstance(this);
        this.logView = findViewById(R.id.logView);
        this.logView.setMovementMethod(new ScrollingMovementMethod());

        initConnectButton();
        initDisconnectButton();
        initSwitchLEDButton();
        statusCheck();
        checkBLESupport();
        checkPermissions();

        disableButtons();
    }


    private void initConnectButton() {
        this.connectButton = findViewById(R.id.connectButton);
        this.connectButton.setOnClickListener(v -> {
            connectButton.setEnabled(false);
            log("Connecting...");
            bleController.connectToDevice(deviceAddress);
        });
    }

    private void initDisconnectButton() {
        this.disconnectButton = findViewById(R.id.disconnectButton);
        this.disconnectButton.setOnClickListener(v -> {
            disconnectButton.setEnabled(false);
            log("Disconnecting...");
            //toast("Disconnecting");
            bleController.disconnect();
        });
    }

    private void initSwitchLEDButton() {
        openControlRoom = findViewById(R.id.switchButton);
        openControlRoom.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlRoom.class);
            startActivity(intent);
        });
    }

    private void disableButtons() {
        runOnUiThread(() -> {
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            openControlRoom.setEnabled(false);
        });
    }

    @SuppressLint("SetTextI18n")
    private void log(final String text) {
        runOnUiThread(() -> logView.setText(logView.getText() + "\n" + text));
    }

    private void toast(final String text){
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            log("\"Access Fine Location\" permission not granted yet!");
            log("Whitout this permission Blutooth devices cannot be searched!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        }
    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void checkBLESupport() {
        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("BLE not supported!");
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent, 1);
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.deviceAddress = null;
        this.bleController = BLEController.getInstance(this);
        this.bleController.addBLEControllerListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            log("[BLE]\tSearching for Arduino nano");
            //toast("[BLE]\tSearching for Arduino nano");
            this.bleController.init();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.bleController.removeBLEControllerListener(this);
    }

    @Override
    public void BLEControllerConnected() {
        log("[BLE]\tConnected");
        runOnUiThread(() -> {
            disconnectButton.setEnabled(true);
            //toast("BLE Connected!");
            openControlRoom.setEnabled(true);
        });
    }

    @Override
    public void BLEControllerDisconnected() {
        log("[BLE]\tDisconnected");
        disableButtons();
        runOnUiThread(() -> {
            connectButton.setEnabled(true);
            toast("BLE Disconnected!");
        });
    }

    @Override
    public void BLEDeviceFound(String name, String address) {
        log("Device " + name + " found with address " + address);
        this.deviceAddress = address;
        this.connectButton.setEnabled(true);
    }
}

