package com.ratewind;

import static android.content.ContentValues.TAG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private TextView tvName, tvType;
    private String displayName, userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tvName = findViewById(R.id.tv_profname);
        tvType = findViewById(R.id.tv_type);

        userType = new Prefs(this).getString("userType", "");

        tvType.setText("You are logged in as " + userType);

        displayName = user.getDisplayName();

        if (displayName != null){
            displayName = displayName.trim();
            displayName = displayName.replaceAll("\\s.*", "");
            tvName.setText(displayName);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private FirebaseAuth auth = FirebaseAuth.getInstance();
        private FirebaseUser user = auth.getCurrentUser();
        private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        private String phoneNo, name, email, accountType;
        private Connection connection;
        SettingsActivity activity;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            user.reload();
            phoneNo = user.getPhoneNumber();

            //*******************************************************LINK PREFERENCES*******************************************************
            final EditTextPreference etname = findPreference("name");
            final EditTextPreference etemail = findPreference("email");
            Preference btnsave = findPreference("save");
            Preference btnlogout = findPreference("signout");
            Preference btndelete = findPreference("delete");
            final SwitchPreference btntheme = findPreference("theme");

            //*********************************************************TO SELECT THEME***********************************************************
            Prefs prefs = new Prefs(getContext());

            btntheme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (btntheme.isChecked()) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        prefs.setBoolean("isDarkModeOn", true);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        prefs.setBoolean("isDarkModeOn", false);
                    }
                    return false;
                }
            });

            //*********************************************************TO CHANGE NAME***********************************************************
            etname.setText(user.getDisplayName());
            etemail.setText(user.getEmail());

            //************************************************************TO SIGN OUT************************************************************
            btnlogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    auth.signOut();
                    showToast("You are now signed out");
                    //Snackbar.make(getActivity().findViewById(android.R.id.content),"You are now signed out", Snackbar.LENGTH_LONG).show();
                    Intent intent = new Intent(getContext(), RegisterActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    getActivity().finish();
                    return true;
                }
            });

            //*********************************************************TO UPDATE PROFILE***********************************************************
            btnsave.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    name = etname.getText().toString();
                    email = etemail.getText().toString();

                    if (name.length() <3){
                        showToast("Please enter a valid name!");
                    } else if (email.isEmpty()){
                        showToast("Please enter a valid email address!");
                    } else {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateEmail(email);
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");
                                            showToast("User profile updated.");
                                        }
                                    }
                                });
                        user.reload();
                        mDatabase.child("users").child(phoneNo).child("email").setValue(email);
                        mDatabase.child("users").child(phoneNo).child("name").setValue(name);
                        updateDB();
                        updateFS(phoneNo, email, name);
                    }
                    return true;
                }
            });

            //*********************************************************TO DELETE ACCOUNT***********************************************************
            btndelete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MaterialAlertDialogBuilder madBuilder = new MaterialAlertDialogBuilder(getActivity(), R.style.ThemeOverlay_App_MaterialAlertDialog);
                    madBuilder.setTitle("DELETE YOUR ACCOUNT").setMessage("Are you sure you want to delete your account? This cannot be undone.").setCancelable(true);
                    madBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            return;
                        }
                    });
                    madBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User account deleted.");
                                                showToast("Your account was successfully deleted. We're sorry to see you go.");
                                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                                getActivity().finish();
                                            }
                                        }
                                    });
                        }
                    });

                    AlertDialog alertDialog = madBuilder.create();
                    alertDialog.show();
                    return true;
                }
            });
        }

        public void showToast(String message){
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) getView().findViewById(R.id.custom_toast_layout));
            TextView tv = (TextView) layout.findViewById(R.id.txtvw);
            tv.setText(message);
            Toast toast = new Toast(getContext());
            toast.setGravity(Gravity.BOTTOM| Gravity.FILL_HORIZONTAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        }

        //***************************************************** UPDATE FIRESTORE DATABASE *************************************************************
        private void updateFS(String tempPhone, String tempEmail, String tempName){
            DocumentReference updateRef = FirebaseFirestore.getInstance().collection("users").document(tempPhone);
            updateRef
                    .update("email", tempEmail,
                            "name", tempName)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }

        //***************************************************** UPDATE SQL DATABASE *************************************************************
        private void updateDB(){
            try {
                do {
                    connection = connectionclass();
                } while (connection==null);

                String acType;
                user.reload();

                acType = new Prefs(getContext()).getString("userType", "");

                String query = "";

                if (acType.equals("Tenant")){
                    query = "UPDATE tenant_users SET email = '" + email + "', name = '" + name + "' WHERE phoneNumber = '" +  phoneNo + "';" +
                            "UPDATE owner_ratings SET ratedName = '" + name + "' WHERE ratedBy = '" +  phoneNo + "';" +
                            "UPDATE tenant_ratings SET name = '" + name + "' WHERE phoneNumber = '" +  phoneNo + "';";
                } else if (acType.equals("Owner")){
                    query = "UPDATE owner_users SET email = '" + email + "', name = '" + name + "' WHERE phoneNumber = '" +  phoneNo + "';" +
                            "UPDATE tenant_ratings SET ratedName = '" + name + "' WHERE ratedBy = '" +  phoneNo + "';" +
                            "UPDATE owner_ratings SET name = '" + name + "' WHERE phoneNumber = '" +  phoneNo + "';";
                }

                PreparedStatement insertStatement = connection.prepareStatement(query);
                insertStatement.executeUpdate();

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        @SuppressLint({"NewApi"})
        public Connection connectionclass() {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Connection con = null;
            String ConnectionURL = null;

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                ConnectionURL = "jdbc:jtds:sqlserver://ratewind.database.windows.net:1433;DatabaseName=ratewind;user=ratewind_admin@ratewind;password=Aobcd8663!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;ssl=request";
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        user.reload();

        boolean isDMOn = new Prefs(this).getBoolean("isDarkModeOn", false);

        if (isDMOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}