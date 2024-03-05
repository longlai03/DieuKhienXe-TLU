package com.example.dieukhienxe_tlu;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MyBluetoothService {
    private static final String TAG = "MY_APP_DEBUG_TAG";
    //UUID này đại diện cho dịch vụ Generic Attribute Profile (GATT)
    private static final UUID MY_UUID = UUID.fromString("00001108-0000-1000-8000-00805f9b34fb");
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public MyBluetoothService(Handler handler, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.handler = handler;
    }

    public void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private int connectionAttempts = 0;
        private boolean isConnected = false;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @Override
        @SuppressLint("MissingPermission")
        public void run() {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            handler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Đang kết nối").sendToTarget();
            while (!isConnected && connectionAttempts < 3) {
                try {
                    mmSocket.connect();
                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Kết nối thành công").sendToTarget();
                    isConnected = true;
                    Log.i(TAG, "----> BluetoothSocket: " + mmSocket);
                    Log.i(TAG, "----> BluetoothSocket Connect: " + mmSocket.isConnected());

                } catch (IOException e) {
                    // Xử lý lỗi kết nối
                    Log.e(TAG, "Connection failed, trying again...", e);
                    connectionAttempts++;
                    if (connectionAttempts == 3) {
                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Kết nối thất bại").sendToTarget();
                        cancel();
                    } else {
                        try {
                            // Đợi một khoảng thời gian trước khi thử lại kết nối
                            Thread.sleep(1000);
                        } catch (InterruptedException interruptedException) {
                            Log.e(TAG, "Thread interrupted", interruptedException);
                        }
                    }
                }
            }
            if(mmSocket.isConnected()) {
                manageConnectedSocket(mmSocket);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;
            int numberOfReading = 0;
            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String receivedMessage = new String(mmBuffer, 0, numBytes);
                    numberOfReading++;
                    Log.d(TAG, "Received message: " + receivedMessage);
                    Log.d(TAG, "Number of Reading: " + numberOfReading);
                    handler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer).sendToTarget();


                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                String sentData = new String(bytes, "UTF-8");
                Log.d(TAG, "Sent data: " + sentData);
                Log.d(TAG, "Outstream: " + mmOutStream.toString());
                handler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, -1, bytes).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                handler.obtainMessage(MessageConstants.MESSAGE_TOAST, -1, -1, "Couldn't send data to the other device").sendToTarget();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public void connectDevice(BluetoothDevice device) {
        disconnect();
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void sendData(byte[] data) {
        if (connectedThread != null && connectedThread.isAlive()) {
            connectedThread.write(data);
        } else {
            // Xử lý khi kết nối bị mất
            Log.e(TAG, "Bluetooth connection is lost");
            handler.obtainMessage(MessageConstants.MESSAGE_TOAST, "Không thể gửi dữ liệu do lỗi kết nối").sendToTarget();
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    public interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
    }
}
