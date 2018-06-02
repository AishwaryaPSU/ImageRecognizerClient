package com.aishwaryagm.objectrecogniser;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {
    //REQUEST_ID refers to the unique id for the intent request which can be used in the onActivityResult method to differentiate between the results of the requests
    static final int REQUEST_ID = 1;
    private ObjectRecogniserAIDL remoteService;
    Bitmap bitmapImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent("com.aishwaryagm.objectrecogniser.services.ObjectRecogniserService");
        serviceIntent.setPackage("com.aishwaryagm.objectrecogniser");
        Log.i("INFO",String.format("bindService is about to be called...."));
        boolean isSuccessful = bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i("INFO",String.format("bindService result %s", isSuccessful));

    }
    public void takePhoto(View view){
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePhotoIntent.resolveActivity(getPackageManager())!=null) {
            startActivityForResult(takePhotoIntent, REQUEST_ID);
        } else {
            Toast.makeText(this,String.format("Camera application not installed"),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ID && resultCode==RESULT_OK){
            Bundle extras = data.getExtras();
            bitmapImage = (Bitmap) extras.get("data");
            ImageView imageToDisplay = findViewById(R.id.takePhoto);
            imageToDisplay.setImageBitmap(bitmapImage);
        }
    }
    public void inspectObjects(View view){
        Log.i("Info","Entered Inspect Objects function");
        Log.i("INFO",String.format("remoteService %s", remoteService));
        Log.i("INFO",String.format("bitmapImage %s", bitmapImage));
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.PNG,100,byteStream);
        byte[] byteArray = byteStream.toByteArray();
        Log.i("INFO",String.format("Byte Array %s",byteArray));
        try {
            remoteService.analyzeImage(byteArray);
        } catch (RemoteException e) {
            Log.e("ERROR",String.format("Exception occurred while calling image analyzer %s",e.getMessage()));
            e.printStackTrace();
        }
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("Info",String.format("ComponentName %s service %s", name, service));
            remoteService = ObjectRecogniserAIDL.Stub.asInterface(service);
            Log.i("Info",String.format("remoteService.getClass().getName() %s", remoteService.getClass().getName()));
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e("Error", String.format(" service binding died %s", name));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
            Log.e("Error", "Service has unexpectedly disconnected");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
    private void selectImage(){
        final CharSequence[] options = {"Take Photo","Choose from Gallery","Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int option) {
                boolean optionSelected = Utility.checkPermission(MainActivity.this);
            }
        })
    }
}


