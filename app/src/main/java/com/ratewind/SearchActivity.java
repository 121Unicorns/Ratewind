package com.ratewind;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity implements RatingAdapter.ListItemClickListener {
    private SearchView searchView;
    private CardView cardView;
    private ListView searchList;
    private ProgressBar progressBar;
    private RatingBar ratingBar;
    private RecyclerView recyclerView;
    private TextView tvName, tvRating, tvNoRating, tvNoUser;
    private ImageView ivBack, ivDown;
    private final ArrayList<Review> myReviews = new ArrayList<Review>();
    private final ArrayList<Review> reviewList = new ArrayList<Review>();
    private ConstraintLayout searchLayout;
    private ListAdapter adapter;
    private RatingAdapter ratingAdapter;
    private String userType, textQuery, selectedUserPhone;
    private Connection connection;
    private Review review, myReview, selectReview;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initViews();

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SearchActivity.this, MainActivity.class));
                finish();
            }
        });

        //*************************************************THE CURRENT USERS' ACCOUNT TYPE*************************************************
        userType = new Prefs(this).getString("userType", "");

        //*************************************************DECLARE THE LIST ADAPTER FOR SEARCH RESULTS*************************************************
        adapter = new ListAdapter(this, myReviews);
        searchList.setAdapter(adapter);

        //*************************************************DECLARE THE LIST ADAPTER FOR THE REVIEWS OF A SELECTED USER*************************************************
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);//Parameter: context, number of columns
        recyclerView.setLayoutManager(layoutManager);
        ratingAdapter = new RatingAdapter(reviewList, this);
        recyclerView.setAdapter(ratingAdapter);

        //************************************************* SEARCHVIEW COMMANDS *************************************************
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                closeKeyboard();
                searchList.setVisibility(View.VISIBLE);

                textQuery = query;
                if (textQuery.length() > 9) {
                    textQuery = query.substring(query.length() - 9);
                }

                progressBar.setVisibility(View.VISIBLE);
                CheckDB checkDB = new CheckDB();
                checkDB.execute();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                cardView.setVisibility(View.GONE);
                tvNoRating.setVisibility(View.GONE);
                tvNoUser.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                searchList.setVisibility(View.GONE);
                return false;
            }
        });

        //*************************************************TO DISPLAY LIST OF REVIEWS WHEN A SEARCH RECORD IS CLICKED*************************************************
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectReview = myReviews.get(i);
                selectedUserPhone = selectReview.getPhoneNumber();
                tvName.setText(selectReview.getName());
                tvRating.setText("");
                cardView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.bringToFront();
                recyclerView.setVisibility(View.VISIBLE);

                GetReviews getReviews = new GetReviews();
                getReviews.execute();
            }
        });

        //*************************************************PROMPT FOR A NEW REVIEW*************************************************
        ivDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlert(selectReview);
            }
        });
    }

    private void initViews() {
        searchList = findViewById(R.id.lv_results);
        searchView = findViewById(R.id.searchview);
        searchLayout = findViewById(R.id.searchlayout);
        progressBar = findViewById(R.id.search_progress);
        ratingBar = findViewById(R.id.rb_rating);
        tvName = findViewById(R.id.tv_name);
        tvRating = findViewById(R.id.tv_rating);
        tvNoRating = findViewById(R.id.tv_noRating);
        tvNoUser = findViewById(R.id.tv_noUser);
        cardView = findViewById(R.id.cv_rating);
        recyclerView = findViewById(R.id.rv_ratings);
        ivBack = findViewById(R.id.iv_back);
        ivDown = findViewById(R.id.iv_down);
    }

    public void onListItemClick(int position) {
        Review chatReview = reviewList.get(position);
        String chatPhone = chatReview.getRatedBy();
        String chatName = chatReview.getRatedName();

        if (user.getPhoneNumber().equals(chatPhone)) {
            showSnackbar("You cannot chat with yourself!");
        } else {
            new MaterialAlertDialogBuilder(SearchActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle("Start Chat")
                    .setMessage("Would you like to chat with " + chatName + "?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent chatIntent = new Intent(SearchActivity.this, ChatActivity.class);
                            chatIntent.putExtra("chatName", chatName);
                            chatIntent.putExtra("chatPhone", chatPhone);
                            startActivity(chatIntent);
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
        }
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(searchLayout, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700));
        TextView textView = (TextView) sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        snackbar.show();
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //FOR A USER WHO DOESN'T EXIST
    private void showNewAlert(String newReview) {
        new MaterialAlertDialogBuilder(SearchActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Create new rating")
                .setMessage("It seems this user doesn't exist. Would you like to create a new rating?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(SearchActivity.this, NewratingActivity.class);
                        intent.putExtra("newReview", newReview);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Prefs prefs = new Prefs(getApplicationContext());
        boolean isDMOn = prefs.getBoolean("isDarkModeOn", false);

        if (isDMOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    //NEW REVIEW FOR EXISTING USERS
    private void showAlert(Review review) {
        new MaterialAlertDialogBuilder(SearchActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Create new rating")
                .setMessage("Would you like to create a new rating?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(SearchActivity.this, NewratingActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("myReview", review);
                        //showSnackbar(review.getName());
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    //*************************************************SEARCH DATABASE FOR QUERY*************************************************
    public class CheckDB extends AsyncTask<String, String, String> {
        String z = "";
        Boolean isSuccess = false;
        String queryName = "";
        String queryPhone = "";

        String query = "";

        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String r) {
            showSnackbar(r);
            if (isSuccess) {
                progressBar.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                tvNoUser.setVisibility(View.VISIBLE);
                //tvNoUser.setText("No users found!");
                if (textQuery.length() >= 9) {
                    GetLostReviews getLostReviews = new GetLostReviews();
                    getLostReviews.execute();
                }
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            if (userType.equals("Tenant")) {
                query = "SELECT DISTINCT phoneNumber, name FROM owner_ratings WHERE phoneNumber LIKE '%" + textQuery + "%'";
                //query = "SELECT DISTINCT * FROM (SELECT * FROM owner_ratings WHERE phoneNumber LIKE '%" + textQuery + "%');";
            } else if (userType.equals("Owner")) {
                query = "SELECT DISTINCT phoneNumber, name FROM tenant_ratings WHERE phoneNumber LIKE '%" + textQuery + "%'";
                //query = "SELECT DISTINCT * FROM (SELECT * FROM tenant_ratings WHERE phoneNumber LIKE '%" + textQuery + "%');";
            }

            try {
                do {
                    connection = connectionclass();
                } while (connection == null);

                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(query);

                if (!rs.next()) {
                    myReviews.clear();
                    z = "No results found!";
                    isSuccess = false;
                } else {
                    myReviews.clear();
                    do {
                        queryName = rs.getString("name");
                        queryPhone = rs.getString("phoneNumber");

                        myReview = new Review();
                        myReview.setName(queryName);
                        myReview.setPhoneNumber(queryPhone);
                        myReviews.add(myReview);

                    }
                    while (rs.next());
                    z = myReviews.size() + " results found.";
                    isSuccess = true;
                    connection.close();
                }
            } catch (Exception e) {
                isSuccess = false;
                z = e.getMessage();
                Log.d("SQLERROR", z);
            }

            return z;
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

    //*************************************************GET REVIEWS FROM DATABASE*************************************************
    public class GetReviews extends AsyncTask<String, String, String> {
        String z = "";
        Boolean isSuccess = false;
        String queryPhone = "";
        String queryRatedBy = "";
        String queryRatedName = "";
        String queryReview = "";
        Float queryRating, average, sum;
        String queryRatingDate = "";
        String reviewQuery = "";
        String queryName = "";
        String queryDate = "";
        String queryType = "";

        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String r) {
            if (isSuccess) {
                progressBar.setVisibility(View.GONE);
                ratingBar.setRating(average);
                tvRating.setText(String.valueOf(average));
                ratingAdapter.notifyDataSetChanged();
            } else {
                tvNoRating.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                tvRating.setText("0.0");
                showAlert(selectReview);
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            if (userType.equals("Tenant")) {
                reviewQuery = "SELECT * FROM owner_ratings WHERE phoneNumber LIKE '%" + selectedUserPhone + "%'";
            } else if (userType.equals("Owner")) {
                reviewQuery = "SELECT * FROM tenant_ratings WHERE phoneNumber LIKE '%" + selectedUserPhone + "%'";
            }

            try {
                do {
                    connection = connectionclass();
                    z = "Check your internet connection";
                } while (connection == null);

                //connection = connectionclass();
                //if (connection == null) {
                //    z = "Check your internet connection";
                //} else {
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(reviewQuery);

                if (!rs.next()) {
                    reviewList.clear();
                    z = "There are no reviews for this user.";
                    isSuccess = false;
                } else {
                    reviewList.clear();
                    do {
                        queryPhone = rs.getString("phoneNumber");
                        queryRatedBy = rs.getString("ratedBy");
                        queryRatedName = rs.getString("ratedName");
                        queryReview = rs.getString("textReview");
                        queryRating = rs.getFloat("rating");
                        queryRatingDate = rs.getString("ratingDate");
                        queryDate = rs.getString("ratingDate");
                        queryType = rs.getString("userType");

                        review = new Review(selectReview.getName(), queryPhone, queryRatedBy, queryRatedName, queryReview, queryRating, queryDate, queryType);
                        reviewList.add(review);
                    }
                    while (rs.next());
                    //z = arrayList.size() + " results found.";
                    sum = 0.0f;
                    for (int i = 0; i < reviewList.size(); i++) {
                        sum = sum + reviewList.get(i).getRating();
                    }
                    average = sum / reviewList.size();

                    //ratingAdapter.notify();
                    isSuccess = true;
                    connection.close();
                }

                //}
            } catch (Exception e) {
                isSuccess = false;
                z = e.getMessage();
                Log.d("SQLERROR", z);
            }
            return z;
        }
    }

    //*************************************************GET REVIEWS FROM DATABASE*************************************************
    public class GetLostReviews extends AsyncTask<String, String, String> {
        String z = "";
        Boolean isSuccess = false;
        String queryPhone = "";
        String queryRatedBy = "";
        String queryRatedName = "";
        String queryReview = "";
        Float queryRating, average, sum;
        String queryRatingDate = "";
        String reviewQuery = "";
        String queryName = "";
        String queryDate = "";
        String queryType = "";

        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String r) {
            if (isSuccess) {
                progressBar.setVisibility(View.GONE);
                ratingBar.setRating(average);
                tvRating.setText(String.valueOf(average));
                ratingAdapter.notifyDataSetChanged();
            } else {
                tvNoRating.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                String extracted = textQuery.substring(textQuery.length() - 9);
                showNewAlert(extracted);
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            if (userType.equals("Tenant")) {
                reviewQuery = "SELECT * FROM owner_ratings WHERE phoneNumber LIKE '%" + textQuery + "%'";
            } else if (userType.equals("Owner")) {
                reviewQuery = "SELECT * FROM tenant_ratings WHERE phoneNumber LIKE '%" + textQuery + "%'";
            }

            try {
                do {
                    connection = connectionclass();
                    z = "Check your internet connection";
                } while (connection == null);

                //connection = connectionclass();
                //if (connection == null) {
                //    z = "Check your internet connection";
                //} else {
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(reviewQuery);

                if (!rs.next()) {
                    reviewList.clear();
                    z = "There are no reviews for this user.";
                    isSuccess = false;
                } else {
                    reviewList.clear();
                    do {
                        queryPhone = rs.getString("phoneNumber");
                        queryRatedBy = rs.getString("ratedBy");
                        queryRatedName = rs.getString("ratedName");
                        queryReview = rs.getString("textReview");
                        queryRating = rs.getFloat("rating");
                        queryRatingDate = rs.getString("ratingDate");
                        queryDate = rs.getString("ratingDate");
                        queryType = rs.getString("userType");

                        review = new Review(selectReview.getName(), queryPhone, queryRatedBy, queryRatedName, queryReview, queryRating, queryDate, queryType);
                        reviewList.add(review);
                    }
                    while (rs.next());
                    //z = arrayList.size() + " results found.";
                    sum = 0.0f;
                    for (int i = 0; i < reviewList.size(); i++) {
                        sum = sum + reviewList.get(i).getRating();
                    }
                    average = sum / reviewList.size();

                    //ratingAdapter.notify();
                    isSuccess = true;
                    connection.close();
                }

                //}
            } catch (Exception e) {
                isSuccess = false;
                z = e.getMessage();
                Log.d("SQLERROR", z);
            }
            return z;
        }
    }
}