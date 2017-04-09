package com.example.basehacks.locuslocation;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, BeaconConsumer {
    MainActivity mService;
    boolean mBound = false;
    private GoogleMap mMap;
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    TextView mDistance;
    boolean mFirst = true;
    String mBeaconAddr;
    FirebaseDatabase mDatabase;
    DatabaseReference mRef;
    Beacon closestBeacon;
    long valueBeforeClose;
    boolean alreadyFar = false;
    long dmvwait;
    long carlsjrwait;
    long theaterwait;
    long dmvcalc;
    long carlsjrcalc;
    long theatercalc;
    Snackbar snackbar;
    boolean mSecond = false;
    int count = 1;
    ArrayList<Double> distances = new ArrayList<Double>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(1100l);
        beaconManager.setForegroundBetweenScanPeriod(0l);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(30000l);
        beaconManager.bind(this);
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("beacons");
        ValueEventListener waitListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dmvwait = (long) dataSnapshot.child("F4:5E:AB:27:4E:D2").getValue();
                theaterwait = (long) dataSnapshot.child("F4:5E:AB:70:43:9F").getValue();
                carlsjrwait = (long) dataSnapshot.child("F4:5E:AB:27:53:2C").getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        mDatabase.getReference("waittimes").addListenerForSingleValueEvent(waitListener);
        ValueEventListener calcListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dmvcalc = dmvwait*((long) dataSnapshot.child("F4:5E:AB:27:4E:D2").getValue());
                theatercalc = theaterwait*((long) dataSnapshot.child("F4:5E:AB:70:43:9F").getValue());
                carlsjrcalc = carlsjrwait*((long) dataSnapshot.child("F4:5E:AB:27:53:2C").getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mRef.addValueEventListener(calcListener);
    }
    @Override
    protected void onDestroy() {
        mRef.child(mBeaconAddr).setValue(valueBeforeClose);
        System.out.println("decremented " + valueBeforeClose);


        beaconManager.unbind(this);
        super.onDestroy();
    }
    @Override
    protected void onStop(){
        super.onStop();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                valueBeforeClose = (long) dataSnapshot.child(mBeaconAddr).getValue() - 1;

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        mRef.addListenerForSingleValueEvent(listener);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                     System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng dmv = new LatLng(37.553341,-122.007197);
        mMap.addMarker(new MarkerOptions().position(dmv).title("DMV"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dmv, 14.0f));
        LatLng theater = new LatLng(37.556268,-122.006871);
        Marker mark = mMap.addMarker(new MarkerOptions().position(theater).title("Theater"));

        LatLng carlsjr = new LatLng(37.560757,-122.011227);
        mMap.addMarker(new MarkerOptions().position(carlsjr).title("Carl's Jr"));
        //mMap.moveCamera(CameraUpdateFactory.zoomBy(1));
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {

            @Override
            public boolean onMarkerClick(Marker arg0) {
                arg0.showInfoWindow();
                if(arg0.getTitle().equals("DMV")){

                    snackbar = Snackbar
                            .make(findViewById(R.id.map), ("Wait time: " + dmvcalc), Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("CLOSE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            snackbar.dismiss();
                        }
                    });

                    snackbar.show();
                }
                else if(arg0.getTitle().equals("Theater")){
                    snackbar = Snackbar
                            .make(findViewById(R.id.map), ("Wait time: " + theatercalc), Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("CLOSE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            snackbar.dismiss();
                        }
                    });

                    snackbar.show();
                }
                else if(arg0.getTitle().equals("Carl's Jr")){
                    snackbar = Snackbar
                            .make(findViewById(R.id.map), ("Wait time: " + carlsjrcalc), Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("CLOSE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            snackbar.dismiss();
                        }
                    });

                    snackbar.show();
                }

                return true;
            }

        });
    }
    public double calculateAverage(ArrayList<Double> marks) {
        double sum = 0.0;
        if(!marks.isEmpty()) {
            for (double mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }
    public static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    @Override
    public void onBeaconServiceConnect() {
        System.out.println("Hi");

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                System.out.println(beacons.size());
                if (beacons.size() > 0) {
                    //mDistance = (TextView) findViewById(R.id.distance);
                    //Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            //find the beacon with shortest distance
                            int idx = -1; //when no beacon is there
                            double min = Double.MAX_VALUE;
                            Object[] beaconArray = beacons.toArray();
                            closestBeacon = (Beacon) beaconArray[0];
                            //ArrayList<Double> distances = new ArrayList<Double>();
                            for (int i = 0; i < beacons.size(); i++) {

                                double d = ((Beacon) beaconArray[i]).getDistance();

                                //distances.add(d);
                                if (round(d, 1) <= 0.9) {
                                    closestBeacon = (Beacon) beaconArray[i];
                                    min = d;
                                    idx = i; //1st beacon in the array
                                }
                            }

                            System.out.println("Distance: " + closestBeacon.getDistance());


                            //if (idx != -1) {
                            //
                            distances.add(closestBeacon.getDistance());
                            if(count % 3 == 0){
                                double avg = calculateAverage(distances);
                                distances.clear();
                                System.out.println("update");
                                String[] ids = {"F4:5E:AB:27:4E:D2", "F4:5E:AB:70:43:9F","F4:5E:AB:27:53:2C"};
                                boolean isValidID = Arrays.asList(ids).contains(closestBeacon.getBluetoothAddress());

                                if(mFirst && isValidID){
                                    System.out.println("Distance: " + closestBeacon.getDistance());
                                    mBeaconAddr = closestBeacon.getBluetoothAddress();
                                    System.out.println("mFirst ran");

                                    ValueEventListener listener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            long oldAddrNum = (long) dataSnapshot.child(mBeaconAddr).getValue() + 1;
                                            mRef.child(mBeaconAddr).setValue(oldAddrNum);
                                            System.out.println("incremented " + oldAddrNum);
                                            mFirst = false;
                                            alreadyFar = false;
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Getting Post failed, log a message
                                            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                                            // ...
                                        }
                                    };
                                    mRef.addListenerForSingleValueEvent(listener);

                                }
                                System.out.println("closest: " + closestBeacon.getBluetoothAddress() + "; mb: " + mBeaconAddr);

                                if(avg > 0.7 && !alreadyFar && !mFirst && isValidID){
                                    mFirst = true;
                                    System.out.println("Distance: " + closestBeacon.getDistance());
                                    alreadyFar = true;
                                    ValueEventListener listener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            // Get Post object and use the values to update the UI
                                            long oldAddrNum = (long) dataSnapshot.child(mBeaconAddr).getValue() - 1;
                                            mRef.child(mBeaconAddr).setValue(oldAddrNum);
                                            System.out.println("too far");

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Getting Post failed, log a message
                                            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                                            // ...
                                        }
                                    };
                                    mRef.addListenerForSingleValueEvent(listener);
                                }
                                System.out.println("closest: " + closestBeacon.getBluetoothAddress() + "; mbeac: " + mBeaconAddr);
                                if(!closestBeacon.getBluetoothAddress().equals(mBeaconAddr) && !alreadyFar && !mFirst && mBeaconAddr != null && closestBeacon != null && isValidID){
                                    System.out.println("I ran");
                                    System.out.println("Distance: " + closestBeacon.getDistance());
                                    ValueEventListener listener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            // Get Post object and use the values to update the UI
                                            String currAddr = closestBeacon.getBluetoothAddress();
                                            long currAddrNum = (long) dataSnapshot.child(currAddr).getValue() + 1;
                                            mRef.child(currAddr).setValue(currAddrNum);
                                            long oldAddrNum = (long) dataSnapshot.child(mBeaconAddr).getValue() - 1;
                                            mRef.child(mBeaconAddr).setValue(oldAddrNum);
                                            System.out.println("Would update");
                                            mBeaconAddr = closestBeacon.getBluetoothAddress();
                                            // ...
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Getting Post failed, log a message
                                            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                                            // ...
                                        }
                                    };
                                    mRef.addListenerForSingleValueEvent(listener);
                                }


                                //mDistance.setText("Beacon ID: " + closestBeacon.getBluetoothAddress());
                                System.out.println();
                            }
                            count++;
                        }
                    });
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}
