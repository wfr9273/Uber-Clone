package com.example.android.uberclone;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;

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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RequestActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    LatLng driverLocation;
    LatLng riderLocation;
    String requestUsername;
    Button acceptRequestButton;
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.requestMap);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        driverLocation = new LatLng(intent.getDoubleExtra("DriverLocationLat", 0.0), intent.getDoubleExtra("DriverLocationLon", 0.0));
        riderLocation = new LatLng(intent.getDoubleExtra("RiderLocationLat", 0.0), intent.getDoubleExtra("RiderLocationLon", 0.0));
        requestUsername = intent.getStringExtra("username");

        acceptRequestButton = (Button) findViewById(R.id.acceptRequestButton);
        relativeLayout = (RelativeLayout) findViewById(R.id.requestRelativeLayout);

        acceptRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("username", requestUsername);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null && objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
                                object.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            Intent googleMapIntent = new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("http://maps.google.com/maps?saddr="
                                                            + driverLocation.latitude + ","
                                                            + driverLocation.longitude + "&daddr="
                                                            + riderLocation.latitude + ","
                                                            + riderLocation.longitude));
                                            startActivity(googleMapIntent);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });

        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                ArrayList<Marker> markers = new ArrayList<>();
                markers.add(mMap.addMarker(new MarkerOptions().title("Your Location").position(driverLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
                markers.add(mMap.addMarker(new MarkerOptions().title("Request Location").position(riderLocation)));
                builder.include(markers.get(0).getPosition());
                builder.include(markers.get(1).getPosition());
                LatLngBounds bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 60);
                mMap.animateCamera(cu);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
