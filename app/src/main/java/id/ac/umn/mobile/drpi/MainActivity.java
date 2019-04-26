package id.ac.umn.mobile.drpi;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.FaceDetector;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.DebugUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    TextView textResponse;
    EditText editTextAddress, editMessage;
    Button buttonFlask;
    Integer portDst;
    String IP;
    static String localNetState;
    static final int PERMISSION_CODE = 66;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = findViewById(R.id.addressET);
        editMessage = findViewById(R.id.messageET);
        buttonFlask = findViewById(R.id.flaskBtn);
        textResponse = findViewById(R.id.response);

        SharedPreferences prefDest = getSharedPreferences("DOORPI", Context.MODE_PRIVATE);

        editTextAddress.setText( prefDest.getString("ip destination", "") );
//        editTextAddress.setText("192.168.43.222");
        IP = editTextAddress.getText().toString();
        portDst = 58283;

        buttonFlask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Send myClientTask = new Send(editTextAddress.getText().toString(), portDst, "WifiCheck;" + getMacAddr());
                myClientTask.execute();
                IP = editTextAddress.getText().toString();
                SharedPreferences.Editor prefs = getSharedPreferences("DOORPI", MODE_PRIVATE).edit();
                prefs.putString("ip destination", IP);
                prefs.apply();
                startActivity(new Intent(MainActivity.this, FlaskActivity.class));
            }
        });

        final Handler handler = new Handler();
        final String[] SSID = {"Cellular"};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String SSID2 = checkNetworkConnection(SSID[0]);
                if (!SSID[0].equals(SSID2)){
                    SSID[0] = SSID2;
                    Send myClientTask = new Send(IP, portDst, "WifiCheck;" + getMacAddr());
                    myClientTask.execute();
                }
//                Toast.makeText(getApplicationContext(), SSID[0], Toast.LENGTH_SHORT).show();
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(runnable);
    }

    public String checkNetworkConnection(String SSID) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isConnected = false;
        if (networkInfo != null && (isConnected = networkInfo.isConnected())) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            SSID = connectionInfo.getSSID();
            Log.d("SSID",  SSID);
        }
        return SSID;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Request Permissions
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
        }
    }

    public class Send extends AsyncTask<Void, Void, Void> {
        String dstAddress, response, messageC;
        int dstPort;
        Send(String addr, int port, String message){
            dstAddress = addr;
            dstPort = port;
            messageC = message;
        }
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                Socket socket = new Socket(dstAddress, dstPort);

                InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
                BufferedReader br = new BufferedReader(inputStream);
                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        socket.getOutputStream()
                                )
                        ), true
                );
                out.println(messageC);
                localNetState = br.readLine();
                Log.d("LocalNet STAAAAAAAAAAAAAAAAAAATE", localNetState);
                response = "Connected.";
                socket.close();
            } catch (IOException e) { e.printStackTrace(); }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            textResponse.setText(response);
            buttonFlask.setEnabled(true);
            super.onPostExecute(result);
        }
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif: all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) { return ""; }

                StringBuilder res1 = new StringBuilder();
                for (byte b: macBytes) {
                    //res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) { res1.deleteCharAt(res1.length() - 1); }
                return res1.toString();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "02:00:00:00:00:00";
    }

}
