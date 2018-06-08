package com.aishwaryagm.objectrecogniser.callbackass;


import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.aishwaryagm.objectrecogniser.CallBackInterface;
import com.aishwaryagm.objectrecogniser.ImageTransmitterAsyncTask;

import java.util.Arrays;

public class CallbackImpl extends CallBackInterface.Stub {

    @Override
    public void update(String[] result) throws RemoteException {
            Log.i("INFO",String.format("The result of gvc : %s", Arrays.toString(result)));
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
