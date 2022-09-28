package com.ratewind;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.ViewHolder> {
    Context context;
    private final List<Review> reviewList;
    private ListItemClickListener mOnClickListener;

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView cardView;
        //ImageView cardImage;
        TextView cardName;
        TextView cardRating;
        TextView cardReview;
        TextView cardDate;
        final private ListItemClickListener mOnClickListener;

        ViewHolder(View itemView, ListItemClickListener onClickListener) {
            super(itemView);
            cardView = (CardView) itemView;
            cardName = itemView.findViewById(R.id.reviewer_name);
            //cardImage = itemView.findViewById(R.id.reviewer_pic);
            cardRating = itemView.findViewById(R.id.reviewer_rating);
            cardReview = itemView.findViewById(R.id.review);
            cardDate = itemView.findViewById(R.id.review_date);
            mOnClickListener = onClickListener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            mOnClickListener.onListItemClick(position);
        }
    }

    public RatingAdapter(ArrayList <Review> reviewList, ListItemClickListener onClickListener) {
        this.reviewList = reviewList;
        this.mOnClickListener = onClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }

        View view = LayoutInflater.from(context).inflate(R.layout.review_card, parent, false);
        return new ViewHolder(view, mOnClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.cardName.setText(reviewList.get(position).getRatedName());
        //holder.cardImage.setImageResource(R.drawable.ic_people);
        holder.cardRating.setText(String.valueOf(reviewList.get(position).getRating()));
        holder.cardReview.setText(reviewList.get(position).getReview());
        holder.cardDate.setText(reviewList.get(position).getRatingDate());
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public interface ListItemClickListener{
        void onListItemClick(int position);
    }
}