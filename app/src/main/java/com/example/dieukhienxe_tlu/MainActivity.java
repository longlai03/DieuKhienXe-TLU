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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    static final int REQUEST_ENABLE_BT = 2;
    static final int REQUEST_PERMISSIONS = 3;
    static final int REQUEST_SELECT_DEVICE = 4;
    private static final int REQUEST_BLUETOOTH_CONNECTION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private Button Connect_btn, Disconnect_btn;
    private ImageButton arrow_up_btn, arrow_down_btn, arrow_left_btn, arrow_right_btn;
    private TextView statusText, CarMovementStateText, DeviceName, DeviceTitle;
    private MyBluetoothService bluetoothService;
    private Handler handler;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getView();

        // Set OnClickListener for buttons
        Connect_btn.setOnClickListener(this);
        Disconnect_btn.setOnClickListener(this);

        // Set OnTouchListener for image buttons
        arrow_up_btn.setOnTouchListener(this);
        arrow_down_btn.setOnTouchListener(this);
        arrow_left_btn.setOnTouchListener(this);
        arrow_right_btn.setOnTouchListener(this);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Bạn đã từ chối bật Bluetooth", Toast.LENGTH_SHORT).show();
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
                                    // Handle message read
                                    // Xử lý dữ liệu nhận được từ kết nối Bluetooth ở đây
                                    byte[] readBuffer = (byte[]) msg.obj;
                                    int numBytes = msg.arg1;
                                    break;
                                case MyBluetoothService.MessageConstants.MESSAGE_WRITE:
                                    // Handle message write
                                    break;
                                case MyBluetoothService.MessageConstants.MESSAGE_TOAST:
                                    // Handle toast message
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
                    Toast.makeText(this, "Lỗi kết nối Bluetooth", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Người dùng từ chối hoặc có lỗi xảy ra
                Toast.makeText(this, "Không thể chọn thiết bị", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "Bluetooth đã được tắt", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.disable();
        } else {
            Toast.makeText(MainActivity.this, "Bluetooth hiện không bật", Toast.LENGTH_SHORT).show();
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
            //Ngắt kết nối Bluetooth
            DisconnectedBluetooth();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.arrow_up_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Tiến");
                // Gửi dữ liệu điều khiển điều khiển xe khi nút TIẾN được nhấn
                sendData("TIEN".getBytes());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        } else if (v.getId() == R.id.arrow_down_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Lùi");
                // Gửi dữ liệu điều khiển điều khiển xe khi nút TIẾN được nhấn
                sendData("LUI".getBytes());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        } else if (v.getId() == R.id.arrow_left_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Trái");
                // Gửi dữ liệu điều khiển điều khiển xe khi nút TIẾN được nhấn
                sendData("TRAI".getBytes());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        } else if (v.getId() == R.id.arrow_right_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Phải");
                // Gửi dữ liệu điều khiển điều khiển xe khi nút TIẾN được nhấn
                sendData("PHAI".getBytes());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        } else if (v.getId() == R.id.arrow_stop_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setStatusText("Dừng");
                // Gửi dữ liệu điều khiển điều khiển xe khi nút TIẾN được nhấn
                sendData("DUNG".getBytes());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                CarMovementStateText.setText("");
            }
        }
        return false;
    }

    private void sendData(byte[] data) {
        // Gọi phương thức sendData từ MyBluetoothService (nếu có)
        if (bluetoothService != null) {
            bluetoothService.sendData(data);
        } else {
            Toast.makeText(this, "Không thể gửi tín hiệu", Toast.LENGTH_SHORT).show();
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
    }

    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
    }
}