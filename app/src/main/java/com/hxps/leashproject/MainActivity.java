package com.hxps.leashproject;

import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hxps.leashproject.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private GpsTracker gpsTracker;
    TextView lat_txt;
    TextView lon_txt;
    TextView ipv4_txt;
    TextView upd_txt;
    int symbol = 0;

    String lon;
    String lat;

    private Handler geoHandler = new Handler();
    private Handler socketHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.hxps.leashproject.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        lat_txt = findViewById(R.id.lat_txt);
        lon_txt = findViewById(R.id.lon_txt);
        ipv4_txt = findViewById(R.id.ipv4_txt);
        upd_txt = findViewById(R.id.upd_txt);

        lat_txt.setText("LAT: ");
        lon_txt.setText("LON: ");
        ipv4_txt.setText("ipv4: " + getIpAccess());

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        geoHandler.removeCallbacks(timeUpdaterRunnable);
        geoHandler.postDelayed(timeUpdaterRunnable, 100);
        socketHandler.removeCallbacks(socketRunnable);
        socketHandler.postDelayed(socketRunnable, 100);


    }

    private Runnable socketRunnable = new Runnable() {
        public void run() {
            try {
                ServerSocket Server = new ServerSocket(5000);
                Toast toast1 = Toast.makeText(getApplicationContext(),
                        "Сокет открыт, жду подключений", Toast.LENGTH_SHORT);
                toast1.show();
                while (true) {
                    Socket connected = Server.accept();
                    Toast toast2 = Toast.makeText(getApplicationContext(),
                            "Подключён клиент", Toast.LENGTH_SHORT);
                    toast2.show();

                    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connected.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connected.getOutputStream()));

                    while (true) {
                        String fromclient = inFromClient.readLine();

                        if (fromclient.equals("q") || fromclient.equals("Q")) {
                            connected.close();
                            break;
                        } else {
                            out.write(lat+":::"+lon);
                            out.flush();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    };

    private Runnable timeUpdaterRunnable = new Runnable() {
        public void run() {
            getLocation();
            geoHandler.postDelayed(this, 200);
        }
    };

    private String nextSymbol() {
        String symbols = "|/-\\";
        if (symbol < 4) {
            symbol++;
            return symbols.substring(symbol - 1, symbol);
        } else {
            symbol = 1;
            return symbols.substring(symbol - 1, symbol);
        }
    }

    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return formatedIpAddress; //192.168.31.2
    }

    public void getLocation() {
        gpsTracker = new GpsTracker(MainActivity.this);
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            lat_txt.setText("LAT: " + String.valueOf(latitude));
            lon_txt.setText("LON: " + String.valueOf(longitude));
            upd_txt.setText("UPD: " + String.valueOf(nextSymbol()));
            lon = String.valueOf(longitude);
            lat = String.valueOf(latitude);
        } else {
            gpsTracker.showSettingsAlert();
        }
    }
}