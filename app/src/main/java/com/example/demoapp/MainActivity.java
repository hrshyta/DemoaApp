package com.example.demoapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private static final String TAG = "DemoAppMain";
    private Button mStart, mStop;
    private TextView mgetIMEIID;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =2* 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private TextView mgetLocation;
    String edit_text_data;
    EditText your_edit_text;

    HttpClient httpclient;
    HttpGet httpget;
    HttpResponse httpresponse;
    HttpResponse hhttpresponse;
    JSONObject myJsonObject = null;
    JSONArray myJsonArray = null;
    String myJsonString = "";

    JSONObject nmyJsonObject = null;
    JSONArray nmyJsonArray = null;
    String nmyJsonString = "";

    InputStream is;
    InputStreamReader isr;
    BufferedReader br;
    StringBuilder sb;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mStart = (Button) findViewById(R.id.Start);
        mStop = (Button) findViewById(R.id.Stop);
        mgetIMEIID = (TextView) findViewById(R.id.text_view_IMEI);
        your_edit_text = (EditText) findViewById(R.id.your_id);

        /*mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();*/
        buildGoogleApiClient();
        mStart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                edit_text_data = your_edit_text.getText().toString();
                new BussinessOwnerHttpAsyncTask().execute();
                String[] mimei1;
                mimei1 = getIMEIID();
                mgetIMEIID.setText("IMEI Number:: " + mimei1[0] + "\n IMSI of Sim:: " + mimei1[1]);
                BTAdapter.startDiscovery();


            }
        });

        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


    }
    class BussinessOwnerHttpAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            String myUrl = "www.google.com/q=" + edit_text_data;
            String encodedURL = "";
            try {
                encodedURL = URLEncoder.encode(myUrl, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            try {
                URL url = new URL(encodedURL);
                Log.d("asca", "" + url);
            } catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            HttpGet httpget = new HttpGet(encodedURL);

            try
            {
                httpresponse = httpclient.execute(httpget);
                System.out.println("httpresponse" + httpresponse);
                Log.i("response", "Response" + httpresponse);
                InputStream is = httpresponse.getEntity().getContent();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();

                String recievingDataFromServer = null;
                while ((recievingDataFromServer = br.readLine()) != null)
                {
                    Log.i("CHECK WHILE", "CHECK WHILE");
                    sb.append(recievingDataFromServer);
                }

                myJsonString = sb.toString();
            }
            catch (ClientProtocolException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return sb.toString();

        }
    }
    private String[] getIMEIID() {
        String[] mimei = new String[5];
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling

        }
        mimei[0] = telephonyManager.getImei();
        mimei[1] = telephonyManager.getSubscriberId();

        return mimei;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                //String macAddress = Settings.Secure.getString(getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
                //String id=BTAdapter.getAddress();
                //String id=intent.getStringExtra(BluetoothDevice.ACTION_UUID);
                TextView rssi_msg = (TextView) findViewById(R.id.textView1);
                rssi_msg.setText(rssi_msg.getText() + name + "::" + rssi + "dBm\n");
            }
        }
    };
    public synchronized void buildGoogleApiClient()
    {
        Log.d(TAG,"api client");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startLocationUpdates();
        Log.d(TAG,"onConnected");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLocation == null)
        {
            startLocationUpdates();
        }
        if (mLocation != null)
        {
            double latitude = mLocation.getLatitude();
            double longitude = mLocation.getLongitude();
        }
        else {
            // Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }
    protected void startLocationUpdates()
    {
        // Create the location request
        Log.d(TAG,"start Location");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }
    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onLocationChanged(Location location)
    {
        //location.getLongitude();
        //location.getLatitude();
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
       // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        mgetLocation = (TextView) findViewById(R.id.text_view_Location);
        mgetLocation.setText("Latitude:"+String.valueOf(location.getLatitude())+":: Longitude"+String.valueOf(location.getLongitude() ));

    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}


