package com.lecheng.hello.ant_bms;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BcService {
    private static final boolean f5D = true;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "AtyMain";
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_NONE = 0;
    private static final String TAG = "BcService";
    private Context context;
    private AcceptThread mAcceptThread;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private final Handler mHandler;
    private int mState = 0;
    byte[] read_data = new byte[600];
    private int read_data_140_i = 0;
    private int read_data_chu_li_140_i;
    private int read_data_chu_li_i;
    private int read_data_i = 0;

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint({"NewApi"})
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = BcService.this.mAdapter.listenUsingRfcommWithServiceRecord(BcService.NAME, BcService.MY_UUID);
            } catch (IOException e) {
                Log.e(BcService.TAG, "listen() failed", e);
            }
            this.mmServerSocket = tmp;
        }

        @SuppressLint({"NewApi"})
        public void run() {
            Log.d(BcService.TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            while (BcService.this.mState != 3) {
                try {
                    BluetoothSocket socket = this.mmServerSocket.accept();
                    if (socket != null) {
                        synchronized (BcService.this) {
                            switch (BcService.this.mState) {
                                case 0:
                                case 3:
                                    try {
                                        socket.close();
                                        break;
                                    } catch (IOException e) {
                                        Log.e(BcService.TAG, "Could not close unwanted socket", e);
                                        break;
                                    }
                                case 1:
                                case 2:
                                    BcService.this.connected(socket, socket.getRemoteDevice());
                                    break;
                            }
                        }
                    }
                } catch (IOException e2) {
                    Log.e(BcService.TAG, "accept() failed", e2);
                }
            }
            Log.i(BcService.TAG, "END mAcceptThread");
            return;
        }

        @SuppressLint({"NewApi"})
        public void cancel() {
            Log.d(BcService.TAG, "cancel " + this);
            try {
                this.mmServerSocket.close();
            } catch (IOException e) {
                Log.e(BcService.TAG, "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        @SuppressLint({"NewApi"})
        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(BcService.MY_UUID);
            } catch (IOException e) {
                Log.e(BcService.TAG, "create() failed", e);
            }
            this.mmSocket = tmp;
        }

        @SuppressLint({"NewApi"})
        public void run() {
            Log.i(BcService.TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            BcService.this.mAdapter.cancelDiscovery();
            try {
                this.mmSocket.connect();
                synchronized (BcService.this) {
                    BcService.this.mConnectThread = null;
                }
                BcService.this.connected(this.mmSocket, this.mmDevice);
            } catch (IOException e) {
                BcService.this.connectionFailed();
                try {
                    this.mmSocket.close();
                } catch (IOException e2) {
                    Log.e(BcService.TAG, "unable to close() socket during connection failure", e2);
                }
                BcService.this.start();
            }
        }

        @SuppressLint({"NewApi"})
        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.e(BcService.TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        @SuppressLint({"NewApi"})
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(BcService.TAG, "create ConnectedThread");
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(BcService.TAG, "temp sockets not created", e);
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(BcService.TAG, "1-BCService ConnectedThread started, run () method is executed");
            byte[] buffer = new byte[1024];
            while (true) {
                int wei_chu_li;
                byte j;
                int wei_chu_li_140;
                Log.i(BcService.TAG, "2-while big circle open! ");
                int bytes = this.mmInStream.read(buffer);
                for (int i = 0; i < bytes; i++) {
                    BcService.this.read_data[BcService.this.read_data_i] = buffer[i];
                    BcService.this.read_data_i = BcService.this.read_data_i + 1;
                    if (BcService.this.read_data_i >= 600) {
                        BcService.this.read_data_i = 0;
                    }
                    BcService.this.read_data_140_i = BcService.this.read_data_i;
                }
                if (BcService.this.read_data_i > BcService.this.read_data_chu_li_i) {
                    wei_chu_li = BcService.this.read_data_i - BcService.this.read_data_chu_li_i;
                } else {
                    wei_chu_li = (BcService.this.read_data_i + 600) - BcService.this.read_data_chu_li_i;
                }
                while (wei_chu_li > 5) {
                    byte[] zhen_tou = new byte[2];
                    for (j = (byte) 0; j < (byte) 2; j = (byte) (j + 1)) {
                        if (BcService.this.read_data_chu_li_i + j < 600) {
                            zhen_tou[j] = BcService.this.read_data[BcService.this.read_data_chu_li_i + j];
                        } else {
                            zhen_tou[j] = BcService.this.read_data[(BcService.this.read_data_chu_li_i + j) - 600];
                        }
                    }
                    try {
                        if ((zhen_tou[0] == (byte) -91 && zhen_tou[1] == (byte) -91) || ((zhen_tou[0] == (byte) 90 && zhen_tou[1] == (byte) 90) || (zhen_tou[0] == (byte) -37 && zhen_tou[1] == (byte) -37))) {
                            Log.i(BcService.TAG, "4.1-run () method internal: header validation passed");
                            byte[] read_data_cache6 = new byte[6];
                            for (j = (byte) 0; j < (byte) 6; j = (byte) (j + 1)) {
                                if (BcService.this.read_data_chu_li_i + j < 600) {
                                    read_data_cache6[j] = BcService.this.read_data[BcService.this.read_data_chu_li_i + j];
                                } else {
                                    read_data_cache6[j] = BcService.this.read_data[(BcService.this.read_data_chu_li_i + j) - 600];
                                }
                            }
                            if (((byte) ((read_data_cache6[2] + read_data_cache6[3]) + read_data_cache6[4])) == read_data_cache6[5]) {
                                for (j = (byte) 0; j < (byte) 6; j++) {
                                    if (BcService.this.read_data_chu_li_i + j < 600) {
                                        BcService.this.read_data[BcService.this.read_data_chu_li_i + j] = (byte) 0;
                                    } else {
                                        BcService.this.read_data[(BcService.this.read_data_chu_li_i + j) - 600] = (byte) 0;
                                    }
                                }
                                if (BcService.this.read_data_chu_li_i < 594) {
                                    BcService.this.read_data_chu_li_i = BcService.this.read_data_chu_li_i + 6;
                                } else {
                                    BcService.this.read_data_chu_li_i = BcService.this.read_data_chu_li_i - 594;
                                }
                                wei_chu_li -= 6;
                                BcService.this.mHandler.obtainMessage(2, bytes, -1, read_data_cache6).sendToTarget();
                            } else {
                                BcService.this.read_data_chu_li_i = BcService.this.read_data_chu_li_i + 1;
                                if (BcService.this.read_data_chu_li_i > 599) {
                                    BcService.this.read_data_chu_li_i = 0;
                                }
                                wei_chu_li--;
                            }
                        } else {
                            BcService.this.read_data_chu_li_i = BcService.this.read_data_chu_li_i + 1;
                            if (BcService.this.read_data_chu_li_i > 599) {
                                BcService.this.read_data_chu_li_i = 0;
                            }
                            wei_chu_li--;
                        }
                    } catch (IOException e) {
                        Log.e(BcService.TAG, "7-Disconnect - IOException e", e);
                        BcService.this.connectionLost();
                        return;
                    }
                }
                if (BcService.this.read_data_140_i > BcService.this.read_data_chu_li_140_i) {
                    wei_chu_li_140 = BcService.this.read_data_140_i - BcService.this.read_data_chu_li_140_i;
                } else {
                    wei_chu_li_140 = (BcService.this.read_data_140_i + 600) - BcService.this.read_data_chu_li_140_i;
                }
                while (wei_chu_li_140 > 139) {
                    byte[] zhen_tou_140 = new byte[4];
                    for (j = (byte) 0; j < (byte) 4; j = (byte) (j + 1)) {
                        if (BcService.this.read_data_chu_li_140_i + j < 600) {
                            zhen_tou_140[j] = BcService.this.read_data[BcService.this.read_data_chu_li_140_i + j];
                        } else {
                            zhen_tou_140[j] = BcService.this.read_data[(BcService.this.read_data_chu_li_140_i + j) - 600];
                        }
                    }
                    if (zhen_tou_140[0] == (byte) -86 && zhen_tou_140[1] == (byte) 85 && zhen_tou_140[2] == (byte) -86 && zhen_tou_140[3] == (byte) -1) {
                        int j2;
                        Log.i(BcService.TAG, "4.2-run () method Internal: 140 header validation passed");
                        short[] read_data_cache140 = new short[140];
                        for (j2 = 0; j2 < 140; j2++) {
                            if (BcService.this.read_data_chu_li_140_i + j2 < 600) {
                                if (BcService.this.read_data[BcService.this.read_data_chu_li_140_i + j2] >= (byte) 0) {
                                    read_data_cache140[j2] = (short) BcService.this.read_data[BcService.this.read_data_chu_li_140_i + j2];
                                } else {
                                    read_data_cache140[j2] = (short) (BcService.this.read_data[BcService.this.read_data_chu_li_140_i + j2] + 256);
                                }
                            } else if (BcService.this.read_data[(BcService.this.read_data_chu_li_140_i + j2) - 600] >= (byte) 0) {
                                read_data_cache140[j2] = (short) BcService.this.read_data[(BcService.this.read_data_chu_li_140_i + j2) - 600];
                            } else {
                                read_data_cache140[j2] = (short) (BcService.this.read_data[(BcService.this.read_data_chu_li_140_i + j2) - 600] + 256);
                            }
                        }
                        int add_140 = 0;
                        for (j2 = 0; j2 < 138; j2++) {
                            if (j2 > 3) {
                                add_140 += read_data_cache140[j2];
                            }
                        }
                        int add_140_1 = (read_data_cache140[138] << 8) + read_data_cache140[139];
                        Log.i(BcService.TAG, "And check the low byte：" + add_140);
                        Log.i(BcService.TAG, "And calibration read：" + add_140_1);
                        if (add_140_1 == add_140) {
                            for (j = (byte) 0; j < (byte) -116; j++) {
                                if (BcService.this.read_data_chu_li_140_i + j < 600) {
                                    BcService.this.read_data[BcService.this.read_data_chu_li_140_i + j] = (byte) 0;
                                } else {
                                    BcService.this.read_data[(BcService.this.read_data_chu_li_140_i + j) - 600] = (byte) 0;
                                }
                            }
                            if (BcService.this.read_data_chu_li_140_i < 460) {
                                BcService.this.read_data_chu_li_140_i = BcService.this.read_data_chu_li_140_i + 140;
                            } else {
                                BcService.this.read_data_chu_li_140_i = BcService.this.read_data_chu_li_140_i - 460;
                            }
                            wei_chu_li_140 -= 140;
                            EventBus.getDefault().post(read_data_cache140);
                        } else {
                            BcService.this.read_data_chu_li_140_i = BcService.this.read_data_chu_li_140_i + 1;
                            if (BcService.this.read_data_chu_li_140_i > 599) {
                                BcService.this.read_data_chu_li_140_i = 0;
                            }
                            wei_chu_li_140--;
                        }
                    } else {
                        BcService.this.read_data_chu_li_140_i = BcService.this.read_data_chu_li_140_i + 1;
                        if (BcService.this.read_data_chu_li_140_i > 599) {
                            BcService.this.read_data_chu_li_140_i = 0;
                        }
                        wei_chu_li_140--;
                    }
                }
                Log.i(BcService.TAG, "6-run () method Internal: run end, untreated number：" + wei_chu_li);
            }
        }

        public void write(byte[] buffer) {
            try {
                this.mmOutStream.write(buffer);
                if (buffer[2] != (byte) 0) {
                    BcService.this.mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(BcService.TAG, "Exception during write", e);
            }
        }

        @SuppressLint({"NewApi"})
        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.e(BcService.TAG, "close() of connect socket failed", e);
            }
        }
    }

    @SuppressLint({"NewApi"})
    public BcService(Context context, Handler handler) {
        this.mHandler = handler;
        this.context = context;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + this.mState + " -> " + state);
        this.mState = state;
        this.mHandler.obtainMessage(1, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return this.mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mAcceptThread == null) {
            this.mAcceptThread = new AcceptThread();
            this.mAcceptThread.start();
        }
        setState(1);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        if (this.mState == 2 && this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        this.mConnectThread = new ConnectThread(device);
        this.mConnectThread.start();
        setState(2);
    }

    @SuppressLint({"NewApi"})
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }
        this.mConnectedThread = new ConnectedThread(socket);
        this.mConnectedThread.start();
        Message msg = this.mHandler.obtainMessage(4);
        Bundle bundle = new Bundle();
        bundle.putString(AtyMain.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        setState(3);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }
        setState(0);
    }

    public void write(byte[] out) {
        synchronized (this) {
            if (this.mState != 3) {
                return;
            }
            ConnectedThread r = this.mConnectedThread;
            r.write(out);
        }
    }

    private void connectionFailed() {
        setState(1);
        Message msg = this.mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString(AtyMain.TOAST, "Unable to connect device");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(1);
        Message msg = this.mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString(AtyMain.TOAST, "Device connection lost");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }
}
