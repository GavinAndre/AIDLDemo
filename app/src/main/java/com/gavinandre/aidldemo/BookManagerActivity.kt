package com.gavinandre.aidldemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.gavinandre.aidl.Book
import com.gavinandre.aidl.IBookManager
import com.gavinandre.aidl.IOnNewBookArrivedListener
import kotlinx.android.synthetic.main.activity_book_manager.*

class BookManagerActivity : AppCompatActivity() {

    private val TAG = BookManagerActivity::class.java.simpleName

    private var mRemoteBookManager: IBookManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_manager)
        bindService()
        initListener()
    }

    private fun bindService() {
        val intent = Intent(this, BookManagerService::class.java)
        intent.setPackage(packageName)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initListener() {
        btnGetBookList.setOnClickListener {
            Toast.makeText(this, "GetBookList", Toast.LENGTH_SHORT).show()
            Thread(Runnable {
                try {
                    mRemoteBookManager?.let { mRemoteBookManager ->
                        val list = mRemoteBookManager.bookList
                        Log.i(TAG, "query book list: $list")
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }).start()
        }
        btnAddBook.setOnClickListener {
            Toast.makeText(this, "AddBook", Toast.LENGTH_SHORT).show()
            Thread(Runnable {
                try {
                    mRemoteBookManager?.apply {
                        it.tag = it.tag?.run {
                            this as Int + 1
                        } ?: 1
                        val id = it.tag as Int
                        val newBook = Book(id, "Book$id")
                        addBook(newBook)
                        Log.i(TAG, "add book: $newBook")
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }).start()
        }
    }

    override fun onDestroy() {
        unBindService()
        super.onDestroy()
    }

    private fun unBindService() {
        try {
            Log.i(TAG, "onDestroy: $mOnNewBookArrivedListener")
            mRemoteBookManager
                ?.takeIf { it.asBinder().isBinderAlive }
                ?.unregisterListener(mOnNewBookArrivedListener)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        unbindService(mConnection)
    }

    private var mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e(TAG, "onServiceConnected: $mRemoteBookManager")
            mRemoteBookManager = IBookManager.Stub.asInterface(service)
            Log.e(TAG, "onServiceConnected: $mRemoteBookManager")
            try {
                mRemoteBookManager?.registerListener(mOnNewBookArrivedListener)
                mRemoteBookManager?.asBinder()?.linkToDeath(mDeathRecipient, 0)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected ThreadName: ${Thread.currentThread().name}")
            mRemoteBookManager = null
        }
    }

    private val mOnNewBookArrivedListener = object : IOnNewBookArrivedListener.Stub() {
        override fun onNewBookArrived(newBook: Book?) {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, newBook).sendToTarget()
        }
    }

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            msg?.apply {
                when (what) {
                    MESSAGE_NEW_BOOK_ARRIVED -> Log.i(TAG, "receive new book: ${msg.obj}")
                }
            } ?: super.handleMessage(msg)
        }
    }

    private val mDeathRecipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            Log.d(TAG, "binder died. ThreadName: ${Thread.currentThread().name}")
            mRemoteBookManager?.asBinder()?.unlinkToDeath(this, 0)
            mRemoteBookManager = null
            // TODO:这里重新绑定远程Service
        }
    }

    companion object {
        const val MESSAGE_NEW_BOOK_ARRIVED = 1
    }
}
