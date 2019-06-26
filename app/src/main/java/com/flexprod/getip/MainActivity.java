package com.flexprod.getip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity {

    TextView responseView;
    TextView detailsView;
   // ProgressBar progressBar;
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
        StringBuilder s1 = new StringBuilder();
        s1.append(getExistingFile(filename));
        s1.append(System.getProperty("line.separator"));

        responseView.setText(s1) ;

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        this.registerReceiver(mNetworkReceiver, filter);

   //     progressBar = findViewById(R.id.progressBar);
        detailsView.setText(Formatter.formatShortFileSize(getApplicationContext(),getExistingFile(filename).toString().length()));

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
        String line, line1="";
        //StringBuilder line1 = new StringBuilder();
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
                    //line1.append("\n");
                      //  line1.append(line);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e)
        {
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
    public void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       //     progressBar.setVisibility(View.VISIBLE);
            StringBuilder s1 = new StringBuilder();
            s1.append(responseView.getText());
            s1.append(dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane"))));
            s1.append(System.getProperty("line.separator"));
            s1.append(wm.getConnectionInfo().getSSID());
            s1.append(System.getProperty("line.separator"));
            responseView.setText(s1);
            writeFile(filename, getExistingFile(filename)
                    + System.getProperty("line.separator")
                    + dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane")))
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
            StringBuilder s1 = new StringBuilder();
            s1.append(responseView.getText());
            s1.append(response);
            s1.append(System.getProperty("line.separator"));
            responseView.setText(s1);
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
                StringBuilder s1 = new StringBuilder();
                s1.append(responseView.getText());
                s1.append(System.getProperty("line.separator"));
                responseView.setText(s1);
                responseView.setBackgroundColor(Color.GREEN);
                responseView.setTextColor(Color.WHITE);
                s1.setLength(0);
                s1.append(responseView.getText());
                s1.append("Network change detected");
                s1.append(System.getProperty("line.separator"));
                responseView.setText(s1);
                responseView.setBackgroundColor(Color.WHITE);
                writeFile(filename, getExistingFile(filename)
                        + System.getProperty("line.separator")
                        + dtf.format(ZonedDateTime.now(ZoneId.of("Australia/Brisbane")))
                        + System.getProperty("line.separator")
                        + "Network change detected"
                        + System.getProperty("line.separator"));
                responseView.setTextColor(Color.BLACK);
                    new RetrieveFeedTask().execute();
                Log.e("GetIP", "Network change");

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }


    }

}

