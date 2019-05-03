package com.example.praty.parkingparks.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.praty.parkingparks.R;
import com.example.praty.parkingparks.adapter.ParkingAdapter;
import com.example.praty.parkingparks.helper.ItemClickListener;
import com.example.praty.parkingparks.model.ParkingSpaces;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ParkingActivity extends AppCompatActivity {

    private static final String TAG = "ParkingActivity";
    private static final int RC_UPLOAD = 100;
    private static final int ERROR_DIALOG_REQUEST =101 ;
    private RecyclerView mRecycler;
    private ParkingAdapter mAdapter;
    private FloatingActionButton mAddButton;
    private EditText mSlots;
    private EditText mDescription;
    private Button mSave;
    private ImageButton mGetLocationButton;
    private ImageButton mImageButton;
    private Uri filePath;
    private boolean isImageChosen=false;
    private boolean mLocationPermissionsGranted=false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mCurrentLocation;
    private List<ParkingSpaces> mSpaces=new ArrayList<>();
    private DatabaseReference mDatabaseRef;
    private LatLng mLatlng;
    private Double latitude=0.0;
    private Double longitude=0.0;
    private StorageReference mRef;
    private FirebaseStorage mStorage;
    private String link="Default";
    private boolean gpsEnabled=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);
        Log.d(TAG, "onCreate: called");

        //Initialization
        mAddButton=(FloatingActionButton) findViewById(R.id.add_button);
        mDatabaseRef= FirebaseDatabase.getInstance().getReference();
        mStorage=FirebaseStorage.getInstance();
        mRef=mStorage.getReference();
        mCurrentLocation=new Location(LocationManager.GPS_PROVIDER);

        //OnClickListener of the floatingaction Button
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: add button clicked");
                getNewParking();
            }
        });


        initRecyclerView();
    }


    private void initRecyclerView() {
        mAdapter=new ParkingAdapter(this,mDatabaseRef);

        mRecycler=(RecyclerView) findViewById(R.id.my_recycler_view);
        GridLayoutManager layoutManager=new GridLayoutManager(this, 1);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setAdapter(mAdapter);
    }

    private void getNewParking() {
        Log.d(TAG, "getNewParking: called");

        //dialog for inflating the dialog_layout to add a new ParkingLotSpace
        final Dialog dialog = new Dialog(ParkingActivity.this);
        dialog.setTitle("dialog");
        dialog.setContentView(R.layout.parking_dialog);

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9);

        dialog.getWindow().setLayout(width,height);
        dialog.show();

        //Initialization of the UI components
        mSlots=(EditText)dialog.findViewById(R.id.slots_edit);
        mDescription=(EditText)dialog.findViewById(R.id.description_edit);
        mGetLocationButton=(ImageButton)dialog.findViewById(R.id.gps_button);
        mImageButton=(ImageButton)dialog.findViewById(R.id.image_button);
        mSave=(Button)dialog.findViewById(R.id.save);

        //ClickListeners
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageFromPhone();
            }
        });

        mGetLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: gps button clicked");
                if(isGPSEnabled()){
                    gpsEnabled=true;
                    Log.d(TAG, "onClick: GPS button: GPS enabled");
                    Toast.makeText(ParkingActivity.this,"GPS already enabled",Toast.LENGTH_SHORT).show();
                }
                else{
                    //If the GPS not enabled, make the user goto settings and enable it
                    Log.d(TAG, "onClick: GPS not enabled");
                    final AlertDialog.Builder builder=new AlertDialog.Builder(ParkingActivity.this);
                    builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent enableGps= new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(enableGps, 121);

                                }
                            });
                    final AlertDialog gpsAlert=builder.create();
                    gpsAlert.show();


                }
            }
        });

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if save Button is pressed, check for valid data and push it to realtime database
                String slots=mSlots.getText().toString();
                boolean cancel=false;
                View focusView=null;

                if(TextUtils.isEmpty(slots)){
                    mSlots.setError("You cannot leave this field empty");
                    focusView=mSlots;
                    cancel=true;
                }

                if(cancel){
                    focusView.requestFocus();
                }
                else {
                    dialog.dismiss();
                    saveDataToFirebase();
                }
            }
        });
    }



    //check for google play services, if valid, check for location permissions
    //if permissions granted, get the device location
    private void saveDataToFirebase() {
        if(isServicesOK()) {
            checkForLocationPermissions();

            if (mLocationPermissionsGranted) {
                Log.d(TAG, "saveDataToFirebase: permissions granted, trying to retrieve location");
                Log.d(TAG, "saveDataToFirebase: gpsEnabled:"+gpsEnabled);
                if(gpsEnabled) {
                    getDeviceLocation();
                }
                else{
                    Toast.makeText(ParkingActivity.this,"Turn on the GPS and try again",Toast.LENGTH_SHORT).show();
                }

               // Log.d(TAG, "saveToFirebase: Latitude:"+latitude+"Longitude:"+longitude);
            }
        }

    }

    //method to get the device location using fusedlocationProvider
    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(ParkingActivity.this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult()!=null) {
                            mCurrentLocation = (Location) location.getResult();
                            Log.d(TAG, "onComplete: location:"+mCurrentLocation);

                            mLatlng=new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                            Log.d(TAG, "onComplete: Latitude:"+mLatlng.latitude+"Longitude:"+mLatlng.longitude);

                            latitude=mLatlng.latitude;
                            longitude=mLatlng.longitude;

                            if(isImageChosen) {
                                uploadImageToFirebase(latitude, longitude);
                            }
                            else{
                                //if there's an image chosen by the user, upload the image to firebase first
                                saveToFirebase(latitude, longitude, "Default");
                            }


                        } else {
                            Toast.makeText(ParkingActivity.this, "Unable to get device location, turn on the GPS and try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    //once the device location is retrieved, add the input data to firebase
    private void saveToFirebase(Double latitude, Double longitude, String linktoimage) {
        Log.d(TAG, "saveToFirebase: latitude:"+latitude+"Longitude:"+longitude);
        String slots=mSlots.getText().toString();
        String description=mDescription.getText().toString();


        //retrieving address using reverse geocoding
        Geocoder geocoder;
        List<Address> addresses;
        String address="Default address";
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            address=addresses.get(0).getAddressLine(0);
            Log.d(TAG, "saveToFirebase: address:"+address);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //description left empty and no image chosen
        if (TextUtils.isEmpty(description) && !isImageChosen) {
            ParkingSpaces obj=new ParkingSpaces(slots, "some random text",
                    address, latitude,longitude,"default description");
            mDatabaseRef.child("ParkingPlaces").push().setValue(obj);

        //description left empty but an image chosen
        } else if (TextUtils.isEmpty(description) && isImageChosen) {
            Log.d(TAG, "saveToFirebase: link:"+linktoimage);
            ParkingSpaces obj=new ParkingSpaces(slots, linktoimage,
                    address, latitude,longitude,"default description");
            mDatabaseRef.child("ParkingPlaces").push().setValue(obj);
            isImageChosen=false;

          //description not empty and an image chosen
        } else if (!TextUtils.isEmpty(description) && isImageChosen) {
            Log.d(TAG, "saveToFirebase: link:"+linktoimage);
            ParkingSpaces obj=new ParkingSpaces(slots, linktoimage,
                    address, latitude,longitude,description);
            mDatabaseRef.child("ParkingPlaces").push().setValue(obj);
            isImageChosen=false;

        }
        //description not empty but no image chosen
        else if(!TextUtils.isEmpty(description) && !isImageChosen){
            ParkingSpaces obj=new ParkingSpaces(slots, "some random text",
                    address, latitude,longitude,description);
            mDatabaseRef.child("ParkingPlaces").push().setValue(obj);

        }
    }

    private void uploadImageToFirebase(final Double latitude, final Double longitude) {
        if(filePath!=null)
        {
            final ProgressDialog progressDialog= new ProgressDialog(this);
            progressDialog.setTitle("Uploading Image...");
            progressDialog.show();

            final StorageReference mReference= mRef.child("images/"+ UUID.randomUUID().toString());
            mReference.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(ParkingActivity.this,"Uploaded",Toast.LENGTH_SHORT).show();
                            mReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    link=uri.toString();
                                    Log.d(TAG, "onSuccess: link"+link);
                                    saveToFirebase(latitude,longitude,link);
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(ParkingActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double Progress=(100.0* taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)Progress+"%");

                        }
                    });
        }

    }

    //method to check for permissions for accessing user's location
    private void checkForLocationPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
            }
            else{
                //if permissions not granted, request for it
                ActivityCompat.requestPermissions(this,permissions,123);
            }
        }
        else{
            //if permissions not granted, request for it
            ActivityCompat.requestPermissions(this,permissions,123);
        }
    }

    //method to check if the GPS is enabled
    private boolean isGPSEnabled() {
        final LocationManager manager=(LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return false;
        }
        return true;
    }

    //method to check if google services are working
    public boolean isServicesOK(){
        int available= GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ParkingActivity.this);

        if(available== ConnectionResult.SUCCESS){
            Log.d("Mainactivity","isServicesOK: play services are working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d("Mainactivity","isServicesOK: error showed up but it's solvable");
            Dialog dialog=GoogleApiAvailability.getInstance().getErrorDialog(ParkingActivity.this,available,ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else{
            Toast.makeText(ParkingActivity.this,"Can't retrieve your coordinates",Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    //intent service to pick an image from the phone storage
    private void chooseImageFromPhone() {
        Intent intent= new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),RC_UPLOAD);
    }

    //method to retrieve intent results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode== RC_UPLOAD && resultCode== RESULT_OK && data!=null && data.getData()!=null){
            filePath=data.getData();
            isImageChosen=true;
        }
        else if(requestCode==121){
            //do something
            gpsEnabled=true;
        }
    }

    //method when the user explicitly grants location access permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mLocationPermissionsGranted=false;
        switch (requestCode){
            case 123:{
                if(grantResults.length>0){
                    for(int i=0;i<grantResults.length;i++){
                        if(grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted=false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted=true;

            }
        }}
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.cleanup();
    }
}
