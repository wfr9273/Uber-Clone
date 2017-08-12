package com.example.android.uberclone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callCancelButton;
    Button logoutButton;
    boolean callOrCancel;
    boolean driverAcceptRequest;
    Handler handler;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.riderMap);
        mapFragment.getMapAsync(this);

        handler = new Handler();

        callCancelButton = (Button) findViewById(R.id.callCancelButton);
        textView = (TextView) findViewById(R.id.info);
        logoutButton = (Button) findViewById(R.id.logout);

        // false (default) -> there is no request, true -> there are requests
        callOrCancel = false;
        driverAcceptRequest = false;

        // Check if there is request from the current user already
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                // if there are requests, toggle the default value
                if (e == null && objects.size() > 0) {
                    callCancelButton.setText("Cancel");
                    callOrCancel = !callOrCancel;
                    checkForUpdates();
                }
            }
        });

        callCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!callOrCancel) {
                    // Call an uber
                    // get the current location of the rider
                    if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            ParseObject request = new ParseObject("Request");
                            request.put("username", ParseUser.getCurrentUser().getUsername());
                            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                            request.put("location", parseGeoPoint);
                            // save the request, and wait for a driver to accept the request
                            request.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        callCancelButton.setText("Cancel");
                                        callOrCancel = !callOrCancel;
                                        checkForUpdates();
                                    }
                                }
                            });
                        } else
                            Toast.makeText(RiderActivity.this, "Could not find location\nPlease try again later", Toast.LENGTH_SHORT).show();

                    }
                } else {
                    // Cancel requests
                    ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                    query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null && objects.size() > 0) {
                                for (ParseObject object : objects)
                                    object.deleteInBackground();
                                callOrCancel = !callOrCancel;
                                callCancelButton.setText("Call Uber");
                            }
                        }
                    });
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.logOut();
                Intent intent = new Intent(RiderActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null)
                updateLocation(location);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null)
                    updateLocation(location);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null)
                    updateLocation(location);
            }
        }
    }

    // update the rider's location when there is no request from this rider
    private void updateLocation(Location location) {
        if (!driverAcceptRequest) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            mMap.addMarker(new MarkerOptions().position(latLng).title("You are here"));
        }
    }

    // update the rider's location and the driver's location who accepts the request
    private void checkForUpdates() {
        // Query the accepted request from the current rider
        ParseQuery<ParseObject> requestQuery = new ParseQuery<ParseObject>("Request");
        requestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        requestQuery.whereExists("driverUsername");
        requestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {
                if (e == null && requests.size() > 0) {
                    driverAcceptRequest = true;
                    // Query the users to find out who accepted the request
                    ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                    userQuery.whereEqualTo("username", requests.get(0).getString("driverUsername"));
                    userQuery.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> users, ParseException e) {
                            if (e == null && users.size() > 0) {
                                // get the driver's current location
                                ParseGeoPoint driverLocation = users.get(0).getParseGeoPoint("location");
                                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    if (location != null) {
                                        // get the rider's current location and calculate the distance
                                        ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                                        double distance = (double) Math.round(userLocation.distanceInMilesTo(driverLocation) * 10) / 10;

                                        //update both driver's and rider's current location on the map
                                        // the color of driver's marker is blue and the color of rider's marker is red
                                        mMap.clear();
                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        ArrayList<Marker> markers = new ArrayList<>();
                                        markers.add(mMap.addMarker(new MarkerOptions()
                                                .title("Your Location").position(new LatLng(location.getLatitude(), location.getLongitude()))));
                                        markers.add(mMap.addMarker(new MarkerOptions().title("Driver's Location")
                                                .position(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude()))
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
                                        builder.include(markers.get(0).getPosition());
                                        builder.include(markers.get(1).getPosition());
                                        LatLngBounds bounds = builder.build();
                                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 60);
                                        mMap.animateCamera(cu);

                                        // if the driver arrives, delete the request from the rider
                                        if (distance < 0.01) {
                                            textView.setText("Your driver arrived");
                                            ParseQuery<ParseObject> requestQuery = new ParseQuery<ParseObject>("Request");
                                            requestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                            requestQuery.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null) {
                                                        for (ParseObject object : objects)
                                                            object.deleteInBackground();
                                                    }
                                                }
                                            });
                                            // 5s for the rider to see the info
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    callCancelButton.setVisibility(View.VISIBLE);
                                                    callCancelButton.setText("Call Uber");
                                                    driverAcceptRequest = false;
                                                    callOrCancel = false;
                                                    textView.setText("");
                                                    if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                                        if (location != null)
                                                            updateLocation(location);
                                                    }
                                                }
                                            }, 5000);
                                        } else {
                                            textView.setText("Your driver is " + Double.toString(distance) + " miles away");
                                            callCancelButton.setVisibility(View.INVISIBLE);
                                        }

                                    }
                                }

                            }
                        }
                    });
                }
                // continue checking and updating until the driver arrives
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForUpdates();
                    }
                }, 2000);
            }
        });
    }
}
