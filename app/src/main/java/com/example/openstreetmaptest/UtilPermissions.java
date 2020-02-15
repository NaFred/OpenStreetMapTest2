package com.example.openstreetmaptest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;

/**
 * @brief This class handles the user permissions on startup of the map
 */
public class UtilPermissions {
    /**
     * @brief This function checks if the permissions are given
     * @param context The app
     * @param allPermissionNeeded  Stringarray of all permissions
     * @return true if permissions are given, else false
     */
    public static boolean hasPermissions(Context context, String... allPermissionNeeded)
    {
        //API >23
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context != null && allPermissionNeeded != null)
            for (String permission : allPermissionNeeded)
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)   //check all permissions
                    return false;
        return true;
    }
}
