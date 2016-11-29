package com.iolab.sdtecsynttest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.iolab.sdtecsynttest.utils.GeoAutoCompleteAdapter;
import com.iolab.sdtecsynttest.utils.JSONParser;
import com.iolab.sdtecsynttest.utils.PermissionUtils;
import com.iolab.sdtecsynttest.utils.PlaceAutocompleteAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.iolab.sdtecsynttest.R.id.btn_nav_toggle_clear_search;
import static com.iolab.sdtecsynttest.R.id.geo_autocomplete;
import static com.iolab.sdtecsynttest.R.id.map;

public class Main extends AppCompatActivity implements
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnCameraChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        PlaceSelectionListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int DEFAULT_ZOOM = 15;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final LatLng tecsynt = new LatLng(50.0063028, 36.2366641);

    private GoogleMap mMap;
    private boolean mapReady = false;
    int curMapType = GoogleMap.MAP_TYPE_NORMAL;
    private Marker mSelectedMarker;
    private boolean mPermissionDenied = false;
    private UiSettings mUiSettings;
    private GoogleApiClient mGoogleApiClient;
    private Animation inAnimation, outAnimation, inHalfAnimation, outHalfAnimation, rotateLeft, rotateRight;
    private boolean mNavBarVisibible = false;
    private boolean mBtnSetNorthDirectionVisibible = true;
    private boolean searchMode = false;
    private Integer THRESHOLD = 2;
    private PlaceAutocompleteAdapter mAdapter;
    // declaration of web-accesss adapter
