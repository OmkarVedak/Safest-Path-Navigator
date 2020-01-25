package edu.asu.cs535.safestpath

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import edu.asu.cs535.safestpath.utils.NetworkService
import edu.asu.cs535.safestpath.utils.crime_types

class ReportCrimeActivity : AppCompatActivity() {

    private lateinit var editTextFilledExposedDropdown: AutoCompleteTextView
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var progress: ProgressBar
    private lateinit var btnReport: MaterialButton
    private lateinit var crimeLocation: TextInputEditText
    private lateinit var crimeLocationInput: TextInputLayout

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_crime)

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        progress = findViewById(R.id.progressReportCrime)
        btnReport = findViewById(R.id.btnReportCrime)
        crimeLocation = findViewById(R.id.edit_crime_location)
        crimeLocationInput = findViewById(R.id.input_crime_location)

        crimeLocation.setOnClickListener { setInputOnClickListener() }
        crimeLocationInput.setEndIconOnClickListener {
            crimeLocationInput.editText!!.setText(getString(R.string.your_location))

            latitude = intent.getDoubleExtra("latitude", 0.0)
            longitude = intent.getDoubleExtra("longitude", 0.0)
        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            R.layout.dropdown_menu_popup_item,
            crime_types
        )
        editTextFilledExposedDropdown = findViewById(R.id.filled_exposed_dropdown)
        editTextFilledExposedDropdown.setAdapter(adapter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
            val place: Place = Autocomplete.getPlaceFromIntent(data!!)
            val latLng = place.latLng!!
            latitude = latLng.latitude
            longitude = latLng.longitude

            crimeLocation.setText(getString(R.string.place_address, place.name, place.address))
        }
    }

    fun reportCrime(view: View) {
        val ctx = this
        progress.visibility = View.VISIBLE
        btnReport.isEnabled = false

        if (latitude != 0.0 && longitude != 0.0) {
            NetworkService().addTopic(latitude, longitude, editTextFilledExposedDropdown.text.toString())
                .onTopicComplete(object : NetworkService.TopicListener {
                    override fun onFail() {
                        ctx.runOnUiThread(Runnable {
                            progress.visibility = View.GONE
                            btnReport.isEnabled = true
                            Toast.makeText(ctx, "Failed to report crime", Toast.LENGTH_SHORT).show()
                        })
                    }

                    override fun onComplete() {
                        progress.visibility = View.GONE
                        btnReport.isEnabled = true
                        Toast.makeText(ctx, "Successfully reported crime", Toast.LENGTH_SHORT).show()
                        navigateUpTo(Intent(baseContext, MainActivity::class.java))
                    }
                })
        }
    }

    private fun setInputOnClickListener() {
        val fields: List<Place.Field> = listOf(
            Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        );

        // Start the autocomplete intent
        val intent: Intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("us")
            .build(this)

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }
}
