package com.ratewind;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.type.DateTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private TextView tvDisplayName;
    private RecyclerView rvChat;
    private FrameLayout flSend;
    private ConstraintLayout chatLayout;
    private EditText etMessage;
    private String chatName, chatNumber, sender, receiver, message;
    private ChatMessage chatMessage = new ChatMessage();
    private ArrayList<ChatMessage> chatList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference messagesRef = db.collection("messages");
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private LinearLayoutManager manager;
    private ChatAdapter adapter;
    private final int VIEW_TYPE_MESSAGE_SENT = 2;
    private final int VIEW_TYPE_MESSAGE_RECEIVED = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();

        chatName = getIntent().getStringExtra("chatName");
        chatNumber = getIntent().getStringExtra("chatPhone");

        getMessages();

        tvDisplayName.setText(chatName);

        flSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message = etMessage.getText().toString();

                if (message.isEmpty()) {
                    showSnackbar("Message cannot be empty!");
                } else {
                    chatMessage = new ChatMessage(sender, chatNumber, message, System.currentTimeMillis());
                    messagesRef.add(chatMessage).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                            etMessage.setText("");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
                }
                chatMessage.setType(VIEW_TYPE_MESSAGE_SENT);
                chatList.add(chatMessage);
                adapter.notifyItemInserted(chatList.size() - 1);
            }
        });

    }

    private void initViews() {
        tvDisplayName = findViewById(R.id.tv_displayname);
        rvChat = findViewById(R.id.rv_chat);
        flSend = findViewById(R.id.layoutSend);
        etMessage = findViewById(R.id.et_msg);
        chatLayout = findViewById(R.id.chatlayout);

        sender = user.getPhoneNumber();

        manager = new LinearLayoutManager(ChatActivity.this, RecyclerView.VERTICAL, false);
        //manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        adapter = new ChatAdapter(ChatActivity.this, chatList);
        rvChat.setLayoutManager(manager);
        rvChat.smoothScrollToPosition(0);
        rvChat.setAdapter(adapter);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(chatLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    //********************************** GETTING THE MESSAGES IN ORDER OF DELIVERY DATE AND TIME FROM FIREBASE **************************
    private void getMessages() {
        chatList.clear();
        messagesRef.orderBy("messageTime")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                ChatMessage chatMsg = new ChatMessage();
                                //Log.d("YYYYYY", "=>" + document.getData());
                                Map<String, Object> map = document.getData();
                                JSONObject jsonObject = new JSONObject(map);
                                //Log.d("YYYYYY", jsonObject.toString());

                                try {
                                    String dbSender = jsonObject.getString("sender");
                                    String dbReceiver = jsonObject.getString("receiver");
                                    String dbMessage = jsonObject.getString("message");
                                    Long dbTime = Long.parseLong(jsonObject.getString("messageTime"));

                                    chatMsg = new ChatMessage();
                                    chatMsg.setSender(dbSender);
                                    chatMsg.setReceiver(dbReceiver);
                                    chatMsg.setMessage(dbMessage);
                                    chatMsg.setMessageTime(dbTime);

                                    if (dbSender.equals(user.getPhoneNumber())) {
                                        chatMsg.setType(VIEW_TYPE_MESSAGE_SENT);
                                    } else if (dbReceiver.equals(user.getPhoneNumber())) {
                                        chatMsg.setType(VIEW_TYPE_MESSAGE_RECEIVED);
                                    }

                                    if (dbSender.equals(chatNumber) && dbReceiver.equals(user.getPhoneNumber()) ||
                                            dbSender.equals(user.getPhoneNumber()) && dbReceiver.equals(chatNumber)) {
                                        chatList.add(chatMsg);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

        /*messagesRef.orderBy("messageTime").whereIn("sender", Arrays.asList(chatNumber, user.getPhoneNumber()))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChatMessage chatMsg = new ChatMessage();
                                //Log.d("YYYYYY", "=>" + document.getData());
                                Map<String, Object> map = document.getData();
                                JSONObject jsonObject =  new JSONObject(map);
                                //Log.d("YYYYYY", jsonObject.toString());

                                try {
                                    String dbSender = jsonObject.getString("sender");
                                    String dbReceiver = jsonObject.getString("receiver");
                                    String dbMessage = jsonObject.getString("message");
                                    Long dbTime = Long.parseLong(jsonObject.getString("messageTime"));

                                    chatMsg = new ChatMessage();
                                    chatMsg.setSender(dbSender);
                                    chatMsg.setReceiver(dbReceiver);
                                    chatMsg.setMessage(dbMessage);
                                    chatMsg.setMessageTime(dbTime);

                                    if (dbSender.equals(user.getPhoneNumber())){
                                        chatMsg.setType(VIEW_TYPE_MESSAGE_SENT);
                                    } else if (dbReceiver.equals(user.getPhoneNumber())){
                                        chatMsg.setType(VIEW_TYPE_MESSAGE_RECEIVED);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                chatList.add(chatMsg);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });*/
    }

}