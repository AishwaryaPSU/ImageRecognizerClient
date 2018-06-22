package objectrecogniserclient;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.aishwaryagm.objectrecogniser.ObjectRecogniserAIDL;
import com.aishwaryagm.objectrecogniser.R;

import objectrecogniserclient.asynctasks.ImageTransmitterAsyncTask;
import objectrecogniserclient.asynctasks.NetworkAvailabilityCheckAsyncTask;
import objectrecogniserclient.constants.ApplicationState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.UUID;

import static android.Manifest.*;

public class MainActivity extends AppCompatActivity {
    //REQUEST_ID refers to the unique id for the intent request which can be used in the onActivityResult method to differentiate between the results of the requests
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_SERVER_INSTALLATION = 3;
    private ObjectRecogniserAIDL remoteService;
    private Bitmap bitmapImage;
    private File photoFile;
    private String myCurrentPhotoPath;
    private static final int REQUEST_PERMISSION_CODE = 4;
    private static final int REQUEST_STORAGE_FOR_APK = 5;
    private ApplicationState applicationState;
    private ImageView imageToDisplay;
    private final CharSequence[] OPTIONS = {"Take Photo", "Choose from Gallery", "Cancel"};
    private int userSelectedOption;
    private final String SERVER_PACKAGE_NAME = "com.aishwaryagm.objectrecogniser";
    private final String SERVICE_ACTION = "com.aishwaryagm.objectrecogniser.services.ObjectRecogniserService";

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
        boolean isServiceAvailable = isAppInstalled(SERVER_PACKAGE_NAME);//isIntentAvailable(this, SERVICE_ACTION);
        Log.i("INFO", String.format("isServiceAvailable : %s", isServiceAvailable));
        if (!isServiceAvailable) {
            Toast.makeText(this, String.format("Please install server application"), Toast.LENGTH_LONG).show();
            installServerAPK();
        } else {
            bindService(SERVICE_ACTION, SERVER_PACKAGE_NAME);
        }
    }

    private void bindService (String serviceAction,String packageName){
        Log.i("INFO", String.format("bindService is about to be called...."));
        Intent serviceIntent = new Intent(serviceAction);
        serviceIntent.setPackage(packageName);
        boolean isSuccessful = bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i("INFO", String.format("bindService result %s", isSuccessful));
        applicationState = ApplicationState.APPLICATION_STARTED;
    }
    public void takePhoto() {
        Log.i("INFO", String.format("Take photo entered"));
        checkWritePermision(REQUEST_PERMISSION_CODE);
    }


    private boolean checkWritePermision(int requestPermissionCode) {
        //create an image file name
        int checkPermission = ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE);
        int permissionGranted = PackageManager.PERMISSION_GRANTED;
        Log.i("Info", String.format("PermissionGranted %s CheckPermission %s", permissionGranted, checkPermission));

        if (checkPermission != permissionGranted) {
            String[] permissions = {permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE};
            this.requestPermissions(permissions, requestPermissionCode);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("INFO", String.format("myCurrentPhotoPath : %s photoFile : %s", myCurrentPhotoPath, photoFile));
        Log.i("INFO", String.format("requestCode : %s resultCode : %s", requestCode, resultCode));
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            setImageView();
        }
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            try {
                InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
                OutputStream outputStream = new FileOutputStream(photoFile);
                copyFileStream(inputStream,outputStream);
                outputStream.close();
                setImageView();
            } catch (Exception exception) {
                Log.e("ERROR", String.format("Exceptionduring reading image from gallery %s", exception.getMessage()));
                exception.printStackTrace();
            }
        }
        if(requestCode == REQUEST_SERVER_INSTALLATION){
            Log.i("INFO",String.format("SERVER installation finished. result code : %s",resultCode));
            boolean isServiceAvailable = isAppInstalled(SERVER_PACKAGE_NAME);
            if(isServiceAvailable) {
                bindService(SERVICE_ACTION, SERVER_PACKAGE_NAME);
            } else {
                Toast.makeText(this,String.format("SERVICE INSTALLATION was unsuccessful"),Toast.LENGTH_LONG).show();
            }
        }
    }

    private void isNetworkConnected() {
        //check for internet available as well rather than just network
        NetworkAvailabilityCheckAsyncTask networkAvailabilityCheckAsyncTask = new NetworkAvailabilityCheckAsyncTask(this);
        networkAvailabilityCheckAsyncTask.execute();
    }

    private void setImageView(){
        String filePath = photoFile.getPath();
        bitmapImage = adjustOrientation(filePath);
        imageToDisplay = findViewById(R.id.takePhoto);
        imageToDisplay.setImageBitmap(bitmapImage);
        applicationState = ApplicationState.PHOTO_TAKEN;
    }

    public void validateNetworkConnection(View view) {
        isNetworkConnected();
    }

    public void inspectObject(){
        try {
            if(photoFile != null) {
                File newPhotoFile = createImage();
                copyFile(photoFile, newPhotoFile);
                Button takePhotoButton = findViewById(R.id.selectTakePhoto);
                takePhotoButton.setVisibility(View.INVISIBLE);
                Button inspectObjButton = findViewById(R.id.inspectObjects);
                inspectObjButton.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Inspecting Objects...", Toast.LENGTH_LONG).show();
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
                TextView resultScrollView = findViewById(R.id.resultTextView);
                TextView resultTextViewDescription = findViewById(R.id.description);
                ImageTransmitterAsyncTask imageTransmitterAsyncTask = new ImageTransmitterAsyncTask(bitmapImage, newPhotoFile, remoteService, resultScrollView, this, progressBar, resultTextViewDescription);
                imageTransmitterAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                applicationState = ApplicationState.INSPECT_OBJECT_CALLED;
            } else {
                Toast.makeText(this, String.format("Please click/select image to inspect!"), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception exception) {
            Log.e("ERROR", String.format("Exception occurred in validateNetworkConnection method , %s", exception.getMessage()));
            exception.printStackTrace();
            Toast.makeText(this, String.format("Inspecting objects failed ..."), Toast.LENGTH_LONG).show();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("Info", String.format("ComponentName %s service %s", name, service));
            remoteService = ObjectRecogniserAIDL.Stub.asInterface(service);
            Log.i("Info", String.format("remoteService.getClass().getName() %s", remoteService.getClass().getName()));
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

    public void selectImage(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo");
        builder.setItems(OPTIONS, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int option) {
                boolean optionSelected = Utility.checkPermission(MainActivity.this);
                Log.i("Info", String.format("optionSelected %s ,option selected :%s, OPTIONS %s", optionSelected, option, Arrays.toString(OPTIONS)));
                selectionHelper(OPTIONS, option, optionSelected, dialogInterface);
                userSelectedOption = option;
            }
        });
        builder.show();
    }

    private void selectionHelper(CharSequence[] options, int option, boolean optionSelected, DialogInterface dialogInterface) {
        if (options[option].equals("Take Photo")) {
            Log.i("INFO", String.format("take photo"));
            if (optionSelected)
                takePhoto();
        } else if (options[option].equals("Choose from Gallery")) {
            Log.i("INFO", String.format("Choose from Gallery"));
            if (optionSelected) {

                Log.i("Info", String.format("galleryIntent is about to be called..."));
                galleryIntent(photoFile);
            }
        } else if (options[option].equals("Cancel")) {
            Log.i("INFO", String.format("Cancel"));
            dialogInterface.dismiss();
        }
    }

    private void galleryIntent(File photoFile) {
        Log.i("Info", String.format("galleryIntent entered..."));
        Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
        Intent galIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        galIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(galIntent, "Select File"), REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.i("Info",String.format("onRequestPermissionsResult entered. requestCode %s permissions %s, grantResults %s", requestCode, permissions, grantResults));
        if (requestCode == REQUEST_STORAGE_FOR_APK && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("INFO", String.format("Permission to store apk granted"));
            copyApk();
        } else {
            Log.i("INFO", String.format("On request permission result entered , user selected option : %s", userSelectedOption));
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    photoFile = createImage();
                    switch (userSelectedOption) {
                        case 0:
                            if (photoFile != null) {
                                startCamera(photoFile);
                            }
                            break;
                        case 1:
                            galleryIntent(photoFile);
                            break;

                    }

                } catch (IOException exception) {
                    Log.e("ERROR", String.format("Exception occurred while creating the image %s", exception.getMessage()));
                    exception.printStackTrace();
                }
            } else {
                Log.i("INFO", "Permission denied");
            }
        }
    }

    private File createImage() throws IOException {
        String imageFileName = UUID.randomUUID().toString();
        Log.i("INFO", String.format("Image file name %s ", imageFileName));
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/Pictures");
        if(!storageDir.exists()){
            boolean directoryCreated = storageDir.mkdir();
            Log.i("INFO",String.format("Storage directory was absent, created now!! .. %s",directoryCreated));
        }
        Log.i("Info", String.format("storageDir %s", storageDir));
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        myCurrentPhotoPath = image.getAbsolutePath();
        Log.i("INFO", String.format("myCurrentPhotoPath : %s, image : %s", myCurrentPhotoPath, image));
        return image;
    }

    private void startCamera(File image) {
        Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", image);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
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
            if (ExifInterface.ORIENTATION_NORMAL != rotation) {
                int rotationInDegrees = exifToDegrees(rotation);
                Log.i("INFO", String.format("rotation In degrees to apply on Image is %s", rotationInDegrees));
                Bitmap rotatedBitmapImage = RotateBitmap(convertedBitmapImage, rotationInDegrees);
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
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onBackPressed() {
        switch (applicationState) {
            case PHOTO_TAKEN:
                applicationState = ApplicationState.APPLICATION_STARTED;
                Log.i("INFO", String.format("Application state is %s and image to display is %s ", applicationState, imageToDisplay));
                imageToDisplay.setImageBitmap(null);
                photoFile = null;
                Log.i("INFO", String.format("set imageToDisplay  to null ,imageToDisplay is %s ", imageToDisplay));
                imageToDisplay.setImageResource(R.color.colorPrimaryDark);
                break;
            case INSPECT_OBJECT_CALLED:
                Toast.makeText(this, String.format("Inspecting Objects, Please Wait..."), Toast.LENGTH_LONG).show();
                break;
            case INSPECT_OBJECT_FINISHED:
                applicationState = ApplicationState.PHOTO_TAKEN;
                Button photoTaken = findViewById(R.id.selectTakePhoto);
                photoTaken.setVisibility(View.VISIBLE);
                Button inspectObjs = findViewById(R.id.inspectObjects);
                inspectObjs.setVisibility(View.VISIBLE);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultTextView.setText("");
                resultTextView.setVisibility(View.INVISIBLE);
                TextView resultTextViewDescription = findViewById(R.id.description);
                resultTextViewDescription.setVisibility(View.INVISIBLE);
                break;
            default:
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    private boolean isAppInstalled(String uri){
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    private void installServerAPK() {
        Log.i("INFO", String.format("Entered installMYapk... "));
        if(checkWritePermision(REQUEST_STORAGE_FOR_APK)){
            copyApk();
        }
    }
    private void copyFileStream(InputStream in, OutputStream out){
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
        } catch (IOException ex) {
            Log.e("ERROR", String.format("Exception occurred in copying file streams!!! %s ", ex.getMessage()));
            ex.printStackTrace();
        }
    }
    private void copyApk(){
        try {
            AssetManager assetManager = getAssets();
            InputStream in = null;
            OutputStream out = null;
            File apkDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Apks");
            if (!apkDirectory.exists()) {
                boolean isDirectoryCreated = apkDirectory.mkdir();
                Log.i("Info", String.format("apkDirectory %s created! %s", apkDirectory, isDirectoryCreated));
            }
            File imageRecogniserAPKfile = File.createTempFile(
                    "ImageRecogniser",
                    ".apk",
                    apkDirectory
            ); //new File(apkDirectory.getPath() + "/ImageRecogniser.apk");
            Log.i("INFO", String.format("imageRecogniserAPKfile : %s", imageRecogniserAPKfile));
            if (true) {

                in = assetManager.open("imageRecogniser.apk");
                Log.i("Info", String.format("imageRecogniser.apk in assets %s", in));
                out = new FileOutputStream(imageRecogniserAPKfile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                in.close();
                in = null;

                out.flush();
                out.close();
                out = null;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri appURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageRecogniserAPKfile); //Uri.fromFile(imageRecogniserAPKfile);
            intent.setDataAndType(appURI,
                    "application/vnd.android.package-archive");
            startActivityForResult(intent, REQUEST_SERVER_INSTALLATION);
        } catch (Exception e) {
            Log.e("ERROR", String.format("Exception in installing server application :%s", e.getMessage()));
            e.printStackTrace();
        }
    }
}


