package com.gavinandre.aidldemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
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

    //绑定远程服务端
    private fun bindService() {
        val intent = Intent(this, BookManagerService::class.java)
        intent.setPackage(packageName)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    //注册按钮监听，调用远程耗时方法需要开启线程
    private fun initListener() {
        btnGetBookList.setOnClickListener {
            Toast.makeText(this, "GetBookList", Toast.LENGTH_SHORT).show()
            Thread {
                try {
                    mRemoteBookManager?.let { mRemoteBookManager ->
                        val list = mRemoteBookManager.bookList
                        Log.i(TAG, "query book list: $list")
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }.start()
        }
        btnAddBook.setOnClickListener {
            Toast.makeText(this, "AddBook", Toast.LENGTH_SHORT).show()
            Thread {
                try {
                    mRemoteBookManager?.apply {
                        it.tag = it.tag?.run {
                            this as Int + 1
                        } ?: 1
                        val id = it.tag as Int
                        val newBook = Book(id, "Book$id")
                        Log.i(TAG, "add book: $newBook")
                        addBook(newBook)
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    override fun onDestroy() {
        unBindService()
        //防止内存泄露
        mHandler.removeCallbacksAndMessages(null)
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

    //服务端连接状态回调
    private var mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e(TAG, "onServiceConnected: $mRemoteBookManager")
            //将服务端返回的Binder对象转换为aidl对象
            mRemoteBookManager = IBookManager.Stub.asInterface(service)
            Log.e(TAG, "onServiceConnected: $mRemoteBookManager")
            try {
                //注册AIDL回调接口
                mRemoteBookManager?.registerListener(mOnNewBookArrivedListener)
                //设置死亡代理
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

    //aidl接口回调
    private val mOnNewBookArrivedListener = object : IOnNewBookArrivedListener.Stub() {
        override fun onNewBookArrived(newBook: Book?) {
            //obtainMessage可以复用Message，减少开销
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, newBook).sendToTarget()
        }
    }

    //使用lambda简化创建handler
    private val mHandler = Handler { msg ->
        when (msg.what) {
            MESSAGE_NEW_BOOK_ARRIVED -> Log.i(TAG, "receive new book: ${msg.obj}")
        }
        false
    }

    //死亡代理回调
    private val mDeathRecipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            Log.d(TAG, "binder died. ThreadName: ${Thread.currentThread().name}")
            //移除死亡代理
            mRemoteBookManager?.asBinder()?.unlinkToDeath(this, 0)
            mRemoteBookManager = null
            // TODO:这里重新绑定远程Service
            //bindService()
        }
    }

    companion object {
        const val MESSAGE_NEW_BOOK_ARRIVED = 1
    }
}
