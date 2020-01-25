package edu.asu.cs535.safestpath

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Task
import com.google.android.gms.location.LocationServices
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.iid.FirebaseInstanceId
import com.google.maps.android.PolyUtil
import edu.asu.cs535.safestpath.models.Crime
import edu.asu.cs535.safestpath.models.MapDirection
import edu.asu.cs535.safestpath.services.LocationUpdatesService
import edu.asu.cs535.safestpath.utils.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnPolylineClickListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mPlacesClient: PlacesClient
    private lateinit var mapView: View
    private lateinit var mDestInput: TextInputEditText
    private lateinit var mSourceInput: TextInputEditText
    private lateinit var mSourceInputText: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var mTextGeoTime: TextView

    private lateinit var currentLatLng: LatLng
    private lateinit var sourceLatLng: LatLng
    private lateinit var destLatLng: LatLng
    private lateinit var locationCallback: LocationCallback
    private var requestingLocationUpdates: Boolean = false
    private lateinit var locationRequest: LocationRequest
    private lateinit var token: String
    private var mService: LocationUpdatesService? = null
    private lateinit var myReceiver: MyReceiver

    private lateinit var polyline: Polyline
    private lateinit var path: String

    private lateinit var startPos: Marker
    private lateinit var endPos: Marker

    private lateinit var pref: SharedPreferences
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    private var ctx = this

    companion object {
        private const val LOCATION_REQUEST_CODE = 100
        private const val LOCATION_SETTINGS_REQUEST_CODE = 200
        private const val AUTOCOMPLETE_REQUEST_CODE_SOURCE = 300
        private const val AUTOCOMPLETE_REQUEST_CODE_DEST = 400
        private const val RC_SIGN_IN = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myReceiver = MyReceiver()

        createNotificationChannel(this)
        registerFirebaseToken()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapView = mapFragment!!.view!!;
        mapFragment.getMapAsync(this)

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (pref.getBoolean("notification_safety", false)) {
            if (mService != null) {
                mService!!.requestLocationUpdates()
            }
            subscribeToTopic("safety")
        }

        prefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == "traffic" && ::mMap.isInitialized) {
                    mMap.isTrafficEnabled = sharedPreferences.getBoolean(key, false)
                }
                if (key == "crimes" && ::mMap.isInitialized) {
                    val crimes = sharedPreferences.getBoolean(key, false)
                    if (crimes) {
                        getNearbyCrimes()
                    } else {
                        mMap.clear()
                    }
                }
                if (key == "notification_safety") {
                    val subscribe = sharedPreferences.getBoolean(key, false)
                    if (subscribe) {
                        if (mService != null) {
                            mService!!.requestLocationUpdates()
                        }
                        subscribeToTopic("safety")
                    } else {
                        if (mService != null) {
                            mService!!.removeLocationUpdates()
                        }
                        unSubscribeTopic("safety")
                    }
                } else if (key == "notification_preferred") {
                    val subscribe = sharedPreferences.getBoolean(key, false)
                    if (subscribe) {
                        subscribeToTopic("preferred")
                    } else {
                        unSubscribeTopic("preferred")
                    }
                }
            }
        pref.registerOnSharedPreferenceChangeListener(prefListener)

        mDestInput = findViewById(R.id.edit_destination)
        mSourceInput = findViewById(R.id.edit_source)
        progressBar = findViewById(R.id.loadingBar)
        mTextGeoTime = findViewById(R.id.text_geo_time)

        mSourceInputText = findViewById(R.id.input_source)
        mSourceInputText.setEndIconOnClickListener {
            mSourceInputText.editText!!.setText(getString(R.string.your_location))
            sourceLatLng = currentLatLng
            if (::destLatLng.isInitialized) {
                getDirections()
            }
        }

        initPlaces()

        registerForContextMenu(mSourceInput)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isTrafficEnabled = pref.getBoolean("traffic", false)
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnPolylineClickListener(this)

        // Set "Current location" button to bottom
        setLocationButton(mapView)

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_REQUEST_CODE && permissions.size == 3 &&
            permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
            permissions[1] == Manifest.permission.ACCESS_COARSE_LOCATION &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("safestPath", "Permission granted")
            buildGoogleApiClient()
            mMap.isMyLocationEnabled = true

        } else {
            Toast.makeText(this, "Location permission denied!! Can't proceed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("safestPath", "Got some result")
            mGoogleApiClient.reconnect()
        } else if (
            (requestCode == AUTOCOMPLETE_REQUEST_CODE_SOURCE && resultCode == RESULT_OK) ||
            (requestCode == AUTOCOMPLETE_REQUEST_CODE_DEST && resultCode == RESULT_OK)
        ) {
            val place: Place = Autocomplete.getPlaceFromIntent(data!!)

            if (requestCode == AUTOCOMPLETE_REQUEST_CODE_SOURCE) {
                sourceLatLng = place.latLng!!
                mSourceInput.setText(getString(R.string.place_address, place.name, place.address))
            } else {
                destLatLng = place.latLng!!
                mDestInput.setText(getString(R.string.place_address, place.name, place.address))
            }

            getDirections()
        } else if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                updateUser(token)

                Log.d("safestPath", "success login")
            } else {
                if (response !== null) {
                    val code = response.error!!.errorCode
                    Log.d("safestPath", "Error code ${code}")
                } else {
                    Log.d("safestPath", "User cancelled sigin")
                }
            }
        }
    }

    override fun onConnected(bundle: Bundle?) {
        mLocationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        builder.setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            Log.d("safestPath", "Task successful")
            setLocationOnMap()
        }
        task.addOnFailureListener { exception ->
            Log.d("safestPath", "Task failure")
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        LOCATION_SETTINGS_REQUEST_CODE
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onInfoWindowClick(marker: Marker?) {
        val crime: Crime = marker!!.tag as Crime
        val body: String =
            "Crime info:\n\nType: ${crime.type}\nIncident Time: ${crime.time}\nLocation: ${crime.latitude},${crime.longitude}"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Crime information")
        intent.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(intent, "Share using"))
    }

    override fun onPolylineClick(polyline: Polyline?) {
        path = PolyUtil.encode(polyline!!.points)
        openContextMenu(mSourceInput)
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu);
        return true
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.polyline_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_report -> {

                NetworkService().reportNearby(path)
                    .onNearbyComplete(object : NetworkService.UnsafeListener {
                        override fun onComplete() {
                            ctx.runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Successfully reported path as unsafe",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }

                        override fun onFail() {
                            ctx.runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Could not report path as unsafe!! Try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_login) {
            createSignInIntent()
            return true
        } else if (id == R.id.action_debug) {
            val intent = Intent(this, BrowseCrimeActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_logout) {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT)
                        .show()
                }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::pref.isInitialized) {
            pref.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, LocationUpdatesService::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver)
        stopLocationUpdates()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )
    }

    override fun onStop() {
        unbindService(mServiceConnection)
        super.onStop()
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun getDirections() {
        if (::sourceLatLng.isInitialized && ::destLatLng.isInitialized) {
            progressBar.visibility = View.VISIBLE
            NetworkService().getMapDirections(
                getLatLngString(sourceLatLng),
                getLatLngString(destLatLng)
            )
                .onDirectionComplete(object : NetworkService.DirectionListener {
                    override fun onFail() {
                        ctx.runOnUiThread {
                            progressBar.visibility = View.GONE
                        }
                    }

                    override fun onComplete(directions: Array<MapDirection>) {
                        ctx.runOnUiThread {
                            val safestPath = directions.maxBy { it.score }!!
                            val coordinates = PolyUtil.decode(safestPath.polyLine!!.points)

                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    getLatLngBound(
                                        safestPath.bounds!!
                                    ), 40
                                )
                            )
                            if (::polyline.isInitialized) {
                                polyline.remove()
                            }
                            if (::startPos.isInitialized && ::endPos.isInitialized) {
                                startPos.remove()
                                endPos.remove()
                            }
                            startPos = mMap.addMarker(MarkerOptions().position(sourceLatLng))
                            endPos = mMap.addMarker(MarkerOptions().position(destLatLng))
                            polyline = mMap.addPolyline(
                                getPolylineOptions(coordinates, ctx)
                            )
                            progressBar.visibility = View.GONE
                        }
                    }
                })
        }
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this).build()
        mGoogleApiClient.connect()
    }

    private fun setLocationOnMap() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000

        requestingLocationUpdates = true
        startLocationUpdates()

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                Log.d("safestPath", "Success request")
                if (location == null) {
                    Log.d("safestPath", "Unknown current location! requesting again...")
                    Handler().postDelayed({
                        mGoogleApiClient.reconnect()
                    }, 1000)
                } else {
                    Log.d("safestPath", location.latitude.toString())
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    sourceLatLng = currentLatLng
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.0f))

                    mDestInput.setOnClickListener {
                        setInputOnClickListener("destination")
                    }
                    mSourceInput.setOnClickListener {
                        setInputOnClickListener("source")
                    }

                    // Browser nearby crimes
                    if (pref.getBoolean("crimes", false)) {
                        getNearbyCrimes()
                    }
                }
            }
    }

    private fun startLocationUpdates() {

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Update UI with location data
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    mTextGeoTime.text = getString(
                        R.string.geo_time,
                        "${location.latitude}, ${location.longitude}"
                    )
                }
            }
        }

        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun setInputOnClickListener(type: String) {
        val fields: List<Place.Field> = listOf(
            Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        );

        // Start the autocomplete intent
        val intent: Intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY, fields
        )
            .setCountry("us")
            .build(this)

        if (type == "source") {
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_SOURCE)
        } else {
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_DEST)
        }
    }


    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            == PackageManager.PERMISSION_DENIED
        ) {
            // Requesting permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                requestCode
            )
        } else {
            buildGoogleApiClient()
            mMap.isMyLocationEnabled = true
        }
    }

    private fun initPlaces() {
        Places.initialize(this, getString(R.string.google_maps_key))
        mPlacesClient = Places.createClient(this)
    }

    private fun registerFirebaseToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("safestPath", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                token = task.result?.token!!
            })
    }

    private fun createSignInIntent() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    fun reportCrime(view: View) {
        val intent = Intent(this, ReportCrimeActivity::class.java)
        intent.putExtra("latitude", currentLatLng.latitude)
        intent.putExtra("longitude", currentLatLng.longitude)
        startActivity(intent)
    }

    private fun getNearbyCrimes() {
        mMap.clear()
        NetworkService().getNearby(
            currentLatLng.latitude, currentLatLng.longitude, 100.0
        ).onNearbyComplete(object : NetworkService.NearbyListener {
            override fun onComplete(crimes: Array<Crime>) {
                ctx.runOnUiThread {
                    crimes.forEach { crime ->
                        val markerOptions = MarkerOptions()
                        markerOptions.position(LatLng(crime.latitude, crime.longitude))
                            .icon(getBitmapDescriptorFromVector(R.drawable.ic_crime_red, ctx))
                            .zIndex(1.0f)
                            .title(crime.type)

                        val crimeWindow = CrimeInfoAdapter(ctx)
                        mMap.setInfoWindowAdapter(crimeWindow)

                        val marker = mMap.addMarker(markerOptions)
                        marker.tag = crime
                    }
                }
            }

            override fun onFail() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            val binder: LocationUpdatesService.LocalBinder =
                service as LocationUpdatesService.LocalBinder
            mService = binder.service;
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mService = null
        }
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location: Location =
                intent!!.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION)!!
        }
    }
}
