// DeviceAdapter.java
package com.almogbb.crimpi.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Added import for Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.almogbb.crimpi.MainActivity;
import com.almogbb.crimpi.R;
import com.almogbb.crimpi.data.BluetoothDeviceEntry;

import java.util.List;
import java.util.Map; // Added import for Map

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<BluetoothDeviceEntry> localDeviceList;
    // NEW: Declare discoveredDevicesMap as a member variable
    private final Map<String, BluetoothDevice> discoveredDevicesMap;
    // NEW: Declare activityContext as a member variable to hold MainActivity instance
    private final MainActivity activityContext;

    // UPDATED CONSTRUCTOR: Now accepts discoveredDevicesMap and MainActivity context
    public DeviceAdapter(List<BluetoothDeviceEntry> deviceList, Map<String, BluetoothDevice> discoveredDevicesMap, MainActivity activityContext) {
        this.localDeviceList = deviceList;
        this.discoveredDevicesMap = discoveredDevicesMap;
        this.activityContext = activityContext;
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
                        // CORRECTED CALL: Call connectToDevice on the MainActivity instance
                        activityContext.connectToDevice(deviceToConnect);
                    } else {
                        // Re-added Toast for error case, using activityContext
                        Toast.makeText(activityContext, "Error: Device object not found.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
