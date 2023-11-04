package com.example.mensasucks

import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Base64.encodeToString
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import com.example.mensasucks.BarcodeActivity.Companion.ws
import com.example.mensasucks.ScreenCaptureService
import java.io.*
import java.util.*


class ScreenCaptureService : Service() {
    private var mMediaProjection: MediaProjection? = null
    private var mStoreDir: String? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null

    private inner class ImageAvailableListener : OnImageAvailableListener {
        fun saveMediaToStorage(bitmap: Bitmap) {
            //Generating a file name
            val filename = "${System.currentTimeMillis()}.jpg"

            //Output stream
            var fos: OutputStream? = null

            //For devices running android >= Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //getting the contentResolver
                applicationContext?.contentResolver?.also { resolver ->

                    //Content resolver will process the contentvalues
                    val contentValues = ContentValues().apply {

                        //putting file information in content values
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    //Inserting the contentValues to contentResolver and getting the Uri
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    //Opening an outputstream with the Uri that we got
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                //These for devices running on android < Q
                //So I don't think an explanation is needed here
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                //Finally writing the bitmap to the output stream that we opened
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)

            }
        }
        override fun onImageAvailable(reader: ImageReader) {
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            try {
                mImageReader!!.acquireLatestImage().use { image ->
                    //here we should check our application

                    var currentApp = "NULL"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
                        val time = System.currentTimeMillis()
                        val appList = usm.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            time - 1000 * 1000,
                            time
                        )
                        if (appList != null && appList.size > 0) {
                            val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
                            for (usageStats in appList) {
                                mySortedMap!![usageStats.lastTimeUsed] = usageStats
                            }
                            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                                currentApp = mySortedMap[mySortedMap.lastKey()]!!.packageName

                            }
                        }
                    } else {

                        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                        val tasks = am.runningAppProcesses
                        currentApp = tasks[0].processName
                    }
                    val mode =getSystemService(APP_OPS_SERVICE) as AppOpsManager
                    var checker=mode.checkOpNoThrow(
                        "android:get_usage_stats",
                        Process.myUid(), applicationContext.packageName
                    )
                    val granted = checker == AppOpsManager.MODE_ALLOWED

                    //it.in4matic.disco
                    if(currentApp.equals("it.in4matic.disco")){
                        if (image != null) {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * mWidth

                            // create bitmap
                            bitmap = Bitmap.createBitmap(
                                mWidth + rowPadding / pixelStride,
                                mHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap?.copyPixelsFromBuffer(buffer)
                            for(i in 0..100){
                                for(j in 0..mWidth-1){
                                    bitmap?.setPixel(j,i, Color.BLACK)

                                }

                            }
                            //bitmap must be converted to string
                            var current_string_image=convertImageIntoString(bitmap as Bitmap)
                            //ipaddress we can take from other activity
                            ws.sendText("{\"image\":\"$current_string_image\"," +
                                    "\"messageIp\":\"${ScreenCaptureActivity.currentIpAddress}\"}");
                            //on the line before we sent some string inside
                            //After we need to send this bitmap to server


                            // write bitmap to a file
                            /*
                            val something = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                IMAGES_PRODUCED.toString() + ".png"
                            )
                            fos = FileOutputStream(something)*/
                            //bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            //we need to send a bitmap file to server to another user if images are different
                            //instead of image saving we need to use another technique for



                            //saveMediaToStorage(bitmap as Bitmap)

                            IMAGES_PRODUCED++
                            Log.e(TAG, "captured image: " + IMAGES_PRODUCED)
                        }




                        //here we should take a screenshot
                        //openScreenShotActivity()
                        //here we should take a screenshot of our activity inside!

                        //here we should return some data from our service inside
                    }











                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos!!.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
                if (bitmap != null) {
                    bitmap!!.recycle()
                }
            }
        }
        private fun convertImageIntoString(bitmap:Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val encoded:String= android.util.Base64.encodeToString(byteArray,android.util.Base64.DEFAULT)
            return encoded
            //after that one we need to send our message to websocket
        }
    }

    private inner class OrientationChangeCallback internal constructor(context: Context?) :
        OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay!!.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                    if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            mHandler!!.post {
                if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)
                if (mOrientationChangeCallback != null) mOrientationChangeCallback!!.disable()
                mMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // create store dir
        val externalFilesDir = getExternalFilesDir(null)

        if (externalFilesDir != null) {
            mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(mStoreDir)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.")
                    stopSelf()
                }
            }
        } else {
            Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.")
            stopSelf()
        }

        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (isStartCommand(intent)) {
            // create notification

            val notification = NotificationUtils.getNotification(this)
            startForeground(notification.first, notification.second)
            // start projection
            val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>(DATA)
            startProjection(resultCode, data)
        } else if (isStopCommand(intent)) {
            stopProjection()
            stopSelf()
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent?) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data!!)
            if (mMediaProjection != null) {
                // display metrics
                mDensity = Resources.getSystem().displayMetrics.densityDpi
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                mDisplay = windowManager.defaultDisplay

                // create virtual display depending on device width / height
                createVirtualDisplay()

                // register orientation change callback
                mOrientationChangeCallback = OrientationChangeCallback(this)
                if (mOrientationChangeCallback!!.canDetectOrientation()) {
                    mOrientationChangeCallback!!.enable()
                }

                // register media projection stop callback
                mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
            }
        }
    }

    private fun stopProjection() {
        if (mHandler != null) {
            mHandler!!.post {
                if (mMediaProjection != null) {
                    mMediaProjection!!.stop()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        // get width and height
        mWidth = Resources.getSystem().displayMetrics.widthPixels
        mHeight = Resources.getSystem().displayMetrics.heightPixels

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME, mWidth, mHeight,
            mDensity, virtualDisplayFlags, mImageReader!!.surface, null, mHandler
        )
        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val RESULT_CODE = "RESULT_CODE"
        private const val DATA = "DATA"
        private const val ACTION = "ACTION"
        private const val START = "START"
        private const val STOP = "STOP"
        private const val SCREENCAP_NAME = "screencap"
        private var IMAGES_PRODUCED = 0
        fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, START)

            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)
            return intent
        }

        fun getStopIntent(context: Context?): Intent {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra(ACTION, STOP)
            return intent
        }

        private fun isStartCommand(intent: Intent): Boolean {
            return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                    && intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == START)
        }

        private fun isStopCommand(intent: Intent): Boolean {
            return intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == STOP
        }

        private val virtualDisplayFlags: Int
            private get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
}