package com.anish.basehacksandroid;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;


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
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends Activity implements BeaconConsumer {
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
    TextView dmvwaittime;
    TextView carlsjrwaittime;
    TextView theaterwaittime;
    long dmvwait;
    long carlsjrwait;
    long theaterwait;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(750l);
        beaconManager.setForegroundBetweenScanPeriod(0l);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(30000l);
        beaconManager.bind(this);
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("beacons");
        dmvwaittime = (TextView) findViewById(R.id.dmvtime);
        carlsjrwaittime = (TextView) findViewById(R.id.carlsjrtime);
        theaterwaittime = (TextView) findViewById(R.id.theatertime);
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
                long dmvcalc = dmvwait*((long) dataSnapshot.child("F4:5E:AB:27:4E:D2").getValue());
                dmvwaittime.setText("Wait time: " + dmvcalc);
                long theatercalc = theaterwait*((long) dataSnapshot.child("F4:5E:AB:70:43:9F").getValue());
                theaterwaittime.setText("Wait time: " + theatercalc);
                long carlsjrcalc = carlsjrwait*((long) dataSnapshot.child("F4:5E:AB:27:53:2C").getValue());
                carlsjrwaittime.setText("Wait time: " + carlsjrcalc);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mRef.addValueEventListener(calcListener);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
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
    protected void onDestroy() {
        mRef.child(mBeaconAddr).setValue(valueBeforeClose);
        System.out.println("decremented " + valueBeforeClose);


        beaconManager.unbind(this);
        super.onDestroy();
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
                            ArrayList<Double> distances = new ArrayList<Double>();
                            for (int i = 0; i < beacons.size(); i++) {

                                double d = ((Beacon) beaconArray[i]).getDistance();
                                System.out.println("Distance: " + d);
                                distances.add(d);
                                if (d < 1) {
                                    closestBeacon = (Beacon) beaconArray[i];
                                    min = d;
                                    idx = i; //1st beacon in the array
                                }
                            }


                            if (idx != -1) {
                                if(mFirst && closestBeacon.getDistance() < 0.5){
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
                                //System.out.println("dist: " + closestBeacon.getDistance());
                                if(closestBeacon.getDistance() > 0.5 && !alreadyFar && !mFirst){
                                    mFirst = true;
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
                                if(!closestBeacon.getBluetoothAddress().equals(mBeaconAddr) && !alreadyFar && !mFirst){
                                    System.out.println("I ran");
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