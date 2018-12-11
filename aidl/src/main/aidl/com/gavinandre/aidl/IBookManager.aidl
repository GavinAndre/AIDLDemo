// IBookManager.aidl
package com.gavinandre.aidl;

import com.gavinandre.aidl.Book;
import com.gavinandre.aidl.IOnNewBookArrivedListener;

interface IBookManager {

    List<Book> getBookList();
    void addBook(in Book book);
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterListener(IOnNewBookArrivedListener listener);

}
