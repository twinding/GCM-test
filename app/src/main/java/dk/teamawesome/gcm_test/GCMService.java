package dk.teamawesome.gcm_test;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

public class GCMService extends IntentService {
    private static final boolean DEBUG = true;

    public static final String USER_RECOVERABLE_ERROR = "dk.teamawesome.gcm_test.USER_RECOVERABLE_ERROR";
    public static final String DEVICE_NOT_SUPPORTED = "dk.teamawesome.gcm_test.DEVICE_NOT_SUPPORTED";

    //Actions
    public static final String INIT = "dk.teamawesome.gcm_test.INIT";
    public static final String REREGISTER = "dk.teamawesome.gcm_test.REREGISTER";
    public static final String CHECK_PLAY_SERVICES = "dk.teamawesome.gcm_test.CHECK_PLAY_SERVICES";

    // Our project ID from Google Development Console
    private static final String SENDER_ID = "566429425839";
    //Tag for log
    private static final String GCM_TAG = "GCM-Test";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private GoogleCloudMessaging gcm;
    private Context context;

    private String regId;

    public GCMService() {
        super("GCMService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case INIT:
                init();
                break;
            case REREGISTER:
                registerInBackground();
                break;
            case CHECK_PLAY_SERVICES:
                checkPlayServices();
                break;
        }
    }

    public void init() {
        context = getApplicationContext();
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId(context);

            if (regId.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(GCM_TAG, "No Google Play Services found");
        }
    }

    /**
     * Check if Play Services is available. If it is not, a dialog will be displayed prompting the user
     * to download Play Services, or enable it in settings.
     */
    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Intent intent = new Intent(USER_RECOVERABLE_ERROR);
                intent.putExtra("resultCode", resultCode);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } else {
                Log.i(GCM_TAG, "This device is not supported");
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DEVICE_NOT_SUPPORTED));
            }
            return false;
        }
        return true;
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (DEBUG) Log.i(GCM_TAG, "getRegistrationId");
        if (registrationId.isEmpty()) {
            Log.i(GCM_TAG, "Registration not found.");
            return "";
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);

        /**
         * Check if app was updated. If so, it must clear the registration ID
         * since the existing registration ID is not guaranteed to work with
         * the new app version.
         */
        if (currentVersion != registeredVersion) {
            Log.i(GCM_TAG, "App version changed");
            return "";
        }
        if (DEBUG) Log.i(GCM_TAG, "Had registration ID " + registrationId);
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the registration ID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
     * Return applications' version number
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could net get package name.");
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    protected void registerInBackground() {
        if (DEBUG) Log.i(GCM_TAG, "registerInBackground");
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    Log.i(GCM_TAG, "Registering");
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered on ID = " + regId;

                    //Send a message to third-party server with our registered ID.
                    sendRegistrationIdToBackend();

                    // Persist the registration ID - no need to register again.
                    storeRegistrationId(context, regId);
                } catch (IOException e) {
                    msg = "Error: " + e.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            }

        }.execute(null,null,null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
        //TODO Send our registration ID to the third party server
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(GCM_TAG, "Storing regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }
}
