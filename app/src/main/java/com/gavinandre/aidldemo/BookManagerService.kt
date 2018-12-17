package com.gavinandre.aidldemo

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import com.gavinandre.aidl.Book
import com.gavinandre.aidl.IBookManager
import com.gavinandre.aidl.IOnNewBookArrivedListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class BookManagerService : Service() {

    private val TAG = BookManagerService::class.java.simpleName

    private val mIsServiceDestroyed = AtomicBoolean(false)

    private val mBookList = CopyOnWriteArrayList<Book>()
    private val mListenerList = RemoteCallbackList<IOnNewBookArrivedListener>()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        //验证权限方式1
        //在onBind函数中做验证，没有声明该权限则不返回binder
        checkCallingOrSelfPermission(ACCESS_BOOK_SERVICE_PERMISSION)
            .takeIf { it == PackageManager.PERMISSION_DENIED }
            ?.let {
                Log.e(TAG, "onBind: permission denied")
                return null
            }
            ?: return mBinder
    }

    override fun onDestroy() {
        mIsServiceDestroyed.set(true)
        super.onDestroy()
    }

    private val mBinder = object : IBookManager.Stub() {
        override fun addBook(book: Book) {
            SystemClock.sleep(3000)
            onNewBookArrived(book)
        }

        override fun getBookList(): MutableList<Book> {
            SystemClock.sleep(3000)
            return mBookList
        }

        override fun registerListener(listener: IOnNewBookArrivedListener?) {
            mListenerList.register(listener)
            val N = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.d(TAG, "registerListener, current size:$N")
        }

        override fun unregisterListener(listener: IOnNewBookArrivedListener?) {
            val success = mListenerList.unregister(listener)
            if (success) {
                Log.d(TAG, "unregister success.")
            } else {
                Log.d(TAG, "not found, can not unregister.")
            }
            val N = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.d(TAG, "unregisterListener, current size:$N")
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            //验证权限方式2
            //在onTransact函数中做验证，没有声明该权限则返回false
            checkCallingOrSelfPermission(ACCESS_BOOK_SERVICE_PERMISSION)
                .takeIf { it == PackageManager.PERMISSION_DENIED }
                ?.let {
                    Log.e(TAG, "onTransact: permission denied")
                    return false
                }

            //验证权限方式3
            //验证包名前缀，不相等则返回false
            packageManager.getPackagesForUid(getCallingUid())
                ?.takeIf { it.isNotEmpty() }
                ?.takeUnless { it[0].startsWith(PACKAGE_NAME_PREFIX) }
                ?.let {
                    Log.e(TAG, "onTransact: package name invalid")
                    return false
                }

            return super.onTransact(code, data, reply, flags)
        }
    }

    fun onNewBookArrived(book: Book) {
        mBookList.add(book)
        val N = mListenerList.beginBroadcast()
        for (i in 0 until N) {
            try {
                mListenerList
                    .getBroadcastItem(i)
                    ?.onNewBookArrived(book)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        mListenerList.finishBroadcast()
    }

    companion object {
        const val ACCESS_BOOK_SERVICE_PERMISSION = "com.gavinandre.aidldemo.permission.ACCESS_BOOK_SERVICE"
        const val PACKAGE_NAME_PREFIX = "com.gavinandre"
    }
}
