package com.example.dieukhienxe_tlu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, CompoundButton.OnCheckedChangeListener {
    static final int REQUEST_ENABLE_BT = 2;
    static final int REQUEST_PERMISSIONS = 3;
    static final int REQUEST_SELECT_DEVICE = 4;
    private Toast currentToast;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private Button Connect_btn, Disconnect_btn;
    private ImageButton arrow_up_btn, arrow_down_btn, arrow_left_btn, arrow_right_btn, stop_btn;
    private TextView statusText, CarMovementStateText, DeviceName, DeviceTitle;
    private Switch lineDectectionSwitch, avoidObjectSwitch, lineDectectionSwitch_And_avoidObjectSwitch;
    private MyBluetoothService bluetoothService;
    private Handler handler;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getView();


        Connect_btn.setOnClickListener(this);
        Disconnect_btn.setOnClickListener(this);

        arrow_up_btn.setOnTouchListener(this);
        arrow_down_btn.setOnTouchListener(this);
        arrow_left_btn.setOnTouchListener(this);
        arrow_right_btn.setOnTouchListener(this);
        stop_btn.setOnTouchListener(this);

        lineDectectionSwitch.setOnCheckedChangeListener(this);
        avoidObjectSwitch.setOnCheckedChangeListener(this);
        lineDectectionSwitch_And_avoidObjectSwitch.setOnCheckedChangeListener(this);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Thiết bị không hỗ trợ Bluetooth");
            currentToast.show();
            return;
        }

        if (!hasPermissions()) {
            requestPermissions();
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        String[] permissions = {
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth đã được bật, mở màn hình BluetoothList
                Intent bluetoothConnectIntent = new Intent(this, BluetoothListActivity.class);
                startActivityForResult(bluetoothConnectIntent, REQUEST_SELECT_DEVICE);
            } else {
                // Người dùng từ chối bật Bluetooth
                showToast("Hãy bật Bluetooth để tiếp tục");
            }
        } else if (requestCode == REQUEST_SELECT_DEVICE) {
            if (resultCode == RESULT_OK) {
                // Lấy thông tin từ Intent
                String selectedDeviceName = data.getStringExtra("SELECTED_DEVICE_NAME");
                String selectedDeviceAddress = data.getStringExtra("SELECTED_DEVICE_ADDRESS");
                if (bluetoothAdapter != null) {
                    bluetoothDevice = bluetoothAdapter.getDefaultAdapter().getRemoteDevice(selectedDeviceAddress);
                    handler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(@NonNull Message msg) {
                            super.handleMessage(msg);
                            switch (msg.what) {
                                case MyBluetoothService.MessageConstants.MESSAGE_READ:
                                    byte[] readBuffer = (byte[]) msg.obj;
                                    int numBytes = msg.arg1;
                                    break;
                                case MyBluetoothService.MessageConstants.MESSAGE_WRITE:
                                    break;
                                case MyBluetoothService.MessageConstants.MESSAGE_TOAST:
                                    String message = (String) msg.obj;
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                    if (message.equals("Kết nối thành công")) {
                                        statusText.setText("Đã kết nối");
                                        statusText.setTextColor(Color.GREEN);
                                    }
                                    if (message.equals("Đang kết nối")) {
                                        statusText.setText("Đang kết nối...");
                                        statusText.setTextColor(Color.parseColor("#c8a900"));
                                    }
                                    if (message.equals("Kết nối thất bại")) {
                                        statusText.setText("Chưa kết nối");
                                        statusText.setTextColor(Color.RED);
                                    }
                                    break;
                            }
                        }
                    };
                    Log.e("MainActivity", "----> BluetoothAdapter': " + bluetoothAdapter);
                    bluetoothService = new MyBluetoothService(handler, bluetoothAdapter);
                    bluetoothService.connectDevice(bluetoothDevice);
                } else {
                    currentToast = Toast.makeText(this, "Lỗi kết nối Bluetooth", Toast.LENGTH_SHORT);
                    currentToast.show();
                }
            } else {
                // Người dùng từ chối hoặc có lỗi xảy ra
                showToast("Không thể chọn thiết bị");
            }
        }
    }

    private void requestBluetoothPermission() {
        if (!bluetoothAdapter.isEnabled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_PERMISSIONS);
                return;
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @SuppressLint("MissingPermission")
    public void DisconnectedBluetooth() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();

        }
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
        if (bluetoothAdapter.isEnabled()) {
            showToast("Bluetooth đã được tắt");
            bluetoothAdapter.disable();
        } else {
            showToast("Bluetooth hiện không bật");
        }
        statusText.setText("Chưa kết nối");
        statusText.setTextColor(Color.RED);
        DeviceTitle.setText("");
        DeviceName.setText("");
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.connect_btn) {
            if (!bluetoothAdapter.isEnabled()) {
                requestBluetoothPermission();
            } else {
                Intent bluetoothConnectIntent = new Intent(this, BluetoothListActivity.class);
                startActivityForResult(bluetoothConnectIntent, REQUEST_SELECT_DEVICE);
            }
        } else if (v.getId() == R.id.disconnect_btn) {
            DisconnectedBluetooth();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.arrow_up_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Tiến");
                sendData("a");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
                sendData("e");
            }
        } else if (v.getId() == R.id.arrow_down_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Lùi");
                sendData("b");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
                sendData("e");
            }
        } else if (v.getId() == R.id.arrow_left_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Trái");
                sendData("d");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
                sendData("e");
            }
        } else if (v.getId() == R.id.arrow_right_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Phải");
                sendData("c");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
                sendData("e");
            }
        } else if (v.getId() == R.id.arrow_stop_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Dừng");
                sendData("e");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        }
        return false;
    }

    private void sendData(String index) {
        byte[] data = index.getBytes();
        if (bluetoothService != null) {
            bluetoothService.sendData(data);
        } else {
            showToast("Không thể gửi tín hiệu");
        }
    }

    private void setStatusText(String msg) {
        CarMovementStateText.setText(msg);
        if (msg != "") {
            CarMovementStateText.setTextColor(Color.GREEN);
        } else {
            CarMovementStateText.setText("...");
            CarMovementStateText.setTextColor(Color.BLACK);
        }
    }

    protected void getView() {
        Connect_btn = findViewById(R.id.connect_btn);
        Disconnect_btn = findViewById(R.id.disconnect_btn);
        statusText = findViewById(R.id.statusText);
        CarMovementStateText = findViewById(R.id.CarMovementStateText);
        DeviceName = findViewById(R.id.DeviceName);
        DeviceTitle = findViewById(R.id.DeviceTitle);
        arrow_up_btn = findViewById(R.id.arrow_up_btn);
        arrow_down_btn = findViewById(R.id.arrow_down_btn);
        arrow_left_btn = findViewById(R.id.arrow_left_btn);
        arrow_right_btn = findViewById(R.id.arrow_right_btn);
        stop_btn = findViewById(R.id.arrow_stop_btn);
        lineDectectionSwitch = findViewById(R.id.line_dectection_switch);
        avoidObjectSwitch = findViewById(R.id.avoid_object_switch);
        lineDectectionSwitch_And_avoidObjectSwitch = findViewById(R.id.line_dectection_and_avoid_dectection_switch);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.line_dectection_switch) {
            if (isChecked) {
                if (bluetoothAdapter.isEnabled()) {
                    sendData("f");
                    CarMovementStateText.setText("Tự động dò line!");
                    CarMovementStateText.setTextColor(Color.parseColor("#c8a900"));
                    showToast("Đã bật chế độ tự động dò line");
                    ButtonDisable();
                } else {
                    showToast("Hãy bật Bluetooth để sử dụng chức năng dò line");
                    buttonView.setChecked(false);
                }
            } else {
                sendData("g");
                CarMovementStateText.setText("");
                showToast("Đã tắt chế độ tự động dò line");
                ButtonEnable();
            }
        } else if (buttonView.getId() == R.id.avoid_object_switch) {
            if (isChecked) {
                if (bluetoothAdapter.isEnabled()) {
                    sendData("h");
                    CarMovementStateText.setText("Tránh vật cản!");
                    CarMovementStateText.setTextColor(Color.parseColor("#c8a900"));
                    showToast("Đã bật chế độ tự động tránh vật cản");
                    ButtonDisable();
                } else {
                    showToast("Hãy bật Bluetooth để sử dụng chức năng tránh vật cản");
                    buttonView.setChecked(false);
                }
            } else {
                sendData("i");
                CarMovementStateText.setText("");
                showToast("Đã tắt chế độ tự động tránh vật cản");
                ButtonEnable();
            }
        } else if (buttonView.getId() == R.id.line_dectection_and_avoid_dectection_switch) {
            if (isChecked) {
                if (bluetoothAdapter.isEnabled()) {
                    sendData("k");
                    CarMovementStateText.setText("Dò line và tránh vật cản!");
                    CarMovementStateText.setTextColor(Color.parseColor("#c8a900"));
                    showToast("Đã bật chế độ tự động dò line và tránh vật cản");
                    ButtonDisable();
                } else {
                    showToast("Hãy bật Bluetooth để sử dụng chức năng dò line và tránh vật cản");
                    buttonView.setChecked(false);
                }
            } else {
                sendData("l");
                CarMovementStateText.setText("");
                showToast("Đã tắt chế độ tự động dò line và tránh vật cản");
                ButtonEnable();
            }
        }
    }

    private void ButtonDisable() {
        arrow_up_btn.setEnabled(false);
        arrow_down_btn.setEnabled(false);
        arrow_left_btn.setEnabled(false);
        arrow_right_btn.setEnabled(false);
        stop_btn.setEnabled(false);
    }

    private void ButtonEnable() {
        arrow_up_btn.setEnabled(true);
        arrow_down_btn.setEnabled(true);
        arrow_left_btn.setEnabled(true);
        arrow_right_btn.setEnabled(true);
        stop_btn.setEnabled(true);
    }
    private void showToast(String message) {
        // Hủy Toast hiện tại nếu có
        if (currentToast != null) {
            currentToast.cancel();
        }
        // Tạo và hiển thị Toast mới
        currentToast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }
    @SuppressLint("MissingPermission")
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            sendData("e");
            bluetoothService.disconnect();
        }
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }
}