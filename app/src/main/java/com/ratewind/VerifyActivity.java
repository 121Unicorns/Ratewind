package com.ratewind;

import static android.content.ContentValues.TAG;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VerifyActivity extends AppCompatActivity {
    private String phoneNumber, mVerificationId, name, email, acType, date, intentFrom;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private EditText etCode;
    private FirebaseAuth mAuth;
    private TextView tvVerify;
    private ImageView ivBack;
    private ConstraintLayout VerifyLayout;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private Connection connection;
    private User myUser = new User();
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private static final String TAG = "PhoneAuthActivity";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        initViews();

        mAuth = FirebaseAuth.getInstance();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date mydate = new Date();
        date = formatter.format(mydate);

        //*********************************************** WE DEFINE THE CALLBACK BEFORE WE REQUEST A VERIFICATION CODE ***********************************************
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                /*String code = credential.getSmsCode();
                if (code != null) {
                    etCode.setText(code);
                }*/
                //showSnackbar("User verified!");

                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    showSnackbar(e.getMessage());
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    showSnackbar(e.getMessage());
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };

        //*********************************************** DETERMINE WHICH ACTIVITY BROUGHT US HERE, REGISTER OR LOGIN ***********************************************
        Prefs prefs = new Prefs(VerifyActivity.this);
        intentFrom = prefs.getString("intentType", "");

        if (intentFrom.equals("Register")) {
            Intent intent = getIntent();
            Bundle bd = intent.getBundleExtra("BUNDLE");
            myUser = (User) bd.getSerializable("User");

            phoneNumber = myUser.getPhoneNumber();
            name = myUser.getName();
            email = myUser.getEmail();
            acType = myUser.getUserType();
            date = myUser.getJoinDate();

        } else if (intentFrom.equals("Login")) {
            phoneNumber = getIntent().getStringExtra("PhoneNo");
        }

        //*********************************************** REQUEST A VERIFICATION CODE TO BE SENT TO THIS PHONE NUMBER ***********************************************
        if (phoneNumber != null) {
            startPhoneNumberVerification(phoneNumber);
        }

        //*********************************************** GO BACK TO PREVIOUS ACTIVITY ***********************************************
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //*********************************************** REQUEST TO VERIFY THE ENTERED CODE IF THE CODE DIDN'T PICK UP AUTOMATICALLY ***********************************************
        tvVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etCode.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    showSnackbar("Enter valid code");
                    closeKeyboard();
                    etCode.requestFocus();
                } else {
                    verifyPhoneNumberWithCode(mVerificationId, code);
                }
            }
        });

    }

    //*********************************************** SENDS THE VERIFICATION CODE TO THE MOBILE NUMBER ***********************************************
    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        // [END start_phone_auth]
    }

    //*********************************************** ESTABLISHES THE CREDENTIAL USED TO VERIFY THE PHONE NUMBER ***********************************************
    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        // [START verify_with_code]
        tvVerify.setVisibility(View.GONE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
        // [END verify_with_code]
    }

    //*********************************************** RESEND THE VERIFICATION CODE IF FAILED ***********************************************
    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)     // ForceResendingToken from callbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    //*********************************************** SIGN THE USER INTO FIREBASE WITH THE COLLECTED CREDENTIAL ***********************************************
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(VerifyActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            tvVerify.setEnabled(false);
                            FirebaseUser user = task.getResult().getUser();
                            Log.d(TAG, "signInWithCredential:success");
                            //Once the phone number is verified, we update the profile with the details collected and proceed to MainActivity
                            if (intentFrom.equals("Register")){
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(myUser.getName())
                                        .build();

                                user.reload();
                                user.updateEmail(myUser.getEmail());
                                user.reload();
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d(TAG, "User profile updated.");
                                                    showToast("User profile updated.");
                                                    updateDB();
                                                    mDatabase.child("users").child(myUser.getPhoneNumber()).setValue(myUser);
                                                    db.collection("users").document(myUser.getPhoneNumber()).set(myUser);

                                                    Intent intent = new Intent(VerifyActivity.this, MainActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            }
                                        });
                            } else {
                                Intent intent = new Intent(VerifyActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            String message = "Something is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";
                            }
                            showSnackbar(message);
                        }
                    }
                });
    }

    private void initViews() {
        etCode = findViewById(R.id.et_code);
        tvVerify = findViewById(R.id.tv_verify);
        ivBack = findViewById(R.id.iv_back);
        VerifyLayout = findViewById(R.id.verifylayout);
    }

    //*********************************************** ADD THE USERS TO THE AZURE DATABASE ***********************************************
    private void updateDB() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date mydate = new Date();
            date = formatter.format(mydate);

            do{
                connection = connectionclass();
            } while (connection == null);

            SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            acType = sharedPreferences.getString("userType", myUser.getUserType());

            String query = "";

            if (acType.equals("Tenant")) {
                query = "INSERT INTO tenant_users (phoneNumber, email, name, userType, joinDate) VALUES (?, ?, ?, ?, ?);";
            } else if (acType.equals("Owner")) {
                query = "INSERT INTO owner_users (phoneNumber, email, name, userType, joinDate) VALUES (?, ?, ?, ?, ?);";
            }

            /*
            PreparedStatement insertStatement = connection
                    .prepareStatement("INSERT INTO users (phoneNumber, email, name, userType, joinDate) VALUES (?, ?, ?, ?, ?);");
             */

            PreparedStatement insertStatement = connection.prepareStatement(query);

            insertStatement.setString(1, myUser.getPhoneNumber());
            insertStatement.setString(2, myUser.getEmail());
            insertStatement.setString(3, myUser.getName());
            insertStatement.setString(4, myUser.getUserType());
            insertStatement.setString(5, myUser.getJoinDate());
            insertStatement.executeQuery();
            //insertStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @SuppressLint({"NewApi", "AuthLeak"})
    public Connection connectionclass() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection con = null;
        String ConnectionURL = null;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            ConnectionURL = "url";
            con = DriverManager.getConnection(ConnectionURL);
        } catch (SQLException se) {
            Log.e("SQLEXCEPTION: ", se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("CLASSNOEXCEPTION: ", e.getMessage());
        } catch (Exception e) {
            Log.e("EXCEPTION: ", e.getMessage());
        }
        return con;
    }

    //*********************************************** THEMING THE SNACKBAR ***********************************************
    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(VerifyLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    //*********************************************** CLOSES THE KEYBOARD ***********************************************
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //*********************************************** SHOW A TOAST THAT LOOKS LIKE THE SNACKBAR FOR WHEN THE SNACKBAR DOESN'T WORK ***********************************************
    public void showToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.custom_toast_layout));
        TextView tv = layout.findViewById(R.id.txtvw);
        tv.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    //*********************************************** LOAD FIREBASE USER ON START ***********************************************
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {

    }

    //*********************************************** RELOAD NIGHT MODE/DAY MODE ***********************************************
    @Override
    protected void onResume() {
        super.onResume();

        boolean isDMOn = new Prefs(this).getBoolean("isDarkModeOn", false);

        if (isDMOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}