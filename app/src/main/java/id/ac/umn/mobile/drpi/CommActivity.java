package id.ac.umn.mobile.drpi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.LoudnessEnhancer;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class CommActivity extends AppCompatActivity {
    private String SERVER;
    private Integer PORT;

    //initialize audio recording
    private static int RECORDING_RATE = 44100 ; // 44100 for music
    private static int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    //audio recorder
    private AudioRecord recorder;
    private AudioTrack audioTrack;

    //buffer size
    public static int SEND_BUFFER_SIZE = 4096;
    public static int RECV_BUFFER_SIZE = 8192;

    //are we currently sending audio data?
    private boolean currentlyAudioExchange = false;
    private boolean callInitiatedAndEnded = false;

    //checking if phone near face or not
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm);

        Intent i = getIntent();
        SERVER = i.getStringExtra("IP_DST");
        PORT = i.getIntExtra("PORT_DST", 0);

        try {
            // Yeah, this is hidden field.
            field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {}

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        final TextView callStatus = findViewById(R.id.callStatusTV);
        final Button voiceChatButton = findViewById(R.id.voiceChatBtn);
        final Button exitCommActivityButton = findViewById(R.id.exitCommActivBtn);

        voiceChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentlyAudioExchange = !currentlyAudioExchange;
                if (currentlyAudioExchange){
                    exitCommActivityButton.setEnabled(false);
                    Log.i("", "Starting the audio stream");
                    callStatus.setText("Hang up call");
                    voiceChatButton.setBackground(ContextCompat.getDrawable(CommActivity.this, R.drawable.hangup));
                    wakeLock.acquire(10*60*1000L /*10 minutes*/);
                    startStreaming();
                }
                else {
                    exitCommActivityButton.setEnabled(true);
                    Log.i("", "Stopping the audio stream");
                    callStatus.setText("Talk to Guest");
                    voiceChatButton.setBackground(ContextCompat.getDrawable(CommActivity.this, R.drawable.call));
                    wakeLock.release();
                    recorder.release();
                    callInitiatedAndEnded = true;
                    finish();
                }
            }
        });

        exitCommActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    recorder.release();
                } catch (Exception e){e.printStackTrace();}
                finish();
            }
        });
    }

    @Override
    public void finish() {
        if (callInitiatedAndEnded){
            setResult(RESULT_OK);
        }
        super.finish();
    }

    public void startStreaming() {
        Log.i("", "Starting the background thread to stream the audio data");
        Thread sendAudio = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("", "Creating the datagram socket");
                    DatagramSocket socket = new DatagramSocket();
                    byte[] buffer = new byte[SEND_BUFFER_SIZE];

                    Log.d("", "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress
                            .getByName(SERVER);
                    Log.d("", "Connected to " + SERVER + ":" + PORT);

                    Log.d("", "Creating the reuseable DatagramPacket");
                    DatagramPacket packet;

                    Log.d("", "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDING_RATE, CHANNEL_IN, FORMAT, SEND_BUFFER_SIZE);

                    Log.d("", "AudioRecord recording...");
                    recorder.startRecording();

                    while (currentlyAudioExchange) {
                        int read = recorder.read(buffer, 0, buffer.length);
                        try{
                            packet = new DatagramPacket(buffer, read, serverAddress, PORT+1);
                            socket.send(packet);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }

                    socket.close();
                    Log.d("", "AudioRecord finished recording");
                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException",e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        sendAudio.start();

        Thread recvAudio = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("", "Creating the datagram socket");
                    DatagramSocket socket = new DatagramSocket();

                    Log.d("", "Creating the buffer of size " + RECV_BUFFER_SIZE);
                    byte[] buffer = new byte[RECV_BUFFER_SIZE];

                    Log.d("", "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress
                            .getByName(SERVER);
                    Log.d("", "Connected to " + SERVER + ":" + PORT);
                    Log.d("", "Setting the audio date to " + RECORDING_RATE + "Hz");

                    int a=0;

                    //Send client IP to server
                    byte[] test = ("wololo").getBytes();
                    DatagramPacket packet = new DatagramPacket(test, test.length,
                            serverAddress, PORT-1);
                    socket.send(packet);

                    // Test Play Audio
                    audioTrack = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(FORMAT)
                                    .setSampleRate(RECORDING_RATE)
                                    .setChannelMask(CHANNEL_OUT)
                                    .build())
                            .setBufferSizeInBytes(RECV_BUFFER_SIZE)
                            .build();
                    audioTrack.play();

//                    // Save the current AudioManager settings
//                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//                    int oldAudioMode = audioManager.getMode();
//                    int oldRingerMode = audioManager.getRingerMode();
//                    boolean isSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
//
//                    // Apply your Audio settings (setSpeakerphoneOn)
//                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
//                    audioManager.setMode(AudioManager.MODE_NORMAL);
//                    audioManager.setSpeakerphoneOn(true);

                    int sessionID = audioTrack.getAudioSessionId();
                    AudioEffect effect = new LoudnessEnhancer(sessionID);
                    ((LoudnessEnhancer) effect).setTargetGain(1000);
                    effect.setEnabled(true);

                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    while (currentlyAudioExchange) {
                        socket.receive(receivePacket);
                        buffer = receivePacket.getData();
                        audioTrack.write(buffer, 0, buffer.length);
                        audioTrack.flush();
                    }

//                    // on finish, restore the settings
//                    audioManager.setSpeakerphoneOn(isSpeakerPhoneOn);
//                    audioManager.setMode(oldAudioMode);
//                    audioManager.setRingerMode(oldRingerMode);

                    socket.close();
                    Log.d("", "AudioRecord finished recording");
                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException",e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        recvAudio.start();
    }
}
