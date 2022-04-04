package com.example.smartbox_app.Adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.smartbox_app.Fragments.ControlFragment
import com.example.smartbox_app.Fragments.IndicatorsFragment

internal class MyAdapter(var context: Context, fm: FragmentManager, var TotalTabs: Int): FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return TotalTabs
    }

    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> {
                IndicatorsFragment()
            }

            1 -> {
                ControlFragment()
            }
            else -> getItem(position)

        }
    }
}