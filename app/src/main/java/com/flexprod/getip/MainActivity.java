package com.flexprod.getip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    boolean isGPSEnabled;
    TextView responseView;
    TextView detailsView;
    static final String API_URL = "https://checkip.amazonaws.com";
    static String filename = "Guy_IP";
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    File tmpFile;
    private MyReceiver mNetworkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("GetIP","Starting the app");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        responseView = findViewById(R.id.responseView2);
        detailsView = findViewById(R.id.editText);
        mNetworkReceiver = new MyReceiver();

        Log.i("GetIP", "Requesting permissions: " );

        // Check if there is a GetIP file for storing results
        tmpFile = new File(filename);
        if (!(tmpFile.exists())) {
            Log.i("GetIP","Creating " + filename + " for first use");
        }
        StringBuilder s2 = new StringBuilder();
        s2.append(getExistingFile(filename));
        s2.append(System.getProperty("line.separator"));

        responseView.setMovementMethod(new ScrollingMovementMethod().getInstance());
        responseView.setMaxLines(20);
        responseView.setText(s2) ;
    // Scroll to end of TextView

        // Tell application we want to listen for network changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        // My Moto G5 Plus cannot go above Android 8.1 (API 27). This action is deprecated at API 28
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        this.registerReceiver(mNetworkReceiver, filter);

        detailsView.setText("Log size: " +Formatter.formatShortFileSize(getApplicationContext(),getExistingFile(filename).toString().length()));

        Button queryButton = findViewById(R.id.button);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RetrieveFeedTask().execute();
            }
        });
        Log.d("GetIP","OnCreate finishes");
    }

    protected  String getExistingFile(String filename) {
        String line;
        String line1="";

        InputStream inputStream;
        try {
            inputStream = openFileInput(filename);
            if (inputStream != null) {
                InputStreamReader inputreader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                try {
                    while ((line = buffreader.readLine()) != null)
                        if (!(line.equals("")) ) {
                        line1 = line1 + System.getProperty("line.separator") + line;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e) {
            String error="";
        }
        return line1;
    }

    protected void writeFile(String filename, String fileContents) {
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }


    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
           WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

           // Check if Location Services has been enabled since GetIP startup
            LocationManager locationManager = (LocationManager)  getApplicationContext().getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                isGPSEnabled = true;
            } else {
                isGPSEnabled = false;
            }

            StringBuilder s1 = new StringBuilder();
            s1.append(responseView.getText());
            s1.append(dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane"))));
            if (!(isGPSEnabled)) {
                s1.append(System.getProperty("line.separator"));
                s1.append("Location Services disabled - SSID will show as unknown");
            }
            s1.append(System.getProperty("line.separator"));
            s1.append(wm.getConnectionInfo().getSSID());
            s1.append(System.getProperty("line.separator"));
            responseView.setText(s1);

            StringBuilder s3 = new StringBuilder();
            if (!(isGPSEnabled)) {
                s3.append(System.getProperty("line.separator"));
                s3.append("Location Services disabled - SSID will show as unknown");
            }

            writeFile(filename, getExistingFile(filename)
                    + System.getProperty("line.separator")
                    + dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane")))
                    + s3
                    + System.getProperty("line.separator")
                    + wm.getConnectionInfo().getSSID());
            //Disable the button to avoid multiple calls overrunning each other
            Button queryButton = findViewById(R.id.button);
            queryButton.setEnabled(false);
        }

        protected String doInBackground(Void... urls) {
            // Do some validation here
            Log.d("GetIP", "About to call to amazon");
            try {
                URL url = new URL(API_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("GetIP", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if (response == null) {
                response = "THERE WAS AN ERROR";
            }
           // progressBar.setVisibility(View.GONE);
            Log.i("GetIP", response);
            StringBuilder s3 = new StringBuilder();
            responseView.setTextColor(Color.BLACK);
            responseView.setBackgroundColor(Color.WHITE);
            s3.append(responseView.getText());
            s3.append(response);
            s3.append(System.getProperty("line.separator"));
            responseView.setText(s3);
            writeFile(filename, getExistingFile(filename)
                    + System.getProperty("line.separator")
                    + response
                    + System.getProperty("line.separator"));
            //Re-enable the Get IP button
            Button queryButton = findViewById(R.id.button);
            queryButton.setEnabled(true);

        }
    }

     class MyReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                // Check if location servcies are available
                LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    isGPSEnabled = true;
                } else {
                   responseView.setText(System.getProperty("line.separator"));
                   responseView.setText("Location Services disabled.");
                   isGPSEnabled = false;
                }
                String id = intent.getStringExtra("notificationID");
                HashMap<String, String> myMap = new HashMap<String, String>();
                myMap.put("1","test");
                if (myMap.get(id) != null)
                    return;
                StringBuilder s4 = new StringBuilder();
                s4.append(responseView.getText());
                s4.append(System.getProperty("line.separator"));
                responseView.setText(s4);
                responseView.setBackgroundColor(Color.GREEN);
                responseView.setTextColor(Color.WHITE);
                s4.setLength(0);
                WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wm.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                s4.append(wm.getConnectionInfo().getSSID());
                s4.append(responseView.getText());
                responseView.setTextColor(Color.RED);
                s4.append("Network change detected");
                s4.append(System.getProperty("line.separator"));
                responseView.setText(s4);
                writeFile(filename, getExistingFile(filename)
                        + System.getProperty("line.separator")
                        + dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane")))
                        + System.getProperty("line.separator")
                        + "Network change detected"
                        + System.getProperty("line.separator"));
                responseView.setTextColor(Color.BLACK);
                    new RetrieveFeedTask().execute();
                Log.e("GetIP", "Network change");
                //Avoid two firings of this intent
                myMap.put(id, "Fired");

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
}

