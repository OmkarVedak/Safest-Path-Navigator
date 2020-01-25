package edu.asu.cs535.safestpath

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import edu.asu.cs535.safestpath.utils.updateUserPreferred

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var txtPreferredLocation: TextView

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            val key: String = preference!!.key
            if (key == "preferred_location") {
                Log.d("safestPath", "Location clicked")
                setInputOnClickListener()

                return true
            }
            return super.onPreferenceTreeClick(preference)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                val latLng = place.latLng!!
                val latitude = latLng.latitude
                val longitude = latLng.longitude
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                with (sharedPref.edit()) {
                    putFloat("preferred_location_lat", latitude.toFloat())
                    putFloat("preferred_location_long", longitude.toFloat())
                    putString("preferred_location_address", getString(R.string.place_address, place.name, place.address))
                    commit()
                }
                updateUserPreferred(latitude, longitude)
                val editTextPreference: EditTextPreference? = findPreference("preferred_location_address")
                editTextPreference!!.text = getString(R.string.place_address, place.name, place.address)
            }
        }

        private fun setInputOnClickListener() {
            val fields: List<Place.Field> = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            );

            // Start the autocomplete intent
            val intent: Intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                .setCountry("us")
                .build(context!!)

            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }
    }
}