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

class BookManagerService : Service() {

    private val TAG = BookManagerService::class.java.simpleName

    //使用CopyOnWriteArrayList来支持并发读写
    private val mBookList = CopyOnWriteArrayList<Book>()
    //使用RemoteCallbackList来支持aidl接口管理
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
        super.onDestroy()
    }

    private val mBinder = object : IBookManager.Stub() {
        override fun addBook(book: Book) {
            //模拟耗时操作
            SystemClock.sleep(3000)
            mBookList.add(book)
            //通知所有注册监听接口的客户端有新书到达
            onNewBookArrived(book)
        }

        override fun getBookList(): List<Book> {
            //模拟耗时操作
            SystemClock.sleep(3000)
            return mBookList
        }

        override fun registerListener(listener: IOnNewBookArrivedListener?) {
            //添加aidl接口
            mListenerList.register(listener)
            //获取aidl接口数量
            val N = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.i(TAG, "registerListener, current size:$N")
        }

        override fun unregisterListener(listener: IOnNewBookArrivedListener?) {
            //移除aidl接口
            val success = mListenerList.unregister(listener)
            if (success) {
                Log.i(TAG, "unregister success.")
            } else {
                Log.i(TAG, "not found, can not unregister.")
            }
            //获取aidl接口数量
            val N = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.i(TAG, "unregisterListener, current size:$N")
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
        val N = mListenerList.beginBroadcast()
        for (i in 0 until N) {
            try {
                //通知所有注册监听接口的客户端有新书到达
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
