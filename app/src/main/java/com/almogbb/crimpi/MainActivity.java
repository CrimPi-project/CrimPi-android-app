// MainActivity.java
package com.almogbb.crimpi;

import com.almogbb.crimpi.fragments.CustomWorkoutFragment;
import com.almogbb.crimpi.fragments.FreestyleWorkoutFragment;
import com.almogbb.crimpi.fragments.HomeFragment;
import com.almogbb.crimpi.fragments.BodyWeightDialogFragment;
import com.almogbb.crimpi.fragments.MyWorkoutsFragment;
import com.almogbb.crimpi.workouts.CustomWorkoutData;
import com.google.android.material.navigation.NavigationView; // NEW

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.ImageButton; // Import for ImageButton
import android.widget.ProgressBar; // Import for ProgressBar
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver; // NEW
import android.content.Intent; // NEW
import android.content.IntentFilter; // NEW

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // Import for LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import for RecyclerView
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager; // NEW: For managing fragments
import androidx.fragment.app.FragmentTransaction; // NEW: For fragment transactions

import com.almogbb.crimpi.adapters.DeviceAdapter;
import com.almogbb.crimpi.data.BluetoothDeviceEntry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BodyWeightDialogFragment.BodyWeightDialogListener {

    private static final String TAG = "CrimPiApp";

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    public BluetoothGatt bluetoothGatt;

    // NEW: Navigation Drawer elements
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // NEW: Reference to the HomeFragment instance
    private HomeFragment homeFragment;
    private FreestyleWorkoutFragment freestyleWorkoutFragment;

    private MyWorkoutsFragment myWorkoutsFragment;

    private CustomWorkoutFragment customWorkoutFragment;
    // NEW: Variable to keep track of the currently active fragment
    private Fragment activeFragment;

    // Main screen UI elements
    private ImageButton bluetoothButton; // The Bluetooth icon button

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

    private static final String HOME_FRAGMENT_TAG = "HomeFragmentTag";
    private static final String FREESTYLE_WORKOUT_FRAGMENT_TAG = "FreestyleFragmentTag";
    private static final String MY_WORKOUTS_FRAGMENT_TAG = "MyWorkoutsFragmentTag";
    private static final String CUSTOM_WORKOUT_FRAGMENT_TAG = "CustomWorkoutFragmentTag";

    @Override
    public void onBodyWeightEntered(float weight) {
        Log.d(TAG, "Body weight entered from dialog: " + weight);
        Toast.makeText(this, getString(R.string.body_weight_saved), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBodyWeightCanceled() {
        Log.d(TAG, "Body weight dialog canceled.");
    }


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
                    // Update Bluetooth button color
                    bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.bluetooth_connected_blue));
                    homeFragment.updateInstructionText(getString(R.string.connected_to_a_crimpi_device));

                    // Update UI of the currently active fragment
                    if (activeFragment instanceof HomeFragment) {
                        ((HomeFragment) activeFragment).showConnectedStateUI();
                    }
                    // No specific connected state UI for Freestyle yet, but can be added
                    // if (activeFragment instanceof FreestyleWorkoutFragment) { /* update UI */ }
                });
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for discoverServices.");
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                runOnUiThread(() -> {
                    // Update Bluetooth button color
                    bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.primary_text_color));

                    // Update UI of the currently active fragment
                    if (activeFragment instanceof HomeFragment) {
                        ((HomeFragment) activeFragment).showDisconnectedStateUI();
                    } else if (activeFragment instanceof FreestyleWorkoutFragment) {
                        ((FreestyleWorkoutFragment) activeFragment).resetWorkoutState();
                        Toast.makeText(MainActivity.this, R.string.disconnected_from_device, Toast.LENGTH_SHORT).show();
                    }
                });
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.");
                BluetoothGattService envSenseService = gatt.getService(ENV_SENSE_SERVICE_UUID);
                if (envSenseService != null) {
                    BluetoothGattCharacteristic tempCharacteristic = envSenseService.getCharacteristic(TEMPERATURE_CHARACTERISTIC_UUID);
                    if (tempCharacteristic != null) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for setCharacteristicNotification.");
                            return;
                        }
                        gatt.setCharacteristicNotification(tempCharacteristic, true);
                        BluetoothGattDescriptor descriptor = tempCharacteristic.getDescriptor(CCCD_UUID);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
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
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // This is called when the Pico W sends a notification (e.g., new temperature)
            if (characteristic.getUuid().equals(TEMPERATURE_CHARACTERISTIC_UUID)) {
                byte[] value = characteristic.getValue();
                if (value != null && value.length >= 2) {
                    int rawTemp = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    float forceValue = rawTemp / 100.0f;

                    runOnUiThread(() -> {
                        // NEW: Update the currently active fragment
                        if (activeFragment instanceof FreestyleWorkoutFragment) {
                            ((FreestyleWorkoutFragment) activeFragment).updateForceFromBLE(forceValue);
                        } else if (activeFragment instanceof CustomWorkoutFragment) { // NEW: Add this block for CustomWorkoutFragment
                            ((CustomWorkoutFragment) activeFragment).updateForceFromBLE(forceValue);
                        }
                        // Add more 'else if' for other fragments that need this data
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
                } else {
                    Log.e(TAG, "CCCD write failed: " + status);
                }
            }
        }
    };


    private void disconnectFromDevice() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "Attempted to disconnect, but bluetoothGatt is null.");
            return;
        }
        // Check BLUETOOTH_CONNECT permission before calling disconnect (required for API 31+)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for bluetoothGatt.disconnect().");
            // Inform user if permission is missing, though this should ideally be handled earlier
            return;
        }
        Log.i(TAG, "Disconnecting from GATT server.");
        bluetoothGatt.disconnect(); // This will trigger onConnectionStateChange with STATE_DISCONNECTED
        // The onConnectionStateChange callback will handle UI updates and closing bluetoothGatt.
    }

    // --- Disconnect Confirmation Dialog ---
    private void showDisconnectConfirmationDialog() {
        // Inflate the custom layout for the dialog
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_disconnect_confirmation, null);

        // Find the buttons in the custom layout
        Button buttonYes = dialogView.findViewById(R.id.buttonYes);
        Button buttonNo = dialogView.findViewById(R.id.buttonNo);

        // Create the AlertDialog
        // IMPORTANT: We no longer use .setPositiveButton() or .setNegativeButton() here,
        // as the buttons are now part of our custom layout.
        AlertDialog disconnectDialog = new AlertDialog.Builder(this, R.style.AlertDialogTransparent)
                .setView(dialogView) // Set the custom view for the dialog
                .create(); // Create the dialog instance

        // Set click listeners for the custom buttons
        buttonYes.setOnClickListener(v -> {
            // User clicked Yes, perform disconnect
            disconnectFromDevice();
            disconnectDialog.dismiss(); // Dismiss the dialog after action
        });

        buttonNo.setOnClickListener(v -> {
            // User clicked No, just close the dialog
            disconnectDialog.dismiss();
        });

        // Show the dialog
        disconnectDialog.show();
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth: STATE_OFF");
                        updateBluetoothStatusUI(); // Update UI when Bluetooth is off
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth: STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth: STATE_ON");
                        updateBluetoothStatusUI(); // Update UI when Bluetooth is on
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth: STATE_TURNING_ON");
                        break;
                }
            }
        }
    };

    private void updateBluetoothStatusUI() {
        // Dynamically get the currently attached HomeFragment
        HomeFragment currentHome = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HomeFragmentTag");

        if (currentHome == null || !currentHome.isAdded()) {
            Log.w(TAG, "HomeFragment not attached yet, skipping UI update.");
            return;
        }

        if (bluetoothAdapter == null) {
            currentHome.updateInstructionText(getString(R.string.bluetooth_not_supported));
            bluetoothButton.setEnabled(false);
            bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.primary_text_color));
            currentHome.showDisconnectedStateUI();
        } else if (!bluetoothAdapter.isEnabled()) {
            currentHome.updateInstructionText(getString(R.string.bluetooth_not_enabled));
            bluetoothButton.setEnabled(false);
            bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.primary_text_color));
            currentHome.showDisconnectedStateUI();
        } else {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothButton.setEnabled(true);

            boolean isConnected = false;
            if (bluetoothGatt != null) {
                try {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                        if (manager != null && bluetoothGatt.getDevice() != null) {
                            if (manager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT)
                                    == BluetoothProfile.STATE_CONNECTED) {
                                isConnected = true;
                            }
                        }
                    } else {
                        Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for checking GATT connection state.");
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException checking GATT connection state: " + e.getMessage());
                }
            }

            if (isConnected) {
                currentHome.updateInstructionText(getString(R.string.connected_to_a_crimpi_device));
                bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.bluetooth_connected_blue));
                currentHome.showConnectedStateUI();
            } else {
                currentHome.updateInstructionText(getString(R.string.crimpi_connect));
                bluetoothButton.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.primary_text_color));
                currentHome.showDisconnectedStateUI();
            }
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save which fragment is currently active by its tag
        if (activeFragment instanceof HomeFragment) {
            outState.putString("activeFragmentTag", HOME_FRAGMENT_TAG);
        } else if (activeFragment instanceof FreestyleWorkoutFragment) {
            outState.putString("activeFragmentTag", FREESTYLE_WORKOUT_FRAGMENT_TAG);
        } else if (activeFragment instanceof MyWorkoutsFragment) {
            outState.putString("activeFragmentTag", MY_WORKOUTS_FRAGMENT_TAG);
        } else if (activeFragment instanceof CustomWorkoutFragment) {
            outState.putString("activeFragmentTag", CUSTOM_WORKOUT_FRAGMENT_TAG);
            // Save workout data
            CustomWorkoutFragment customFrag = (CustomWorkoutFragment) activeFragment;
            if (customFrag.getWorkoutData() != null) {
                outState.putSerializable("customWorkoutData", customFrag.getWorkoutData());
            }
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar and set it as the ActionBar
        // Reference to the new Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide the default title
            getSupportActionBar().setTitle(null); // Ensure title is null
        }

        // Initialize Navigation Drawer components
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Initialize custom menu button and set its click listener
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView); // Open drawer from the side it's configured for (end/right)
            }
        });

        // Set listener for navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            switch (id) {
                case R.id.nav_home:
                    loadFragment(homeFragment);
                    break;
                case R.id.nav_freestyle_workout:
                    loadFragment(freestyleWorkoutFragment);
                    break;
                case R.id.nav_my_workouts:
                    loadFragment(myWorkoutsFragment);
                    break;
            }
            drawerLayout.closeDrawer(navigationView); // Close the drawer after item selection
            return true;
        });

        View headerView = navigationView.getHeaderView(0); // Get the header view (assuming it's the first one)
        if (headerView != null) {
            ImageButton bodyWeightButton = headerView.findViewById(R.id.bodyWeightButton);
            if (bodyWeightButton != null) {
                bodyWeightButton.setOnClickListener(v -> {
                    BodyWeightDialogFragment dialog = BodyWeightDialogFragment.newInstance(this);
                    dialog.show(getSupportFragmentManager(), "BodyWeightDialog");
                });
            } else {
                Log.e(TAG, "Body Weight Button not found in nav_header_main.xml");
            }
        } else {
            Log.e(TAG, "Navigation header view is null.");
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> fragments = fm.getFragments();

            for (int i = fragments.size() - 1; i >= 0; i--) {
                Fragment f = fragments.get(i);
                if (f != null && f.isVisible()) {
                    activeFragment = f;
                    updateNavigationDrawerSelection(f);
                    break;
                }
            }
        });
