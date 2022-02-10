package com.example.panelscan;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;



import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.example.usbarduino.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    ImageView status;


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            //serialPort.read(mCallback);
                            status.setVisibility(View.VISIBLE);

                        } else {
                            Toast.makeText(getApplicationContext(),"PORT NOT OPEN",Toast.LENGTH_SHORT).show();serialPort.setBaudRate(9600);
                            //Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),"PORT IS NULL",Toast.LENGTH_SHORT).show();serialPort.setBaudRate(9600);
                        //Log.d("SERIAL", "PORT IS NULL");
                    }

                } else {
                    Toast.makeText(getApplicationContext(),"PERM NOT GRANTED",Toast.LENGTH_SHORT).show();serialPort.setBaudRate(9600);
                    //Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                startUSB();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close();
                status.setVisibility(View.INVISIBLE);
            }
        }

    };
    public void startUSB() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if(deviceVID==0x67B){
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                }
                if (!keep)
                    break;
            }
        }


    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        startUSB();
        EditText partcode=findViewById(R.id.partcode);
        EditText station=findViewById(R.id.station);
        status=findViewById(R.id.imageView);
        status.setVisibility(View.INVISIBLE);
        TextView output=findViewById(R.id.output);
        partcode.requestFocus();
        station.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //do here your stuff
                    String partCodeText=partcode.getText().toString();
                    String finalText;
                    try{
                        finalText=partCodeText+station.getText().toString()+partCodeText.substring(partCodeText.length()-1)+"\n\r";
                        output.setText(finalText);
                        if(serialPort.open()){serialPort.write(finalText.getBytes());}
                        partcode.setText(null);
                        station.setText(null);
                        Toast.makeText(getApplicationContext(),finalText,Toast.LENGTH_SHORT).show();
                        partcode.requestFocus();
                    }
                    catch(Exception ex){
                        Toast.makeText(getApplicationContext(),"Error!",Toast.LENGTH_SHORT).show();
                        partcode.setText(null);
                        station.setText(null);
                        partcode.requestFocus();
                    }

                    return true;
                }
                return false;
            }
        });
    }



}