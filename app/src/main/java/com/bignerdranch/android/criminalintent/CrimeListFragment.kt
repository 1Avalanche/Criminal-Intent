package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateFormat
import android.view.*
import android.widget.Button
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {

    /**
     * Требуемый интерфейс
     */
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null
    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: ListAdapter<Crime, CrimeHolder> = CrimeAdapter(emptyList())
    private val showedCrimes = mutableListOf<Crime>()
    private lateinit var textViewEmpty: TextView
    private lateinit var buttonAddCrime: Button

        private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        textViewEmpty = view.findViewById(R.id.empty_list_text)
        buttonAddCrime = view.findViewById(R.id.add_crime_button)
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        crimeListViewModel.getView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            } else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun updateUI(crimes: List<Crime>) {
        if (crimes.isNotEmpty()) {
            textViewEmpty.isInvisible = true
            buttonAddCrime.isInvisible = true
        }
        if (crimes.isEmpty()) {
            textViewEmpty.isVisible = true
            buttonAddCrime.isVisible = true
            buttonAddCrime.setOnClickListener {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
            }

        }

        adapter = CrimeAdapter(crimes)
        crimeRecyclerView.adapter = adapter
        crimeListViewModel.crimeListLiveData.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    showedCrimes -> adapter.submitList(showedCrimes)
                }
        )

    }

    private inner class CrimeHolder (view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

        init {
            itemView.setOnClickListener(this)
        }


        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            val sdf = SimpleDateFormat("dd/MM/yyyy, hh:mm")
            val currentDate = sdf.format(crime.date)
            dateTextView.text = currentDate
            solvedImageView.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
            showedCrimes.add(crime)
            Log.d("ADD", "Crime with title ${crime.title} was added in showCrimes-list" )
        }
    }

    private inner class CrimeDiff : DiffUtil.ItemCallback<Crime>() { //NEW
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }
    }


    private inner class CrimeAdapter(var crimes: List<Crime>) : ListAdapter<Crime, CrimeHolder>(CrimeDiff()) { //NEW



        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            var view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun getItemCount() = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}