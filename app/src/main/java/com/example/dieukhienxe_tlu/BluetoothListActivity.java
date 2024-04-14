package com.example.dieukhienxe_tlu;

import static com.example.dieukhienxe_tlu.MainActivity.REQUEST_PERMISSIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;

public class BluetoothListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private Toast currentToast;
    private ImageButton BackButton, RefreshButton;
    private ListView ListView;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> detectedDevices;
    private ArrayList<BluetoothDevice> pairedDevices;
    private BluetoothAdapter bluetoothAdapter;
    private Switch switchDevice;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_list);
        BackButton = findViewById(R.id.BackButton);
        RefreshButton = findViewById(R.id.RefreshButton);
        ListView = findViewById(R.id.ListView);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        detectedDevices = new ArrayList<>();
        ListView.setAdapter(deviceListAdapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        switchDevice = findViewById(R.id.switch_device);

        if (bluetoothAdapter == null) {
            currentToast = Toast.makeText(this, "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_SHORT);
            currentToast.show();
            finish();
            return;
        }

        if (!hasPermissions()) {
            requestPermissions();
        }

        ListView.setOnItemClickListener(this);
        BackButton.setOnClickListener(this);
        RefreshButton.setOnClickListener(this);

        switchDevice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentToast != null) {
                currentToast.cancel();
            }
            if (isChecked) {
                currentToast = Toast.makeText(this, "Tìm kiếm thiết bị khả dụng đã được bật", Toast.LENGTH_SHORT);
                currentToast.show();
                startDiscovery();
            } else {
                currentToast = Toast.makeText(this, "Tìm kiếm thiết bị khả dụng đã được tắt", Toast.LENGTH_SHORT);
                currentToast.show();
                showPairedDevices();
            }
        });
    }


    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Xử lý khi quyền không được cấp phép
                    finish();
                }
            }
        }
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                processDiscoveredDevice(device);
            }
        }
    };


    @SuppressLint("MissingPermission")
    private void processDiscoveredDevice(BluetoothDevice device) {
        if (device.getName() != null && device != null && !detectedDevices.contains(device)) {
            detectedDevices.add(device);
            deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
            deviceListAdapter.notifyDataSetChanged();
        }
    }


    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (!bluetoothAdapter.isEnabled()) {
            currentToast = Toast.makeText(this, "Bluetooth chưa được bật", Toast.LENGTH_SHORT);
            currentToast.show();
        }
        deviceListAdapter.clear();
        detectedDevices.clear();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }


    @SuppressLint("MissingPermission")
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.ListView) {
            if (switchDevice.isChecked()) {
                BluetoothDevice selectedDevice = detectedDevices.get(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable._31548142_599994815449197_7414353767399410449_n);
                builder.setTitle("Thông tin thiết bị Bluetooth");
                builder.setMessage("Tên: " + selectedDevice.getName() + "\n"
                        + "Địa chỉ: " + selectedDevice.getAddress() + "\n"
                        + "Loại: " + selectedDevice.getBluetoothClass() + "\n"
                        + "Trạng thái kết nối: " + selectedDevice.getBondState());

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                BluetoothDevice selectedDevice = pairedDevices.get(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable._31548142_599994815449197_7414353767399410449_n);
                builder.setTitle("Thông tin thiết bị Bluetooth");
                builder.setMessage("Tên: " + selectedDevice.getName() + "\n"
                        + "Địa chỉ: " + selectedDevice.getAddress() + "\n"
                        + "Loại: " + selectedDevice.getBluetoothClass() + "\n"
                        + "Trạng thái kết nối: " + selectedDevice.getBondState() + "\n");
                builder.setNegativeButton("HỦY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("GHÉP NỐI", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(selectedDevice.getAddress() != null && selectedDevice.getName() != null){
                            Log.e("BluetoothListActivity","UUID"+ Arrays.toString(selectedDevice.getUuids()));
                            String selectedDeviceAddress = selectedDevice.getAddress();
                            String selectedDeviceName = selectedDevice.getName();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("SELECTED_DEVICE_ADDRESS", selectedDeviceAddress);
                            resultIntent.putExtra("SELECTED_DEVICE_NAME", selectedDeviceName);
                            resultIntent.putExtra("SELECTED_DEVICE", selectedDevice);
                            setResult(RESULT_OK, resultIntent);

                        }
                        // Sau khi kết nối thành công và truyền dữ liệu về MainActivity:
                        dialog.dismiss();
                        finish();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }

        }
    }

    private void refreshDeviceList() {
        deviceListAdapter.clear();
        detectedDevices.clear();

        if (!switchDevice.isChecked()) {
            getPairedDevices();
        } else {
            startDiscovery();
        }
    }

    @SuppressLint("MissingPermission")
    private void getPairedDevices() {
        pairedDevices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
        for (BluetoothDevice device : pairedDevices) {
            deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
            deviceListAdapter.notifyDataSetChanged();
        }
    }

    private void showPairedDevices() {
        deviceListAdapter.clear();
        detectedDevices.clear();
        getPairedDevices();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!switchDevice.isChecked()) {
            getPairedDevices();
        } else {
            registerReceiver(myReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            startDiscovery();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (switchDevice.isChecked())
            unregisterReceiver(myReceiver);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.BackButton) {
            finish();
        } else if (v.getId() == R.id.RefreshButton) {
            refreshDeviceList();
        }
    }
}