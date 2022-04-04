package com.example.smartbox_app.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.smartbox_app.MqttClient
import com.example.smartbox_app.R



/**
 * A simple [Fragment] subclass.
 * Use the [IndicatorsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IndicatorsFragment : Fragment() {
    private lateinit var viewLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Indicators", "Indicators Fragment created")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewLayout = inflater.inflate(R.layout.fragment_indicators, container, false)

        val appContext = requireContext().applicationContext
        val mqttClient = MqttClient(appContext, viewLayout)
        mqttClient.connect()


        return viewLayout
    }


}