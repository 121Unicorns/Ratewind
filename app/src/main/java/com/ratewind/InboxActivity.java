package com.ratewind;

import static android.content.ContentValues.TAG;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.protobuf.Value;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class InboxActivity extends AppCompatActivity {
    private ConstraintLayout inboxLayout;
    private ListView lvConvos;
    private ImageView ivBack;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference messagesRef = db.collection("messages");
    private CollectionReference usersRef = db.collection("users");
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private ArrayList<User> messageUsers = new ArrayList<>();
    private ArrayList<String> messagePhones = new ArrayList<>();
    private InboxAdapter inboxAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        initViews();
        getMessages();

        inboxAdapter = new InboxAdapter(this, messageUsers);
        lvConvos.setAdapter(inboxAdapter);

        lvConvos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Snackbar.make(constraintLayout, "You have selected " + socials[position], BaseTransientBottomBar.LENGTH_LONG).show();
                //Toast.makeText(SecondActivity.this, "You have selected " + socials[position], Toast.LENGTH_LONG).show();
                //showToast("You have selected " + messageUsers.get(position).getPhoneNumber());
                String chatName = messageUsers.get(position).getName();
                String chatPhone = messageUsers.get(position).getPhoneNumber();
                Intent chatIntent = new Intent(InboxActivity.this, ChatActivity.class);

                chatIntent.putExtra("chatName", chatName);
                chatIntent.putExtra("chatPhone", chatPhone);
                startActivity(chatIntent);
            }
        });

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });
    }

    private void initViews() {
        inboxLayout = findViewById(R.id.inboxlayout);
        lvConvos = findViewById(R.id.lv_list);
        ivBack = findViewById(R.id.iv_back);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(inboxLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
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

        boolean isDMOn = new Prefs(this).getBoolean("isDarkModeOn", false);

        if (isDMOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    //********************************** GETTING THE MESSAGES IN ORDER OF DELIVERY DATE AND TIME FROM FIREBASE **************************
    private void getMessages() {
        messagePhones.clear();
        messagesRef.whereGreaterThanOrEqualTo("sender", Objects.requireNonNull(user.getPhoneNumber()))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getData().isEmpty()) {
                                    showSnackbar("You have no chats!");
                                }
                                //Log.d("YYYYYY", "=>" + document.getData());
                                Map<String, Object> map = document.getData();
                                JSONObject jsonObject = new JSONObject(map);
                                //Log.d("YYYYYY", jsonObject.toString());

                                try {
                                    String dbReceiver = jsonObject.getString("receiver");

                                    if (!messagePhones.contains(dbReceiver)) {
                                        messagePhones.add(dbReceiver);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

        messagesRef.whereGreaterThanOrEqualTo("receiver", user.getPhoneNumber())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getData().isEmpty()) {
                                    showSnackbar("You have no chats!");
                                }
                                //Log.d("YYYYYY", "=>" + document.getData());
                                Map<String, Object> map = document.getData();
                                JSONObject jsonObject = new JSONObject(map);
                                //Log.d("YYYYYY", jsonObject.toString());

                                try {
                                    String dbSender = jsonObject.getString("sender");

                                    if (!messagePhones.contains(dbSender)) {
                                        messagePhones.add(dbSender);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        getUsers(messagePhones);
                    }
                });
    }

    private void getUsers(ArrayList <String> msgPhones){
        messageUsers.clear();
        for (int i = 0; i<msgPhones.size(); i++){
            usersRef.whereEqualTo("phoneNumber", msgPhones.get(i))
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.getData().isEmpty()) {
                                        showSnackbar("You have no chats!");
                                    }
                                    //Log.d("YYYYYY", "=>" + document.getData());
                                    Map<String, Object> map = document.getData();
                                    JSONObject jsonObject = new JSONObject(map);
                                    //Log.d("YYYYYY", jsonObject.toString());

                                    try {
                                        String dbJoinDate = jsonObject.getString("joinDate");
                                        String dbEmail = jsonObject.getString("email");
                                        String dbName = jsonObject.getString("name");
                                        String dbPhoneNo = jsonObject.getString("phoneNumber");
                                        String dbUserType = jsonObject.getString("userType");
                                        messageUsers.add(new User(dbPhoneNo, dbEmail, dbName, dbUserType, dbJoinDate));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                inboxAdapter.notifyDataSetChanged();
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }
    }
}