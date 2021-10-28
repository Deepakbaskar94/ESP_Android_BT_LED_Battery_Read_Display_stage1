package com.led.led;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;


public class ledControl extends AppCompatActivity {

    Button btnOn, btnOff, btnDis;
    TextView percent, health, source, temperature, status, voltage;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    IntentFilter intentfilter;
    BroadcastReceiver batteryBroadcast;
    int check;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgets
        btnOn = (Button)findViewById(R.id.button2);
        btnOff = (Button)findViewById(R.id.button3);
        btnDis = (Button)findViewById(R.id.button4);
        percent = (TextView)findViewById(R.id.percent);
        health = (TextView)findViewById(R.id.health);
        source = (TextView)findViewById(R.id.source);
        temperature = (TextView)findViewById(R.id.temperature);
        status = (TextView)findViewById(R.id.status);
        voltage = (TextView)findViewById(R.id.voltage);

        new ConnectBT().execute(); //Call the class to connect

        batteryLevel(); // Call the method to get battery level




        //commands to be sent to bluetooth
        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });

    }



    @SuppressLint("SetTextI18n")
    private void chargingstatus(Intent intent){
        int statusTemp = intent.getIntExtra("status", -1);
        switch (statusTemp)
        {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                status.setText("Unknown");
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                status.setText("Charging");
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                status.setText("Discharge");
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                status.setText("Not Charging");
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                status.setText("Full");
                break;
            default:
                status.setText("Null");
        }
    }

    @SuppressLint("SetTextI18n")
    private void sethealth(Intent intent){
        int val = intent.getIntExtra("health", 0);
        switch (val)
        {
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                health.setText("Unknown");
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                health.setText("Good");
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                health.setText("Over Heat");
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                health.setText("Dead");
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                health.setText("Over Voltage");
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                health.setText("Unspecified Failure");
                break;
            default:
                health.setText("Cold");
        }
    }

    @SuppressLint("SetTextI18n")
    private void getchargingsource(Intent intent){
        int source1 = intent.getIntExtra("plugged", -1);
        switch (source1)
        {
            case BatteryManager.BATTERY_PLUGGED_AC:
                source.setText("AC");
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                source.setText("USB");
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                source.setText("WIRELESS");
                break;
            default:
                source.setText("NULL");
        }
    }

    private void batteryLevel() {
        intentfilter = new IntentFilter();
        intentfilter.addAction(Intent.ACTION_BATTERY_CHANGED);

        batteryBroadcast = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            public void onReceive(Context context, Intent intent) {

                if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()))
                {
                    check = intent.getIntExtra("level", -1);
                    Log.d("value check", String.valueOf(+check));


                    /*if(check <= 20) {
                        turnOnLed();
                        Log.d("Led on update",null);
                    }
                    else if (check >= 90) {
                        turnOffLed();
                        Log.d("Led off update",null);
                    }
                    else {
                        turnOffLed();
                    }*/

                    //switch (check)
                    //check.setText(intent.getIntExtra("level", 0) + "%");

                    percent.setText(intent.getIntExtra("level", 0) + "%");

                    sethealth(intent);

                    getchargingsource(intent);

                    float temp = (float) (intent.getIntExtra("temperature", 0)*0.1);
                    temperature.setText(temp+ " Celcius");

                    float voltTemp = (float) (intent.getIntExtra("voltage", 0)*0.001);
                    voltage.setText(voltTemp+ "V");

                    chargingstatus(intent);
                }

            }
        };
    }


    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("OFF".getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                //btSocket.getOutputStream().write("ON".toString().getBytes());
                btSocket.getOutputStream().write("ON".getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


//add menu option in Top
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





    @SuppressLint("StaticFieldLeak")
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(batteryBroadcast, intentfilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryBroadcast);
    }
}
