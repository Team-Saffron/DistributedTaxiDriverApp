package com.uber.dtu.taxidriverpassengerseekingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText ipAddress;
    private Button sendRequest;
    private GPSTracker gps;
    private Thread networkIO = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendRequest = (Button)findViewById(R.id.send);
        sendRequest.setOnClickListener(this);
        ipAddress = (EditText) findViewById(R.id.IP);
    }


    @Override
    public void onClick(View v) {

        String ip = ipAddress.getText().toString();
        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        gps = new GPSTracker(MainActivity.this);

        // Check if GPS enabled
        if(gps.canGetLocation()) {

            Toast.makeText(getApplicationContext(),"Request is being Processed", Toast.LENGTH_LONG).show();

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            String msg = "" + latitude + "," + longitude;

            networkIO = new Thread(new SendMessage(msg,ip));
            networkIO.start();

        } else {
            // Can't get location.
            // GPS or network is not enabled.
            // Ask user to enable GPS/network in settings.
            Toast.makeText(getApplicationContext(),"GPS or n/w is not enabled", Toast.LENGTH_LONG).show();
            gps.showSettingsAlert();
        }
    }


    private class SendMessage implements Runnable {
        private String mMsg;
        private String IP_ADDRESS;y

        public SendMessage(String msg,String address) {
            mMsg = msg;
            IP_ADDRESS = address;
        }

        public void run() {
            try {
                Log.i("Connecting to " + IP_ADDRESS + " on port " + 50505,"CLIENT");
                Socket client = new Socket(IP_ADDRESS, 50505);

                Log.i("Just connected to " + client.getRemoteSocketAddress(),"Client");
                OutputStream outToServer = client.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);

                out.writeUTF(mMsg);
                InputStream inFromServer = client.getInputStream();
                DataInputStream in = new DataInputStream(inFromServer);

                String dest = in.readUTF();
                Log.i("Server says " + dest,"CLIENT");
                client.close();

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?saddr="+mMsg+"&daddr="+dest));
                startActivity(intent);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
