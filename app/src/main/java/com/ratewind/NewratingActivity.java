package com.ratewind;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewratingActivity extends AppCompatActivity {
    private TextView tvSubmit;
    private EditText etName, etReview;
    private RadioButton rbTenant, rbLandlord;
    private RatingBar ratingBar;
    private String phoneNumber, name, ratedBy, ratedName, review, ratingDate, acType, selectedType;
    private ProgressBar pbSubmit;
    private Review newReview, myReview;
    private float rating;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private Connection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newrating);

        initViews();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date mydate = new Date();
        ratingDate = formatter.format(mydate);

        //Get the review from intent
        Bundle bundle = getIntent().getExtras();
        myReview = (Review) bundle.getSerializable("myReview");

        if (myReview!=null){
            try{
                name = myReview.getName();
                phoneNumber = myReview.getPhoneNumber();
                acType = myReview.getUserType();
                etName.setText(name);
                etName.setEnabled(false);

                if (myReview.getUserType().equals("Owner")) {
                    rbLandlord.setChecked(true);
                } else if (myReview.getUserType().equals("Tenant")) {
                    rbTenant.setChecked(true);
                }
                rbLandlord.setEnabled(false);
                rbTenant.setEnabled(false);
            } catch (NullPointerException e){
                Log.d("NPE", e.toString());
            }
        }

        tvSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myReview == null){
                    phoneNumber = getIntent().getStringExtra("newReview");
                }
                ratedBy = user.getPhoneNumber();
                ratedName = user.getDisplayName();
                review = etReview.getText().toString();
                rating = ratingBar.getRating();

                if (rbLandlord.isChecked()){
                    acType = "Owner";
                } else if (rbTenant.isChecked()){
                    acType = "Tenant";
                }

                if (review.length() < 5) {
                    showToast("Please type your review!");
                } else {
                    newReview = new Review(phoneNumber, name, ratedBy, ratedName, review, rating, ratingDate, acType);
                    updateDB();
                }
            }
        });

    }

    //***************************************************** UPDATE SQL DATABASE *************************************************************
    private void updateDB() {
        pbSubmit.setVisibility(View.VISIBLE);
        try {
            do {
                connection = connectionclass();
            } while (connection == null);

            String query = "";

            if (acType.equals("Tenant")) {
                query = "IF EXISTS (SELECT * FROM tenant_ratings WHERE (phoneNumber = '"+ phoneNumber +"') AND (ratedBy = '" + ratedBy + "')) " +
                        "BEGIN UPDATE tenant_ratings SET name = '" + name + "', ratedName = '" + ratedName + "', rating = '" + rating + "', " +
                        "textReview = '"+ review + "', ratingDate = '" + ratingDate + "' WHERE (phoneNumber = '" + phoneNumber + "') AND " +
                        "(ratedBy = '" + ratedBy + "'); " +
                        "END " +
                        "ELSE BEGIN INSERT INTO tenant_ratings (phoneNumber, name, ratedBy, ratedName, rating, textReview, ratingDate, userType) VALUES ('"
                        + phoneNumber + "', '" + name + "', '" + ratedBy+ "', '" + ratedName+ "', '" + rating + "', '" + review+ "', '" + ratingDate + "', '" + acType + "'); " +
                        "END;";
            } else if (acType.equals("Owner")) {
                query = "IF EXISTS (SELECT * FROM owner_ratings WHERE (phoneNumber = '"+ phoneNumber +"') AND (ratedBy = '" + ratedBy + "')) " +
                        "BEGIN UPDATE owner_ratings SET name = '" + name + "', ratedName = '" + ratedName + "', rating = '" + rating + "', " +
                        "textReview = '"+ review + "', ratingDate = '" + ratingDate + "' WHERE (phoneNumber = '" + phoneNumber + "') AND " +
                        "(ratedBy = '" + ratedBy + "'); " +
                        "END " +
                        "ELSE BEGIN INSERT INTO owner_ratings (phoneNumber, name, ratedBy, ratedName, rating, textReview, ratingDate, userType) VALUES ('"
                        + phoneNumber + "', '" + name + "', '" + ratedBy+ "', '" + ratedName+ "', '" + rating + "', '" + review+ "', '" + ratingDate + "', '" + acType + "'); " +
                        "END;";
            }

            PreparedStatement insertStatement = connection.prepareStatement(query);
            insertStatement.executeUpdate();

            pbSubmit.setVisibility(View.GONE);
            showToast("Review submitted successfully!");
            startActivity(new Intent(NewratingActivity.this, SearchActivity.class));
            finish();

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

    private void initViews() {
        tvSubmit = findViewById(R.id.tv_submit);
        etName = findViewById(R.id.et_name);
        rbTenant = findViewById(R.id.rb_tenant);
        rbLandlord = findViewById(R.id.rb_landlord);
        etReview = findViewById(R.id.et_review);
        ratingBar = findViewById(R.id.rb_rating);
        pbSubmit = findViewById(R.id.submit_progress);
    }

    public void showToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.custom_toast_layout));
        TextView tv = (TextView) layout.findViewById(R.id.txtvw);
        tv.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
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
    }
}