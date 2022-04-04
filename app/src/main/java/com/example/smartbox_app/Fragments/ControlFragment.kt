package com.example.smartbox_app.Fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

import com.example.smartbox_app.MqttClient
import com.example.smartbox_app.R
import java.io.IOException
import java.io.InputStream
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [ControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControlFragment : Fragment() {
    private lateinit var viewLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Control", "Control Fragment created")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewLayout = inflater.inflate(R.layout.fragment_control, container, false)

        val modeSelector = viewLayout.findViewById<Switch>(R.id.auto_mode_switch)
        val startButton = viewLayout.findViewById<Button>(R.id.start_button)
        val stopButton = viewLayout.findViewById<Button>(R.id.stop_button)
        val activationTimeSelector = viewLayout.findViewById<TimePicker>(R.id.activation_time)
        val periodicModeSelector = viewLayout.findViewById<Switch>(R.id.periodic_mode_switch)
        val repeatDaysSelector = viewLayout.findViewById<SeekBar>(R.id.repeat_days)
        val appContext = requireContext().applicationContext

        val host = getNetConfig(appContext)?.getProperty("http_host")
        val deveui = getNetConfig(appContext)?.getProperty("deveui")
        val queue = Volley.newRequestQueue(appContext)

        modeSelector.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Log.d("Control", "Send auto control")
                //Toast
                startButton.isEnabled = false
                stopButton.isEnabled = false
                modeSelector.text = "Auto"

                val activationHour = activationTimeSelector.hour
                val activationMinute = activationTimeSelector.minute
                val periodicMode = periodicModeSelector.isChecked
                val repeatDays = repeatDaysSelector.progress
                val jsonModeAuto = "{\"mode\":\"auto\",\"activation_hour\":\"$activationHour\",\"activation_minute\":" +
                        "\"$activationMinute\",\"periodic\":\"$periodicMode\",\"repeat_days\":\"$repeatDays\"}"
                val jsonObject = JSONObject(jsonModeAuto)
                createRequest("https://hookb.in/kxXgGwR1E3UBjzggWlyN", queue, jsonObject)
            } else {
                Log.d("Control", "Send manual control")
                //Toast
                startButton.isEnabled = true
                stopButton.isEnabled = true
                modeSelector.text = "Manual"

                val jsonModeManual = "{\"mode\":\"manual\"}"
                val jsonObject = JSONObject(jsonModeManual)
                val url = host + "/control_auto/" + deveui
                createRequest(url, queue, jsonObject)
            }
        }

        startButton.setOnClickListener {
            Log.d("Control", "Start Pump")
            val jsonStartPump = "{\"start\":\"true\"}}"
            val jsonObject = JSONObject(jsonStartPump)
            val url = host + "/start_pump/" + deveui
            createRequest(url, queue, jsonObject)
        }

        stopButton.setOnClickListener {
            Log.d("Control", "Stop Pump")
            val jsonStopPump = "{\"start\":\"false\"}}"
            val jsonObject = JSONObject(jsonStopPump)
            val url = host + "/stop_pump/" + deveui
            createRequest(url, queue, jsonObject)
        }
        return viewLayout
    }

    fun getNetConfig(context: Context): Properties? {
        val resources: Resources = context.getResources()
        try {
            val rawResource: InputStream = resources.openRawResource(R.raw.config)
            val properties = Properties()
            properties.load(rawResource)
            return properties
        } catch (e: Resources.NotFoundException) {
            Log.e(MqttClient.TAG, "Unable to find the config file")
            e.printStackTrace()
        } catch (e: IOException) {
            Log.e(MqttClient.TAG, "Failed to open config file")
            e.printStackTrace()
        }
        return null
    }

    private fun createRequest(url: String, queue: RequestQueue, msg: JSONObject){

        val stringRequest = JsonObjectRequest(Request.Method.POST, url,msg,
            Response.Listener { response ->
                Log.d("Control", "Request performed")
                Log.d("Control", "$response")
            },
            Response.ErrorListener {
                Log.d("Control", "Request error")
            })
        queue.add(stringRequest)
    }
}