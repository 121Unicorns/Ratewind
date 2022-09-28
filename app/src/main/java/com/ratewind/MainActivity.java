package com.ratewind;

import static android.content.ContentValues.TAG;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private GridView gvDashboard;
    private GridAdapter gridAdapter;
    private TextView tvProfName;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private String displayName = "";
    private User myUser = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        user.reload();

        if (user == null || user.isAnonymous()) {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            finish();
        }

        user.reload();
        if (user != null) {
            do {
                user.reload();
                displayName = user.getDisplayName();
            } while (displayName==null);

            displayName = displayName.trim();
            displayName = displayName.replaceAll("\\s.*", "");
            tvProfName.setText(displayName);
        }

        gridAdapter = new GridAdapter(this);
        gvDashboard.setAdapter(gridAdapter);
        gvDashboard.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //"Search", "My Ratings", "Settings", "New Ratings", "Chat", "Get Help"
                String choice = gridAdapter.getItem(i);
                if (choice.equals("Search")) {
                    startActivity(new Intent(MainActivity.this, SearchActivity.class));
                } else if (choice.equals("My Ratings")) {
                    startActivity(new Intent(MainActivity.this, RatingsActivity.class));
                } else if (choice.equals("Settings")) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                } else if (choice.equals("New Ratings")) {
                    startActivity(new Intent(MainActivity.this, NewratingActivity.class));
                } else if (choice.equals("Chat")) {
                    startActivity(new Intent(MainActivity.this, InboxActivity.class));
                } /*else if (choice.equals("Get Help")) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }*/ else {
                    String selected = gridAdapter.getItem(i);
                    Toast.makeText(MainActivity.this, selected, Toast.LENGTH_LONG).show();
                }
            }
        });

        mDatabase.child("users").orderByChild("phoneNumber").equalTo(user.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Gson gson = new Gson();
                        String s1 = gson.toJson(snapshot.getValue());
                        JSONObject jsonObject = new JSONObject(s1);
                        JSONArray jsonArray = jsonObject.toJSONArray(jsonObject.names());

                        for (int i = 0; i<jsonArray.length(); i++){
                            JSONObject jsonContent = (JSONObject) jsonArray.get(i);
                            String userType = jsonContent.getString("userType");

                            Prefs prefs = new Prefs(getApplicationContext());
                            prefs.setString("userType", userType);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initViews() {
        gvDashboard = findViewById(R.id.gv_dashboard);
        tvProfName = findViewById(R.id.tv_profname);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Prefs prefs = new Prefs(getApplicationContext());
        boolean isDMOn = prefs.getBoolean("isDarkModeOn", false);

        if (isDMOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        user.reload();
        if (user != null) {
            do {
                user.reload();
                displayName = user.getDisplayName();
            } while (displayName==null);

            displayName = displayName.trim();
            displayName = displayName.replaceAll("\\s.*", "");
            tvProfName.setText(displayName);
        }
    }
}
