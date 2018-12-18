// IOnNewBookArrivedListener.aidl
package com.gavinandre.aidl;

import com.gavinandre.aidl.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book newBook);
}
