package com.aishwaryagm.objectrecogniser;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aishwaryagm.objectrecogniser.constants.ApplicationState;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static android.Manifest.*;

public class MainActivity extends AppCompatActivity {
    //REQUEST_ID refers to the unique id for the intent request which can be used in the onActivityResult method to differentiate between the results of the requests
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private ObjectRecogniserAIDL remoteService;
    private Bitmap bitmapImage;
    private File photoFile ;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String myCurrentPhotoPath;
    private static final int REQUEST_PERMISSION_CODE=3;
    ImageTransmitterAsyncTask imageTransmitterAsyncTask;
    private ApplicationState applicationState;
    private ImageView imageToDisplay;

    public ApplicationState getApplicationState() {
        return applicationState;
    }

    public void setApplicationState(ApplicationState applicationState) {
        this.applicationState = applicationState;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent("com.aishwaryagm.objectrecogniser.services.ObjectRecogniserService");
        serviceIntent.setPackage("com.aishwaryagm.objectrecogniser");
        Log.i("INFO",String.format("bindService is about to be called...."));
        boolean isSuccessful = bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i("INFO",String.format("bindService result %s", isSuccessful));
        applicationState = ApplicationState.APPLICATION_STARTED;
    }
    public void takePhoto(){
        checkWritePermision();
    }

    @SuppressWarnings("")
    private void checkWritePermision() {
        //create an image file name
        int checkPermission = ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE);
        int permissionGranted = PackageManager.PERMISSION_GRANTED;
        Log.i("Info",String.format("PermissionGranted %s CheckPermission %s", permissionGranted, checkPermission));

        if (checkPermission != permissionGranted) {
            String[] permissions ={permission.WRITE_EXTERNAL_STORAGE,permission.READ_EXTERNAL_STORAGE};
            this.requestPermissions(permissions,REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if((requestCode==REQUEST_CAMERA || requestCode==REQUEST_GALLERY) && resultCode==RESULT_OK){
            Log.i("INFO",String.format("myCurrentPhotoPath : %s photoFile : %s",myCurrentPhotoPath,photoFile));
            String filePath = photoFile.getPath();
            bitmapImage = adjustOrientation(filePath);
            imageToDisplay = findViewById(R.id.takePhoto);
            imageToDisplay.setImageBitmap(bitmapImage);
            applicationState = ApplicationState.PHOTO_TAKEN;
        }
    }
    public void inspectObjects(View view){
            Button takePhotoButton = findViewById(R.id.selectTakePhoto);
            takePhotoButton.setVisibility(View.INVISIBLE);
            Button inspectObjButton = findViewById(R.id.inspectObjects);
            inspectObjButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this,"Inspecting Objects...",Toast.LENGTH_LONG).show();
            ProgressBar progressBar =findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            TextView resultScrollView = findViewById(R.id.resultTextView);
            TextView resultTextViewDescription= findViewById(R.id.description);
            ImageTransmitterAsyncTask imageTransmitterAsyncTask =new ImageTransmitterAsyncTask(bitmapImage,photoFile,remoteService,resultScrollView,this,progressBar,resultTextViewDescription);
            imageTransmitterAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            applicationState = ApplicationState.INSPECT_OBJECT_CALLED;
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
    public void selectImage(View view){
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int option) {
                boolean optionSelected = Utility.checkPermission(MainActivity.this);
                Log.i("Info", String.format("optionSelected %s", optionSelected));
                if(options[option].equals("Take Photo")){
                    if(optionSelected)
                        takePhoto();
                }else if (options[option].equals("Choose from Gallery")){
                    if(optionSelected) {
                        Log.i("Info", String.format("galleryIntent is about to be called..."));
                        galleryIntent();
                    }
                }else if (options[option].equals("Cancel")){
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }
    private void galleryIntent() {
        Log.i("Info", String.format("galleryIntent entered..."));
        Intent galIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(galIntent, "Select File"), REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                photoFile = createImage();

                if (photoFile != null) {
                    startCamera(photoFile);
                }
            } catch (IOException exception) {
                Log.e("ERROR", String.format("Exception occurred while creating the image %s", exception.getMessage()));
                exception.printStackTrace();
            }
        } else {
            Log.i("INFO", "Permission denied");
        }
    }

    private File createImage() throws IOException {
        String imageFileName = UUID.randomUUID().toString();
        Log.i("INFO",String.format("Image file name %s ",imageFileName));
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.i("Info", String.format("storageDir %s", storageDir));
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        myCurrentPhotoPath = image.getAbsolutePath();
        Log.i("INFO",String.format("myCurrentPhotoPath : %s, image : %s",myCurrentPhotoPath,image));
        return image;
    }

    private void startCamera(File image) {
        Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.fileprovider", image);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePhotoIntent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, String.format("Camera application not installed"), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap adjustOrientation(String imageFilePath) {
        Bitmap convertedBitmapImage = BitmapFactory.decodeFile(imageFilePath);
        try {
            ExifInterface exif = new ExifInterface(imageFilePath);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.i("INFO", String.format("CUrrent rotation degree in Exif %s", rotation));
            if(ExifInterface.ORIENTATION_NORMAL!=rotation){
                int rotationInDegrees = exifToDegrees(rotation);
                Log.i("INFO", String.format("rotation In degrees to apply on Image is %s", rotationInDegrees));
                Bitmap rotatedBitmapImage = RotateBitmap(convertedBitmapImage,rotationInDegrees);
                return rotatedBitmapImage;
            }
        } catch (IOException e) {
            Log.e("ERROR", String.format("Exception Occurred %s", e));
            e.printStackTrace();
            return null;
        }
        return convertedBitmapImage;
    }
    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    private static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onBackPressed() {
        switch (applicationState){
            case PHOTO_TAKEN:
                applicationState = ApplicationState.APPLICATION_STARTED;
                Log.i("INFO",String.format("Application state is %s and image to display is %s ",applicationState,imageToDisplay));
                imageToDisplay.setImageBitmap(null);
                imageToDisplay.setImageResource(R.color.colorPrimaryDark);
                break;
            case INSPECT_OBJECT_CALLED:
                Toast.makeText(this,String.format("Inspecting Objects, Please Wait..."),Toast.LENGTH_LONG).show();
                break;
            case INSPECT_OBJECT_FINISHED:
                applicationState = ApplicationState.PHOTO_TAKEN;
                Button photoTaken =  findViewById(R.id.selectTakePhoto);
                photoTaken.setVisibility(View.VISIBLE);
                Button inspectObjs =  findViewById(R.id.inspectObjects);
                inspectObjs.setVisibility(View.VISIBLE);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultTextView.setText("");
                resultTextView.setVisibility(View.INVISIBLE);
                TextView resultTextViewDescription = findViewById(R.id.description);
                resultTextViewDescription.setText("");
                resultTextViewDescription.setVisibility(View.INVISIBLE);
                break;
            default:
                super.onBackPressed();
        }
    }
}


