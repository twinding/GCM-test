package dk.teamawesome.gcm_test;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends ActionBarActivity {
    private static final boolean DEBUG = true;

    // Our project ID from Google Development Console
    private static final String SENDER_ID = "566429425839";
    //Tag for log
    private static final String GCM_TAG = "GCM-Test";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private TextView mDisplay;
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private SharedPreferences prefs;
    private Context context;

    private String regId;

    private GCMService gcmService;
    private boolean gcmBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(GCM_TAG, "OnCreate");

        IntentFilter userRecoverableError = new IntentFilter(GCMService.USER_RECOVERABLE_ERROR);
        IntentFilter deviceNotSupported = new IntentFilter(GCMService.DEVICE_NOT_SUPPORTED);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                GooglePlayServicesUtil.getErrorDialog(intent.getExtras().getInt("resultCode"), MainActivity.this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        },userRecoverableError);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(MainActivity.this, "Device not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
        },deviceNotSupported);

        Intent intent = new Intent(this, GCMService.class);
        bindService(intent, gcmConnection, Context.BIND_AUTO_CREATE);

        mDisplay = (TextView) findViewById(R.id.mDisplay);
        context = getApplicationContext();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gcmBound) {
            unbindService(gcmConnection);
            gcmBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection gcmConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GCMService.LocalBinder binder = (GCMService.LocalBinder) service;
            gcmService = binder.getService();
            gcmBound = true;
            gcmService.init();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gcmBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (gcmBound) gcmService.checkPlayServices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendData(View view) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    Bundle data = new Bundle();
                    data.putString("my_message", "Hello World");
                    data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                    String id = Integer.toString(msgId.incrementAndGet());
                    gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                    msg = "Sent message";
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                Log.i(GCM_TAG, "Message sent");
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    public void printID(View view) {
        String result = gcmService.getId();
        TextView textView = (TextView) findViewById(R.id.mDisplay);
        textView.setText("ID is: " + result + "\n");
        Log.i(GCM_TAG, "regId: " + result);
    }

    public void reRegister(View view) {
        gcmService.registerInBackground();
    }
}
