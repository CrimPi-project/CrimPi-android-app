// BluetoothDeviceEntry.java
package com.almogbb.crimpi.data; // New package: com.almogbb.crimpi.data

/**
 * Data class to hold Bluetooth device information for the RecyclerView.
 * This class is now a top-level public class in the 'data' package.
 */
public class BluetoothDeviceEntry {
    public String name;
    public String address;

    public BluetoothDeviceEntry(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
