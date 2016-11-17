package com.example.ibrahim.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    TextView myLabel;
    EditText myTextbox;
    // BluetoothAdapter lets you perform fundamental Bluetooth tasks, such as initiate device discovery
    // y3ne de l bt2dr t5lek tt3ml m3 l Bluetooth asln
    BluetoothAdapter mBluetoothAdapter;
    // BluetoothSocket to listen for connection requests from other devices, and start a scan for Bluetooth LE devices.
    BluetoothSocket mmSocket;
    //Represents a remote Bluetooth device. A BluetoothDevice lets you create a connection with the respective device or query
    // y3ne l ghaz l moqtrn m3ak w t2dr tt3aml m3ah w kda da l hyb2a l arduino isa :D
    BluetoothDevice mmDevice;
    // An output stream accepts output bytes and sends them to some sink
    // y3ne n hb3t data mn l app bta3e masln f na bb3tha 3la hy2t bytes w de btkon fe OutputStream variable
    OutputStream mmOutputStream;
    // 3ks l fo2 lw na hst2bl data htkon 3la hy2t bytes bardo
    InputStream mmInputStream;
    // tab3n de m3rofa , 3obara 3n 4wyat process 3ayz anzm 4o8lohm w kda
    Thread workerThread;
    // array mn no3 bytes bt2bl bytes data
    byte[] readBuffer;
    int readBufferPosition;
    // The Java volatile keyword guarantees visibility of changes to variables across threads.
    // y3ne b5tsar by2ol hy4of t8yer 3la 7aga yes or no
    volatile boolean stopWorker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button openButton = (Button) findViewById(R.id.open);
        Button closeButton = (Button) findViewById(R.id.close);
        Button onButton = (Button) findViewById(R.id.onButton);
        Button offButton = (Button) findViewById(R.id.offButton);
        myLabel = (TextView) findViewById(R.id.label);
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    findBT();
                    openBT();
                } catch (IOException ex) {
                }
            }
        });
        onButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    onButton();
                } catch (Exception e) {
// TODO: handle exception
                }
            }
        });
        offButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    offButton();
                } catch (Exception e) {
// TODO: handle exception
                }
            }
        });
//Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                } catch (IOException ex) {
                }
            }
        });
    }

    void findBT() {
        // b3ml access 3la l bluetooth bta3e
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // lw l ghaz mfeho4 bluetooth hy2ol No bluetooth adapter available
            myLabel.setText("No bluetooth adapter available");
        }
        //hna b2ol lw l bluetooth m2fol aft7o
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        // hna na 3mlt set w de 7aga zay l array 3lan a5zn feh kol l aghza l l bluetooth bta3e l2tha
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // hna pairedDevices.size() > 0 at2ka ano l bluetooth bta3e la2t device
        if (pairedDevices.size() > 0) {
            // de gomlt for 3l4an alf 3la kol l aghza l bluetooth bta3e l2tha
            for (BluetoothDevice device : pairedDevices) {
                // hna b2a b2olo lw wa7d mn l aghza l l2tha asmo linvor l hwa hykon asm l arduino
                // a2olo b2a a3ml eqtran beh tmhedan 3l4an a3ml send w receive ll data
                if (device.getName().equals("linvor")) //this name have to be replaced with your bluetooth device name
                {
                    mmDevice = device;
                    // Log.v de 7aga zay notification bs ll developer fl android studio 3l4an at2kd no 4o8le tmam
                    Log.v("ArduinoBT", "findBT found device named " + mmDevice.getName());
                    Log.v("ArduinoBT", "device address is " + mmDevice.getAddress());
                    break;
                }
            }
        }
        // b3d ma ft7na l bluetooth w 3mlna aqtran f bn2ol Bluetooth Device Found :D
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException {
        // UUID de 7aga t2dr t2ol enha btsa3d 3la eno l connection ykon more secure w kda
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        // createRfcommSocketToServiceRecord
        // y3ne ready to start a secure outgoing connection to this remote device using uuid.
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        // bbd2a l connetion
        mmSocket.connect();
        // bbd2a ab2a mota7 ene  ab3ta data mn w ela l bluetooth bta3e
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();
        myLabel.setText("Bluetooth Opened");
    }

    // hna b2a fl metod de bytm ersal w estqbal l data :D
    void beginListenForData() {
        // Handler de class btmkne send and process Message and Runnable objects associated with a thread's MessageQueue
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        // stopWorker false y3ne mfe4 ay t8yeer 7sl lsa fl arduino masln
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        //This interface is designed to provide a common protocol for objects that wish to execute code while they are active.
        // For example, Runnable is implemented by class Thread.
        // Being active simply means that a thread has been started and has not yet been stopped.
        workerThread = new Thread(new Runnable() {
            // run() de m3naha en l thread htbd2a 4o8l w kda
            public void run() {
                // b2olo tol ma l thread 48ala w kman by7sl t8yeer ll arduino masln
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    // try y3ne grb kza w catch y3ne lw mnf34 l tgrba 2ol kza
                    try {
                        // na 3mlt variable mn no3 int asmo bytesAvailable w lw hwa >0 y3ne fe data gaya ll bluetooth
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            // 3mlt packetBytes array b siza ysawe 3dd l byte l gaya ll bluetooth
                            byte[] packetBytes = new byte[bytesAvailable];
                            // hna na b2a b2ra l data now ! b3d m astlmtha
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                // hna b2olo lw la2et  element fl array equal b
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    // a3ml copy ll array readBuffer 7otha fe encodedBytes array
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            // htb3 l data l fl encodedBytes array
                                            myLabel.setText(data);
                                        }
                                    });
                                } else {
                                    // lw element fl array not equal b
                                    // b2olo zwd byte fl buffer read
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start(); // a2olo abd2aa l thread w lma tbd2a l thread hyd5ol ynfz l f run();
    }

    // a3ml handle l ay error
    void onButton() throws IOException {
        mmOutputStream.write("1".getBytes());
    }

    // a3ml handle l ay error
    void offButton() throws IOException {
        mmOutputStream.write("2".getBytes());
    }

    // b2fl b2a ay 7aga ft7tha foo2 
    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
}
