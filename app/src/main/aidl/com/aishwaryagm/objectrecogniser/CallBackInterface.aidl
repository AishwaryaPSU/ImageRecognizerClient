// CallBackInterface.aidl
package com.aishwaryagm.objectrecogniser;

// Declare any non-default types here with import statements

oneway interface CallBackInterface {
     void update(in String[] result);
}
