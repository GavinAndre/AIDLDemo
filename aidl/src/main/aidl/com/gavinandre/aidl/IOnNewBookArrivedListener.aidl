// IOnNewBookArrivedListener.aidl
package com.gavinandre.aidl;

import com.gavinandre.aidl.Book;

// Declare any non-default types here with import statements

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book newBook);
}
