package com.example.mensasucks

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.ipsec.ike.TunnelModeChildSessionParams.ConfigRequestIpv4Address
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import java.security.InvalidParameterException

class ScreenCaptureActivity : AppCompatActivity() {
    private val REQUEST_CODE = 100
    companion object{
        lateinit var roomNumberEditText:EditText
        lateinit var ipAddressEditText:EditText
        lateinit var currentIpAddress:String
        lateinit var roomNumber:String
    }

    /****************************************** Activity Lifecycle methods  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //request a permission

        // start projection
        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener { startProjection() }
        val receivingButton=findViewById<Button>(R.id.receive)
        roomNumberEditText=findViewById<EditText>(R.id.roomNumber)
        ipAddressEditText=findViewById<EditText>(R.id.personId)
        receivingButton.setOnClickListener {
            //Here we should go to another intent
            roomNumber=roomNumberEditText.text.toString()
            currentIpAddress=ipAddressEditText.text.toString()
            if(!roomNumber.equals("")&&!currentIpAddress.equals("")) {
                var intentToGo = Intent(this, BarcodeActivity::class.java)
                intentToGo.putExtra("roomNumber", roomNumber)
                intentToGo.putExtra("ipAddress",currentIpAddress)
                startActivity(intentToGo)
            }

        }

        //u should also add some edittext for getting messages from certain room via websocket


        // stop projection
        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnClickListener { stopProjection() }
        //u need a button below for request a permission
        val permissionButton = findViewById<Button>(R.id.permissionbutton)
        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        roomNumber= roomNumberEditText.text.toString()
        currentIpAddress= ipAddressEditText.text.toString()
        if(!roomNumber.equals("") and !currentIpAddress.equals("")) {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //here you should take data for a websocket in android
                    //here we have a room number and set and an ipaddress

                    BarcodeActivity.ws =
                        WebSocketFactory().createSocket("ws://192.168.43.102:8000/ws/chat/" + roomNumber);
                    val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()


                    StrictMode.setThreadPolicy(policy)
                    BarcodeActivity.ws.connect()
                    startService(
                        ScreenCaptureService.getStartIntent(
                            this,
                            resultCode,
                            data
                        )
                    )
                }
            }
        }

    }

    /****************************************** UI Widget Callbacks  */
    private fun startProjection() {
        val mProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    private fun stopProjection() {
        startService(ScreenCaptureService.getStopIntent(this))
    }
}