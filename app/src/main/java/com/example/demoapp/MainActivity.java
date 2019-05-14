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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private static final String TAG = "DemoAppMain";
    private Button mStart, mStop;
    private TextView mgetIMEIID, responseTextView;
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2 * 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private TextView mgetLocation;
    String edit_text_data;
    EditText your_edit_text;
    int rssi;
    String name;
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
    BufferedOutputStream out;
    OutputStream os;
    StringBuilder sb;
    private Handler uiUpdater = null;
    boolean handled;
    // Child thread sent message type value to activity main thread Handler.
    private static final int REQUEST_CODE_SHOW_RESPONSE_TEXT = 1;
    // The key of message stored server returned data.
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    ScrollView mScrollView;
    TextView txtUser;
    long timeBeforeThreadStart;
    boolean isInputEventOccurred;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mStart = (Button) findViewById(R.id.Start);
        mStop = (Button) findViewById(R.id.Stop);
        mgetIMEIID = (TextView) findViewById(R.id.text_view_IMEI);
        your_edit_text = (EditText) findViewById(R.id.your_id);
        timeBeforeThreadStart = System.currentTimeMillis();

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_text_data = your_edit_text.getText().toString();
                your_edit_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        handled = false;
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            // TODO do something
                            handled = true;
                            if(handled)
                            {
                                new BussinessOwnerHttpAsyncTask().execute();
                                buildGoogleApiClient();
                                BTAdapter.startDiscovery();
                                String[] mimei1;
                                mimei1 = getIMEIID();
                                mgetIMEIID.setText("IMEI Number:: " + mimei1[0] + "\n IMSI of Sim:: " + mimei1[1]);
                            }
                          /*  Thread t = new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    // TODO Auto-generated method stub
                                    if (handled)
                                    {
                                        // write code to close app

                                    }
                                   // SystemClock.sleep(30000);
                                }
                            });
                            t.start();*/ 
                        }

                        return handled;
                    }
                });

            }
        });
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterReceiver(receiver);
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }
        });


    }


    class BussinessOwnerHttpAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String[] mimei1;
            mimei1 = getIMEIID();
            HttpURLConnection urlConnection = null;
            String response = null;
            edit_text_data = your_edit_text.getText().toString();

            try {
                URL url = new URL(edit_text_data);
                urlConnection = (HttpURLConnection) url.openConnection();
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(mimei1[0]);
                writer.flush();
                writer.close();
                out.close();

                urlConnection.connect();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line=mimei1[0];
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                response = result.toString();
                Log.d("test", "result from server: " + result.toString());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            mScrollView = (ScrollView) findViewById(R.id.SCROLLER_ID);
            txtUser = (TextView) findViewById(R.id.http_url_response_text_view);
            txtUser.setText(response);
        }

    }

    private void scrollToBottom() {
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(0, txtUser.getBottom());
            }
        });
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                //String macAddress = Settings.Secure.getString(getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
                //String id=BTAdapter.getAddress();
                //String id=intent.getStringExtra(BluetoothDevice.ACTION_UUID);
                TextView rssi_msg = (TextView) findViewById(R.id.textView1);
                rssi_msg.setText(rssi_msg.getText() + name + "::" + rssi + "dBm\n");
            }
        }
    };

    public synchronized void buildGoogleApiClient() {
        Log.d(TAG, "api client");
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
        Log.d(TAG, "onConnected");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation == null) {
            startLocationUpdates();
        }
        if (mLocation != null) {
            double latitude = mLocation.getLatitude();
            double longitude = mLocation.getLongitude();
        } else {
            // Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        Log.d(TAG, "start Location");
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

    }

    @Override
    public void onLocationChanged(Location location) {

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

        super.onDestroy();
    }
}


