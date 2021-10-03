package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File
import java.nio.file.Path

class PhotoDialogFragment(val photoFile: File) : DialogFragment() {

    private lateinit var photoView: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_photo, container, false)
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        val image = getScaledBitmap(photoFile.path, requireActivity())
        photoView.setImageBitmap(image)
        return view

    }


    fun getScaledBitmap(path: String, activity: Activity) : Bitmap {
        val size = Point()
        activity.display?.getRealSize(size)
        return getScaledBitmap(path, size.x, size.y)
    }




}