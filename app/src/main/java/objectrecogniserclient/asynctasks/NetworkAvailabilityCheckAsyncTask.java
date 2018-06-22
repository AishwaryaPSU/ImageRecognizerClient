package objectrecogniserclient.asynctasks;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import objectrecogniserclient.MainActivity;

/**
 * Created by aishwaryagm on 6/15/18.
 */

public class NetworkAvailabilityCheckAsyncTask extends AsyncTask<Void, Void, Void> {
    MainActivity activity;

    public NetworkAvailabilityCheckAsyncTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            Log.i("INFO", String.format("Network info presesnt : %s", networkInfo));
            InetAddress ipAddr = InetAddress.getByName("google.com");
            Log.i("INFO", String.format("IP address : %s", ipAddr.getAddress()));
            if (networkInfo != null && !ipAddr.equals("")) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.inspectObject();
                    }
                });

            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, String.format("Please connect to internet..."), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("ERROR", String.format("Exception occurred while checking for internet connection: %s", e.getMessage()));
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, String.format("Could not connect to internet!"), Toast.LENGTH_SHORT).show();
                }
            });
        }
        return null;
    }
}

//    @Override
//    protected void onPostExecute(NetworkInfo networkInfo) {
//        try {
//            InetAddress ipAddr = InetAddress.getByName("google.com");
//            Log.i("INFO",String.format("IP address : %s",ipAddr.getAddress()));
//            if(networkInfo!=null && !ipAddr.equals("")){
//                activity.inspectObject();
//            } else {
//                Toast.makeText(activity, String.format("Please connect to internet..."), Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception exception) {
//            Log.i("Error",String.format("Exception occurred %s ",exception.getMessage()));
//            exception.printStackTrace();
//        }
//    }

