package com.example.basehacks.locuslocation;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


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
import java.util.Collection;

public class MainActivity extends Service implements BeaconConsumer {
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
    /** The service is starting, due to a call to startService() */
    /** indicates how to behave if the service is killed */
    int mStartMode;

    /** interface for clients that bind */
    IBinder mBinder;

    /** indicates whether onRebind should be used */
    boolean mAllowRebind;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }
    public class LocalBinder extends Binder {
        MainActivity getService() {
            // Return this instance of LocalService so clients can call public methods
            return MainActivity.this;
        }
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(Intent intent) {

    }
    public void onCreate() {
        //setContentView(R.layout.activity_main);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(100l);
        beaconManager.setForegroundBetweenScanPeriod(0l);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(30000l);
        beaconManager.bind(this);
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("beacons");

    }


    public void onStop(){
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
    public void onDestroy() {
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
                                distances.add(d);
                                if (d < 1) {
                                    closestBeacon = (Beacon) beaconArray[i];
                                    min = d;
                                    idx = i; //1st beacon in the array
                                }
                            }


                            if (idx != -1) {
                                if(mFirst && closestBeacon.getDistance() < 0.8){
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
                                System.out.println("dist: " + closestBeacon.getDistance());
                                if(closestBeacon.getDistance() > 0.8 && !alreadyFar){
                                    ValueEventListener listener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            // Get Post object and use the values to update the UI
                                            long oldAddrNum = (long) dataSnapshot.child(mBeaconAddr).getValue() - 1;
                                            mRef.child(mBeaconAddr).setValue(oldAddrNum);
                                            System.out.println("too far");
                                            mFirst = true;
                                            alreadyFar = true;
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
                                if(!closestBeacon.getBluetoothAddress().equals(mBeaconAddr) && !alreadyFar){
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
