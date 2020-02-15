package com.example.openstreetmaptest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import java.util.HashMap;
import java.util.Map;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.SEND_SMS;

/**
 * @brief This class describes the splash screen at the app start. It is a welcome screen for the user.
 */
public class Splash extends Activity {
    private static final int PERMISSION_ALL = 0;
    private Handler h;  //handler for the screen
    private Runnable r; //runnable app (main Activity)

    /**
     * @brief This function creates the welcome screen of the app
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        h = new Handler();
        r = new Runnable() {
            /**
             * @brief This function starts the activity (MainActivity)
             */
            @Override
            public void run() {
                Toast.makeText(Splash.this, "Runnable started", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Splash.this, MainActivity.class)); //start MainActivity
                finish(); //Exit splash screen
            }
        };

        String[] PERMISSIONS = {        //all permissions
                SEND_SMS,
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
        };

        if(!UtilPermissions.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);   //request permissions again and again if not given yet
        }
        else
            h.postDelayed(r, 1500);     //delay thread
    }

    /**
     * @brief This functions evalutaes the permission result
     * @param requestCode code for every permission
     * @param permissions   all permissions
     * @param grantResults  result of permission asked
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        int index = 0;
        Map<String, Integer> PermissionsMap = new HashMap<String, Integer>();   //create hashmap for permissions
        for (String permission : permissions){
            PermissionsMap.put(permission, grantResults[index]);    //fill map
            index++;        //count permissions asked and fill map
        }

        if((PermissionsMap.get(ACCESS_FINE_LOCATION) != 0)
                || PermissionsMap.get(SEND_SMS) != 0){  //when permissions not given
            Toast.makeText(this, "Location and SMS permissions are a must", Toast.LENGTH_SHORT).show();
            finish();   //kill thread
        }
        else
        {
            h.postDelayed(r, 1500); //delay thread
        }
    }
}