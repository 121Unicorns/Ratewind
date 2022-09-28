package com.ratewind;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ReceivedFragment extends Fragment implements RatingAdapter.ListItemClickListener {
    private RecyclerView rvReceived;
    private final ArrayList<Review> reviewList = new ArrayList<Review>();
    private RatingAdapter ratingAdapter;
    private ProgressBar progressBar;
    private TextView tvName, tvRating;
    private String userType;
    private Connection connection;
    private RatingBar ratingBar;
    private Review review;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_received, container, false);

        rvReceived = view.findViewById(R.id.rv_receive);
        tvName = view.findViewById(R.id.tv_name);
        tvRating = view.findViewById(R.id.tv_rating);
        ratingBar = view.findViewById(R.id.rb_rating);
        progressBar = view.findViewById(R.id.review_progress);

        userType = new Prefs(getContext()).getString("userType", "");

        GetReviews getReviews = new GetReviews();
        getReviews.execute();

        //*************************************************DECLARE THE LIST ADAPTER FOR THE REVIEWS OF A SELECTED USER*************************************************
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());//Parameter: context, number of columns
        rvReceived.setLayoutManager(layoutManager);
        ratingAdapter = new RatingAdapter(reviewList, this);
        rvReceived.setAdapter(ratingAdapter);

        return view;
    }

    @Override
    public void onListItemClick(int position) {

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
        String query = "";
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
                tvName.setText(queryName);
                ratingAdapter.notifyDataSetChanged();
            } else {
                progressBar.setVisibility(View.GONE);
                tvRating.setText("0.0");
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            if (userType.equals("Tenant")) {
                query = "SELECT * FROM tenant_ratings WHERE phoneNumber LIKE '%" + user.getPhoneNumber() + "%'";
            } else if (userType.equals("Owner")) {
                query = "SELECT * FROM owner_ratings WHERE phoneNumber LIKE '%" + user.getPhoneNumber() + "%'";
            }

            try {
                do {
                    connection = connectionclass();
                } while (connection == null);

                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(query);

                if (!rs.next()) {
                    reviewList.clear();
                    z = "There are no reviews for this user.";
                    isSuccess = false;
                } else {
                    reviewList.clear();
                    do {
                        queryName = rs.getString("name");
                        queryPhone = rs.getString("phoneNumber");
                        queryRatedBy = rs.getString("ratedBy");
                        queryRatedName = rs.getString("ratedName");
                        queryReview = rs.getString("textReview");
                        queryRating = rs.getFloat("rating");
                        queryRatingDate = rs.getString("ratingDate");
                        queryDate = rs.getString("ratingDate");
                        queryType = rs.getString("userType");

                        review = new Review(queryName, queryPhone, queryRatedBy, queryRatedName, queryReview, queryRating, queryDate, queryType);
                        reviewList.add(review);
                    }
                    while (rs.next());
                    //z = arrayList.size() + " results found.";
                    sum = 0.0f;
                    for (int i = 0; i < reviewList.size(); i++) {
                        sum = sum + reviewList.get(i).getRating();
                    }
                    average = sum / reviewList.size();

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
}