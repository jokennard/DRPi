package id.ac.umn.mobile.drpi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FlaskActivity extends AppCompatActivity {

    final boolean[] execute = {true};
    Button testBtn, stopTestBtn, unlockDoorBtn, addNewFaceBtn, audioChatBtn;
    TextView response, connected;

    String ipDest, IPAddr, ApiUrl, buttonPressed;
    int portDst;

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flask);

        SharedPreferences prefDest = getSharedPreferences("DOORPI", Context.MODE_PRIVATE);
        ipDest = prefDest.getString("ip destination", "");

        portDst = 58283;
        IPAddr = "http://" + ipDest + ":5000/";
        ApiUrl = IPAddr + "api/inquire";

        iv = findViewById(R.id.faceIV);

        stopTestBtn = findViewById(R.id.flaskBtnStop);
        testBtn = findViewById(R.id.flaskBtnFlask);
        unlockDoorBtn = findViewById(R.id.unlockDoorBtn);
        audioChatBtn = findViewById(R.id.audioBtnFlask);
        addNewFaceBtn = findViewById(R.id.addNewFaceBtn);
        connected = findViewById(R.id.tvIsConnected);
        response = findViewById(R.id.responseFlaskTV);

        checkNetworkConnection();

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewFaceBtn.setVisibility(View.GONE);
                testBtn.setVisibility(View.GONE);
                stopTestBtn.setVisibility(View.VISIBLE);
                buttonPressed = "bellPressed";
                execute[0] = true;

                final Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        send();
                        if (execute[0])
                            handler.postDelayed(this, 2000);
                    }
                };

                handler.post(runnable);
            }
        });

        stopTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                execute[0] = false;
                testBtn.setVisibility(View.VISIBLE);
                stopTestBtn.setVisibility(View.GONE);
//                Toast.makeText(FlaskActivity.this, "Test Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        audioChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPressed = "UDPTalk";
                send();

                Intent i = new Intent(FlaskActivity.this, CommActivity.class);
                i.putExtra("IP_DST", ipDest);
                i.putExtra("PORT_DST", portDst);
                startActivityForResult(i, 1999);
            }
        });

        unlockDoorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPressed = "openDoor";
                send();
            }
        });

        addNewFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddFaceActivity cdd = new AddFaceActivity(FlaskActivity.this);
                cdd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
                cdd.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 1999) {
            buttonPressed = "UDPTalk";
            send();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean checkNetworkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isConnected = false;
        if (networkInfo != null && (isConnected = networkInfo.isConnected())) {
            // show "Connected" & type of network "WIFI or MOBILE"
            connected.setText(String.format("Connected %s", networkInfo.getTypeName()));
            // change background color to red
            connected.setBackgroundColor(0xFF7CCC26);

        } else {
            // show "Not Connected"
            connected.setText("Not Connected");
            // change background color to green
            connected.setBackgroundColor(0xFFFF0000);
        }

        return isConnected;
    }

    // Edit ini aja
    private String HttpPost(String myUrl) throws IOException, JSONException {
        String result = "";

        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();

        // 5. Get JSON response from server
        StringBuilder JSONresult = new StringBuilder();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while((line = reader.readLine()) != null){
            JSONresult.append(line);
        }

        // 6. Convert JSON to Map
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Gson gson = new Gson();
        Map<String, Object> myMap = gson.fromJson(JSONresult.toString(), type);

        // 6. return response message
        Date currentTime = Calendar.getInstance().getTime();

        if(buttonPressed.equals("bellPressed")){
            String gambarURL = IPAddr + "images/" + myMap.get("faceLink").toString();
            String identitas;
            try{
                identitas = myMap.get("identity").toString();
            }
            catch (Exception e){
                identitas = "bleh";
            }

            return myMap.get("state").toString() + "\n" + currentTime + "::" + gambarURL + "::" + identitas;
        }
        else if (buttonPressed.equals("openDoor")){
            return myMap.get("state").toString() + "\n" + currentTime + "::ech::ech2";
        }
        else if (buttonPressed.equals("UDPTalk")){
            return myMap.get("state").toString() + "\n" + currentTime + "::ech::ech2";
        }

        //return conn.getResponseMessage()+" sudah diterima";
        return myMap.get("state").toString() + "\n" + currentTime + "::ech::ech2";
    }

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                try {
                    return HttpPost(urls[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "Error!";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            String [] separated = result.split("::");

            // bikin notification disini
            // kalau ada face detected, tambahin tombol sama edittext buat Nama sama KategoriOrang
            // tambahin tombal AddData, buat tambahin data gambar (name, user_data), sama addface nanti, terus train

            response.setText(separated[0]);
//            Log.d("link", separated[1]);
//            Log.d("name", separated[2]);
            String [] separated2 = separated[0].split("\n");
            Log.d("", separated2[0] + "|");
            if (separated2[0].equals("bell is pressed")){
//                Log.d("state", separated[1]);
//                Log.d("link", separated2[0]);
//                Log.d("name", separated2[1]);
                execute[0] = false;
                testBtn.setVisibility(View.VISIBLE);
                stopTestBtn.setVisibility(View.GONE);

                // Kadang gambar cuma loading setengah
                Picasso.get()
                        .load(separated[1])
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .networkPolicy(NetworkPolicy.NO_CACHE)
                        .into(iv);

                if (MainActivity.localNetState == null){
                    Log.d("Isi", "Kosong");
                }
                else Log.d("LocalNetState", MainActivity.localNetState);
                SharedPreferences.Editor prefs = getSharedPreferences("DOORPI", MODE_PRIVATE).edit();
                String message;

                Log.d("AAAAAAAAAAAAA", separated[2]);

                if (separated[2].equals("noFace")){
                    message = "No face is detected though...";
                }
                else if (separated[2].equals("unknown")){
                    message = "I do not know this person, but you may!";
                    addNewFaceBtn.setVisibility(View.VISIBLE);
                    addNewFaceBtn.setText("Add New Face Data");
                    prefs.putString("identity", "unknown");
                }
                else{
                    message = separated[2] + " is at the front door.";
                    addNewFaceBtn.setVisibility(View.VISIBLE);
                    addNewFaceBtn.setText("Update Face Data");
                    prefs.putString("identity", separated[2]);
                }
                prefs.apply();
                createNotification("DRPi : Bell is pressed!", message);
            }
        }
    }

    public void send() {
        // perform HTTP POST request
        if(checkNetworkConnection())
            new HTTPAsyncTask().execute(ApiUrl);
        else
            Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show();
    }

    // Edit ini aja
    private JSONObject buidJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("name", "test title");
        jsonObject.accumulate("text",  "open door");
        jsonObject.accumulate("door state",  buttonPressed);
//        jsonObject.accumulate("text",  "jack and jill went up the hill to fetch a pail of water!");

        return jsonObject;
    }

    private void setPostRequestContent(HttpURLConnection conn, JSONObject jsonObject) throws IOException {
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonObject.toString());
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public void createNotification(String title, String message) {
        /*Creates an explicit intent for an Activity in your app*/
        Intent resultIntent = new Intent(FlaskActivity.this , FlaskActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(FlaskActivity.this,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.apartment_doorbell);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(FlaskActivity.this, NOTIFICATION_CHANNEL_ID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setCategory(NotificationCompat.CATEGORY_EVENT);
        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
//                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setSound(soundUri)
                .setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Changing Default mode of notification
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            // Creating an Audio Attribute
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);

            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Doorbell - Raspberry Pi",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(soundUri, audioAttributes);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;

            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
        mNotificationManager.notify(0 /* Request Code */, mBuilder.build());
    }

}
