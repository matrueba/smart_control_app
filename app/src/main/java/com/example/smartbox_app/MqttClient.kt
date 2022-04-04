package com.example.smartbox_app

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.TextView
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.*


class MqttClient(context: Context, viewLayout: View) {

    private val mqttClient : MqttAndroidClient

    private var timer: Chronometer

    private val tempView = viewLayout.findViewById<TextView>(R.id.air_temperature_value)
    private val humView = viewLayout.findViewById<TextView>(R.id.air_humidity_value)
    private val waterView = viewLayout.findViewById<TextView>(R.id.water_level_value)
    private val groundView = viewLayout.findViewById<TextView>(R.id.ground_humidity_value)

    private val pumpIndicator = viewLayout.findViewById<TextView>(R.id.pump_indicator)

    private val host = getMqttConfig(context)?.getProperty("mqtt_host")
    private val port = getMqttConfig(context)?.getProperty("mqtt_port")?.toInt()
    private val serverURI = "tcp://$host:$port"

    init {

        timer = viewLayout.findViewById(R.id.pump_time)
        timer.base = SystemClock.elapsedRealtime()


        mqttClient = MqttAndroidClient(context, serverURI, "smartbox_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                handleMessages(topic, message)
            }
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
            }
        })
    }

    companion object {
        const val TAG = "MqttClient"
    }

    fun getMqttConfig(context: Context): Properties? {
        val resources: Resources = context.getResources()
        try {
            val rawResource: InputStream = resources.openRawResource(R.raw.config)
            val properties = Properties()
            properties.load(rawResource)
            return properties
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Unable to find the config file")
            e.printStackTrace()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open config file")
            e.printStackTrace()
        }
        return null
    }


    fun handleMessages(topic: String, message: MqttMessage?){
        val payload = message.toString()
        if (topic.contains("TEMPERATURE")) {
            val value = JSONObject(payload).getString("value")
            tempView.text = "${value}ºC"
            if (value.toInt() >= 35){
                tempView.setTextColor(Color.parseColor("#CC0909"))
            }else if((10 <= value.toInt()) && value.toInt() < 35){
                tempView.setTextColor(Color.parseColor("#028107"))

            }else {
                tempView.setTextColor(Color.parseColor("#36A8DC"))
            }
        } else if (topic.contains("AIR_HUMIDITY")){
            val value = JSONObject(payload).getString("value")
            humView.text = "${value}%"
            if (value.toInt() >= 60){
                tempView.setTextColor(Color.parseColor("#36A8DC"))
            }else if((30 <= value.toInt()) && value.toInt() < 60){
                tempView.setTextColor(Color.parseColor("#028107"))

            }else {
                tempView.setTextColor(Color.parseColor("#CC0909"))
            }
        } else if (topic.contains("GROUND_HUMIDITY")){
            val value = JSONObject(payload).getString("value")
            groundView.text = "${value}%"
            if (value.toInt() >= 60){
                groundView.setTextColor(Color.parseColor("#36A8DC"))
            }else if((30 <= value.toInt()) && value.toInt() < 60){
                groundView.setTextColor(Color.parseColor("#028107"))

            }else {
                groundView.setTextColor(Color.parseColor("#CC0909"))
            }
        } else if (topic.contains("WATER_LEVEL")){
            val value = JSONObject(payload).getString("value")
            waterView.text = "${value}%"
            if (value.toInt() >= 60){
                waterView.setTextColor(Color.parseColor("#36A8DC"))
            }else if((30 <= value.toInt()) && value.toInt() < 60){
                waterView.setTextColor(Color.parseColor("#028107"))

            }else {
                waterView.setTextColor(Color.parseColor("#CC0909"))
            }
        } else if (topic.contains("RESULT")) {
            val result = JSONObject(payload).getString("value")
            val response = JSONObject(payload).getString("command_response")
            when (response) {
                "start_pump" -> {
                    if (result == "OK") {
                        pumpIndicator.text = "ACTIVADO"
                        pumpIndicator.setBackgroundColor(Color.parseColor("#01BF09"))
                        timer.start()
                    }
                }
                "stop_pump" -> {
                    if (result == "OK") {
                        pumpIndicator.text = "DESACTIVADO"
                        pumpIndicator.setBackgroundColor(Color.parseColor("#000000"))
                        timer.stop()
                        timer.base = SystemClock.elapsedRealtime()
                    }
                }
                else -> {
                    Log.d(TAG, "Command value $response not recognized")
                }
            }
        }else {
            Log.d(TAG, "Topic $topic not recognized")
        }
    }

    fun connect() {
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    subscribe("TEMPERATURE")
                    subscribe("air_humidity")
                    subscribe("command")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    fun subscribe(subscriptionTopic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(subscriptionTopic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "Subscribed to topic '$subscriptionTopic'")
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "Subscription to topic '$subscriptionTopic' failed!")
                }
            })
        } catch (e: MqttException) {
            Log.d(TAG, "Exception whilst subscribing to topic '$subscriptionTopic'")
            e.printStackTrace()
        }
    }


    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
