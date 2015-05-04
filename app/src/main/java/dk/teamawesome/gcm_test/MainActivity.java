package dk.teamawesome.gcm_test;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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


public class MainActivity extends ActionBarActivity {
    public static final boolean DEBUG = true;

    //Tag for log
    private static final String GCM_TAG = "GCM-Test";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private GCMService gcmService;
    private boolean gcmBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (DEBUG) Log.i(GCM_TAG, "OnCreate");

        /**
         * Register intent filters for the checks for Google Play Services that are performed in
         * the GCMService.checkPlayServices() method.
         */
        IntentFilter userRecoverableError = new IntentFilter(GCMService.USER_RECOVERABLE_ERROR);
        IntentFilter deviceNotSupported = new IntentFilter(GCMService.DEVICE_NOT_SUPPORTED);

        /**
         * Broadcast receivers for the intents registered above. The first receiver displays a
         * prompt to download Play Services, or enable in the settings. The second receiver is for
         * the event that it is not possible to get Play Services running; thus the app will not be
         * usable on the device as it relies on Play Services for GCM.
         */
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

    /**
     * Debugging method for printing the ID that the device gets when registering on GCM.
     */
    public void printID(View view) {
        String result = gcmService.getId();
        TextView textView = (TextView) findViewById(R.id.mDisplay);
        textView.setText("ID is: " + result + "\n");
        Log.i(GCM_TAG, "regId: " + result);
    }

    /**
     * Debugging method for re-registering the device on GCM. Should hopefully not be necessary
     * when the app is finalized and does not change. Probably related to the fact that Google
     * requires re-registering all devices whenever a new version of the app is released.
     */
    public void reRegister(View view) {
        gcmService.registerInBackground();
    }
}
