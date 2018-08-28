package com.example.arunlakhera.loginregistration;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    //a constant for detecting the login intent result
    private static final int RC_SIGN_IN = 234;

    //Tag for the logs optional
    private static final String TAG = "LoginApp";

    //creating a GoogleSignInClient object
    GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private DatabaseReference mDatabase;
    private FirebaseUser user;

    EditText emailId_EditText;
    EditText password_EditText;
    String email;
    String password;
    String userId;
    Boolean emailVerified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        //intialized the FirebaseAuth object
        mAuth = FirebaseAuth.getInstance();

        //Then we need a GoogleSignInOptions object
        //And we need to build it as below
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        //Then we will get the GoogleSignInClient object from GoogleSignIn class
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Call sign in function once the Signin button is clicked
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });

        findViewById(R.id.button_SignUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailSignUp();
            }
        });

    }

    /**
     * Function to sign in using Google Sign In
     */
    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);

            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebase Auth WithGoogle:" + acct.getId());

        email = acct.getEmail();
        password = "Gmail Password";

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "Sign In Successful");
                            updateDatabase();

                            Intent signUpIntent = new Intent(MainActivity.this, ProfileActivity.class);
                            signUpIntent.putExtra("userEmail", email);
                            startActivity(signUpIntent);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "Sign In failed", task.getException());
                            showToast("Sign in Error Occurred");
                        }

                    }
                });
    }

    /**
     * Function for Sign In/Sign Up
     */
    public void emailSignUp(){

        emailId_EditText = findViewById(R.id.editText_EmailId);
        password_EditText = findViewById(R.id.editText_Password);

        email = String.valueOf(emailId_EditText.getText());
        password = String.valueOf(password_EditText.getText());

        if (email.isEmpty()) {
            showToast("Email ID is required for Sign Up...");
            emailId_EditText.requestFocus();
        }else if (!isValidEmail(email)){

            showToast("Please enter a Valid Email ID...");
            emailId_EditText.requestFocus();

        }else if (password.isEmpty()) {

            showToast("Please enter a Password...");
            emailId_EditText.requestFocus();

        }else{

            // Email Id is in correct format , try to Login the User
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                emailVerified = user.isEmailVerified();

                                if(emailVerified){
                                    // Call function to move user to next screen on successful Sign In
                                    // Sign up success, take signed-up user's information to home activity
                                    Intent signUpIntent = new Intent(MainActivity.this, ProfileActivity.class);
                                    signUpIntent.putExtra("userEmail", email);
                                    startActivity(signUpIntent);
                                    finish();

                                }else {
                                    showToast("Sign In-Please verify your email ID by clicking on the Verification link sent to " + user.getEmail());
                                }

                            } else {

                                // If sign in fails, display a message to the user.
                                String err = "";

                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    err += "Please provide a Valid Email Id and Password..";
                                } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {

                                    String errorCode = ((FirebaseAuthInvalidUserException) task.getException()).getErrorCode();

                                    if (errorCode.equals("ERROR_USER_NOT_FOUND")) {
                                        err += "No matching user found. Created New Account.";
                                        newSignUp();


                                    } else if (errorCode.equals("ERROR_USER_DISABLED")) {
                                        err += "User account has been disabled.";
                                    } else {
                                        err += task.getException().getLocalizedMessage();
                                    }

                                }else {
                                    err += task.getException().getLocalizedMessage();
                                }

                                showToast(err);

                            }

                        }

                    });

        }
    }

    /**
     * Function for New Signup
     * */
    public void newSignUp(){

        // Sign Up the user with Email ID and Password provided or show error if any
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            // Call function to update the Firebase Database
                            updateDatabase();

                            // Send Link
                            sendEmailLink();

                            emailVerified = user.isEmailVerified();

                            if(emailVerified){
                                // SignUp user
                                signUpUser();
                            }else {

                                showToast("To Sign Up - Please verify your email ID by clicking on the Verification link sent to " + user.getEmail());
                            }

                        } else {

                            // If sign in fails, display a message to the user.
                            String err = "";

                            String errorCode = ((FirebaseAuthUserCollisionException) task.getException()).getErrorCode();

                            if (errorCode.equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                                err = "User already exists. You can use different Email ID to Sign Up...";
                                showToast(err);
                            }else{
                                err = errorCode;
                            }

                        }
                    }
                });

    }

    public void sendEmailLink(){

        final FirebaseUser user = mAuth.getCurrentUser();

        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            showToast("Verification link sent to " + user.getEmail());
                        } else {
                            Log.e(TAG, "sendEmailVerification", task.getException());
                            showToast("Failed to send verification email.");
                        }
                    }
                });
    }

    /**
     * Function to Update the Firebase Database
     */

    private void updateDatabase() {

        // Initialize the instance of Firebase database to get current logged in users information
        user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = String.valueOf(user.getUid());
        // Initialize Firebase Database Instance to the table Users
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Save the Users information in Users table in Firebase
        mDatabase.child(userId).child("EmailId").setValue(email);
        mDatabase.child(userId).child("password").setValue(password);
    }

    /**
     * Function to Signup New user and save the data in Firebase
     */
    public void signUpUser(){

        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = String.valueOf(user.getUid());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUserRef = mDatabase.child("Users/").child(userId);

        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Sign up success, take signed-up user's information to home activity
                Intent signUpIntent = new Intent(MainActivity.this, ProfileActivity.class);
                signUpIntent.putExtra("userEmail", email);
                startActivity(signUpIntent);
                finish();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Database Error Occure");
            }
        });
    }

    /**
     * Function to check if the Email Id entered is in valid format i.e contains @ and .com
     */
    private static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Function for Toast Message
     * */

    public void showToast(String msg){
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }


}
