package com.mybox.mytracker.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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
import com.google.maps.android.SphericalUtil;
import com.mybox.mytracker.R;
import com.mybox.mytracker.collections.MyLocation;
import com.mybox.mytracker.collections.User;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.WeakHashMap;

public class MapsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnInfoWindowCloseListener {

    private final String TAG = "MapsActivity";
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LatLng mLastPosition;
    private UiSettings mUiSettings;
    private GoogleMap mMap;
    private Marker mMyMarker;
    private WeakHashMap<Marker, String> mMarkers = new WeakHashMap<Marker, String>();
    private List<LatLng> mLatLngs = new ArrayList<>();
    private ParseUser mUser;
    private LatLngBounds bounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /*Set context */
        mContext = this;

        mUser = ParseUser.getCurrentUser();

        if (mUser == null) {
            Intent loginActivity = new Intent(mContext, LoginActivity.class);
            startActivity(loginActivity);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Zoom out", Snackbar.LENGTH_LONG)
                        .setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

                            }
                        }).show();

                moveCameraToLastPosition();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView emailTextview = (TextView) navigationView.getHeaderView(0).findViewById(R.id.email);
        TextView nameTextview = (TextView) navigationView.getHeaderView(0).findViewById(R.id.name);

        if (mUser != null) {

            emailTextview.setText(mUser.getEmail());
            nameTextview.setText(mUser.getUsername());
        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a mMyMarker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mUiSettings = mMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMapToolbarEnabled(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // Should we show an explanation?
            ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

        } else {

            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                String markerId = mMarkers.get(marker);
                return false;//don't show the info-windowns
            }
        });

        // Setting an info window adapter allows us to change the both the contents and look of the
        // info window.
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Set listeners for marker events.  See the bottom of this class for their behavior.
        //mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        if (mMap != null && mLastLocation != null) {
            putPositionToMap();
            setRelationshipMarker("FRIENDS");
            setcurrentMarker();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }


        } else {

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
        }
    }

    private void setcurrentMarker() {

        LatLng myLocation = mLatLngs.get(mLatLngs.size() - 1);
        mLastPosition = myLocation;

        Geocoder geocoder = new Geocoder(mContext);
        List<Address> addresses = null;
        String addressText = null;
        try {
            addresses = geocoder.getFromLocation(myLocation.latitude, myLocation.longitude, 1);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);

            addressText = String.format("%s %s %s",
                    address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) + "," : "",
                    address.getMaxAddressLineIndex() > 1 ? address.getAddressLine(1) + "," : "",
                    address.getCountryName());
        }


        mMyMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_home))
                .anchor(0.0f, 1.0f) // Anchors the mMyMarker on the bottom left
                .position(myLocation)
                .title("Sang Nguyen")
                .snippet("Dang di choi nhe"));

        if (addressText != null) {
            mMyMarker.setSnippet(addressText);
        }

        mMyMarker.showInfoWindow();

        mMarkers.put(mMyMarker, "MY_MARKER");

        ParseObject lastLocation = null;
        if (mUser != null) {
            lastLocation = mUser.getParseObject(User.location);
        }

        ParseObject location = new ParseObject("");
        location.put(MyLocation.time, new Date());
        if (addressText != null) {
            location.put(MyLocation.label, addressText);
        }
        if (lastLocation == null && mUser != null) {

            location.put(MyLocation.latittude, myLocation.latitude);
            location.put(MyLocation.longtidude, myLocation.longitude);
            mUser.put(MyLocation.collectionName, location);
            mUser.saveInBackground();

        } else if(lastLocation != null && mUser != null){

            Double lastLatitudeStored = lastLocation.getDouble(MyLocation.latittude);
            Double lastLongtudeStored = lastLocation.getDouble(MyLocation.latittude);

            LatLng lastLocationStored = new LatLng(lastLatitudeStored, lastLongtudeStored);
            double meters = SphericalUtil.computeDistanceBetween(myLocation, lastLocationStored);

            if (meters > 500) {

                location.put(MyLocation.latittude, myLocation.latitude);
                location.put(MyLocation.longtidude, myLocation.longitude);
                mUser.put(MyLocation.collectionName, location);
                mUser.saveInBackground();
            }
        }


    }

    private void moveCameraToLastPosition() {

              /* move camera */
        mMap.moveCamera(CameraUpdateFactory
                .newLatLng(mLastPosition));

            /*  1: World
             *  5: Landmass/continent
             *  10: City
             *  15: Streets
             *  20: Buildings
             */
        // Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLastPosition)      // Sets the center of the map to Mountain View
                .zoom(15)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    private void putPositionToMap() {
        Double latitude = null;
        Double longitude = null;


        int numMarkersInRainbow = 10;
        for (int i = 0; i < numMarkersInRainbow; i++) {
            latitude = -30 + 10 * Math.sin(i * Math.PI / (numMarkersInRainbow - 1));
            longitude = 135 - 10 * Math.cos(i * Math.PI / (numMarkersInRainbow - 1));
            LatLng relationLocation = new LatLng(latitude, longitude);
            mLatLngs.add(relationLocation);


        }

        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        LatLng myLocation = new LatLng(latitude, longitude);
        mLatLngs.add(myLocation);


        // Pan to see all markers in view.
        // Cannot zoom to bounds until the map has a size.
        final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
        if (mapView.getViewTreeObserver().isAlive()) {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation") // We use the new method when supported
                @SuppressLint("NewApi") // We check which build version we are using.
                @Override
                public void onGlobalLayout() {
                    com.google.android.gms.maps.model.LatLngBounds.Builder Builder = new LatLngBounds.Builder();
                    for (LatLng latLng : mLatLngs) {

                        Builder.include(latLng);

                    }

                    bounds = Builder
                            .build();


                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                }
            });
        }
    }

    private void setRelationshipMarker(String type) {

        int numMarkersInRainbow = mLatLngs.size() - 1;
        for (int i = 0; i < numMarkersInRainbow; i++) {

            LatLng relationLocation = mLatLngs.get(i);

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_home))
                    .icon(BitmapDescriptorFactory.defaultMarker(i * 360 / numMarkersInRainbow))
                    .anchor(0.0f, 1.0f) // Anchors the marker on the bottom left
                    .position(relationLocation)
                    .title("Mummy " + i)
                    .snippet("Dang di choi nhe " + i));
            marker.showInfoWindow();

            mMarkers.put(marker, "abc1" + i);

        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mMap != null && mLastLocation != null) {
            putPositionToMap();
            setRelationshipMarker("FRIENDS");
            setcurrentMarker();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {

            return null;

        }

        @Override
        public View getInfoContents(Marker marker) {

            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            int badge;
            // Use the equals() method on a Marker to check for equals.  Do not use ==.
//            if (marker.equals(mPerth)) {
//                badge = R.drawable.badge_wa;
//            } else {
            // Passing 0 to setImageResource will clear the image view.
            badge = 0;
//            }
            ((ImageView) view.findViewById(R.id.badge)).setImageResource(badge);

            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
                snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }
        }
    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        mLastPosition = marker.getPosition();
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();
        mLastPosition = marker.getPosition();
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        //Toast.makeText(this, "Close Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        //Toast.makeText(this, "Info Window long click", Toast.LENGTH_SHORT).show();
    }


}
