// MainActivity.java
package com.almogbb.crimpi; // IMPORTANT: Ensure this package name matches your project's package name

import android.Manifest;
import android.app.AlertDialog; // Import for AlertDialog
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater; // Import for LayoutInflater
import android.view.View;
import android.view.ViewGroup; // Import for ViewGroup
import android.widget.ImageButton; // Import for ImageButton
import android.widget.ImageView; // Import for ImageView
import android.widget.ProgressBar; // Import for ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager; // Import for LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import for RecyclerView

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CrimPiBLEApp";

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;

    // Main screen UI elements
    private ImageButton bluetoothButton; // The Bluetooth icon button
    private ImageView centralLogoImageView; // CrimPi logo
    private TextView instructionTextView; // "Please connect to a CrimPi device"
    private TextView receivedNumberTextView; // Displays received temperature

    // Dialog UI elements (will be found when dialog is created)
    private AlertDialog scanDialog;
    private ProgressBar scanProgressBar;
    private TextView noDevicesFoundText;

    // Data for RecyclerView Adapter
    private final List<BluetoothDeviceEntry> deviceList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    // Map to hold discovered BLE devices (for de-duplication)
    private final Map<String, BluetoothDevice> discoveredDevicesMap = new HashMap<>(); // MAC Address to BluetoothDevice

    // --- UUIDs (MUST MATCH Pico W's main.py) ---
    private static final UUID ENV_SENSE_SERVICE_UUID = UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB");
    private static final UUID TEMPERATURE_CHARACTERISTIC_UUID = UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");


    // Callback for BLE scan results
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (results != null) {
                for (ScanResult result : results) {
                    processScanResult(result);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE Scan Failed: " + errorCode);
            runOnUiThread(() -> {
                if (scanDialog != null && scanDialog.isShowing()) {
                    scanProgressBar.setVisibility(View.GONE);
                    noDevicesFoundText.setText(R.string.scan_failed + errorCode + R.string.try_again);
                    noDevicesFoundText.setVisibility(View.VISIBLE);
                }
                Toast.makeText(MainActivity.this, "BLE Scan Failed: " + errorCode, Toast.LENGTH_LONG).show();
            });
        }
    };

    // Callback for GATT client events (connection, service discovery, characteristic changes)
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                runOnUiThread(() -> {
                    // Hide disconnected state UI, show connected state UI
                    bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.bluetooth_connected_blue));
                    centralLogoImageView.setVisibility(View.GONE);
                    instructionTextView.setVisibility(View.GONE);
                    receivedNumberTextView.setVisibility(View.VISIBLE);
                });
                // After connection, discover services
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for discoverServices.");
                    return;
                }
                gatt.discoverServices(); // Start GATT service discovery
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                runOnUiThread(() -> {
                    // Show disconnected state UI, hide connected state UI
                    bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.primary_text_color));
                    centralLogoImageView.setVisibility(View.VISIBLE);
                    instructionTextView.setText(R.string.crimpi_connect);
                    instructionTextView.setVisibility(View.VISIBLE);
                    receivedNumberTextView.setVisibility(View.GONE);
                    receivedNumberTextView.setText("N/A"); // Reset display
                });
                // Close GATT client and clear reference
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
                // Restart scan to allow re-connection (optional, but good practice)
                // startBleScan(); // Removed auto-restart scan on disconnect for cleaner flow
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.");
                runOnUiThread(() -> {
                });

                // 1. Find and set up Temperature Characteristic (for receiving from Pico W)
                BluetoothGattService envSenseService = gatt.getService(ENV_SENSE_SERVICE_UUID);
                if (envSenseService != null) {
                    BluetoothGattCharacteristic tempCharacteristic = envSenseService.getCharacteristic(TEMPERATURE_CHARACTERISTIC_UUID);
                    if (tempCharacteristic != null) {
                        // Enable notifications for temperature
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for setCharacteristicNotification.");
                            return;
                        }
                        gatt.setCharacteristicNotification(tempCharacteristic, true);

                        // Write to the CCCD to enable notifications on the peripheral side
                        BluetoothGattDescriptor descriptor = tempCharacteristic.getDescriptor(CCCD_UUID);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            // Perform the write operation for the descriptor
                            boolean writeSuccess = gatt.writeDescriptor(descriptor);
                            Log.d(TAG, "Subscribed to Temperature Characteristic notifications: " + writeSuccess);
                        } else {
                            Log.w(TAG, "CCCD descriptor not found for Temperature Characteristic.");
                        }
                    } else {
                        Log.w(TAG, "Temperature Characteristic not found: " + TEMPERATURE_CHARACTERISTIC_UUID);
                    }
                } else {
                    Log.w(TAG, "Environmental Sensing Service not found: " + ENV_SENSE_SERVICE_UUID);
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                runOnUiThread(() -> {
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // This is called when the Pico W sends a notification (e.g., new temperature)
            if (characteristic.getUuid().equals(TEMPERATURE_CHARACTERISTIC_UUID)) {
                // Temperature is typically a 16-bit signed integer in 0.01 degree Celsius units
                byte[] value = characteristic.getValue();
                if (value != null && value.length >= 2) {
                    // Use ByteBuffer to correctly interpret the little-endian 16-bit integer
                    int rawTemp = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    float temperature = rawTemp / 100.0f;

                    runOnUiThread(() -> {
                        // Fix: Use String.format with Locale.getDefault() to avoid implicit locale warning
                        receivedNumberTextView.setText(String.format(Locale.getDefault(), "%.2f", temperature)); // Display only the number
                    });
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (descriptor.getUuid().equals(CCCD_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "CCCD write successful. Notifications enabled.");
                    runOnUiThread(() -> {
                    });
                } else {
                    Log.e(TAG, "CCCD write failed: " + status);
                    runOnUiThread(() -> {
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize main screen UI elements
        bluetoothButton = findViewById(R.id.bluetoothButton);
        centralLogoImageView = findViewById(R.id.centralLogoImageView);
        instructionTextView = findViewById(R.id.instructionTextView);
        receivedNumberTextView = findViewById(R.id.receivedNumberTextView);

        // Get Bluetooth services
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Check if Bluetooth is supported and enabled
        if (bluetoothAdapter == null) {
            // Bluetooth not supported, disable button and show message
            instructionTextView.setText(R.string.bluetooth_not_supported);
            bluetoothButton.setEnabled(false);
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth not enabled, disable button and show message
            instructionTextView.setText(R.string.bluetooth_not_enabled);
            bluetoothButton.setEnabled(false);
        } else {
            // Bluetooth is ready, enable button
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            instructionTextView.setText(R.string.crimpi_connect); // Default instruction
            bluetoothButton.setEnabled(true);
        }

        // Set up Bluetooth button click listener to show scan dialog
        bluetoothButton.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                showScanDialog();
            }
        });

        // Initialize RecyclerView adapter (empty initially)
        deviceAdapter = new DeviceAdapter(deviceList);
    }

    // --- Permission Handling ---
    private boolean checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Location permission is required for BLE scanning on Android 6.0+ (API 23-30)
        // For Android 12 (API 31) and above
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
            return false; // Permissions not yet granted
        }
        return true; // All necessary permissions are already granted
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All permissions granted, show scan dialog
                showScanDialog();
            } else {
                // Permissions denied
                instructionTextView.setText("");
                bluetoothButton.setEnabled(false);
                Toast.makeText(this, R.string.bluetooth_permissions_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- BLE Scan Dialog Logic ---
    private void showScanDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_bluetooth_scan, null);
        dialogBuilder.setView(dialogView);

        // Initialize dialog UI elements
        scanProgressBar = dialogView.findViewById(R.id.scanProgressBar);
        RecyclerView deviceRecyclerView = dialogView.findViewById(R.id.deviceRecyclerView);
        noDevicesFoundText = dialogView.findViewById(R.id.noDevicesFoundText);

        // Set up RecyclerView
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceRecyclerView.setAdapter(deviceAdapter);

        // Reset list and visibility for new scan
        int oldSize = deviceList.size(); // Get current size before clearing
        deviceList.clear(); // Clear the underlying data list
        if (oldSize > 0) { // Only notify if there were items to remove
            deviceAdapter.notifyItemRangeRemoved(0, oldSize); // Notify adapter of specific removal
        }
        // Original line that caused the warning: // deviceAdapter.notifyDataSetChanged();
        noDevicesFoundText.setVisibility(View.GONE);
        scanProgressBar.setVisibility(View.VISIBLE); // Show progress bar when scan starts

        // Create and show the dialog
        scanDialog = dialogBuilder.create();
        scanDialog.show();

        // Start BLE scan
        startBleScan();
    }

    // --- BLE Scanning Logic ---
    private void startBleScan() {
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "Bluetooth LE Scanner not available.", Toast.LENGTH_SHORT).show();
            if (scanDialog != null && scanDialog.isShowing()) {
                scanProgressBar.setVisibility(View.GONE);
                noDevicesFoundText.setText(R.string.scanner_not_available);
                noDevicesFoundText.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Stop any ongoing scan before starting a new one
        stopBleScan();

        // --- FIX FOR WARNING: Use notifyItemRangeRemoved instead of notifyDataSetChanged for clearing RecyclerView ---
        int oldSize = deviceList.size(); // Get current size before clearing
        deviceList.clear(); // Clear the underlying data list
        if (oldSize > 0) { // Only notify if there were items to remove
            deviceAdapter.notifyItemRangeRemoved(0, oldSize); // Notify adapter of specific removal
        }
        // Original line that caused the warning: // deviceAdapter.notifyDataSetChanged();
        // This is now replaced by the above more specific notification.
        discoveredDevicesMap.clear(); // Clear the map for de-duplication
        // --- END FIX ---

        if (scanDialog != null && scanDialog.isShowing()) {
            scanProgressBar.setVisibility(View.VISIBLE);
            noDevicesFoundText.setVisibility(View.GONE);
        }

        Log.d(TAG, "Starting BLE scan...");

        // Add explicit permission checks with Toast feedback
        boolean hasScanPermission;
        boolean hasConnectPermission;

        // Note: The permission checks below are simplified for this snippet.
        // The full `MainActivity.java` in the immersive artifact handles API level differences.

        hasScanPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        hasConnectPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        if (!hasScanPermission || !hasConnectPermission) {
            Toast.makeText(this, "Bluetooth (Scan/Connect) permissions not granted.", Toast.LENGTH_LONG).show();
            if (scanDialog != null && scanDialog.isShowing()) {
                scanProgressBar.setVisibility(View.GONE);
                noDevicesFoundText.setText(R.string.permissions_not_granted);
                noDevicesFoundText.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Check if Bluetooth is actually enabled (system-wide)
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled. Please enable it in settings.", Toast.LENGTH_LONG).show();
            if (scanDialog != null && scanDialog.isShowing()) {
                scanProgressBar.setVisibility(View.GONE);
                noDevicesFoundText.setText(R.string.bluetooth_disabled);
                noDevicesFoundText.setVisibility(View.VISIBLE);
            }
            return;
        }

        // --- Scan Filters and Settings ---
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(ENV_SENSE_SERVICE_UUID)).build();
        scanFilters.add(filter);

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);

        // Stop scan after a period (e.g., 10 seconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopBleScan();
            runOnUiThread(() -> {
                if (scanDialog != null && scanDialog.isShowing()) {
                    scanProgressBar.setVisibility(View.GONE);
                    if (deviceList.isEmpty()) {
                        noDevicesFoundText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }, 10000);
    }

    private void stopBleScan() {
        if (bluetoothLeScanner == null) return;

        boolean hasScanPermission;
        boolean hasConnectPermission;

        hasScanPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        hasConnectPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        if (!hasScanPermission || !hasConnectPermission) {
            Log.w(TAG, "Attempted to stop scan without BLUETOOTH_SCAN/CONNECT permissions.");
            return;
        }

        bluetoothLeScanner.stopScan(scanCallback);
        Log.d(TAG, "BLE scan stopped.");
    }

    // --- Helper method to process a single ScanResult ---
    private void processScanResult(ScanResult result) {
        if (result != null && result.getDevice() != null) {
            String currentDeviceName;
            String currentDeviceAddress;
            BluetoothDevice device = result.getDevice();

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, cannot get device name/address.");
                currentDeviceName = "Unknown Device (Perm Denied)";
            } else {
                currentDeviceName = device.getName();
            }
            currentDeviceAddress = device.getAddress();

            if (currentDeviceName == null || currentDeviceName.isEmpty()) {
                currentDeviceName = "N/A";
            }

            // --- Confirmation Logic ---
            // Only add to list if it's confirmed to be a Pico W (starts with "Pico")
            boolean isPicoW = currentDeviceName.startsWith("Pico");

            if (isPicoW) {
                final String finalDeviceName = currentDeviceName;
                final String finalDeviceAddress = currentDeviceAddress;

                // Only add if it's a new device or name has changed
                if (!discoveredDevicesMap.containsKey(finalDeviceAddress)) {
                    discoveredDevicesMap.put(finalDeviceAddress, device); // Store the actual BluetoothDevice object
                    runOnUiThread(() -> {
                        deviceList.add(new BluetoothDeviceEntry(finalDeviceName, finalDeviceAddress));
                        deviceAdapter.notifyItemInserted(deviceList.size() - 1);
                        noDevicesFoundText.setVisibility(View.GONE); // Hide "No devices found" if a device is found
                        scanProgressBar.setVisibility(View.GONE);
                    });
                    Log.d(TAG, "Found confirmed Pico W: Name='" + finalDeviceName + "', Address='" + finalDeviceAddress + "'");
                }
            } else {
                Log.d(TAG, "Skipping non-PicoW device: " + currentDeviceName + " (" + currentDeviceAddress + ")");
            }
        }
    }

    // --- Connection Logic ---
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for connectGatt.");
            Toast.makeText(this, "BLUETOOTH_CONNECT permission needed to connect.", Toast.LENGTH_SHORT).show();
            return;
        }
        // If already connected, close previous connection
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        // Connect to the GATT server on the device
        try {
            bluetoothGatt = device.connectGatt(this, false, gattCallback); // 'false' for direct connection
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Missing BLUETOOTH_CONNECT permission for connectGatt.", e);
            Toast.makeText(this, "Permission error: Cannot connect to device.", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(() -> {
            try {
                // Update main screen status
                instructionTextView.setText(getString(R.string.connection_attempt,
                        (device.getName() != null ? device.getName() : device.getAddress())));
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: Cannot get device name/address for UI update.", e);
                instructionTextView.setText(R.string.connection_attempt_permission_error);
                Toast.makeText(MainActivity.this, "Permission error: Cannot display device name.", Toast.LENGTH_SHORT).show();
            }
        });

        // Dismiss the scan dialog once connection attempt starts
        if (scanDialog != null && scanDialog.isShowing()) {
            scanDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBleScan(); // Ensure scan is stopped when activity is destroyed
        // Close GATT connection if it exists
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission missing during onDestroy for bluetoothGatt.close().");
            }
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    // --- RecyclerView Adapter and ViewHolder Classes ---

    // Data class to hold device information for the RecyclerView
    private static class BluetoothDeviceEntry {
        String name;
        String address;

        BluetoothDeviceEntry(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    // Adapter for the RecyclerView
    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

        private final List<BluetoothDeviceEntry> localDeviceList;

        public DeviceAdapter(List<BluetoothDeviceEntry> deviceList) {
            this.localDeviceList = deviceList;
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the item_bluetooth_device.xml layout for each item
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            BluetoothDeviceEntry device = localDeviceList.get(position);
            holder.deviceName.setText(device.name);
            // The deviceLogo ImageView is already in item_bluetooth_device.xml
            // No need to set address text as per updated item_bluetooth_device.xml
        }

        @Override
        public int getItemCount() {
            return localDeviceList.size();
        }

        // ViewHolder for each device item
        class DeviceViewHolder extends RecyclerView.ViewHolder {
            ImageView deviceLogo; // The ImageView for the logo
            TextView deviceName;

            DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
                deviceLogo = itemView.findViewById(R.id.deviceLogo);
                deviceName = itemView.findViewById(R.id.deviceName);

                // Set click listener for the entire item view
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        BluetoothDeviceEntry selectedEntry = localDeviceList.get(position);
                        // Retrieve the actual BluetoothDevice object from the map
                        BluetoothDevice deviceToConnect = discoveredDevicesMap.get(selectedEntry.address);
                        if (deviceToConnect != null) {
                            connectToDevice(deviceToConnect);
                        } else {
                            Toast.makeText(MainActivity.this, "Error: Device object not found.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}
