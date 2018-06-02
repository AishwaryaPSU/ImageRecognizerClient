package com.aishwaryagm.objectrecogniser;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.v4.content.ContextCompat;

/**
 * Created by aishwaryagm on 6/2/18.
 */

class Utility {
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE =123;

    public static boolean checkPermission(final Context context){
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>= VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_DENIED){

            }
        }
    }
}
