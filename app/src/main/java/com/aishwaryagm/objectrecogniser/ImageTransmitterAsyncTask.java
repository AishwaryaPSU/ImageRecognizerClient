package com.aishwaryagm.objectrecogniser;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.TestLooperManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aishwaryagm.objectrecogniser.callbackass.CallbackImpl;
import com.aishwaryagm.objectrecogniser.constants.ApplicationState;


import org.w3c.dom.Text;

import java.io.File;
import java.util.Arrays;

/**
 * Created by aishwaryagm on 6/2/18.
 */

public class ImageTransmitterAsyncTask extends AsyncTask<Void,Void,Void>  {
    private Bitmap bitmapImage;
    private File photoFile ;
    private ObjectRecogniserAIDL remoteService;
    private String[] resultArray = {};
    private TextView resultTextView;
    private MainActivity mainActivity;
    private ProgressBar progressBar;
    private TextView resultTextDescription;

    public ImageTransmitterAsyncTask(Bitmap bitmapImage,File photoFile,ObjectRecogniserAIDL remoteService, TextView resultTextView,MainActivity mainActivity,ProgressBar progressBar,TextView resultTextDescription){
        this.bitmapImage=bitmapImage;
        this.photoFile=photoFile;
        this.remoteService=remoteService;
        this.resultTextView=resultTextView;
        this.mainActivity=mainActivity;
        this.progressBar=progressBar;
        this.resultTextDescription=resultTextDescription;
    }
    @Override
    protected Void doInBackground(Void... voids) {
        Log.i("Info","Entered Inspect Objects function");
        Log.i("INFO",String.format("remoteService %s", remoteService));
        CallBackInterface callbackInterfaceObject = new CallBackInterface.Stub() {
            @Override
            public void update(String[] result) throws RemoteException {
                Log.i("Info", String.format("result Recieved at client %s", Arrays.toString(result)));
                resultArray = result;
                print(resultArray);
            }
        };
        Log.i("INFO",String.format("callbackInterfaceObject %s", callbackInterfaceObject));
        String photoFilePath = photoFile.getPath();
        try {
            Log.i("INFO",String.format("SEnding photofile path %s ", photoFilePath));
            remoteService.analyzeImageByPath(photoFilePath, callbackInterfaceObject);
        } catch (RemoteException e) {
            Log.e("ERROR",String.format("Exception occurred while calling image analyzer %s",e.getMessage()));
            e.printStackTrace();
        }
        return null;
    }
    public void print(final String[] resultArray){
        Log.i("INFO",String.format("Inside ImageTransmitter Print method %s",Arrays.toString(resultArray)));
        String preFinalText = "";
        for(String elementOfResultArray : resultArray){
            preFinalText= preFinalText+"\n"+elementOfResultArray;
        }
        final String finalText=preFinalText;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                resultTextView.setVisibility(View.VISIBLE);
                resultTextDescription.setVisibility(View.VISIBLE);
                resultTextView.setMovementMethod(new ScrollingMovementMethod());
                resultTextView.setText(finalText);
                mainActivity.setApplicationState(ApplicationState.INSPECT_OBJECT_FINISHED);
            }
        });
    }
}
