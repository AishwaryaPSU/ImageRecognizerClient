// ObjectRecogniserAIDL.aidl
package com.aishwaryagm.objectrecogniser;
import com.aishwaryagm.objectrecogniser.ParcelableImageBytes;
import com.aishwaryagm.objectrecogniser.CallBackInterface;

// Declare any non-default types here with import statements

interface ObjectRecogniserAIDL {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    String[] analyzeImage(in byte[] inputImageInBytes);

    String[] analyzeImageImpr(in ParcelableImageBytes parcelableImageBytes);

    oneway void analyzeImageByPath(in String filePath, in CallBackInterface callbackObject);
}
