package com.googolmo.fs.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import com.googolmo.fs.R;
import com.googolmo.fs.utils.AsyncImageLoader;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import fi.foyt.foursquare.api.entities.Photo;

import java.util.List;

/**
 * User: googolmo
 * Date: 12-5-30
 * Time: 下午4:49
 */
public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private List mList;
    private LayoutInflater mInflater;

    public ImageAdapter(Context context, List list) {
        this.mContext = context;
        this.mList = list;
        mInflater = LayoutInflater.from(this.mContext);
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public List getList() {
        return mList;
    }

    public void setList(List list) {
        this.mList = list;
    }

    @Override
    public int getCount() {
        return this.mList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.photo, null);
            holder = new ViewHolder();
            holder.photo = (ImageView)convertView.findViewById(R.id.photo_imageview);
            convertView.setTag(holder);
//            holder.photo.setLayoutParams(new GridLayout.LayoutParams(80,80));
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        String url = ((Photo)mList.get(position)).getUrl();
        Log.d(ImageAdapter.class.getName(), url);
//        holder.photo.setLayoutParams(new GridLayout.LayoutParams());
        holder.photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        AsyncImageLoader.loadImage(this.mContext, url, holder.photo);
        convertView.setBackgroundColor(Color.GRAY);


        return convertView;
    }

    static class ViewHolder {
        ImageView photo;
    }
}