//    private GooglePlacesAutocompleteAdapter mAdapter;
    private LatLngBounds mBounds;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Location mSavedLocation;
    private CameraPosition mCameraPosition;
    private Activity mActivity;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private SettingsHelper mSettingsHelper;
    private Polyline line;
    private String distance;
    private String duration;

    @BindView(R.id.navigation_panel)
    View mNavBar;

    @BindView(R.id.btn_set_north_direction)
    View mBtnSetNorthDirection;

    @BindView(R.id.btn_nav_toggle_clear_search)
    AppCompatImageView mBtnNavToggleClearSearch;

    @BindView(geo_autocomplete)
    com.iolab.sdtecsynttest.DelayAutoCompleteTextView mAutoCompleteTextView;

    @BindView(R.id.map)
    View mapView;

    @BindView(R.id.route_dialog)
    View routeDialog;

    @BindView(R.id.distance_value)
    TextView distanceValue;

    @BindView(R.id.duration_value)
    TextView durationValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mSavedLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        mActivity = this;
        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        setUpAnimation();
        setUpAutocompleteTextViewFocusBehavior();
        buildGoogleApiClient();
    }

    private void setUpAutocompleteTextViewFocusBehavior() {
        mAutoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_close);
                    mAutoCompleteTextView.setCursorVisible(true);
                    mAutoCompleteTextView.setFocusableInTouchMode(true);
                    searchMode = true;
                } else {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_menu_right);
                    mAutoCompleteTextView.setCursorVisible(false);
                    mAutoCompleteTextView.setFocusable(false);
                    searchMode = false;
                }
            }
        });
        mAutoCompleteTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mAutoCompleteTextView.setFocusableInTouchMode(true);
                return false;
            }
        });
    }

    private void setUpAnimation() {
        inAnimation = new AlphaAnimation(0, 1);
        inAnimation.setDuration(500);
        outAnimation = new AlphaAnimation(1, 0);
        outAnimation.setDuration(500);
        inHalfAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_half_in);
        outHalfAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_half_out);
        rotateLeft = AnimationUtils.loadAnimation(this, R.anim.rotate_left);
        rotateRight = AnimationUtils.loadAnimation(this, R.anim.rotate_right);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapReady = true;
        mMap = googleMap;

        mSettingsHelper = new SettingsHelper(this);
        if(mSettingsHelper.hasMapState())
            mSettingsHelper.loadMapState();

        enableMyLocation();
        //first config
        configPlacesCoder();
        setUpPlacesCoder();
        //setUpGeoCoder();
     /*
     * Set the map's camera position to the saved or current location of the device.
     * If the previous state was saved, set the position to the saved state.
     * If the current location is unknown, use a default position and zoom value.
     */
        if(mSettingsHelper.hasMapState()){
            mSettingsHelper.loadMapState();
        }
        else if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mSavedLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mSavedLocation.getLatitude(),
                            mSavedLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tecsynt, DEFAULT_ZOOM));
        }

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnCameraChangeListener(this);

        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setMyLocationButtonEnabled(false);

        mMap.addMarker(new MarkerOptions().position(tecsynt).title("Tecsynt"));
    }

    @OnClick(btn_nav_toggle_clear_search)
    public void toggleNavBar(AppCompatImageView img) {
        if(!searchMode) {
            if (mNavBarVisibible) {
                mNavBar.startAnimation(outAnimation);
                mNavBar.setVisibility(View.GONE);
                mNavBarVisibible = false;

                mBtnNavToggleClearSearch.startAnimation(rotateLeft);
            } else {
                mNavBar.setVisibility(View.VISIBLE);
                mNavBar.startAnimation(inAnimation);
                mNavBarVisibible = true;

                mBtnNavToggleClearSearch.startAnimation(rotateRight);
            }
        }
        else{
            mAutoCompleteTextView.setText("");
        }
    }

    @OnClick(R.id.btn_map_type)
    public void changeMapType(View cardView) {
        if (mapReady && curMapType == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            curMapType = GoogleMap.MAP_TYPE_HYBRID;
        } else if (mapReady && curMapType == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            curMapType = GoogleMap.MAP_TYPE_NORMAL;
        } else {
            Snackbar.make(findViewById(map), "Map is not ready", Snackbar.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.btn_locate_me)
    public void locateMe(CardView cardView) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(map), "Permission are missing", Snackbar.LENGTH_LONG).show();
            return;
        }
        checkGPSstatus();

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (gps_enabled && network_enabled) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            LatLng latLng = new LatLng(latitude, longitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            mMap.animateCamera(cameraUpdate);
        }
    }

    private void checkGPSstatus() {
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("GPS network not enabled");
            dialog.setPositiveButton("Open location settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    mActivity.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                }
            });
            dialog.show();
        }
    }

    @OnClick(R.id.btn_set_north_direction)
    public void setNorthDirection(View cardView){
        CameraPosition cameraPosition = mMap.getCameraPosition();
        cameraPosition = new CameraPosition.Builder(cameraPosition)
                .bearing(0)                // Sets the orientation of the camera to east
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if(cameraPosition.bearing != 0 && !mBtnSetNorthDirectionVisibible){
            mBtnSetNorthDirection.startAnimation(inHalfAnimation);
            mBtnSetNorthDirectionVisibible = true;
        }
        else if(cameraPosition.bearing == 0 && mBtnSetNorthDirectionVisibible){
            mBtnSetNorthDirection.startAnimation(outHalfAnimation);
            mBtnSetNorthDirectionVisibible = false;
        }
        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }

    //second places provider. unused
    private void setUpGeoCoder(){
        mAutoCompleteTextView = (DelayAutoCompleteTextView) findViewById(geo_autocomplete);
        mAutoCompleteTextView.setThreshold(THRESHOLD);
        mAutoCompleteTextView.setAdapter(new GeoAutoCompleteAdapter(this));

        mAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                GeoSearchResult result = (GeoSearchResult) adapterView.getItemAtPosition(position);
                mAutoCompleteTextView.setText(result.getAddress());
            }
        });

        mAutoCompleteTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0)
                {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_close);
                    searchMode = true;
                }
                else
                {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_menu_right);
                    searchMode = false;
                }
            }
        });
    }

    private void configPlacesCoder(){
        AutocompleteFilter filter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT)
                .build();

        //setup mBounds
        onCameraChange(mMap.getCameraPosition());
        mBounds = new LatLngBounds(new LatLng(49.91, 36.13), new LatLng(50.052781, 36.33));
        mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, mBounds, filter);

        //adapter for web access
//        mAdapter = new GooglePlacesAutocompleteAdapter(this);

        mAutoCompleteTextView.setAdapter(mAdapter);
    }
    private void setUpPlacesCoder(){
        mAutoCompleteTextView.setThreshold(THRESHOLD);

        mAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//                final AutocompletePrediction item = mAdapter.getItem(position);
                final AutocompletePrediction item = (AutocompletePrediction) parent.getItemAtPosition(position);
                final String placeId = item.getPlaceId();
                final CharSequence primaryText = item.getPrimaryText(null);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

                Toast.makeText(getApplicationContext(), "Clicked: " + primaryText,
                        Toast.LENGTH_SHORT).show();
            }
        });
        mAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0)
                {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_close);
                    searchMode = true;
                }
                else
                {
                    mBtnNavToggleClearSearch.setImageResource(R.mipmap.ic_menu_right);
                    searchMode = false;
                }
            }
        });
    }

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
           }
            final Place place = places.get(0);
            mAutoCompleteTextView.setText(place.getName() + ", " +
                    place.getAddress() + ", " + place.getPhoneNumber() +
                    ", " + place.getWebsiteUri());

            String attributions = (String) place.getAttributions();
            String snippet = (String) place.getAddress();
            if (attributions != null) {
                snippet = snippet + "\n" + attributions;
            }

            mMap.addMarker(new MarkerOptions()
                    .position(place.getLatLng())
                    .title((String) place.getName())
                    .snippet(snippet));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_ZOOM));

            mSettingsHelper.saveMarker(place.getLatLng(),(String) place.getName(), snippet);

            places.release();
        }
    };

    @OnClick(R.id.btn_search)
    public void updateMarkers(View view) {
        if (mMap == null) {
            return;
        }

        mSettingsHelper.clear();

        //wait adapter to load data
        new Thread() {
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                    for (int i=0; i<mAdapter.getCount(); i++){
                        AutocompletePrediction item = mAdapter.getItem(i);
                        final String placeId = item.getPlaceId();

                        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                                .getPlaceById(mGoogleApiClient, placeId);
                        placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                    }
                } catch (InterruptedException e) {
                }
            }
        }.start();

