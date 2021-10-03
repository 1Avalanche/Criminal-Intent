package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog.show
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.format.DateFormat.getDateFormat
import android.util.Log
import android.view.Gravity.apply
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat.apply
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

private const val ARG_CRIME_ID = "crime_id"
private const val TAG = "CrimeFragment"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM d, ''yy"
private const val REQUEST_CONTACT = 1

class CrimeFragment : Fragment(),  DatePickerFragment.Callbacks {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var pickContactIntent: Intent
    private lateinit var pickContactIntentCall: Intent
    private lateinit var pickConcactIntentPhoto: Intent
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var launcherCall: ActivityResultLauncher<Intent>? = null
    private var launcherPhoto: ActivityResultLauncher<Intent>? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)

        //интент на выбор человека в списке контактов
        pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = Intent()
                val backIntent = result.data ?: intent
                backIntentDataHandle(backIntent)
                }
            }

        //интент на получение номера телефона
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launcherCall?.launch(pickContactIntentCall)
            } else { //explain to user requesting permissions }
            }
        }
        pickContactIntentCall = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        launcherCall = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                var intent = Intent()
                var backIntent = result.data ?: intent
                pickPhoneNumber(backIntent)
            }
        }

        //работа с камерой
        launcherPhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                requireActivity().revokeUriPermission(photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callButton = view.findViewById(R.id.call_to_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView



        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer { crime ->
                    crime?.let {
                        this.crime = crime
                        photoFile = crimeDetailViewModel.getPhotoFile(crime)
                        photoUri = FileProvider.getUriForFile(requireActivity(),
                                "com.bignerdranch.android.criminalintent.fileprovider",
                                photoFile)
                        updateUI()
                    }
                }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Это пространство оставлено пустым специально
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                //и это
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport()) //текст сообщения
                putExtra(
                        Intent.EXTRA_SUBJECT, //тема сообщения
                        getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }


        suspectButton.setOnClickListener {
            launcher?.launch(pickContactIntent)
        }

        callButton.setOnClickListener {

            when {
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> launcherCall?.launch(pickContactIntentCall)
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)}
            }
        }


        photoButton.setOnClickListener {
            pickConcactIntentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pickConcactIntentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                    packageManager.resolveActivity(pickConcactIntentPhoto,
                            PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                photoButton.isEnabled = false
            }
            val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(pickConcactIntentPhoto,
                            PackageManager.MATCH_DEFAULT_ONLY)
            for (cameraActivity in cameraActivities) {
                requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            launcherPhoto?.launch(pickConcactIntentPhoto)
        }

        photoView.setOnClickListener {
            val photoFragment = PhotoDialogFragment(photoFile)
            photoFragment.show(childFragmentManager, "PhotoFragmentDialog")
        }

    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }

        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription =
                    getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription =
                    getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun backIntentDataHandle(data: Intent) {
            val contactUri: Uri? = data.data
            // Указать, для каких полей ваш запрос должен возвращать значения.
            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            // Выполняемый здесь запрос — contactUri похож на предложение "where"
            if (contactUri != null) {
                val cursor = requireActivity().contentResolver
                        .query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }
                    // Первый столбец первой строки данных —
                    // это имя вашего подозреваемого.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
                cursor?.close()
            }
    }

    private fun pickPhoneNumber(data: Intent) {
        val contactUri: Uri? = data.data
        val queryFields = ContactsContract.CommonDataKinds.Phone._ID
        if (contactUri != null) {
            val cursor = requireActivity().contentResolver
                    .query(contactUri, null, queryFields, null, null)
            cursor.use {
                if (it?.count == 0) {
                    return
                }
                it?.moveToFirst()
                val number = it?.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                val dialNumber = Intent(Intent.ACTION_CALL)
                dialNumber.data = Uri.parse("tel: $number")
                startActivity(dialNumber)

            }
            cursor?.close()
        }
    }

    private fun getCrimeReport() : String { //возвращается длинную строку с отчетом-описанием преступления
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report,
                crime.title, dateString, solvedString, suspect)
    }


    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }

    }
}