//        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
//            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//            if (current != null) {
//                updateNavigationDrawerSelection(current);
//            }
//        });
        // Initialize main screen UI elements (bluetoothButton remains)
        bluetoothButton = findViewById(R.id.bluetoothButton);

        // Get Bluetooth services
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Initialize RecyclerView adapter (empty initially)
        deviceAdapter = new DeviceAdapter(deviceList, discoveredDevicesMap, this);

        // NEW: Initialize both fragments and load the initial HomeFragment
        if (savedInstanceState == null) { // Only add fragments if not recreating activity
            homeFragment = new HomeFragment();
            freestyleWorkoutFragment = new FreestyleWorkoutFragment(); // Instantiate Freestyle Fragment
            myWorkoutsFragment = new MyWorkoutsFragment();
            customWorkoutFragment = new CustomWorkoutFragment(); // NEW: Initialize CustomWorkoutFragment

            // Load HomeFragment initially
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, homeFragment, "HomeFragmentTag")
                    .commit();
            activeFragment = homeFragment; // Set active fragment
            navigationView.setCheckedItem(R.id.nav_home);
            updateBluetoothStatusUI(); // Update UI for the loaded fragment
        } else {
            // Retrieve the saved tag of the active fragment
            String savedTag = savedInstanceState.getString("activeFragmentTag", "HomeFragmentTag");

            FragmentManager fm = getSupportFragmentManager();

            // Find existing fragments by their tags
            homeFragment = (HomeFragment) fm.findFragmentByTag("HomeFragmentTag");
            if (homeFragment == null) {
                homeFragment = new HomeFragment();
            }

            freestyleWorkoutFragment = (FreestyleWorkoutFragment) fm.findFragmentByTag("FreestyleFragmentTag");
            if (freestyleWorkoutFragment == null) {
                freestyleWorkoutFragment = new FreestyleWorkoutFragment();
            }

            customWorkoutFragment = (CustomWorkoutFragment) fm.findFragmentByTag("CustomWorkoutFragmentTag");
            if (customWorkoutFragment == null) {
                // Try to restore saved workout data if you saved it previously (step 3 will do that)
                CustomWorkoutData savedData = (CustomWorkoutData) savedInstanceState.getSerializable("customWorkoutData");
                if (savedData != null) {
                    customWorkoutFragment = CustomWorkoutFragment.newInstance(savedData);
                }
            }

            // Decide which one should be active based on the saved tag
            switch (savedTag) {
                case "FreestyleFragmentTag":
                    activeFragment = freestyleWorkoutFragment;
                    break;
                case "MyWorkoutsFragmentTag":
                    activeFragment = myWorkoutsFragment;
                    break;
                case "CustomWorkoutFragmentTag":
                    activeFragment = customWorkoutFragment;
                    break;
                default:
                    activeFragment = homeFragment;
                    break;
            }

            // Replace container with the restored active fragment (attach tag again)
            fm.beginTransaction()
                    .replace(R.id.fragment_container, activeFragment, savedTag)
                    .commit();
        }

        // Set up Bluetooth button click listener to show scan dialog
        bluetoothButton.setOnClickListener(v -> {
            // 1. Basic Bluetooth Adapter check
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(MainActivity.this, R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Check permissions first for all BLE operations
            if (!checkAndRequestPermissions()) {
                return;
            }

            // Get the current bluetoothGatt instance *once* for this click event
            final BluetoothGatt currentBluetoothGatt = bluetoothGatt;
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            // 3. Now that permissions are confirmed, determine if we are connected or should scan.
            boolean isCurrentlyConnected = false;

            if (manager != null && currentBluetoothGatt != null) {
                try {
                    BluetoothDevice device = currentBluetoothGatt.getDevice();
                    if (device != null) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            if (manager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED) {
                                isCurrentlyConnected = true;
                            }
                        } else {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for getConnectionState.");
                        }
                    } else {
                        Log.w(TAG, "currentBluetoothGatt.getDevice() returned null. Treating as disconnected.");
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException while checking GATT connection state: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Permission error checking connection state.", Toast.LENGTH_SHORT).show();
                } catch (NullPointerException e) {
                    Log.e(TAG, "NullPointerException when accessing currentBluetoothGatt.getDevice() or getConnectionState: " + e.getMessage());
                }
            }

            if (isCurrentlyConnected) {
                showDisconnectConfirmationDialog();
            } else {
                showScanDialog();
            }
        });
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        String tag;
        int menuItemId;

        if (fragment instanceof HomeFragment) {
            tag = "HomeFragmentTag";
            menuItemId = R.id.nav_home;
        } else if (fragment instanceof FreestyleWorkoutFragment) {
            tag = "FreestyleFragmentTag";
            menuItemId = R.id.nav_freestyle_workout;
        } else if (fragment instanceof MyWorkoutsFragment) {
            tag = "MyWorkoutsFragmentTag";
            menuItemId = R.id.nav_my_workouts;
        } else if (fragment instanceof CustomWorkoutFragment) {
            tag = "CustomWorkoutFragmentTag";
            menuItemId = -1; // No nav item
        } else {
            tag = fragment.getClass().getSimpleName();
            menuItemId = -1;
        }

        // Check if fragment is already added
        Fragment existing = fm.findFragmentByTag(tag);
        if (existing != null) {
            fragment = existing;
        }

        // Avoid crashing by removing current fragment if it's not the same
        if (activeFragment != null && activeFragment != fragment && activeFragment.isAdded()) {
            if (activeFragment instanceof CustomWorkoutFragment) {
                ((CustomWorkoutFragment) activeFragment).stopWorkout();
            }
            transaction.hide(activeFragment);
        }

        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            transaction.show(fragment);
        }

        // Only add to backstack if it's not HomeFragment
        transaction.addToBackStack(tag);

        transaction.commit();
        activeFragment = fragment;

        // Update navigation drawer selection
        if (navigationView != null && menuItemId != -1) {
            navigationView.setCheckedItem(menuItemId);
        }

        // Reset workout state for Freestyle
        if (fragment instanceof FreestyleWorkoutFragment) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activeFragment != null && activeFragment.isAdded()) {
                    ((FreestyleWorkoutFragment) activeFragment).resetWorkoutState();
                }
            });
        }
    }

    private void updateNavigationDrawerSelection(Fragment fragment) {
        if (navigationView == null) return;

        int checkedItemId = -1;

        if (fragment instanceof HomeFragment) {
            checkedItemId = R.id.nav_home;
        } else if (fragment instanceof FreestyleWorkoutFragment) {
            checkedItemId = R.id.nav_freestyle_workout;
        } else if (fragment instanceof MyWorkoutsFragment) {
            checkedItemId = R.id.nav_my_workouts;
        }

        if (checkedItemId != -1) {
            navigationView.setCheckedItem(checkedItemId);
        } else {
            navigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < navigationView.getMenu().size(); i++) {
                navigationView.getMenu().getItem(i).setChecked(false);
            }
            navigationView.getMenu().setGroupCheckable(0, true, true);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(); // Go to the previous fragment
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
    public void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for connectGatt.");
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
            return;
        }

        runOnUiThread(() -> {
            try {
                // Update instruction text in the active fragment
                String deviceDisplayName = (device.getName() != null ? device.getName() : device.getAddress());
                if (activeFragment instanceof HomeFragment) {
                    ((HomeFragment) activeFragment).updateInstructionText(getString(R.string.connection_attempt, deviceDisplayName));
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: Cannot get device name/address for UI update.", e);
                runOnUiThread(() -> {
                    if (activeFragment instanceof HomeFragment) {
                        ((HomeFragment) activeFragment).updateInstructionText(getString(R.string.connection_attempt_permission_error));
                    }
                });
            }
        });

        // Dismiss the scan dialog once connection attempt starts
        if (scanDialog != null && scanDialog.isShowing()) {
            scanDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the BroadcastReceiver when the activity is paused
        unregisterReceiver(bluetoothStateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the BroadcastReceiver when the activity is resumed
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
        // Also update UI in case Bluetooth state changed while app was paused
        updateBluetoothStatusUI();
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
}
