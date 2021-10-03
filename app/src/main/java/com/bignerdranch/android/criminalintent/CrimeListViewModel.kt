package com.bignerdranch.android.criminalintent

import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.lifecycle.ViewModel

class CrimeListViewModel : ViewModel() {
//    val crimes = mutableListOf<Crime>()
//    init {
//        for (i in 0 until 100) {
//            val crime = Crime()
//            crime.title = "Crime #$i"
//            crime.isSolved = i % 2 == 0
//            crimes += crime
//        }

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()

    lateinit var textViewEmpty: TextView
    lateinit var buttonAddCrime: Button

    fun getView(view: View) {
        textViewEmpty = view.findViewById(R.id.empty_list_text)
        buttonAddCrime = view.findViewById(R.id.add_crime_button)
    }

    fun addCrime(crime: Crime) {
        crimeRepository.addCrime(crime)
//        textViewEmpty.isInvisible = true
//        buttonAddCrime.isInvisible = true
    }

}