//  Below is Google Search for place according place type, but not query

//        @SuppressWarnings("MissingPermission")
//        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
//                .getCurrentPlace(mGoogleApiClient, null);
//            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
//            @Override
//            public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
//                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
//                    // Add a marker for each place near the device's current location, with an
//                    // info window showing place information.
//                    String attributions = (String) placeLikelihood.getPlace().getAttributions();
//                    String snippet = (String) placeLikelihood.getPlace().getAddress();
//                    if (attributions != null) {
//                        snippet = snippet + "\n" + attributions;
//                    }
//
//                    mMap.addMarker(new MarkerOptions()
//                            .position(placeLikelihood.getPlace().getLatLng())
//                            .title((String) placeLikelihood.getPlace().getName())
//                            .snippet(snippet));
//                }
//                // Release the place likelihood buffer.
//                likelyPlaces.release();
//            }
//        });
    }

    @Override
    public void onMapClick(final LatLng point) {
        // Any showing info window closes when the map is clicked.
        // Clear the currently selected marker.
        if(mSelectedMarker != null){
            mSelectedMarker.setIcon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        mSelectedMarker = null;

        mAutoCompleteTextView.clearFocus();
        mapView.requestFocus();

        if (mapView != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mapView.getWindowToken(), 0);
        }

        routeDialog.setVisibility(View.GONE);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (marker.equals(mSelectedMarker)) {
            mSelectedMarker.setIcon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mSelectedMarker = null;
            return true;
        }

        if(mSelectedMarker != null){
            mSelectedMarker.setIcon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        mSelectedMarker = marker;
        mSelectedMarker.setIcon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            LatLng latLng = new LatLng(latitude, longitude);
            new GetDirectionsAsync().execute(marker.getPosition(), latLng);
        }
        else{
            Toast.makeText(this.getApplicationContext(), "Current location is unknown. Click locate me button", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_COARSE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this.getApplicationContext(), "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this.getApplicationContext(), "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, connectionResult.RESOLUTION_REQUIRED);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("TAG", "Location services connection failed with code==>" + connectionResult.getErrorCode());
            Log.e("TAG", "Location services connection failed Because of==> " + connectionResult.getErrorMessage());
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPlaceSelected(Place place) {

    }

    @Override
    public void onError(Status status) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onStop() {
        mSettingsHelper.saveMapState();
        super.onStop();
    }


    @Override
    protected void onResume() {
        super.onResume();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    public class SettingsHelper{

        public static final String APP_PREF = "mysettings";
        public static final String APP_PREF_MAP_CAMERA_POSITION = "mapCameraPosition";

        public static final String APP_PREF_MARKER_COUNT = "MarkerCount";

        public static final String APP_PREF_MARKER_LAT = "markerLat";
        public static final String APP_PREF_MARKER_LNG = "markerLng";
        public static final String APP_PREF_MARKER_TITLE = "markerTitle";
        public static final String APP_PREF_MARKER_SNIPPET = "MarkerSnippet";

        private Context mContext;
        private SharedPreferences mSharedPreferences;

        public SettingsHelper(Context context) {
            this.mContext = context;
            this.mSharedPreferences = mContext.getSharedPreferences(APP_PREF, Context.MODE_PRIVATE);

        }

        public boolean hasMapState(){
            String pos = mSharedPreferences.getString(APP_PREF_MAP_CAMERA_POSITION, "");
            if (!pos.equals("")){
                return true;
            }
            return false;
        }

        public void loadMapState(){

            for(int i=1; i<=mSharedPreferences.getInt(APP_PREF_MARKER_COUNT, 0); i++){

                LatLng latLng = new LatLng(
                        Double.valueOf(mSharedPreferences.getString(APP_PREF_MARKER_LAT + i, "0")),
                        Double.valueOf(mSharedPreferences.getString(APP_PREF_MARKER_LNG + i, "0"))
                        );
                String title = mSharedPreferences.getString(APP_PREF_MARKER_TITLE + i, "0");
                String snippet = mSharedPreferences.getString(APP_PREF_MARKER_SNIPPET + i, "0");

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(title)
                        .snippet(snippet));
            }

            String json = mSharedPreferences.getString(APP_PREF_MAP_CAMERA_POSITION, "");

            JSONObject obj = null;
            try {
                obj = new JSONObject(json);
                if(obj != null) {
                    JSONObject latLngObj = obj.getJSONObject("target");

                    LatLng latLng = new LatLng(
                            Double.valueOf(latLngObj.getString("latitude")),
                            Double.valueOf(latLngObj.getString("longitude")));

                    CameraPosition cameraPosition = new CameraPosition(latLng,
                            Float.valueOf(obj.getString("zoom")),
                            Float.valueOf(obj.getString("tilt")),
                            Float.valueOf(obj.getString("bearing"))
                    );

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            } catch (Throwable t) {
                Log.e("My App", "Could not parse malformed JSON: \"" + json + "\"");
            }
        }

        public void clear() {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.clear();
            editor.apply();
        }

        public void saveMapState(){
            SharedPreferences.Editor editor = mSharedPreferences.edit();

            Gson gson = new Gson();
            String json = gson.toJson(mMap.getCameraPosition());
            editor.putString(APP_PREF_MAP_CAMERA_POSITION, json);
            editor.apply();
        }

        public void saveMarker(LatLng latLng, String name, String snippet){

            int count = mSharedPreferences.getInt(APP_PREF_MARKER_COUNT, 0);
            int number = 1;
            if(count != 0) number = count + 1;

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(APP_PREF_MARKER_LAT + number, String.valueOf(latLng.latitude));
            editor.putString(APP_PREF_MARKER_LNG + number, String.valueOf(latLng.longitude));
            editor.putString(APP_PREF_MARKER_TITLE + number, name);
            editor.putString(APP_PREF_MARKER_SNIPPET + number, snippet);

            editor.putInt(APP_PREF_MARKER_COUNT, number);

            editor.apply();
        }
    }

    class GetDirectionsAsync extends AsyncTask<LatLng, Void, List<LatLng>> {

        JSONParser jsonParser;
        String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<LatLng> doInBackground(LatLng... params) {
            LatLng start = params[0];
            LatLng end = params[1];

            HashMap<String, String> points = new HashMap<>();
            points.put("origin", start.latitude + "," + start.longitude);
            points.put("destination", end.latitude + "," + end.longitude);

            jsonParser = new JSONParser();

            JSONObject obj = jsonParser.makeHttpRequest(DIRECTIONS_URL, "GET", points, true);

            if (obj == null) return null;

            try {
                List<LatLng> list = null;

                JSONArray routeArray = obj.getJSONArray("routes");
                JSONObject routes = routeArray.getJSONObject(0);
                JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                String encodedString = overviewPolylines.getString("points");
                list = decodePoly(encodedString);

                JSONArray newTempARr = routes.getJSONArray("legs");
                JSONObject newDisTimeOb = newTempARr.getJSONObject(0);

                JSONObject distOb = newDisTimeOb.getJSONObject("distance");
                JSONObject timeOb = newDisTimeOb.getJSONObject("duration");

                distance = distOb.getString("text");
                duration = timeOb.getString("text");

                return list;

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<LatLng> pointsList) {

            if (pointsList == null) return;

            if (line != null){
                line.remove();
            }

            PolylineOptions options = new PolylineOptions().width(5).color(Color.MAGENTA).geodesic(true);
            for (int i = 0; i < pointsList.size(); i++) {
                LatLng point = pointsList.get(i);
                options.add(point);
            }
            line = mMap.addPolyline(options);

            showDialog();
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void showDialog() {
        routeDialog.setVisibility(View.VISIBLE);
        distanceValue.setText(distance);
    }

    @OnClick(R.id.legs)
    public void legs(Button button){
        String shortDistance = distance.substring(0, distance.indexOf(" "));
        durationValue.setText(String.valueOf(Integer.parseInt(shortDistance)/5));
    }
    @OnClick(R.id.bike)
    public void bike(Button button){
        String shortDistance = distance.substring(0, distance.indexOf(" "));
        durationValue.setText(String.valueOf(Integer.parseInt(shortDistance)/20));
    }
    @OnClick(R.id.car)
    public void car(Button button){
        String shortDistance = distance.substring(0, distance.indexOf(" "));
        durationValue.setText(String.valueOf(Integer.parseInt(shortDistance)/70));
    }
}
