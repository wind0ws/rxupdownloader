package com.threshold.rxupdownloader

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter


class MainActivityFragmentPageAdapter(private val fragments:Array<Fragment> ,fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment = fragments[position]

    override fun getCount(): Int = fragments.size

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "DownloadFragment"
            1 -> "UploadFragment"
            else -> throw IllegalStateException("bad position: $position")
        }
    }

}