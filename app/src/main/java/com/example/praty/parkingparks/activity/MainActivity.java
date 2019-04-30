package com.example.praty.parkingparks.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.praty.parkingparks.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;

    private GoogleApiClient mGoogleApiClient;

    private SignInButton mButton;
    private ProgressBar mProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton=(SignInButton) findViewById(R.id.SignInButton);
        mButton.setColorScheme(SignInButton.COLOR_DARK);
        mButton.setSize(SignInButton.SIZE_STANDARD);
        mProgress=(ProgressBar) findViewById(R.id.progressBar);
        setupGoogleSignInText(mButton);
        FirebaseApp.initializeApp(this);
        mAuth=FirebaseAuth.getInstance();


        configureSignIn();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgress.setVisibility(View.VISIBLE);
                signIn();
            }
        });

    }

    //custom text on the google sign in button
    private void setupGoogleSignInText(SignInButton mSignInButton) {
        for(int i=0;i<mSignInButton.getChildCount();i++)
        {
            View v= mSignInButton.getChildAt(i);

            if(v instanceof TextView){
                TextView tv=(TextView) v;
                tv.setText("Sign in with Google");
                return;
            }
        }
    }

    //method to configure google sign in options to request IdToken of the user's google account
    private void configureSignIn() {
        Log.d(TAG, "configureSignIn: called");
        GoogleSignInOptions gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        //do nothing
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mGoogleApiClient.connect();

    }

    //method to open the sign in window

    private void signIn() {

        Intent signInIntent=Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }

    //method to fetch results of the google sign in
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e);
            }

        }
    }

    //if google sign in successful, authenticate with firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount account){

        AuthCredential credential= GoogleAuthProvider.getCredential(account.getIdToken(),null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()){
                            mProgress.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Login successful",Toast.LENGTH_SHORT).show();
                            setupSharedPreferences();
                            Log.d(TAG, "onComplete: sign in success with firebase");

                            //if authentication successful, move to the ParkingActivity
                            Intent intent=new Intent(MainActivity.this, ParkingActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            mProgress.setVisibility(View.GONE);
                            Log.d(TAG, "onComplete: sign in failure with firebase:"+task.getException());
                        }

                    }
                });

    }

    //method to save SharedPreferences for google sign in
    private void setupSharedPreferences() {
        SharedPreferences prefs= getSharedPreferences("ActivityPREF", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=prefs.edit();
        editor.putBoolean("activity_executed",true);
        editor.apply();
    }

}
