package com.ratewind;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class ListAdapter extends ArrayAdapter<Review> implements View.OnClickListener{

    Context context;
    ArrayList<Review> reviews;

    private static class ViewHolder {
        TextView tvName;
        TextView tvPhone;
    }

    public ListAdapter(Context context, ArrayList<Review> reviews) {
        super(context, R.layout.list_item, reviews);
        this.reviews = reviews;
        this.context=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Review review = getItem(position);
        ViewHolder viewHolder;
        final View result;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            viewHolder.tvName = (TextView) convertView.findViewById(R.id.tv_listname);
            viewHolder.tvPhone = (TextView) convertView.findViewById(R.id.tv_listnumber);
            result=convertView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        viewHolder.tvName.setText(review.getName());
        viewHolder.tvPhone.setText(review.getPhoneNumber());

        return convertView;
    }

    @Override
    public void onClick(View view) {
        int position=(Integer) view.getTag();

    }
}
