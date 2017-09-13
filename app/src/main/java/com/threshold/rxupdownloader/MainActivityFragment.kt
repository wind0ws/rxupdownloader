package com.threshold.rxupdownloader

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main.*


class MainActivityFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.offscreenPageLimit=2
        viewPager.adapter = MainActivityFragmentPageAdapter(arrayOf(DownloadFragment(),UploadFragment()),fragmentManager)
        tabLayout.setupWithViewPager(viewPager)
    }
}
