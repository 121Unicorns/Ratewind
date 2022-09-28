package com.ratewind;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hbb20.CountryCodePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.Serializable;

public class RegisterActivity extends AppCompatActivity {

    private TextView tvSignup;
    private String phoneNumber, phone, phoneStore, accountType, name, email, joinDate;
    private CountryCodePicker ccp;
    private EditText etPhone, etCode, etName, etEmail;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private RadioButton rbTenant, rbLandlord;
    private ConstraintLayout regLayout;
    private LinearLayout llSignin;
    private CheckBox chkAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (user != null){
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }

        initViews();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date mydate = new Date();
        joinDate = formatter.format(mydate);

        llSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });

        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard();
                name = etName.getText().toString();
                email = etEmail.getText().toString();
                phone = etPhone.getText().toString();

                if (rbLandlord.isChecked()) {
                    accountType = "Owner";
                } else if (rbTenant.isChecked()) {
                    accountType = "Tenant";
                }

                if (!chkAgree.isChecked()){
                    chkAgree.requestFocus();
                    showSnackbar("You must agree to the Terms and Conditions of use to continue!");
                } else if (name.length() <4){
                    etName.requestFocus();
                    showSnackbar("Please enter a valid name!");
                } else if (email.isEmpty()){
                    etEmail.requestFocus();
                    showSnackbar("Please enter a valid email address!");
                } else if (phone.length() <9 ||phone.length() >10 ){
                    etPhone.requestFocus();
                    showSnackbar("Please enter a valid phone number!");
                } else {
                    ccp.registerCarrierNumberEditText(etPhone);
                    phoneNumber = ccp.getFullNumberWithPlus();
                    phoneStore = ccp.getFullNumber();

                    mDatabase.child("users").orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                showSnackbar("That phone number is already in use! Try a different one.");
                            } else {
                                mDatabase.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dSnapshot) {
                                        if (dSnapshot.exists()) {
                                            showSnackbar("That email address is already in use! Try a different one.");
                                        } else {
                                            Prefs prefs = new Prefs(getApplicationContext());
                                            prefs.setString("Name", name);
                                            prefs.setString("userType", accountType);
                                            prefs.setString("intentType", "Register");

                                            User myUser = new User (phoneNumber, email, name, accountType, joinDate);
                                            Intent intent = new Intent(RegisterActivity.this, VerifyActivity.class);
                                            Bundle args = new Bundle();
                                            args.putSerializable("User", (Serializable)myUser);
                                            intent.putExtra("BUNDLE", args);
                                            //intent.putExtra("Name", name);
                                            //intent.putExtra("Email", email);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        });
    }

    private void initViews(){
        tvSignup = findViewById(R.id.tv_signup);
        ccp = findViewById(R.id.ccp_code);
        etPhone = findViewById(R.id.et_phone);
        etCode = findViewById(R.id.et_code);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        rbTenant = findViewById(R.id.rb_tenant);
        rbLandlord = findViewById(R.id.rb_landlord);
        regLayout = findViewById(R.id.registerlayout);
        chkAgree = findViewById(R.id.chk_agree);
        llSignin = findViewById(R.id.ll_signin);
    }

    private void showSnackbar(String message){
        Snackbar snackbar = Snackbar.make(regLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    private void closeKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}