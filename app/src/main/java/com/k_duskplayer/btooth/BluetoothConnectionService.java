package com.k_duskplayer.btooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by K_Dusk on 08/05/2019.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    private int mState;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState=STATE_NONE;
        start();
    }

    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                sendData( "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            }catch (IOException e){
                sendData( "AcceptThread: IOException: " + e.getMessage() );
            }

            mmServerSocket = tmp;


        }



        public void run(){
            sendData( "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            while (mState!=STATE_CONNECTING)
            {
                try{
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    //sendData( "run: RFCOM server socket start.....");
                    SystemClock.sleep(300);

                    socket = mmServerSocket.accept();

                    sendData( "run: RFCOM server socket accepted connection.");

                }catch (IOException e){
                    sendData( "AcceptThread: IOException: " + e.getMessage() );
                }
            }


            if (socket != null) {
                synchronized (BluetoothConnectionService.this) {
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            connected(socket);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:

                            try {
                                socket.close();
                            } catch (IOException e) {
                                sendData( "AcceptThread socket close error: "+e);
                            }
                            break;
                    }
                }
            }

            sendData( "END mAcceptThread ");
        }

        public void cancel() {
            sendData( "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                sendData( "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }

    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            sendData( "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }




        public void run(){
            BluetoothSocket tmp = null;
            sendData( "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice


            try {
                sendData( "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " +MY_UUID_INSECURE );
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
                //tmp = createBluetoothSocket(mmDevice);
            } catch (IOException e) {
                sendData( "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                SystemClock.sleep(300);

                mmSocket.connect();
            } catch (IOException e) {
                sendData("connect thread error: "+ e);
                try {
                    mmSocket.close();
                    //mmSocket.connect();
                    sendData( "run: Closed Socket.");
                } catch (IOException e1) {
                    sendData( "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }

                sendData( "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );

            }

            synchronized (BluetoothConnectionService.this) {
                mConnectThread = null;
            }

            connected(mmSocket);
        }
        public void cancel() {
            try {
                sendData( "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                sendData( "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    public synchronized void start() {
        //sendData( "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_LISTEN;

        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device,UUID uuid){
        //sendData( "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth"
                ,"Please Wait...",true);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null)
            {
                mConnectThread.cancel(); mConnectThread = null;
            }
        }

        // Cancel running thread
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
        mState = STATE_CONNECTING;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            sendData("ConnectedThread: Starting.");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[2048];// buffer store for the stream

            //byte[] buf = new byte[2048];
            //StringBuffer st = new StringBuffer();
            int bytes; // bytes returned from read()
            //StringBuilder readMessage = new StringBuilder();
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    if( mmInStream.available() > 0 )
                    {
                        SystemClock.sleep(350);
                        bytes = mmInStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        //st.append(incomingMessage);
                        //String sendmess = new String(buf);
                        Intent incomingDataIntent = new Intent("incomingMessage");
                        incomingDataIntent.putExtra("thedata", incomingMessage);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingDataIntent);
                    }

                }
                catch (IOException e) {
                    //sendData( "write: Error reading Input Stream. " + e.getMessage() );
                    sendData("Connected Device Error");
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            sendData(mBluetoothAdapter.getName()+": " + text);
            byte[] bte = text.getBytes(Charset.defaultCharset());
            try {
                mmOutStream.write(bte);
            } catch (IOException e) {
                //sendData( "write: Error writing output stream. " + e.getMessage() );
                sendData("Write Error");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                sendData( "cancel: close() of mmSocket in Connectedthread failed. " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        //sendData( "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions

        // Cancel the thread
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }
        if (mConnectThread != null)
        {
            mConnectThread.cancel(); mConnectThread = null;
        }
        if (mInsecureAcceptThread != null)
        {
            mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;
        }
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
        mState = STATE_CONNECTED;
    }

    void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        //sendData( "write:");
        //perform the write
        mConnectedThread.write(out);
    }

    void cancel() {

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel(); mConnectedThread = null;
        }
        if (mConnectThread != null)
        {
            mConnectThread.cancel(); mConnectThread = null;
        }
        if (mInsecureAcceptThread != null)
        {
            mInsecureAcceptThread.cancel(); mInsecureAcceptThread = null;
        }
        mState = STATE_NONE;
        sendData("connection with "+mmDevice.getName()+" closed");
    }

    private void sendData(String text){
        Intent logText = new Intent("logtext");
        logText.putExtra("data", text);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(logText);
    }

}
