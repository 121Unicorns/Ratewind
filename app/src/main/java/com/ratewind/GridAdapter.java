package com.ratewind;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridAdapter extends BaseAdapter  implements AdapterView.OnItemClickListener{

    private Context context;
    public Integer[] cardImgs = {
            R.drawable.search,
            R.drawable.ratings,
            R.drawable.gear,
            //R.drawable.rate2,
            R.drawable.chat,
            R.drawable.help
    };

    public String[] cardTitles = {
            "Search", "My Ratings", "Settings", "Chat", "Get Help"
    };

    //"Search", "My Ratings", "Settings", "New Ratings", "Chat", "Get Help"

    public GridAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return cardImgs.length;
    }

    @Override
    public String getItem(int i) {
        return cardTitles[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.card_layout, viewGroup, false);
        }
        TextView tvTitle;
        ImageView ivImage;
        tvTitle = (TextView) view.findViewById(R.id.card_title);
        ivImage = (ImageView) view.findViewById(R.id.card_image);

        tvTitle.setText(String.valueOf(cardTitles[i]));
        ivImage.setImageResource(cardImgs[i]);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
