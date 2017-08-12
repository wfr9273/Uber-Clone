package com.example.android.uberclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.value;

public class DriverActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;
    Location driverCurrentLocation;
    ListView listView;
    ArrayList<String> arrayList;
    ArrayList<LatLng> requestLocation;
    ArrayList<String> usernames;
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);
        setTitle("Nearby Requests");

        listView = (ListView) findViewById(R.id.list);
        arrayList = new ArrayList<>();
        usernames = new ArrayList<>();
        requestLocation = new ArrayList<>();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                driverCurrentLocation = location;
                getRequestList();
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            driverCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            getRequestList();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                driverCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                getRequestList();
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(DriverActivity.this, RequestActivity.class);
                intent.putExtra("DriverLocationLat", driverCurrentLocation.getLatitude());
                intent.putExtra("DriverLocationLon", driverCurrentLocation.getLongitude());
                intent.putExtra("RiderLocationLat", requestLocation.get(i).latitude);
                intent.putExtra("RiderLocationLon", requestLocation.get(i).longitude);
                intent.putExtra("username", usernames.get(i));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getRequestList();
            }
        }
    }

    private void getRequestList() {
        arrayList.clear();
        requestLocation.clear();
        usernames.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        final ParseGeoPoint driverGeoLocation = new ParseGeoPoint(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude());
        query.whereNear("location", driverGeoLocation);
        query.whereDoesNotExist("driverUsername");
        query.setLimit(15);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        for (ParseObject object : objects) {
                            ParseGeoPoint requestGeoLocation = object.getParseGeoPoint("location");
                            requestLocation.add(new LatLng(requestGeoLocation.getLatitude(), requestGeoLocation.getLongitude()));
                            double distance = requestGeoLocation.distanceInMilesTo(driverGeoLocation);
                            arrayList.add(String.format("%.2f", distance) + " miles");
                            usernames.add(object.getString("username"));
                        }
                    } else {
                        requestLocation.add(new LatLng(0, 0));
                        arrayList.add("No Request...");
                        usernames.add("");
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Log.i("Failed", e.getMessage());
                }
            }
        });
    }
}
