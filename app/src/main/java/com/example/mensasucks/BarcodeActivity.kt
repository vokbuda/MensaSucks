package com.example.mensasucks

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream


class BarcodeActivity : AppCompatActivity() {
    companion object{
        lateinit var  ws: WebSocket
        lateinit var barcode:ImageView
        lateinit var ipAddress:String
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)
        barcode=findViewById<ImageView>(R.id.barcode)
        var roomNumber=intent.getStringExtra("roomNumber")//here u can see a room number
        ipAddress= intent.getStringExtra("ipAddress").toString()

        //here we shoulg get our intent---image and set inside of our activity
        //right now we got a roomNumber intent, we need to start listen from webserver
        if (roomNumber != null) {

            socketConnect(roomNumber)
        }
        //here we just listen our message



    }
    fun socketConnect(roomNumber:String){
        //
        Log.d("RECEIVE COMPONENT","CHECK HERE")
        ws = WebSocketFactory().createSocket("ws://192.168.43.102:8000/ws/chat/"+roomNumber);
        //here we need listen our webserver and setimage if it had been received in android
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()


        StrictMode.setThreadPolicy(policy)
        ws.connect()
        ws.setMaxPayloadSize(100000)
        ws.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: com.neovisionaries.ws.client.WebSocket?, text: String?) {
                text?.let {



                    val Jobject = JSONObject(JSONObject(text).get("message").toString())

                    //here we should get our image
                    //Before displayNotification we need to get our message



                    //here u can choose an id
                    if(!Jobject.get("messageIp").equals(ipAddress)){
                        val message:String=Jobject.get("image") as String
                        StringToImage(message)
                        //here we need to convert a string into image and setup our image

                    }
                }

            }
        })
        //ws.sendText("{\"message\":\"$text\"}");
        //ws.addListener()

    }
    private fun StringToImage(important:String){
        val decodedString: ByteArray = Base64.decode(important, Base64.NO_WRAP)
        val inputStream: InputStream = ByteArrayInputStream(decodedString)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        barcode.setImageBitmap(bitmap)
    }

}