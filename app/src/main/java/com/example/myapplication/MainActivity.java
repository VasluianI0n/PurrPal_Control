package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.codertainment.dpadview.DPadView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class MainActivity extends AppCompatActivity {


    Activity activity;

    private TextView textView;

    ImageView cameraView;
    private DPadView dPadView;
    private Button button1, button2, button3, button4;
    private String espIpAddress = "http://192.168.4.1/"; // Replace with the ESP8266 IP address

    boolean buttonState = false;

    boolean dPadPressed = false;

    private ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        dPadView = findViewById(R.id.dpad);
        imageButton = findViewById(R.id.lightButton);
        cameraView = findViewById(R.id.cameraView);

        textView = findViewById(R.id.testView);
        activity = MainActivity.this;



        dPadView.setOnDirectionPressListener(new Function2<DPadView.Direction, Integer, Unit>() {
            @Override
            public Unit invoke(DPadView.Direction direction, Integer integer) {
//                textView.setText(integer.toString());
                if(direction == DPadView.Direction.UP){
                    if(integer == 2){
                        if(dPadPressed == false) {
                            new SendDataTask().execute(0, 1);
                            dPadPressed = true;
                        }
                    }else{
                        new SendDataTask().execute(0, 0);
                        dPadPressed = false;
                    }
                }else if(direction == DPadView.Direction.LEFT){
                    if(integer == 2){
                        if(dPadPressed == false){
                            new SendDataTask().execute(1, 1);
                            dPadPressed = true;
                        }
                    }else{
                        dPadPressed = false;
                        new SendDataTask().execute(1, 0);
                    }
                }else if(direction == DPadView.Direction.RIGHT){
                    if(integer == 2){
                        if(dPadPressed == false){
                            new SendDataTask().execute(2, 1);
                            dPadPressed = true;
                        }
                    }else{
                        dPadPressed = false;
                        new SendDataTask().execute(2, 0);
                    }
                }else if(direction == DPadView.Direction.DOWN){
                    if(integer == 2){
                        if(dPadPressed == false){
                            new SendDataTask().execute(3, 1);
                            dPadPressed = true;
                        }
                    }else{
                        dPadPressed = false;
                        new SendDataTask().execute(3, 0);
                    }
                }
                return null;
            }
        });


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(buttonState == false){
                    imageButton.setBackground(getDrawable(R.drawable.rounded_button_on));
                    imageButton.setImageDrawable(getDrawable(R.drawable.flashlight_on));
                    buttonState = true;
                    new SendDataTask().execute(4, 1);
                }else{
                    imageButton.setBackground(getDrawable(R.drawable.rounded_button_off));
                    imageButton.setImageDrawable(getDrawable(R.drawable.flashlight_off));
                    buttonState = false;
                    new SendDataTask().execute(4, 0);
                }
            }
        });

        new StreamVideoTask().execute(espIpAddress);
    }

    private class StreamVideoTask extends AsyncTask<String, Bitmap, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            String urlStream = urls[0];
            InputStream inputStream = null;
            try {
                URL url = new URL(urlStream);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                inputStream = new BufferedInputStream(connection.getInputStream());

                byte[] buffer = new byte[8192]; // Adjust buffer size as needed
                int bytesRead;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    int startOfFrame = 0;
                    int endOfFrame = 0;
                    for (int i = 0; i < bytesRead - 1; i++) {
                        if (buffer[i] == (byte) 0xFF && buffer[i + 1] == (byte) 0xD8) {
                            startOfFrame = i;
                        } else if (buffer[i] == (byte) 0xFF && buffer[i + 1] == (byte) 0xD9) {
                            endOfFrame = i + 2; // Include the end marker
                            Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, startOfFrame, endOfFrame - startOfFrame);
                            publishProgress(bitmap);
                            startOfFrame = i + 2;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... bitmaps) {
            Bitmap bitmap = bitmaps[0];
            if (bitmap != null) {
                cameraView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(MainActivity.this, "Failed to decode frame", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private class SendDataTask extends AsyncTask<Integer, Void, String> {

        @Override
        protected String doInBackground(Integer... params) {
            int id = params[0];
            int state = params[1];
            try {
                URL url = new URL(espIpAddress + "button?id=" + id + "&state=" + state);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {

            if (result != null) {

//                textView.setText(result);
            } else {

//                textView.setText("Failed to Send Data");
            }
        }
    }
}
